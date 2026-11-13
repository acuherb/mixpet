# -*- coding: utf-8 -*-
"""
migration/etl/etl_master.py — 主数据迁移：emember → client，epet → pet

设计要点
- 幂等：按 client.legacy_member_no / pet.legacy_pet_no 唯一键 upsert，重跑不重复插
- 老客户免审：active=1 + audit_status=1（迁移客户不该再去审核队列报道）
- 密码：沿用 client 表列默认 hash（也就是系统默认口令），老客户首次登录后可自行改密；
  老系统 PASSWORD 不可逆，不能迁
- 缺失客户的宠物：跳过并计数（不造"孤儿宠物"），对账报告里列出
- 性别：emember.SEX 男/女 → client_gender 1/0；epet.SEX 雄/雌 → pet_sex 1/0

用法：
    python3 etl_master.py            # dry-run：只读 + 对账，不写库
    python3 etl_master.py --apply    # 写库（可重复执行）
"""
import re
import sys
from datetime import datetime

from common import mssql_query, mysql_conn, EtlReport, parse_arg_mode

# 与 client.client_password 列默认值一致（系统默认口令的 bcrypt hash）
DEFAULT_PWD_HASH = "$2a$10$isx1czTSQaNBZiYiwo/TjuEb40VFhH6VqAXOsHfFK39ls.DxziPye"


# ---------------------------------------------------------------- 清洗工具
def clean(v, maxlen=None):
    """去首尾空白 + 换行归一（sqlcmd 输出可能带换行），可选截断。"""
    if v is None:
        return None
    s = str(v).replace("\r", " ").replace("\n", " ").strip()
    if not s or s.lower() == "null":
        return None
    return s[:maxlen] if maxlen else s


def norm_tel(*cands):
    """按优先级取第一个像电话的值。"""
    for c in cands:
        s = clean(c, 32)
        if s and re.fullmatch(r"[\d\-+ ]{5,20}", s):
            return s
    return None


def parse_dt(v):
    """'2026-03-18 14:58:44.727' / '2026-03-18' → datetime"""
    s = clean(v)
    if not s:
        return None
    s = s.split(".")[0]
    for fmt in ("%Y-%m-%d %H:%M:%S", "%Y-%m-%d"):
        try:
            return datetime.strptime(s, fmt)
        except ValueError:
            continue
    return None


def to_dec(v, default=None):
    """'.00' / '1.10' 这类老库数值文本 → Decimal 可用的 float。"""
    s = clean(v)
    if not s:
        return default
    try:
        return round(float(s), 2)
    except ValueError:
        return default


# ---------------------------------------------------------------- client
def build_clients(rep: EtlReport):
    rows = mssql_query(
        "SELECT MEMBER_ID, MEMBER_ID_SHOW, FULLNAME, MOBILE, TEL_HOME, TEL_OFFICE,"
        " SEX, BIRTHDAY_TRUE, ADDRESS, EMAIL, IDNO, MEMO, COST_ALL, M_LEVEL, REGDATE"
        " FROM emember ORDER BY REGDATE"
    )
    rep.source_rows = len(rows)
    out = []
    for r in rows:
        legacy_no = clean(r["MEMBER_ID"], 32)
        if not legacy_no:
            rep.skip("?", "无 MEMBER_ID（老会员号），无法建立追溯键")
            continue
        tel = norm_tel(r["MOBILE"], r["TEL_HOME"], r["TEL_OFFICE"])
        name = clean(r["FULLNAME"], 32) or ("顾客" + (tel[-4:] if tel else legacy_no[-4:]))
        sex_raw = clean(r["SEX"])
        gender = 1 if sex_raw == "男" else (0 if sex_raw == "女" else None)
        # 备注线：老系统信息在 mixpet 无对应列，但报表/客诉追溯需要，统一拼进 client_info
        info = []
        memo = clean(r["MEMO"], 120)
        if memo:
            info.append(memo)
        cost = to_dec(r["COST_ALL"])
        if cost:
            info.append(f"老系统累计消费￥{cost:.2f}")
        for extra in (clean(r["ADDRESS"], 60), clean(r["EMAIL"], 40), clean(r["IDNO"], 20)):
            if extra:
                info.append(extra)
        # M_LEVEL 实测空/'' 占绝对多数，映射不到具体等级 → 统一落普通级（level_key=0）
        lv_raw = clean(r["M_LEVEL"])
        level = 0
        if lv_raw and lv_raw.isdigit() and 0 <= int(lv_raw) <= 3:
            level = int(lv_raw)
        elif lv_raw:
            rep.warn(f"会员 {legacy_no} M_LEVEL='{lv_raw}' 非 0-3，落普通级")
        out.append({
            "client_username": legacy_no,
            "client_password": DEFAULT_PWD_HASH,
            "client_name": name,
            "client_gender": gender,
            "client_tel": tel,
            "client_tel2": norm_tel(r["TEL_OFFICE"], r["TEL_HOME"]) if tel == norm_tel(r["MOBILE"]) else None,
            "client_birthday": (lambda d: d.date() if d else None)(parse_dt(r["BIRTHDAY_TRUE"])),
            "client_info": clean("；".join(info), 255),
            "client_address": clean(r["ADDRESS"], 128),
            "client_email": clean(r["EMAIL"], 64),
            "client_id_card": clean(r["IDNO"], 18),
            "client_level": level,
            "client_source": "MIGRATION",
            "active": 1,
            "audit_status": 1,
            "create_time": parse_dt(r["REGDATE"]) or datetime.now(),
            "legacy_member_no": legacy_no,
        })
    rep.rows = out
    return out


# ---------------------------------------------------------------- pet
def build_pets(rep: EtlReport, known_members=None):
    rows = mssql_query(
        "SELECT PET_ID, FULLNAME, SPECIES, BREED, SEX, BIRTHDAY, BIRTHDAY_TRUE, AGE,"
        " WEIGHT, MEMBER_ID, CUTDATE, MEMO, DESCRIPTION, EATDRINK, LIVINGSPACE, REASON"
        " FROM epet ORDER BY PET_ID"
    )
    rep.source_rows = len(rows)
    out = []
    for r in rows:
        legacy_no = clean(r["PET_ID"], 32)
        member_no = clean(r["MEMBER_ID"], 32)
        if not legacy_no:
            rep.skip("?", "无 PET_ID，无法建立追溯键")
            continue
        if not member_no:
            rep.skip(legacy_no, "无 MEMBER_ID，孤儿宠物不迁移")
            continue
        # dry-run 阶段就把"客户不存在"暴露出来，别等 apply 写库时才失败
        if known_members is not None and member_no not in known_members:
            rep.skip(legacy_no, f"客户 {member_no} 不在 emember 中，孤儿宠物不迁移")
            continue
        sex_raw = clean(r["SEX"]) or ""
        # 老库性别是复合值：雄 / 雌 / 雄(已去势) / 雌(已绝育) / 3（异常）
        if "雄" in sex_raw or "公" in sex_raw or sex_raw == "男":
            sex = 1
        elif "雌" in sex_raw or "母" in sex_raw or sex_raw == "女":
            sex = 0
        else:
            # mixpet pet_sex NOT NULL DEFAULT 0：未知性别落默认值，别造"公/母"
            sex = 0
            if sex_raw:
                rep.warn(f"宠物 {legacy_no} SEX='{sex_raw}' 无法识别，落默认值 0")
        neutered = 1 if ("去势" in sex_raw or "绝育" in sex_raw or clean(r["CUTDATE"])) else 0
        birthday = parse_dt(r["BIRTHDAY_TRUE"]) or parse_dt(r["BIRTHDAY"])
        # 品种优先 BREED；mixpet 的 pet_variety 存的就是品种（金毛犬/柴犬…）
        variety = clean(r["BREED"], 32) or clean(r["SPECIES"], 32)
        info = [clean(r["DESCRIPTION"], 80), clean(r["MEMO"], 80)]
        weight = to_dec(r["WEIGHT"])
        out.append({
            "pet_name": clean(r["FULLNAME"], 16) or "未命名",
            "pet_variety": variety,
            "pet_sex": sex,
            "pet_birthday": birthday.date() if birthday else None,
            "pet_weight": weight if weight and weight > 0 else None,
            "pet_info": clean("；".join([x for x in info if x]), 255),
            "pet_sterilization": neutered,
            "pet_alive": 1,
            "pet_note": clean(r["MEMO"], 255),
            "pet_source": 1,
            "active": 1,
            "legacy_pet_no": legacy_no,
            "legacy_member_no": member_no,
            "create_time": birthday or datetime.now(),
        })
    rep.rows = out
    return out


# ---------------------------------------------------------------- 写库（upsert）
CLIENT_SQL = (
    "INSERT INTO client (client_username, client_password, client_name, client_gender,"
    " client_tel, client_tel2, client_birthday, client_info, client_address, client_email,"
    " client_id_card, client_level, client_source, active, audit_status, create_time, legacy_member_no)"
    " VALUES (%(client_username)s,%(client_password)s,%(client_name)s,%(client_gender)s,"
    " %(client_tel)s,%(client_tel2)s,%(client_birthday)s,%(client_info)s,%(client_address)s,"
    " %(client_email)s,%(client_id_card)s,%(client_level)s,%(client_source)s,%(active)s,"
    " %(audit_status)s,%(create_time)s,%(legacy_member_no)s)"
    " ON DUPLICATE KEY UPDATE client_name=VALUES(client_name), client_tel=VALUES(client_tel),"
    " client_gender=VALUES(client_gender), client_info=VALUES(client_info),"
    " client_birthday=VALUES(client_birthday), client_level=VALUES(client_level)"
)

PET_SQL = (
    "INSERT INTO pet (pet_name, pet_variety, pet_sex, pet_birthday, pet_weight, pet_info,"
    " pet_sterilization, pet_alive, pet_note, pet_source, active, client_id, create_time, legacy_pet_no)"
    " VALUES (%(pet_name)s,%(pet_variety)s,%(pet_sex)s,%(pet_birthday)s,%(pet_weight)s,%(pet_info)s,"
    " %(pet_sterilization)s,%(pet_alive)s,%(pet_note)s,%(pet_source)s,%(active)s,"
    " (SELECT client_id FROM client WHERE legacy_member_no=%(legacy_member_no)s),%(create_time)s,"
    " %(legacy_pet_no)s)"
    " ON DUPLICATE KEY UPDATE pet_name=VALUES(pet_name), pet_variety=VALUES(pet_variety),"
    " pet_sex=VALUES(pet_sex), pet_birthday=VALUES(pet_birthday), pet_weight=VALUES(pet_weight),"
    " pet_info=VALUES(pet_info), pet_sterilization=VALUES(pet_sterilization),"
    " client_id=VALUES(client_id)"
)


def apply_clients(rows, rep):
    conn = mysql_conn()
    try:
        with conn.cursor() as cur:
            # 已存在的迁移客户先按 legacy 键更新，不存在则插入；逐行更好定位失败行
            ok = 0
            for r in rows:
                try:
                    cur.execute(CLIENT_SQL, r)
                    ok += 1
                except Exception as e:  # noqa: BLE001
                    rep.skip(r["legacy_member_no"], f"写入失败: {str(e)[:80]}")
            conn.commit()
            rep.applied = ok
    finally:
        conn.close()


def apply_pets(rows, rep):
    conn = mysql_conn()
    try:
        with conn.cursor() as cur:
            # 先确认客户全部落库（否则子查询得到 NULL → client_id NOT NULL 报错）
            cur.execute("SELECT legacy_member_no FROM client WHERE legacy_member_no IS NOT NULL")
            have = {x["legacy_member_no"] for x in cur.fetchall()}
            ok = 0
            for r in rows:
                if r["legacy_member_no"] not in have:
                    rep.skip(r["legacy_pet_no"], f"客户 {r['legacy_member_no']} 未迁移，跳过")
                    continue
                try:
                    cur.execute(PET_SQL, r)
                    ok += 1
                except Exception as e:  # noqa: BLE001
                    rep.skip(r["legacy_pet_no"], f"写入失败: {str(e)[:80]}")
            conn.commit()
            rep.applied = ok
    finally:
        conn.close()


def main():
    apply_mode = parse_arg_mode(sys.argv)

    rc = EtlReport("emember → client")
    clients = build_clients(rc)
    if apply_mode:
        apply_clients(clients, rc)
    rc.dump()

    rp = EtlReport("epet → pet")
    pets = build_pets(rp, {c["legacy_member_no"] for c in clients})
    if apply_mode:
        apply_pets(pets, rp)
    rp.dump()

    if not apply_mode:
        print("以上为 DRY-RUN（未写库）。加 --apply 执行写入。")


if __name__ == "__main__":
    main()
