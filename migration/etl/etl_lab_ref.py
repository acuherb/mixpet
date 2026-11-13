# -*- coding: utf-8 -*-
"""
etl_lab_ref.py — epbmaster 参考值列 → mixpet.lab_ref_range 转换器
规则依据：migration/FIELD_MAPPING.md §2.7.2（P0-5 定案）

源数据画像（2026-11-13 petsinit dry-run 实测）：
- 参考值藏在 epbmaster 的 DOG/DOG_Y/DOG2..4、CAT 系列、RABBIT/HAMSTER/TOTORO 列；
- 药品/用品行也借用同列存剂量（如 A05 头孢曲松钠 DOG=80），必须按 PART_NUM
  前缀过滤：y*（化验/检验）与 DEV*（设备检验项目）才进转换；
- 垃圾值 ')' 需剔除；
- 值格式："3.50-5.80" 区间 / "80" 单值 / "Q"|"L" 等定性代码；
- epbmastercheckvalue 是定性项的选项字典（±/—/+），拼入 ref_text；
- 老系统无危急值列、无公母分段 → critical_low/high、sex 留 NULL。

段映射（不造数据原则：无依据的年龄边界一律不拆数值段）：
  DOG  → 犬 成年(≥12月)          DOG_Y → 犬 幼年(≤12月)
  CAT  → 猫 成年(≥12月)          CAT_Y → 猫 幼年(≤12月)
  DOG2/3/4, DOG_Y2/3, CAT2/3/4, CAT_Y2/3 → 对应物种，ref_text 保留原文、
      月龄段留空（老系统无边界定义，⚠ 需医院确认后再补拆）
  RABBIT/HAMSTER/TOTORO → 兔/仓鼠/龙猫，月龄不限
"""
import re
import sys
from datetime import datetime

from common import mssql_query, mysql_conn, EtlReport, parse_arg_mode

# 列 → (species, age_min, age_max) ；扩展列 boundary=None 表示无依据
SEG_MAP = {
    "DOG": ("犬", 12, None),   "DOG_Y": ("犬", None, 12),
    "CAT": ("猫", 12, None),   "CAT_Y": ("猫", None, 12),
    "DOG2": ("犬", None, None), "DOG3": ("犬", None, None), "DOG4": ("犬", None, None),
    "DOG_Y2": ("犬", None, None), "DOG_Y3": ("犬", None, None),
    "CAT2": ("猫", None, None), "CAT3": ("猫", None, None), "CAT4": ("猫", None, None),
    "CAT_Y2": ("猫", None, None), "CAT_Y3": ("猫", None, None),
    "RABBIT": ("兔", None, None),
    "HAMSTER": ("仓鼠", None, None),
    "TOTORO": ("龙猫", None, None),
}
EXT_COLS = {c for c, (_, lo, hi) in SEG_MAP.items() if lo is None and hi is None
            and c not in ("RABBIT", "HAMSTER", "TOTORO")}

RANGE_RE = re.compile(r"^(-?\d+(?:\.\d+)?)\s*[-~—–]\s*(-?\d+(?:\.\d+)?)$")
VALUE_RE = re.compile(r"^(-?\d+(?:\.\d+)?)$")

# 检验项料号前缀（其余前缀为药品/用品/美容，其 DOG 列是剂量，绝不进参考值）
LAB_PREFIX_RE = re.compile(r"^(y|dev)", re.IGNORECASE)


def parse_ref(raw: str):
    """'3.50-5.80'→(low,high,None)；'80'→(80,80,None)；其他→(None,None,原文)"""
    v = raw.strip()
    m = RANGE_RE.match(v)
    if m:
        return float(m.group(1)), float(m.group(2)), None
    m = VALUE_RE.match(v)
    if m:
        f = float(m.group(1))
        return f, f, None
    return None, None, v


def main(apply_mode: bool):
    rep = EtlReport("epbmaster 参考值 → lab_ref_range")

    # ---- 1. 定性选项字典：PART_NUM → 选项拼接 ----
    qual_map = {}
    for r in mssql_query("SELECT PART_NUM, [DESC] FROM epbmastercheckvalue WHERE ISNULL([DESC],'')<>''"):
        qual_map.setdefault(r["PART_NUM"], []).append(r["DESC"])

    # ---- 2. 源数据：检验项行（有任一参考值列非垃圾值） ----
    cols = list(SEG_MAP.keys())
    col_expr = ", ".join(cols)
    src = mssql_query(f"""
        SELECT PART_NUM, DESCRIPT, UOM, {col_expr}
        FROM epbmaster
        WHERE (LOWER(PART_NUM) LIKE 'y%' OR LOWER(PART_NUM) LIKE 'dev-%')
    """)
    rep.source_rows = len(src)

    # 每行每列生成 0..1 条目标行
    seen_qual = set()
    for r in src:
        pno = r["PART_NUM"]
        name = (r.get("DESCRIPT") or r.get("NOR_NAME") or "").strip()
        if not name:
            rep.skip(pno, "无项目名（DESCRIPT/NOR_NAME 皆空）")
            continue
        unit = (r.get("UOM") or "").strip() or None
        for col in cols:
            raw = (r.get(col) or "").strip()
            if not raw or raw == ")":
                continue
            species, amin, amax = SEG_MAP[col]
            low, high, text = parse_ref(raw)
            if col in EXT_COLS and low is not None:
                # 扩展段无年龄边界依据：数值不拆，整段原文进 ref_text
                text, low, high = raw, None, None
                rep.warn(f"[{pno}] {col}（{name}）数值参考值缺年龄段定义，暂存 ref_text 原文: {raw}")
            if text is None and pno in qual_map and pno not in seen_qual:
                # 定性项：选项拼入 ref_text（每个 PART_NUM 只拼一次，挂成年段行）
                text = "定性选项: " + " / ".join(qual_map[pno])
                seen_qual.add(pno)
            rep.rows.append({
                "legacy_no": pno, "item_name": name, "species": species,
                "sex": None, "age_min_month": amin, "age_max_month": amax,
                "ref_low": low, "ref_high": high, "ref_text": text, "unit": unit,
                "col": col,
            })
        if pno in qual_map and pno not in seen_qual and not any(
                (r.get(c) or "").strip() not in ("", ")") for c in cols):
            # 纯定性项（所有参考值列皆空，仅有选项字典）：生成一条纯 ref_text 行
            seen_qual.add(pno)
            rep.rows.append({
                "legacy_no": pno, "item_name": name, "species": "犬", "sex": None,
                "age_min_month": None, "age_max_month": None,
                "ref_low": None, "ref_high": None,
                "ref_text": "定性选项: " + " / ".join(qual_map[pno]), "unit": unit,
                "col": "-",
            })
            rep.warn(f"[{pno}] {name} 纯定性项（无数值列），挂犬/不限月龄，需人工归物种")

    # ---- 3. lab_test 映射（兜底 ref_range + test_id 回填） ----
    conn = mysql_conn() if apply_mode else None
    if apply_mode:
        cur = conn.cursor()
        cur.execute("SELECT test_id, test_name FROM lab_test WHERE is_del=0")
        test_by_name = {t["test_name"].strip(): t["test_id"] for t in cur.fetchall()}
    else:
        cur = None
        test_by_name = {}
    match_new, match_hit = [], 0
    for row in rep.rows:
        tid = test_by_name.get(row["item_name"])
        row["test_id"] = tid
        if tid:
            match_hit += 1
        else:
            match_new.append(row)

    # ---- 4. 写库（幂等：先清本 ETL 旧数据再插，重复执行不翻倍） ----
    if apply_mode:
        now = datetime.now()
        cur.execute("DELETE FROM lab_ref_range WHERE legacy_no IS NOT NULL")
        cur.executemany(
            "INSERT INTO lab_ref_range (test_id, legacy_no, species, sex, age_min_month,"
            " age_max_month, ref_low, ref_high, ref_text, unit, active, create_time)"
            " VALUES (%(test_id)s,%(legacy_no)s,%(species)s,%(sex)s,%(age_min_month)s,"
            " %(age_max_month)s,%(ref_low)s,%(ref_high)s,%(ref_text)s,%(unit)s,1,%(ct)s)",
            [{**r, "ct": now} for r in rep.rows],
        )
        # lab_test 兜底：新检验项建档（成年段值汇入 ref_range 文本，保证列表不空）
        name_unit = {}
        name_ref = {}
        for r in rep.rows:
            key = r["item_name"]
            name_unit.setdefault(key, r["unit"])
            if r["species"] == "犬" and r["age_min_month"] == 12 and r["ref_low"] is not None:
                name_ref[key] = f"{r['ref_low']:g}-{r['ref_high']:g}"
        for name in {r["item_name"] for r in match_new}:
            cur.execute(
                "INSERT INTO lab_test (test_name, test_group, test_unit, ref_range,"
                " test_price, active, create_time, update_time, is_del)"
                " VALUES (%s,'迁移',%s,%s,0,1,%s,%s,0)",
                (name, name_unit.get(name), name_ref.get(name), now, now),
            )
            cur.execute("SELECT LAST_INSERT_ID() AS tid")
            tid = cur.fetchone()["tid"]
            cur.execute("UPDATE lab_ref_range SET test_id=%s WHERE legacy_no=%s",
                        (tid, next(r["legacy_no"] for r in match_new if r["item_name"] == name)))
        conn.commit()
        rep.applied = len(rep.rows)
        cur.execute("SELECT COUNT(*) AS c FROM lab_ref_range")
        rep.rows = [{"total": cur.fetchone()["c"]}]
    else:
        # dry-run 抽样
        print("抽样（前 8 行）:")
        for r in rep.rows[:8]:
            seg = f"{r['species']}" + (f" {r['age_min_month']}-{r['age_max_month']}月" if r['age_min_month'] or r['age_max_month'] else "")
            val = f"{r['ref_low']:g}-{r['ref_high']:g}" if r["ref_low"] is not None else (r["ref_text"] or "")
            print(f"  [{r['legacy_no']}] {r['item_name']} | {seg} | {val} | 单位:{r['unit'] or '-'} | 列:{r['col']}")
        print(f"\nlab_test 名称映射: 命中 {match_hit} 行 / 将新建检验项 {len({r['item_name'] for r in match_new})} 个")

    rep.dump()
    if apply_mode:
        conn.close()


if __name__ == "__main__":
    main(parse_arg_mode(sys.argv))
