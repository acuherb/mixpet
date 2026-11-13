# -*- coding: utf-8 -*-
"""
migration/etl/etl_balance.py — 储值迁移（emember_dc + emember_dc_detail → client_balance + 期初流水）

数据口径
- 老系统 emember_dc 是"储值/次卡"主表（MEMBER_ID_DC 卡号），DC_A..DC_D 是卡权益权重（实测恒为 100），
  不是余额；真实余额只能按流水算：
      卡余额 = SUM(COST_TYPE='充值' 的 COST) - SUM(其它类型 的 COST)
- 一人多卡 → 按客户聚合，client.client_balance 累加，并写**一条** MIGRATION_OPEN 期初流水
  （ref_type='MEMBER_DC'，note 列全部卡号，便于日后逐卡对账）
- 幂等：按 change_type='MIGRATION_OPEN' + client_id 判重，重跑不会二次加钱

用法：
    python3 etl_balance.py            # dry-run
    python3 etl_balance.py --apply
"""
import sys

from common import mssql_query, mysql_conn, EtlReport, parse_arg_mode
from etl_master import clean, to_dec


def build(rep: EtlReport):
    rows = mssql_query(
        "SELECT d.MEMBER_ID_DC, d.MEMBER_ID, d.MEMO,"
        " SUM(CASE WHEN t.COST_TYPE='充值' THEN CAST(t.COST AS DECIMAL(18,2))"
        "          ELSE -CAST(t.COST AS DECIMAL(18,2)) END) AS bal,"
        " COUNT(t.ID) AS n"
        " FROM emember_dc d LEFT JOIN emember_dc_detail t ON t.MEMBER_ID_DC=d.MEMBER_ID_DC"
        " GROUP BY d.MEMBER_ID_DC, d.MEMBER_ID, d.MEMO"
    )
    rep.source_rows = len(rows)
    by_member = {}
    for r in rows:
        member_no = clean(r["MEMBER_ID"], 32)
        bal = to_dec(r["bal"], 0) or 0
        if not member_no:
            rep.skip(clean(r["MEMBER_ID_DC"]), "卡无 MEMBER_ID")
            continue
        if bal <= 0:
            # 余额≤0 的卡（已用完/透支）不写期初，避免把负数带进新账
            if bal < 0:
                rep.warn(f"卡 {clean(r['MEMBER_ID_DC'])} 余额 {bal:.2f} 为负，未迁移")
            continue
        agg = by_member.setdefault(member_no, {"bal": 0.0, "cards": []})
        agg["bal"] += bal
        agg["cards"].append(f"{clean(r['MEMBER_ID_DC'])}:{bal:.2f}")
    out = [
        {"legacy_member_no": k, "balance": round(v["bal"], 2), "cards": v["cards"]}
        for k, v in by_member.items()
    ]
    rep.rows = out
    rep.warn(f"有余额客户 {len(out)} 位，合计 ￥{sum(x['balance'] for x in out):.2f}")
    return out


def apply(rows, rep):
    conn = mysql_conn()
    ok = 0
    try:
        with conn.cursor() as cur:
            cur.execute("SELECT client_id, legacy_member_no, client_balance FROM client"
                        " WHERE legacy_member_no IS NOT NULL")
            cmap = {x["legacy_member_no"]: x for x in cur.fetchall()}
            cur.execute("SELECT client_id FROM client_balance_log WHERE change_type='MIGRATION_OPEN'")
            done = {x["client_id"] for x in cur.fetchall()}
            for r in rows:
                c = cmap.get(r["legacy_member_no"])
                if not c:
                    rep.skip(r["legacy_member_no"], "客户未迁移")
                    continue
                if c["client_id"] in done:
                    rep.skip(r["legacy_member_no"], "期初已写入（幂等跳过）")
                    continue
                before = float(c["client_balance"] or 0)
                after = round(before + r["balance"], 2)
                cur.execute("UPDATE client SET client_balance=%s WHERE client_id=%s", (after, c["client_id"]))
                cur.execute(
                    "INSERT INTO client_balance_log (client_id, change_type, amount, balance_before,"
                    " balance_after, ref_type, ref_id, operator, note, create_time)"
                    " VALUES (%s,'MIGRATION_OPEN',%s,%s,%s,'MEMBER_DC',0,'migration',%s,NOW())",
                    (c["client_id"], r["balance"], before, after,
                     ("老系统储值卡迁移 卡号 " + "、".join(r["cards"]))[:255]),
                )
                ok += 1
            conn.commit()
            rep.applied = ok
    finally:
        conn.close()


def main():
    apply_mode = parse_arg_mode(sys.argv)
    rep = EtlReport("emember_dc → client_balance + MIGRATION_OPEN")
    rows = build(rep)
    if apply_mode:
        apply(rows, rep)
    rep.dump()


if __name__ == "__main__":
    main()
