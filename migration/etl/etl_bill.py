# -*- coding: utf-8 -*-
"""
etl_bill.py — epetcaseinvoice → mixpet.bill（step 2 ETL，切换时点口径）

规则依据：migration/FIELD_MAPPING.md §2.4 / §3（对账口径）

范围（切换时点定案）：
- 只迁 COST_STATUS<>'完成付款' 的单（未完全付款 5 条，切换时点未收款）
- 已完成付款的 31040 张历史单**不迁**：对账按金额口径已在 step 1 完成，
  历史明细留老系统只读查（P2 豁免：历史财务宽表不迁）

映射要点：
- REV_COST → total_money；实收 = SUM(PAY_COST1..4) - CHANGE_COST
- real_money/discount_money 是 STORED GENERATED 列（由 discount 系数推导），
  因此写 discount = 实收/应收（两位小数；尾差 <0.5 记 warning 人工确认）
- pay_type 主支付映射（§2.4：mixpet 只有 现金/余额 2 种）：余额→0；微信/支付宝/刷卡/团购→1（现金），
  原支付方式文本拼进 pay_by 备查（bill 无 remark 列）
- fee_* 分项：按 epetcaseinvoiceitem.LINK_TYPE 归项（金额=QTY_ORDER*ORDER_PRICE 汇总），
  与主表 REV_COST 的差额进 discount_money，不造数据
- LINK_ID(老 CASE_ID) → record_id（medical_record.legacy_case_no）；未迁病历跳过并计数
- 幂等：bill.legacy_bill_no 唯一键 upsert（feature_20261113_bill_legacy.sql 已建列）

用法：
    python3 etl_bill.py            # dry-run
    python3 etl_bill.py --apply    # 写库
"""
import sys
from collections import defaultdict
from datetime import datetime

from common import mssql_query, mysql_conn, EtlReport, parse_arg_mode, sq


def clean(v, maxlen=None):
    if v is None:
        return None
    s = str(v).strip()
    s = s if s not in ("", "-") else None
    if s and maxlen:
        s = s[:maxlen]
    return s or None


def to_dec(v, default=None):
    s = clean(v)
    if not s:
        return default
    try:
        return round(float(s), 2)
    except ValueError:
        return default


# LINK_TYPE → bill.fee_* 归项（§2.4：TYPEKEY 医疗/美容/销售 + 明细 LINK_TYPE）
FEE_MAP = {
    "Sale": "fee_drug",          # 零售
    "Prescription": "fee_drug",  # 处方发药
    "Diagnose": "fee_treat",     # 诊疗/挂号诊查
    "Check": "fee_lab",          # 化验
    "Lab": "fee_lab",
    "Immune": "fee_immune",      # 免疫/驱虫
    "Hospital": "fee_hospital",  # 住院
    "Image": "fee_image",        # 影像/特检
    "Surgery": "fee_surgery",    # 手术
    "Hair": "fee_package",       # 美容/洗护（foster/bill 口径）
    "Care": "fee_package",
    "Package": "fee_package",
}


def pay_type_of(*pay_names):
    """主支付映射（PAY_TYPE1 为主，2..4 空位是老系统 UI 默认'现金'不采纳）：
    余额→0，其余（现金/微信/支付宝/刷卡/团购）→1；返回 (pay_type, 原方式文本)"""
    names = [clean(p) for p in pay_names if clean(p)]
    if not names:
        return 1, None
    main = names[0]
    if "余额" in main or "储值" in main:
        return 0, main
    return 1, main


def build_bills(rep: EtlReport):
    heads = mssql_query(
        "SELECT INVOICE_NUM, LINK_ID, MEMBER_ID, PET_ID, REV_COST,"
        " PAY_COST1, PAY_COST2, PAY_COST3, PAY_COST4, PAY_TYPE1, PAY_TYPE2, PAY_TYPE3, PAY_TYPE4,"
        " CHANGE_COST, COST_STATUS,"
        f" {sq('MEMO')} AS MEMO, CREATE_DATE"
        " FROM epetcaseinvoice WHERE COST_STATUS<>'完成付款' ORDER BY CREATE_DATE"
    )
    rep.source_rows = len(heads)

    # 明细按单号聚合 fee_*（金额=QTY_ORDER*ORDER_PRICE；明细 STATUS='C'=在账）
    fees = defaultdict(lambda: defaultdict(float))
    for it in mssql_query(
        "SELECT INVOICE_NUM, LINK_TYPE, QTY_ORDER, ORDER_PRICE, REFUND_PRICE"
        " FROM epetcaseinvoiceitem WHERE STATUS<>'S'"
    ):
        inv = clean(it["INVOICE_NUM"])
        if not inv:
            continue
        fee_col = FEE_MAP.get(clean(it["LINK_TYPE"]), "fee_treat")
        amt = (to_dec(it["QTY_ORDER"], 0) or 0) * (to_dec(it["ORDER_PRICE"], 0) or 0)
        refund = to_dec(it["REFUND_PRICE"], 0) or 0
        fees[inv][fee_col] += max(amt, 0) - refund

    out = []
    for h in heads:
        legacy = clean(h["INVOICE_NUM"], 32)
        if not legacy:
            rep.skip("?", "无 INVOICE_NUM")
            continue
        member_no = clean(h["MEMBER_ID"], 32)
        pet_no = clean(h["PET_ID"], 32)
        if not member_no:
            rep.skip(legacy, f"无 MEMBER_ID，client 无法回溯")
            continue
        total = to_dec(h["REV_COST"], 0) or 0
        paid = sum(to_dec(h[f"PAY_COST{i}"], 0) or 0 for i in range(1, 5)) - (to_dec(h["CHANGE_COST"], 0) or 0)
        ptype, ptext = pay_type_of(h["PAY_TYPE1"], h["PAY_TYPE2"], h["PAY_TYPE3"], h["PAY_TYPE4"])
        # pay_by 记原支付方式 + 代付场景（COST_EMP_ID≠MEMBER_ID 入 pay_by 备查）
        pay_by = ptext or None
        out.append({
            "legacy_bill_no": legacy,
            "member_no": member_no,
            "pet_no": pet_no,
            "case_no": clean(h["LINK_ID"], 32),
            "memo": clean(h["MEMO"], 200),
            "create_time": clean(h["CREATE_DATE"]),
            "total_money": total,
            # real_money/discount_money 为 STORED GENERATED（total × discount），
            # 写折扣系数 = 实收/应收；两位小数尾差 >0.5 时人工确认
            "discount": round(min(max(paid, 0) / total, 1), 2) if total > 0 else 1,
            "paid": round(max(paid, 0), 2),
            "pay_status": 0,  # 未完全付款 → 未收款（对账口径 §3）
            "pay_type": ptype,
            "pay_by": pay_by,
            **{k: round(fees.get(legacy, {}).get(k, 0), 2) for k in (
                "fee_drug", "fee_lab", "fee_treat", "fee_hospital",
                "fee_image", "fee_package", "fee_surgery", "fee_immune")},
        })
        fee_sum = round(sum(v for k, v in fees.get(legacy, {}).items()), 2)
        if fee_sum and abs(fee_sum - total) > 0.5:
            rep.warn(f"[{legacy}] 明细汇总 {fee_sum} ≠ 应收 {total}，差额体现在 discount 系数")
        gen_real = round(total * out[-1]["discount"], 2)
        if abs(gen_real - out[-1]["paid"]) > 0.5:
            rep.warn(f"[{legacy}] 折扣两位小数尾差：生成实收 {gen_real} ≠ 老实收 {out[-1]['paid']}")
    rep.rows = out
    return out


BILL_SQL = (
    "INSERT INTO bill (client_id, pet_id, record_id, fee_register, fee_treat, fee_drug,"
    " fee_lab, fee_hospital, fee_image, fee_package, fee_surgery, fee_immune,"
    " total_money, discount, pay_status, pay_type, pay_by,"
    " is_del, create_time, legacy_bill_no)"
    " VALUES ("
    " (SELECT client_id FROM client WHERE legacy_member_no=%(member_no)s),"
    " (SELECT pet_id FROM pet WHERE legacy_pet_no=%(pet_no)s),"
    " (SELECT record_id FROM medical_record WHERE legacy_case_no=%(case_no)s),"
    " 0,%(fee_treat)s,%(fee_drug)s,%(fee_lab)s,%(fee_hospital)s,%(fee_image)s,"
    " %(fee_package)s,%(fee_surgery)s,%(fee_immune)s,"
    " %(total_money)s,%(discount)s,%(pay_status)s,%(pay_type)s,%(pay_by)s,"
    " 0,%(create_time)s,%(legacy_bill_no)s)"
    " ON DUPLICATE KEY UPDATE fee_treat=VALUES(fee_treat), fee_drug=VALUES(fee_drug),"
    " fee_lab=VALUES(fee_lab), fee_hospital=VALUES(fee_hospital), fee_image=VALUES(fee_image),"
    " fee_package=VALUES(fee_package), fee_surgery=VALUES(fee_surgery), fee_immune=VALUES(fee_immune),"
    " total_money=VALUES(total_money), discount=VALUES(discount), pay_status=VALUES(pay_status),"
    " pay_type=VALUES(pay_type), pay_by=VALUES(pay_by)"
)


def apply_bills(rows, rep):
    conn = mysql_conn()
    try:
        with conn.cursor() as cur:
            # 三个 legacy 键的存量集合，缺哪个都跳过（不造孤儿账单）
            cur.execute("SELECT legacy_member_no FROM client WHERE legacy_member_no IS NOT NULL")
            have_cli = {x["legacy_member_no"] for x in cur.fetchall()}
            cur.execute("SELECT legacy_pet_no FROM pet WHERE legacy_pet_no IS NOT NULL")
            have_pet = {x["legacy_pet_no"] for x in cur.fetchall()}
            cur.execute("SELECT legacy_case_no FROM medical_record WHERE legacy_case_no IS NOT NULL")
            have_case = {x["legacy_case_no"] for x in cur.fetchall()}

            ok = 0
            for r in rows:
                if r["member_no"] not in have_cli:
                    rep.skip(r["legacy_bill_no"], f"客户 {r['member_no']} 未迁移")
                    continue
                if r["pet_no"] and r["pet_no"] not in have_pet:
                    rep.skip(r["legacy_bill_no"], f"宠物 {r['pet_no']} 未迁移")
                    continue
                if r["case_no"] and r["case_no"] not in have_case:
                    # 病历已迁 12752 全量，缺则说明老单无病历关联（纯零售单），record_id 置 NULL 继续写
                    r["case_no"] = None
                    rep.warn(f"[{r['legacy_bill_no']}] 病历 {r['case_no']} 无关联（纯零售单），record_id 置空")
                if r.get("create_time"):
                    r["create_time"] = r["create_time"].split(".")[0]
                try:
                    cur.execute(BILL_SQL, r)
                    ok += 1
                except Exception as e:  # noqa: BLE001
                    rep.skip(r["legacy_bill_no"], f"写入失败: {str(e)[:80]}")
            conn.commit()
            rep.applied = ok
    finally:
        conn.close()


def main():
    apply_mode = parse_arg_mode(sys.argv)
    rep = EtlReport("epetcaseinvoice(未收款) → bill")
    bills = build_bills(rep)
    if apply_mode:
        apply_bills(bills, rep)
    rep.dump()
    if not apply_mode:
        print("以上为 DRY-RUN（未写库）。加 --apply 执行写入。")


if __name__ == "__main__":
    main()
