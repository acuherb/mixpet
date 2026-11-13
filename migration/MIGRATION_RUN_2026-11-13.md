# 生产数据迁移实跑记录（2026-11-13）

数据源：`/home/debian/下载/PetSystem/web/DatabaseBack/202603220943.bak`（297MB，2026-03-22 生产备份）
还原方式：docker `mssql-pets` 容器内 RESTORE 为 **petsprod** 库（不动 petsinit 模板库）

```bash
docker cp 202603220943.bak mssql-pets:/var/opt/mssql/backup/
docker exec mssql-pets sqlcmd -Q "RESTORE DATABASE petsprod FROM DISK=... WITH MOVE 'petsb' TO ..., MOVE 'petsb_log' TO ..."
```

## 一、源库数据画像

| 表 | 行数 | 说明 |
|---|---|---|
| epetcase | 12753 | 病历（C完成 11683 / P预约 1070） |
| epetcaseprescriptionitem | 51061 | 处方明细（主表 STATUS='F' 才有效） |
| epetcaseprescription | 14589 | 处方主表（F正常 14574） |
| epetjianyi | 4603 | **免疫/驱虫记录**（疫苗 3100 / 驱虫 1495） |
| epetjianyiitem | 8933 | 免疫清单 |
| epet | 4668 | 宠物 |
| emember | 3572 | 会员 |
| emember_dc / detail | 136 / 2549 | 储值卡 / 流水（充 48.6 万，消 47.7 万） |
| epetcaseinvoice | 31045 | 收费单（未迁，切换时点做未收款） |

## 二、已迁移结果（全量幂等，可重跑）

| 目标表 | 行数 | ETL | 幂等键 |
|---|---|---|---|
| client | 3572 | etl_master.py | legacy_member_no（唯一） |
| pet | 4666 | etl_master.py | legacy_pet_no（唯一） |
| medical_record | 12752 | etl_case.py | legacy_case_no（跳过已迁） |
| prescription | 51061 | etl_case.py | legacy_item_id（唯一，upsert） |
| quarantine_cert | 4594 | etl_case.py | legacy_no（唯一） |
| immune_record | 4213 | etl_immune.py | (pet_id, 接种时间, 药名) |
| client_balance_log | 34 条 / ￥9065 | etl_balance.py | MIGRATION_OPEN + client_id |

一致性核对：诊断 10640（=源 INIT_DIAG 10640）；状态 C11683/P1069（=源）；宠物挂接 12752/12752；
储值期初 ￥9065.00 = 卡汇总 ￥9065.00。

## 三、关键口径与取舍

1. **老系统"检疫单"实为免疫/驱虫记录**（JY_TYPE 疫苗/驱虫 + 说明 + 药品清单），
   新系统拆两处落库：`immune_record`（业务实体，可查可提醒）+ `quarantine_cert`（打印单据留痕）。
2. **病历医生归属**：老 DOCT_ID 是 4 位工号（0002/0010…），与 mixpet 医生账号无对应，
   统一挂默认医生（doctor_id=1），老工号原文写 internal_note，不造假归属。
3. **药品**：`drug_id=0`（药品库未迁），名称/规格/单位/单价全部落 prescription 快照列，
   处方笺打印与历史追溯完整；等 epbmaster→drug 迁移后用 PART_NUM 回填。
4. **老客户免审**：active=1 + audit_status=1；密码沿用系统默认 hash（老系统 PASSWORD 不可逆）。
5. **储值余额**：`emember_dc.DC_A..D` 恒为 100，是卡权益权重不是余额；
   真实余额按流水算（充值 − 其它），一人多卡聚合后写一条 MIGRATION_OPEN 期初。
6. **性别**：老库是复合值（雄 / 雌 / 雄(已去势) / 雌(已绝育)），按关键字判性并顺带置绝育标记（515 只）。

## 四、坑（后续 ETL 必看）

- `sqlcmd` 默认 `-w 80` 会把长列折行、字段里的 CR/LF 会把一条记录劈成多行，**必须**
  `-w 32767` 且对长文本用 `REPLACE(...CHAR(13),' ')...CHAR(10)...` 清洗（common.py `sq()`）。
- `-y` 与 `-W` 互斥，不能同时给。
- 处方明细 `item.CASE_ID` 有空值，需用 `ISNULL(NULLIF(i.CASE_ID,''), p.CASE_ID)` 兜底。
- `epetcaseprescriptionitem.DAYS` 是 nvarchar，有 `'4天'` 这类文本，要正则取数字。
- `pet_sex`/`immune_record.dose_cnt` 等列 NOT NULL 无默认，空值会整行写入失败。

## 五、待办

- epbmaster → drug（药品字典/价格/库存期末余量）
- epetcaseinvoice → bill（切换时点只迁未收款）
- ~~体检报告（ReportCheckup* 7 种）、B超/X光（ReportTypeB/X）打印页~~ ✅ 2026-11-13 已移植：mrt2json.py 转 11 个版式 JSON + z-mrtPrint.vue 通用渲染，详见 GAP_LIST「老版式打印页移植」段
