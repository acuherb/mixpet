# -*- coding: utf-8 -*-
"""
migration/etl/etl_immune.py — epetjianyi(+item) → immune_record（免疫/驱虫业务记录）

为什么还要迁一遍（此前已写 quarantine_cert）：
- quarantine_cert 是"打印单据留痕"（纸面产物）
- immune_record 是"业务实体"（能查、能算下次接种、能提醒、能统计）
老系统一份免疫记录同时承担这两件事，新系统拆开，所以两边都要有。
疫苗名取清单首项（一单通常 1 针），数量取 QTY_ORDER 合计，下次时间 = 接种日 + PRE_DAYS。

幂等：immune_record 无 legacy 列，用 (pet_id, vaccinate_time, drug_name) 判重。

用法：python3 etl_immune.py [--apply]
"""
import sys
from datetime import timedelta

from common import mssql_query, mysql_conn, EtlReport, parse_arg_mode, sq
from etl_master import clean, parse_dt, to_dec


def build(rep: EtlReport):
    rows = mssql_query(
        "SELECT JY_ID, JY_TYPE, JY_DATE, JY_DESCRIPT, PET_ID, PRE_DAYS, LINK_PRIS_ID"
        f" FROM epetjianyi ORDER BY JY_ID"
    )
    rep.source_rows = len(rows)
    items = mssql_query(
        "SELECT JY_ID, QTY_ORDER,"
        f" {sq('(SELECT TOP 1 DESCRIPT FROM epbmaster b WHERE b.PART_NUM=i.PART_NUM)', 128)} AS DESCRIPT"
        " FROM epetjianyiitem i ORDER BY JY_ID, ID"
    )
    first_item = {}
    qty_sum = {}
    for it in items:
        jy = clean(it["JY_ID"])
        if not jy:
            continue
        if jy not in first_item:
            first_item[jy] = clean(it["DESCRIPT"], 64)
        qty_sum[jy] = qty_sum.get(jy, 0) + (to_dec(it["QTY_ORDER"], 0) or 0)

    out = []
    for r in rows:
        jy_no = clean(r["JY_ID"], 32)
        pet_no = clean(r["PET_ID"], 32)
        if not jy_no or not pet_no:
            rep.skip(str(jy_no), "缺 JY_ID/PET_ID")
            continue
        jy_type = clean(r["JY_TYPE"], 16) or "免疫"
        drug = first_item.get(jy_no) or jy_type
        vt = parse_dt(r["JY_DATE"])
        pre = to_dec(r["PRE_DAYS"], 0) or 0
        note = clean((jy_type + "：" + (clean(r["JY_DESCRIPT"], 300) or "")).strip("："), 255)
        out.append({
            "legacy_pet_no": pet_no,
            "drug_name": drug,
            "dose_cnt": qty_sum.get(jy_no) or 1,
            "vaccinate_time": vt,
            "next_time": (vt + timedelta(days=int(pre))) if (vt and pre > 0) else None,
            "note": note,
            "create_time": vt,
            "src_key": jy_no,
        })
    rep.rows = out
    return out


def apply(rows, rep):
    conn = mysql_conn()
    ok = 0
    try:
        with conn.cursor() as cur:
            cur.execute("SELECT p.legacy_pet_no, p.pet_id, p.client_id FROM pet p"
                        " WHERE p.legacy_pet_no IS NOT NULL")
            pets = {x["legacy_pet_no"]: x for x in cur.fetchall()}
            cur.execute("SELECT pet_id, vaccinate_time, drug_name FROM immune_record")
            done = {(x["pet_id"], str(x["vaccinate_time"]), x["drug_name"]) for x in cur.fetchall()}
            for r in rows:
                p = pets.get(r["legacy_pet_no"])
                if not p:
                    rep.skip(r["src_key"], f"宠物 {r['legacy_pet_no']} 未迁移")
                    continue
                key = (p["pet_id"], str(r["vaccinate_time"]), r["drug_name"])
                if key in done:
                    rep.skip(r["src_key"], "已迁移（幂等跳过）")
                    continue
                cur.execute(
                    "INSERT INTO immune_record (pet_id, client_id, drug_id, drug_name, dose_cnt,"
                    " vaccinate_time, next_time, note, create_time)"
                    " VALUES (%s,%s,0,%s,%s,%s,%s,%s,%s)",
                    (p["pet_id"], p["client_id"], r["drug_name"], r["dose_cnt"],
                     r["vaccinate_time"], r["next_time"], r["note"], r["create_time"]),
                )
                ok += 1
            conn.commit()
            rep.applied = ok
    finally:
        conn.close()


def main():
    apply_mode = parse_arg_mode(sys.argv)
    rep = EtlReport("epetjianyi → immune_record")
    rows = build(rep)
    if apply_mode:
        apply(rows, rep)
    rep.dump()


if __name__ == "__main__":
    main()
