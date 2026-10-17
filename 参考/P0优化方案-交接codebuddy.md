# P0 优化方案 · MixPet 宠物医院管理系统

> 写给 codebuddy 的执行文档。本文只讲 P0（处方智能用量 + 病历增强），P1/P2 另开文档。
> 参考系统「小弗宠物医院管理系统」的实现目标，结合本仓库现状核对后的「补缺」清单。
> **最重要的一点：不要重做已有的东西，改动面极小、风险最低。**

---

## 0. 现状核对结论（务必先读，避免重复开发）

| 子项 | 数据库 | 前端 | 后端 | 结论 |
|---|---|---|---|---|
| 处方智能算量（按体重/固定剂量） | ✅ 列已在 `db-backup/feature_20261017_drug_dose_rule.sql`（`drug.dose_per_kg` / `drug.dose_fixed`） | ✅ 模型/表单/算量逻辑已写好 | ❌ 后端实体缺字段 | **唯一断点在后端实体** |
| 病历体征（体重/体温/心率/呼吸/分诊） | ✅ | ✅ `d-appoint.vue` 表单已有 | ✅ `MedicalRecord.java` 已实现 | 已完成 |
| 病历完整性自检（保存/完结校验） | — | ❌ | ❌ | **真正缺口** |
| 血压字段 | ❌ | ❌ | ❌ | 参考教程明确「体温心率呼吸血压体重」，缺血压 |

---

## 1. P0-1 处方智能用量：修后端断点（最关键，改动最小）

### 1.1 根因

接诊弹窗开方逻辑 `d-appoint.vue` 里的 `doseCalc()`（按体重/固定量自动填 `quantity`）依赖药品对象里的 `doseFixed` / `dosePerKg`。但后端实体 `Drug.java` **没有这两个字段**，导致：

- MyBatis-Plus 存库时，前端提交的 `dosePerKg/doseFixed` 被丢弃，DB 列一直为 null；
- 查询 `GET /drug` 时也不回传这两个字段，前端永远读不到。

于是 `doseCalc()` 永远走进不了算量分支（`d.doseFixed` / `d.dosePerKg` 恒为 `undefined`），功能静默失败。

### 1.2 改动步骤

**① 后端实体补字段** — `shipPet/src/main/java/com/ship/model/entity/Drug.java`

```java
/** 每kg体重单次用量(单位同药剂单位)，空=不自动算量 */
private BigDecimal dosePerKg;
/** 固定单次用量(与体重无关，优先)，空=不自动算量 */
private BigDecimal doseFixed;
```

字段名即下划线驼峰映射 `dose_per_kg` / `dose_fixed`，无需 XML。

**② 后端接口补非负校验** — `shipPet/src/main/java/com/ship/controller/DrugController.java` 的 `save()` / `update()`，仿照现有 `drugPrice` 校验写法：

```java
if (drug.getDosePerKg() != null && drug.getDosePerKg().compareTo(BigDecimal.ZERO) < 0)
    return Result.error("每kg单次用量不能为负数");
List.of(drug.getDoseFixed(), drug.getDosePerKg())
```

### 1.3 验收

1. `GET /drug` 返回体包含 `dosePerKg` / `doseFixed`；
2. 药品新建/编辑保存后能回读这两个值；
3. 给演示数据配一条非空的 `dose_per_kg`（或 `dose_fixed`），接诊选该药 → 自动弹 `doseTip` 提示并预填 `quantity`；
4. **不动现有成交/库存/退款计费链路**：`quantity` 仍是销售/扣库存单位，[MedicalRecordServiceImpl.saveRecord](shipPet/src/main/java/com/ship/service/impl/MedicalRecordServiceImpl.java) 的「校验库存 → 快照 → 计药费」逻辑保持不变。

### 1.4 可选增强（对照参考《处方用量自动计算》教程，可与前端一起做）

现有 `doseCalc()` 只把「单次剂量取整」当作 `quantity`，未实现参考中的：

- 按「单次 × 频次 × 天数」算总量；
- 片剂按规格含量自动换算数量；
- 液体药品按 mL 按实际用量销售。

若本轮要做，建议：

- 接诊处方行增加「频次（次/日）」「天数」两个输入：
  `quantity = ceil(单次剂量 × 频次 × 天数)`（仍作销售/扣库存单位）；
- [Prescription.java](shipPet/src/main/java/com/ship/model/entity/Prescription.java) 增加结构化字段 `dailyTimes / singleDose / singleDoseUnit`（供处方笺打印），不要把结构化信息塞进自由文本 `usageInfo`；
- 处方笺 `RxSheetVo` 与打印模板展示「单次剂量 × 频次」。

---

## 2. P0-2 病历增强：血压 + 完整性自检

体征主体已实现，这里只补参考里真正缺的「血压」和「完整性」。

### 2.1 血压字段

**① 增量 SQL**（新建 `db-backup/feature_20261017_medical_record_bp.sql`）：

```sql
ALTER TABLE `medical_record`
    ADD COLUMN `blood_pressure` varchar(16) NULL DEFAULT NULL COMMENT '血压(收缩/舒张,如120/80)' AFTER `respiratory_rate`;
```

**② 后端实体** — `MedicalRecord.java` 在 `respiratoryRate` 之后补：

```java
/** 血压(收缩/舒张,如 120/80) */
private String bloodPressure;
```

**③ 前端表单** — `ship-ui/src/views/doctorPage/d-appoint.vue` 呼吸（`respiratoryRate`）下方新增：

```html
<el-form-item label="血压(mmHg)">
  <el-input v-model="recordForm.bloodPressure" maxlength="16" placeholder="如：120/80" style="width: 150px"/>
</el-form-item>
```

**④ 病历详情/完整档案**：把 `bloodPressure` 与体重/体温/心率/呼吸一起渲染。

### 2.2 病历完整性自检（核心）

设计原则参照参考《病历书写教程》「完成前检查缺漏—前后呼应」：**保存软提示、完结硬校验**，不打断正常接诊开单。

**① 后端 `saveRecord()`（软提示不阻断）** — `MedicalRecordServiceImpl.saveRecord`：

- 不改变 `Result(code=1)` 成功路径；
- 在返回里附带 `warnings: List<String>`（或 Result 扩一个空数组字段），对下列为空者给出文案：
  - 主诉 `chiefComplaint`
  - 现病史 `presentIllness`
  - 既往史 `pastHistory`
  - 体格检查 `physicalExam`
  - 诊断 `diagnosis`
  - 体重 `weight`（体征项）

**② 后端完结接口（硬校验）** — 完结病历的接口逻辑中：

```java
if (StrUtil.isWhite(record.getChiefComplaint()) || StrUtil.isWhite(record.getDiagnosis()))
    return Result.error("主诉 / 诊断未填写，为避免病历不完整请先补全");
```

**③ 前端** — 接诊弹窗：

- 保存后将 `warnings` 渲染为 `el-alert type="warning"`；
- 完结按钮若收到 `Result.error` 直接 `ElMessage.error` 展示。

---

## 3. 交付与验证规范（生产标准）

- **隔离提交**：P0-1 与 P0-2 分两个 commit；SQL 变更必须先落 `db-backup/` 新脚本，再改代码。
- **回滚底线**：P0-1 是「加字段补断点」，不动现有成交/库存/退款计费逻辑；测试重点「算量只影响 `quantity` 预填，不影响后端校验与扣库存」。
- **命令**：后端 `mvn -q compile` + 重启；前端 `npm run build`（TypeScript typecheck 会拦截类型错误）。
- **演示数据**：给已配 `dose_per_kg` / `dose_fixed` 的药品留一条，确认接诊能自动算量。

---

## 4. 一句话交接给 codebuddy

「处方智能算量」别重写，去把后端 `Drug.java` 的两个字段补上，链路就通了；病历体征别重复，补「血压」+「完整性自检」即可。改动面最小、风险最低。