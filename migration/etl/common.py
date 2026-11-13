# -*- coding: utf-8 -*-
"""
migration/etl/common.py — PetSystem(MSSQL, docker 容器) → mixpet(MariaDB) ETL 公共层

设计约定（step 2 拍板）：
- MSSQL 读：docker exec mssql-pets sqlcmd 输出 TSV（零编译依赖，pymssql 在 py3.13 无 wheel）
- MariaDB 写：pymysql（--break-system-packages --user 安装）
- 双模式：dry-run（默认，只读+统计+抽样，不写库） / --apply（真实写入）
- 每个转换器必须输出对账小节：源行数 / 目标行数 / 跳过明细，作为放行依据

用法：
    python3 etl_lab_ref.py            # dry-run
    python3 etl_lab_ref.py --apply    # 写库
"""
import os
import subprocess
import sys
import re
from datetime import datetime

PROJ = os.environ.get("MIXPET_PROJ", "/home/debian/公共/mixpet")

# ---------------------------------------------------------------- .env 解析
def load_env():
    env = {}
    path = os.path.join(PROJ, ".env")
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            k, v = line.split("=", 1)
            env[k.strip()] = v.strip()
    return env

ENV = load_env()

# ---------------------------------------------------------------- MSSQL 读
MSSQL_CONTAINER = os.environ.get("PET_MSSQL_CONTAINER", "mssql-pets")
MSSQL_USER = "sa"
MSSQL_PASSWORD = "Mixpet2026!Sql"
MSSQL_DB = "petsinit"
SEP = "\x01"  # 不可能出现于数据的分隔符

def mssql_query(sql: str):
    """执行 MSSQL 查询，返回 [dict]。列名取自结果集表头。"""
    cmd = [
        "docker", "exec", MSSQL_CONTAINER,
        "/opt/mssql-tools18/bin/sqlcmd",
        "-S", "localhost", "-U", MSSQL_USER, "-P", MSSQL_PASSWORD,
        "-d", MSSQL_DB, "-W", "-C", "-s", SEP,
        "-Q", "SET NOCOUNT ON;\n" + sql,
    ]
    out = subprocess.run(cmd, capture_output=True, text=True)
    if out.returncode != 0:
        raise RuntimeError(f"MSSQL 查询失败: {out.stderr[:500]}")
    lines = [ln for ln in out.stdout.splitlines() if ln.strip()]
    if len(lines) < 2:
        return []
    header = lines[0].split(SEP)
    rows = []
    for ln in lines[2:]:  # 跳过第2行 '----' 分隔线
        cols = ln.split(SEP)
        rows.append({h: (cols[i].strip() if i < len(cols) else "") for i, h in enumerate(header)})
    return rows

# ---------------------------------------------------------------- MariaDB 写
def mysql_conn(autocommit=False):
    import pymysql
    return pymysql.connect(
        host=ENV.get("DB_HOST", "localhost"),
        port=int(ENV.get("DB_PORT", "3306")),
        user=ENV["DB_USER"],
        password=ENV["DB_PASSWORD"],
        database=ENV.get("DB_NAME", "pet_mix"),
        charset="utf8mb4",
        autocommit=autocommit,
        cursorclass=pymysql.cursors.DictCursor,
    )

# ---------------------------------------------------------------- ETL 框架
class EtlReport:
    """对账报告：源行数/目标行数/跳过明细，迁移放行依据。"""
    def __init__(self, name: str):
        self.name = name
        self.source_rows = 0      # 源表候选行
        self.rows = []            # 将生成/已生成的目标行
        self.skipped = []         # (原因, 源标识) 跳过明细
        self.warnings = []        # 不阻塞但需人工确认项
        self.applied = None       # dry-run=None / apply=实际写入行数

    def skip(self, key, reason):
        self.skipped.append((key, reason))

    def warn(self, msg):
        self.warnings.append(msg)

    def dump(self):
        w = 78
        print("=" * w)
        print(f"ETL 对账报告: {self.name}   模式: {'DRY-RUN' if self.applied is None else 'APPLY'}")
        print(f"时间: {datetime.now():%Y-%m-%d %H:%M:%S}")
        print("-" * w)
        print(f"源候选行: {self.source_rows}")
        print(f"目标生成行: {len(self.rows)}" + (f"   实际写入: {self.applied}" if self.applied is not None else ""))
        if self.skipped:
            print(f"跳过 {len(self.skipped)} 行:")
            for key, reason in self.skipped[:20]:
                print(f"   - [{key}] {reason}")
            if len(self.skipped) > 20:
                print(f"   ... 其余 {len(self.skipped) - 20} 条略")
        if self.warnings:
            print(f"⚠ 需人工确认 {len(self.warnings)} 项:")
            for m in self.warnings[:20]:
                print(f"   ! {m}")
        print("=" * w)


def parse_arg_mode(argv) -> bool:
    """返回 apply=True/False。"""
    return "--apply" in argv

def die(msg):
    print(f"❌ {msg}", file=sys.stderr)
    sys.exit(1)
