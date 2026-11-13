# -*- coding: utf-8 -*-
"""
etl_drug.py — epbmaster 商品主档 → mixpet.drug（step 2 ETL）

规则依据：migration/FIELD_MAPPING.md §2.7 / §2.7.1（P0-4 定案）

设计要点
- 只迁 IS_STOCK='Y' 且 STATUS='A'（在用库存商品，A 态 607 条；S 停用与 IS_STOCK=N
  服务项目不迁——lab_test/image_test 已覆盖服务项，挂号费走 bill.fee_register 口径）
- 库存以 estocks 为准（QTY_ON_HAND/QTY_MIN），epbmaster.QTY_ON_HAND 不作口径（§2.7 定案）
- 价格口径：STD_COST=售价 → drug_price；COST_PRICE=成本 → cost_price
  （STD_COST_A/B/C 是 ABC 三档会员价，不迁，mixpet 用"单价+折扣"替代）
- 效期 DATE_EXPIRATION → expire_date（4626 条批次效期的商品级主数据，近效期预警用）
- 品类：按 DESCRIPT 关键词分类（疫苗/驱虫/保健/外用/中成药/西药），非药零售商品默认
  用品（粮/零食/玩具/器械……），§2.7.1 "判不了 → 其他" 由报告列出人工核对
- ABC 档、剂量（dose_per_kg/dose_fixed）等不迁：老系统无该数据或需要兽医逐条确认
- 幂等：drug.legacy_part_no 唯一键 upsert（feature_20261113_drug_legacy.sql 已建列）
- 美容次卡商品（IS_HAIRING_CARD='Y'）→ count_card_template（name 判重）

用法：
    python3 etl_drug.py            # dry-run：只读 + 对账，不写库
    python3 etl_drug.py --apply    # 写库（可重复执行）
"""
import re
import sys
from datetime import datetime

from common import mssql_query, mysql_conn, EtlReport, parse_arg_mode, sq


# ---------------------------------------------------------------- 清洗工具
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


def to_int(v, default=None):
    d = to_dec(v)
    return int(d) if d is not None else default


def parse_date(v):
    s = clean(v)
    if not s:
        return None
    for fmt in ("%Y-%m-%d %H:%M:%S", "%Y-%m-%d"):
        try:
            return datetime.strptime(s[:len(fmt) + 4].rstrip(), fmt).date()
        except ValueError:
            continue
    # sqlcmd 默认输出 'Mar  3 2024 12:00AM' 之类，交给 MySQL 宽松解析兜底
    return s


# ---------------------------------------------------------------- 品类分类
# 主判：段码（A 态库存商品 108 段码逐段人工核样定案，2026-11-13）
#   093=疫苗苗 | 094/095=驱虫 | 036/138/139/134/135=保健品 | 042/112/043=外用
#   034/037/038/039/040=针片剂西药 | 其余段码=用品（粮/零食/玩具/器械/耗材）
SEG_TYPE = {
    "093": "疫苗", "094": "驱虫", "095": "驱虫",
    "036": "保健", "138": "保健", "139": "保健", "134": "保健", "135": "保健",
    "042": "外用", "112": "外用", "043": "外用",
    "034": "西药", "037": "西药", "038": "西药", "039": "西药", "040": "西药",
}

# 兜底：混合段码（035 口服药/专科制剂、041 洗剂/驱虫喷雾、044 消毒/耗材、045 麻醉剂/器械）
# 及关键词防漏（§2.7.1：判不了 → 用品/其他）
RULES = [
    ("疫苗", r"疫苗|DHPP|VACCIN|RABIS|FEL-O-VAX|NOBIVAC|PUPPY DP|联苗|狂犬|杯状大师"),
    ("驱虫", r"驱虫|DRONTAL|尼可信|NEXGARD|福来恩|FRONTLINE|CERTIFECT|爱沃克|ADVANTAGE|"
            r"大宠爱|REVOLUTION|博来恩|BRAVECTO|LANBOTO|朗莫特|朗达清|PRAZIQUANTEL|米尔贝肟|"
            r"吡虫啉|阿苯达唑|海乐旺"),
    ("保健", r"营养膏|维生素|VITAMIN|奶粉|MILK|发育宝|关节|氨基|软骨|益生菌|卵磷脂|"
            r"微量元素|补钙|钙片|化毛|美毛|鱼油|营养液|肾透|肾衰停|胰舒|复合酶|调理|抑情|"
            r"补血|贫血|肝功|免疫增强|微生态"),
    ("外用", r"香波|SHAMPOO|浴液|滴耳|滴眼|EYE\s?DROP|EAR\s?DROP|喷剂|消毒|酒精|碘伏|"
            r"药浴|洗液|外伤|软膏|ONIMENT|SCAR|喷雾|耳漂|耳可舒|酮康唑|洗耳|洁耳|眼舒|"
            r"敷料|抑菌液"),
    ("中成药", r"散$|丸$|口服液|颗粒剂|膏滋|板蓝根|双黄连|藿香|清瘟"),
    ("西药", r"\btab\b|\bcap\b|\binj\b|tablet|capsul|INJECTION|注射液|片$|胶囊|悬浮液|混悬液|"
            r"solution|syrup|糖浆|抗生素|头孢|CEPH|AMOXI|DOXY|METRO|IVERMECTIN|芬苯达唑|"
            r"氟尼辛|美洛昔康|MELOXICAM|托芬那酸|马波沙星|MARBOFLOX|恩诺沙星|ENROFLOX|"
            r"阿莫西林|爱波克|APOQUEL|昔布|罗硝唑|RONIDAZOLE|ISOFLURANE|麻醉|肾衰停|"
            r"透皮|止吐宁|止吐"),
]


def classify(seg: str, desc: str):
    # 混合段码与关键词兜底优先（段码粒度不够细的场景）
    if seg not in ("035", "041", "044", "045") and seg in SEG_TYPE:
        return SEG_TYPE[seg]
    d = desc or ""
    for label, pat in RULES:
        if re.search(pat, d, re.IGNORECASE):
            return label
    return SEG_TYPE.get(seg, "用品")  # §2.7.1：IS_STOCK=Y 非药零售商品默认用品


# ---------------------------------------------------------------- 源数据
def build_drugs(rep: EtlReport):
    rows = mssql_query(
        "SELECT PART_NUM,"
        f" {sq('DESCRIPT')} AS DESCRIPT, {sq('SPEC')} AS SPEC, UOM, {sq('PACKAGE')} AS PACKAGE,"
        f" {sq('MANUFACTURER')} AS MANUFACTURER, STD_COST, COST_PRICE,"
        f" {sq('ATTENTION')} AS ATTENTION, {sq('APPLICATION')} AS APPLICATION,"
        f" {sq('BARCODE')} AS BARCODE, DATE_EXPIRATION, WARR_MN,"
        " IS_HAIRING_CARD, CARD_TIMES"
        " FROM epbmaster WHERE IS_STOCK='Y' AND STATUS='A' ORDER BY PART_NUM"
    )
    rep.source_rows = len(rows)

    # 库存以 estocks 为准（§2.7 定案）；按 PART_NUM 聚合防多仓行
    stock = {}
    for s in mssql_query(
        "SELECT PART_NUM, SUM(QTY_ON_HAND) qty, MAX(QTY_MIN) qmin FROM estocks GROUP BY PART_NUM"
    ):
        stock[clean(s["PART_NUM"])] = (to_int(s["qty"], 0), to_int(s["qmin"], 0))

    out, other_sample = [], []
    for r in rows:
        legacy = clean(r["PART_NUM"], 32)
        if not legacy:
            rep.skip("?", "无 PART_NUM，无法建立追溯键")
            continue
        name = clean(r["DESCRIPT"], 64)
        if not name:
            rep.skip(legacy, "无 DESCRIPT（商品名），mixpet drug_name 非空")
            continue
        dtype = classify(legacy[:3], name)
        if dtype == "用品":
            other_sample.append(name)
        # drug_info：老系统放不下的说明性文本统一拼备注（ATTENTION 注意事项优先）
        info = [clean(r["ATTENTION"], 180), clean(r["APPLICATION"], 60)]
        qty, qmin = stock.get(legacy, (None, None))
        out.append({
            "drug_name": name,
            "drug_spec": clean(r["SPEC"], 64),
            "brand": clean(r["MANUFACTURER"], 64),
            "drug_unit": clean(r["UOM"], 16) or clean(r["PACKAGE"], 16),
            "drug_type": dtype,
            "drug_price": to_dec(r["STD_COST"], 0) or 0,
            "cost_price": to_dec(r["COST_PRICE"], 0) or 0,
            "drug_stock": qty if qty and qty > 0 else 0,
            "warning_stock": qmin if qmin is not None else 0,
            "expire_date": parse_date(r["DATE_EXPIRATION"]),
            "drug_info": ("；".join([x for x in info if x]) or None) and ("；".join([x for x in info if x]))[:255],
            "active": 1,
            "legacy_part_no": legacy,
        })

    # 品类分布（对账用）
    dist = {}
    for d in out:
        dist[d["drug_type"]] = dist.get(d["drug_type"], 0) + 1
    rep.warnings.append("品类分布: " + ", ".join(f"{k}={v}" for k, v in sorted(dist.items(), key=lambda x: -x[1])))
    # 粮食类被归入"用品"，单独列出便于人工抽查（§2.7.1 定案：零售商品默认用品）
    food = [d["drug_name"] for d in out if d["drug_type"] == "用品" and re.search(r"粮|奶糕|罐头|猫条|冻干", d["drug_name"], re.IGNORECASE)]
    rep.warnings.append(f"用品类中的粮/罐头/零食样例({len(food)}条): " + "；".join(food[:8]))
    if other_sample:
        rep.warnings.append(f"其他样例: " + "；".join(other_sample[:8]))

    rep.rows = out
    return out, stock


# ---------------------------------------------------------------- 次卡模板
def build_card_templates(rep: EtlReport):
    rows = mssql_query(
        "SELECT PART_NUM,"
        f" {sq('DESCRIPT')} AS DESCRIPT, STD_COST, CARD_TIMES"
        " FROM epbmaster WHERE IS_HAIRING_CARD='Y' ORDER BY PART_NUM"
    )
    rep.source_rows = len(rows)
    out = []
    for r in rows:
        name = clean(r["DESCRIPT"], 64)
        if not name:
            rep.skip(clean(r["PART_NUM"], 32), "无 DESCRIPT，跳过次卡")
            continue
        out.append({
            "name": name,
            "service_type": "美容",
            "times": to_int(r["CARD_TIMES"], 1) or 1,
            "price": to_dec(r["STD_COST"], 0) or 0,
        })
    rep.rows = out
    return out


DRUG_SQL = (
    "INSERT INTO drug (drug_name, drug_spec, brand, drug_unit, drug_type, drug_price,"
    " cost_price, drug_stock, warning_stock, expire_date, drug_info, active, legacy_part_no)"
    " VALUES (%(drug_name)s,%(drug_spec)s,%(brand)s,%(drug_unit)s,%(drug_type)s,%(drug_price)s,"
    " %(cost_price)s,%(drug_stock)s,%(warning_stock)s,%(expire_date)s,%(drug_info)s,"
    " %(active)s,%(legacy_part_no)s)"
    " ON DUPLICATE KEY UPDATE drug_name=VALUES(drug_name), drug_spec=VALUES(drug_spec),"
    " brand=VALUES(brand), drug_unit=VALUES(drug_unit), drug_type=VALUES(drug_type),"
    " drug_price=VALUES(drug_price), cost_price=VALUES(cost_price),"
    " drug_stock=VALUES(drug_stock), warning_stock=VALUES(warning_stock),"
    " expire_date=VALUES(expire_date), drug_info=VALUES(drug_info), active=VALUES(active)"
)

CARD_SQL = (
    "INSERT INTO count_card_template (name, service_type, times, price, active)"
    " SELECT %(name)s, %(service_type)s, %(times)s, %(price)s, 1"
    " WHERE NOT EXISTS (SELECT 1 FROM count_card_template WHERE name=%(name)s)"
)


def apply_drugs(rows, rep):
    conn = mysql_conn()
    try:
        with conn.cursor() as cur:
            ok = 0
            for r in rows:
                try:
                    cur.execute(DRUG_SQL, r)
                    ok += 1
                except Exception as e:  # noqa: BLE001
                    rep.skip(r["legacy_part_no"], f"写入失败: {str(e)[:80]}")
            conn.commit()
            rep.applied = ok
    finally:
        conn.close()


def apply_cards(rows, rep):
    conn = mysql_conn()
    try:
        with conn.cursor() as cur:
            ok = 0
            for r in rows:
                try:
                    cur.execute(CARD_SQL, r)
                    ok += cur.rowcount  # NOT EXISTS 判重：重复执行 rowcount=0
                except Exception as e:  # noqa: BLE001
                    rep.skip(r["name"], f"写入失败: {str(e)[:80]}")
            conn.commit()
            rep.applied = ok
    finally:
        conn.close()


def main():
    apply_mode = parse_arg_mode(sys.argv)

    rd = EtlReport("epbmaster(A态库存商品) → drug")
    drugs, _ = build_drugs(rd)
    if apply_mode:
        apply_drugs(drugs, rd)
    rd.dump()

    rc = EtlReport("epbmaster(美容次卡) → count_card_template")
    cards = build_card_templates(rc)
    if apply_mode:
        apply_cards(cards, rc)
    rc.dump()

    if not apply_mode:
        print("以上为 DRY-RUN（未写库）。加 --apply 执行写入。")


if __name__ == "__main__":
    main()
