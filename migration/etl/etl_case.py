# -*- coding: utf-8 -*-
"""
migration/etl/etl_case.py — 诊疗数据迁移

覆盖三块（都按 legacy 键幂等，可重复执行）：
  1) epetcase → appointment(历史挂号单) + medical_record
  2) epetcaseprescription(+item) → prescription（药品快照写入，drug_id=0 不依赖药品库）
  3) epetjianyi(+item) → quarantine_cert（老系统「免疫/驱虫记录」，即打印的检疫单）

关键取舍
- 医生：老 DOCT_ID 是 4 位工号（0002/0010…），与 mixpet 医生账号无对应关系，
  历史病历统一挂默认医生（doctor_id=1），老工号原文写入 internal_note，不造假归属。
- 病历状态：老 C(完成) → 1；P(预约未就诊) → 0
- 挂号单：medical_record.appointment_id 非空，故每条病历补一条 is_suc=1 的历史挂号
- 药品：drug_id=0（药品库尚未迁移），名称/规格/单位/单价全部落快照列，
  保证处方笺打印与历史追溯完整；等药品库迁移后用 legacy PART_NUM 回填
- 老处方 STATUS='F' 才是正常单（D/B 为作废/调账），只迁 F

用法：
    python3 etl_case.py            # dry-run
    python3 etl_case.py --apply    # 写库
"""
import json
import re
import sys
from datetime import datetime

from common import mssql_query, mysql_conn, EtlReport, parse_arg_mode, sq
from etl_master import clean, parse_dt, to_dec

DEFAULT_DOCTOR_ID = 1   # 童德统：历史病历归属兜底
DEFAULT_DEPT_ID = 1     # 内科


# ---------------------------------------------------------------- 病历
def build_records(rep: EtlReport):
    rows = mssql_query(
        "SELECT CASE_ID, PET_ID, DOCT_ID, DATETIME, STATUS,"
        f" {sq('INIT_DIAG', 300)} AS INIT_DIAG, {sq('INIT_DIAG1', 100)} AS INIT_DIAG1,"
        f" {sq('INIT_DIAG2', 100)} AS INIT_DIAG2, {sq('SYMPTOM', 400)} AS SYMPTOM,"
        f" {sq('DETAILS', 2000)} AS DETAILS, {sq('DOCT_ADVICE', 400)} AS DOCT_ADVICE,"
        f" {sq('MEMO', 300)} AS MEMO, {sq('ZS', 400)} AS ZS, {sq('ZD', 400)} AS ZD, {sq('YZ', 400)} AS YZ,"
        " WEIGHT, TEMP_ZS, MDF_DATE"
        " FROM epetcase ORDER BY CASE_ID"
    )
    rep.source_rows = len(rows)
    out = []
    for r in rows:
        case_no = clean(r["CASE_ID"], 32)
        pet_no = clean(r["PET_ID"], 32)
        if not case_no:
            rep.skip("?", "无 CASE_ID")
            continue
        if not pet_no:
            rep.skip(case_no, "无 PET_ID")
            continue
        created = parse_dt(r["DATETIME"]) or parse_dt(r["MDF_DATE"]) or datetime.now()
        st = (clean(r["STATUS"]) or "C").upper()
        status = 1 if st == "C" else 0
        # 诊断：INIT_DIAG 为主，INIT_DIAG1/2 是补充诊断
        diags = [clean(r["INIT_DIAG"], 200), clean(r["INIT_DIAG1"], 40), clean(r["INIT_DIAG2"], 40)]
        diagnosis = clean("；".join([d for d in diags if d]), 255)
        note = []
        doct_no = clean(r["DOCT_ID"])
        if doct_no:
            note.append(f"老系统医生工号 {doct_no}")
        if st != "C":
            note.append(f"老系统状态 {st}")
        memo = clean(r["MEMO"], 120)
        if memo:
            note.append(memo)
        weight = to_dec(r["WEIGHT"])
        temp = to_dec(r["TEMP_ZS"])
        out.append({
            "legacy_case_no": case_no,
            "legacy_pet_no": pet_no,
            "doctor_id": DEFAULT_DOCTOR_ID,
            "department_id": DEFAULT_DEPT_ID,
            "chief_complaint": clean(r["SYMPTOM"], 255) or clean(r["ZS"], 255),
            "present_illness": clean(r["DETAILS"], 2000),
            "diagnosis": diagnosis or clean(r["ZD"], 255),
            "advice": clean(r["DOCT_ADVICE"], 255) or clean(r["YZ"], 255),
            "internal_note": clean("；".join(note), 255),
            "weight": weight if weight and weight > 0 else None,
            "temperature": temp if temp and 30 <= temp <= 45 else None,
            "status": status,
            "create_time": created,
            "finish_time": parse_dt(r["MDF_DATE"]) if status == 1 else None,
        })
    rep.rows = out
    return out


# ---------------------------------------------------------------- 处方
def build_prescriptions(rep: EtlReport):
    rows = mssql_query(
        "SELECT i.ID, i.PRIS_ID, ISNULL(NULLIF(i.CASE_ID,''), p.CASE_ID) AS CASE_ID, i.PART_NUM, i.QTY, i.OUT_PRICE,"
        f" {sq('i.DAYS', 32)} AS DAYS, {sq('i.PRAR_ROAD', 128)} AS PRAR_ROAD,"
        f" {sq('i.OP_VAL', 32)} AS OP_VAL, {sq('i.OP_VAL2', 32)} AS OP_VAL2,"
        f" {sq('i.MEMO', 200)} AS MEMO, i.MDF_DATE,"
        f" {sq('b.DESCRIPT', 128)} AS DESCRIPT, {sq('b.UOM', 16)} AS UOM"
        " FROM epetcaseprescriptionitem i"
        " JOIN epetcaseprescription p ON p.PRIS_ID=i.PRIS_ID AND p.STATUS='F'"
        " LEFT JOIN epbmaster b ON b.PART_NUM=i.PART_NUM"
        " ORDER BY i.ID"
    )
    rep.source_rows = len(rows)
    out = []
    for r in rows:
        item_id = clean(r["ID"], 32)
        case_no = clean(r["CASE_ID"], 32)
        name = clean(r["DESCRIPT"], 64) or clean(r["PART_NUM"], 64)
        if not item_id or not case_no:
            rep.skip(str(item_id), "缺 ID/CASE_ID")
            continue
        qty = to_dec(r["QTY"], 0)
        # DAYS 是 nvarchar，实测有 '4天' 这种带单位文本
        days_raw = clean(r["DAYS"]) or ""
        m = re.search(r"\d+", days_raw)
        days = int(m.group()) if m else None
        out.append({
            "legacy_item_id": item_id,
            "legacy_case_no": case_no,
            "drug_name": name,
            "drug_spec": clean(r["MEMO"], 64),
            "drug_unit": clean(r["UOM"], 16),
            "drug_price": to_dec(r["OUT_PRICE"]),
            "quantity": int(qty) if qty and qty > 0 else 1,
            "duration_days": days if days and days > 0 else None,
            "usage_info": clean(r["PRAR_ROAD"], 128),
            "single_dose": to_dec(r["OP_VAL"]),
            "single_dose_unit": clean(r["OP_VAL2"], 16),
            "create_time": parse_dt(r["MDF_DATE"]) or datetime.now(),
        })
    rep.rows = out
    return out


# ---------------------------------------------------------------- 免疫/检疫记录
def build_quarantine(rep: EtlReport):
    rows = mssql_query(
        "SELECT JY_ID, JY_TYPE, JY_DATE,"
        f" {sq('JY_DESCRIPT', 800)} AS JY_DESCRIPT, JY_OP, STATUS, PET_ID, LINK_PRIS_ID"
        " FROM epetjianyi ORDER BY JY_ID"
    )
    rep.source_rows = len(rows)
    items = mssql_query(
        "SELECT JY_ID, PART_NUM, QTY_ORDER, ORDER_PRICE,"
        f" {sq('MEMO', 200)} AS MEMO,"
        f" {sq('(SELECT TOP 1 DESCRIPT FROM epbmaster b WHERE b.PART_NUM=i.PART_NUM)', 128)} AS DESCRIPT,"
        f" {sq('(SELECT TOP 1 UOM FROM epbmaster b WHERE b.PART_NUM=i.PART_NUM)', 16)} AS UOM"
        " FROM epetjianyiitem i ORDER BY JY_ID, ID"
    )
    by_jy = {}
    for it in items:
        by_jy.setdefault(clean(it["JY_ID"]), []).append(it)

    out = []
    for r in rows:
        jy_no = clean(r["JY_ID"], 32)
        pet_no = clean(r["PET_ID"], 32)
        if not jy_no or not pet_no:
            rep.skip(str(jy_no), "缺 JY_ID/PET_ID")
            continue
        jy_type = clean(r["JY_TYPE"], 16) or "免疫"
        desc = clean(r["JY_DESCRIPT"], 400)
        content = jy_type + ("：" + desc if desc else "")
        item_rows = []
        for it in by_jy.get(jy_no, []):
            qty = to_dec(it["QTY_ORDER"], 0)
            item_rows.append({
                "drugName": clean(it["DESCRIPT"], 64) or clean(it["PART_NUM"], 64),
                "drugSpec": clean(it["MEMO"], 64),
                "drugUnit": clean(it["UOM"], 16),
                "quantity": int(qty) if qty and qty > 0 else 1,
                "dosage": clean(it["MEMO"], 64),
                "usageInfo": None,
            })
        out.append({
            "legacy_no": jy_no,
            "legacy_pet_no": pet_no,
            "owner_name": None,
            "owner_tel": None,
            "pet_name": None,
            "species": None,
            "breed": None,
            "sex": None,
            "age_desc": None,
            "weight": None,
            "content": content,
            "items_json": json.dumps(item_rows, ensure_ascii=False),
            "create_time": parse_dt(r["JY_DATE"]) or datetime.now(),
        })
    rep.rows = out
    return out


# ---------------------------------------------------------------- 写库
REC_SQL = (
    "INSERT INTO medical_record (appointment_id, pet_id, client_id, doctor_id, department_id,"
    " chief_complaint, present_illness, diagnosis, advice, internal_note, weight, temperature,"
    " status, create_time, finish_time, legacy_case_no)"
    " VALUES (%(appointment_id)s,%(pet_id)s,%(client_id)s,%(doctor_id)s,%(department_id)s,"
    " %(chief_complaint)s,%(present_illness)s,%(diagnosis)s,%(advice)s,%(internal_note)s,"
    " %(weight)s,%(temperature)s,%(status)s,%(create_time)s,%(finish_time)s,%(legacy_case_no)s)"
)

APPT_SQL = (
    "INSERT INTO appointment (client_id, pet_id, pet_owner_id, appointment_date, department_id,"
    " doctor_id, appointment_info, is_suc, miss, create_time)"
    " VALUES (%(client_id)s,%(pet_id)s,%(pet_owner_id)s,%(appointment_date)s,%(department_id)s,"
    " %(doctor_id)s,%(appointment_info)s,1,0,%(create_time)s)"
)

PRIS_SQL = (
    "INSERT INTO prescription (record_id, drug_id, drug_name, drug_spec, drug_unit, drug_price,"
    " quantity, duration_days, usage_info, single_dose, single_dose_unit, create_time, legacy_item_id)"
    " VALUES (%(record_id)s,0,%(drug_name)s,%(drug_spec)s,%(drug_unit)s,%(drug_price)s,"
    " %(quantity)s,%(duration_days)s,%(usage_info)s,%(single_dose)s,%(single_dose_unit)s,"
    " %(create_time)s,%(legacy_item_id)s)"
    " ON DUPLICATE KEY UPDATE drug_name=VALUES(drug_name), quantity=VALUES(quantity),"
    " drug_price=VALUES(drug_price), duration_days=VALUES(duration_days),"
    " usage_info=VALUES(usage_info), record_id=VALUES(record_id)"
)

QC_SQL = (
    "INSERT INTO quarantine_cert (cert_no, record_id, pet_id, client_id, doctor_id, owner_name,"
    " owner_tel, pet_name, species, breed, sex, age_desc, weight, content, items_json,"
    " create_time, legacy_no)"
    " VALUES (%(cert_no)s,%(record_id)s,%(pet_id)s,%(client_id)s,%(doctor_id)s,%(owner_name)s,"
    " %(owner_tel)s,%(pet_name)s,%(species)s,%(breed)s,%(sex)s,%(age_desc)s,%(weight)s,"
    " %(content)s,%(items_json)s,%(create_time)s,%(legacy_no)s)"
    " ON DUPLICATE KEY UPDATE content=VALUES(content), items_json=VALUES(items_json)"
)


def load_pet_map(cur):
    """legacy_pet_no → (pet_id, client_id, pet_name, species/breed/sex/weight/生日/主人/电话)"""
    cur.execute(
        "SELECT p.legacy_pet_no, p.pet_id, p.client_id, p.pet_name, p.pet_variety, p.pet_sex,"
        " p.pet_weight, c.legacy_member_no, c.client_name, c.client_tel"
        " FROM pet p JOIN client c ON c.client_id=p.client_id WHERE p.legacy_pet_no IS NOT NULL"
    )
    return {x["legacy_pet_no"]: x for x in cur.fetchall()}


def apply_records(rows, rep):
    conn = mysql_conn()
    ok = 0
    try:
        with conn.cursor() as cur:
            pets = load_pet_map(cur)
            cur.execute("SELECT legacy_case_no FROM medical_record WHERE legacy_case_no IS NOT NULL")
            done = {x["legacy_case_no"] for x in cur.fetchall()}
            for r in rows:
                if r["legacy_case_no"] in done:
                    rep.skip(r["legacy_case_no"], "已迁移（幂等跳过）")
                    continue
                p = pets.get(r["legacy_pet_no"])
                if not p:
                    rep.skip(r["legacy_case_no"], f"宠物 {r['legacy_pet_no']} 未迁移")
                    continue
                appt = {
                    "client_id": p["client_id"], "pet_id": p["pet_id"], "pet_owner_id": p["client_id"],
                    "appointment_date": r["create_time"], "department_id": r["department_id"],
                    "doctor_id": r["doctor_id"],
                    "appointment_info": ("老系统病例 " + r["legacy_case_no"])[:64],
                    "create_time": r["create_time"],
                }
                cur.execute(APPT_SQL, appt)
                rec = dict(r, appointment_id=cur.lastrowid, pet_id=p["pet_id"], client_id=p["client_id"])
                cur.execute(REC_SQL, rec)
                ok += 1
            conn.commit()
            rep.applied = ok
    finally:
        conn.close()


def apply_prescriptions(rows, rep):
    conn = mysql_conn()
    ok = 0
    try:
        with conn.cursor() as cur:
            cur.execute("SELECT legacy_case_no, record_id FROM medical_record WHERE legacy_case_no IS NOT NULL")
            rec_map = {x["legacy_case_no"]: x["record_id"] for x in cur.fetchall()}
            for r in rows:
                rid = rec_map.get(r["legacy_case_no"])
                if not rid:
                    rep.skip(r["legacy_item_id"], f"病历 {r['legacy_case_no']} 未迁移")
                    continue
                try:
                    cur.execute(PRIS_SQL, dict(r, record_id=rid))
                    ok += 1
                except Exception as e:  # noqa: BLE001
                    rep.skip(r["legacy_item_id"], f"写入失败: {str(e)[:70]}")
            conn.commit()
            rep.applied = ok
    finally:
        conn.close()


def apply_quarantine(rows, rep):
    conn = mysql_conn()
    ok = 0
    try:
        with conn.cursor() as cur:
            pets = load_pet_map(cur)
            cur.execute("SELECT legacy_case_no, record_id FROM medical_record WHERE legacy_case_no IS NOT NULL")
            rec_map = {x["legacy_case_no"]: x["record_id"] for x in cur.fetchall()}
            for r in rows:
                p = pets.get(r["legacy_pet_no"])
                if not p:
                    rep.skip(r["legacy_no"], f"宠物 {r['legacy_pet_no']} 未迁移")
                    continue
                # 检疫单挂在宠物最近一次病历上（record_id 非空约束）
                cur.execute(
                    "SELECT record_id FROM medical_record WHERE pet_id=%s ORDER BY create_time DESC LIMIT 1",
                    (p["pet_id"],),
                )
                last = cur.fetchone()
                if not last:
                    rep.skip(r["legacy_no"], "该宠物无已迁移病历，检疫单无法挂接")
                    continue
                rec = {
                    "legacy_no": r["legacy_no"],
                    "record_id": last["record_id"],
                    "pet_id": p["pet_id"], "client_id": p["client_id"],
                    # 老系统无开单人对应关系，留空更诚实
                    "doctor_id": None,
                    "owner_name": p["client_name"], "owner_tel": p["client_tel"],
                    "pet_name": p["pet_name"], "species": p["pet_variety"], "breed": p["pet_variety"],
                    "sex": "公" if p["pet_sex"] == 1 else "母",
                    "age_desc": None, "weight": p["pet_weight"],
                    "content": r["content"], "items_json": r["items_json"],
                    "create_time": r["create_time"],
                    # 迁移单沿用老号，便于客户拿老单据来核对
                    "cert_no": r["legacy_no"],
                }
                try:
                    cur.execute(QC_SQL, rec)
                    ok += 1
                except Exception as e:  # noqa: BLE001
                    rep.skip(r["legacy_no"], f"写入失败: {str(e)[:70]}")
            conn.commit()
            rep.applied = ok
    finally:
        conn.close()


def main():
    apply_mode = parse_arg_mode(sys.argv)

    r1 = EtlReport("epetcase → medical_record")
    recs = build_records(r1)
    if apply_mode:
        apply_records(recs, r1)
    r1.dump()

    r2 = EtlReport("epetcaseprescriptionitem → prescription")
    pris = build_prescriptions(r2)
    if apply_mode:
        apply_prescriptions(pris, r2)
    r2.dump()

    r3 = EtlReport("epetjianyi → quarantine_cert（免疫/驱虫记录）")
    qc = build_quarantine(r3)
    if apply_mode:
        apply_quarantine(qc, r3)
    r3.dump()


if __name__ == "__main__":
    main()
