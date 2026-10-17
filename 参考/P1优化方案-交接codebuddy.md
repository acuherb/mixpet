# P1 优化方案 · MixPet 宠物医院管理系统

> 写给 codebuddy 的执行文档。本文讲 P1（病历打印/PDF 导出 + 商品批量入库）。
> 参考系统「小弗宠物医院管理系统」教程为对标目标。
> **重要：P1 两项前端大体已实现，本文是「现状核对 + 缺口增强」，不是从零开发。**

---

## 0. 现状核对（务必先读）

| 子项 | 现状 | 参考目标 | 缺口判断 |
|---|---|---|---|
| 病历打印多模板 | ✅ 已有：打印预览弹窗 `d-appoint.vue` 三模板宫格（国标病历 std / 彩色详情版 color / 处方笺 rx），iframe 实时预览；独立打印页 `d-recordPrint.vue`（std/color 两模板，黑白国标，含辅助检查段） | 黑白病历、处方笺、国标模板、彩色详情版（4 类） | **基本达成**，缺「黑白简化(单色针打友好)」模板 |
| PDF 导出 | ⚠️ 仅「打印 → 目标另存为 PDF」（浏览器行为），`d-appoint.vue` 与 `d-recordPrint.vue` 均有提示文案 | 一键导出 PDF 电子病历 | **无真正的 .pdf 文件下载**，是主要缺口 |
| Excel 批量入库 | ✅ 已有 `y-drugInbound.vue`：从 Excel 复制 5 列(药品名/数量/批号/有效期/备注) 粘贴→自动匹配药品→可逐行改→**串行逐条调用单条接口** `POST /drug/inbound`（`reqDrugInbound`）入库 | Excel 批量入库 | **基本达成**，但为「粘贴+多次请求」；后端无 batch 接口、不支持 .xlsx 文件上传 |
| 后端批量接口 | ❌ 仅有单条 `POST /drug/inbound`（`DrugController` 第 93 行），无 batch | 一次性批量写入更高效 | 真正缺口 |

---

## 1. P1-1 病历打印/PDF：补「真实 PDF 下载」+「黑白简化模板」

### 1.1 现有实现（确认可用，勿重写）

- 打印预览弹窗：[d-appoint.vue](ship-ui/src/views/doctorPage/d-appoint.vue#L384-L405)（`printTpls` 数组 + `printSrc` computed + `doFramePrint`）；
- 独立打印页：[d-recordPrint.vue](ship-ui/src/views/doctorPage/d-recordPrint.vue#L7-L40)（`style` 为 `std`/`color`，`doPrint` 调系统打印）。

### 1.2 增强 A：一键下载真实 PDF（主要缺口，推荐做）

不依赖浏览器「另存为」，增加一个「导出 PDF 下载」按钮，任选方案：

- **方案一（前端，最快）**：以 `d-recordPrint.vue` 为标准，引入 `html2pdf.js`（或 jspdf+html2canvas）：
  ```ts
  import html2pdf from 'html2pdf.js'
  const exportPdf = (): void => {
    const el = document.querySelector('.rp-sheet')
    html2pdf().set({ margin: 8, filename: `病历-No.${full.record.recordId}.pdf`, image: {type: 'jpeg', quality: 0.95}, html2canvas: {scale: 2}, jsPDF: {unit: 'mm', format: 'a4', orientation: 'portrait'} }).from(el).save()
  }
  ```
  注意：打印页本身用了 `@media print` 的 `.no-print` 隐藏工具栏与 `.rp-page` 分页，导出时需临时给 `html2canvas` 忽略 `.no-print` 或有独立隐藏的只导出容器。
- **方案二（后端，服务端 PDF）**：Controller 生成 PDF 字节流（iText/Flying-Saucer 或后端模板），`response` 写流下载。成本高，建议排在方案一之后作为二期。

### 1.3 增强 B：黑白简化模板（对齐「黑白病历」四类目标）

在 `printTpls`（`d-appoint.vue`）数组里新增一项 `mono`「黑白简化」；并在 `d-recordPrint.vue` 的 `style` radio 加 `mono`：

```ts
{ style: 'mono', name: '黑白简化', color: '#7f8c98', desc: '单色紧凑，适合针打/纸质归档', path: '/#/doctorPage/d-recordPrint?recordId=...&style=mono' }
```

实现要点：`.rp-sheet.mono` 里 `filter: grayscale(1)` + 更小的字号/更紧凑表格边距，去掉背景色强调，保持信息完整（同 std 字段）。

### 1.4 验收

1. 「导出 PDF」按钮能直接下载 `.pdf`（不是让用户在打印对话框手动选另存为）；
2. 下载的 PDF 排版与 `d-recordPrint` 预览一致，A4、分页不出现表格被切断；
3. 黑白简化模板在普通屏与针打（用系统打印再验一次）均可用。

---

## 2. P1-2 商品批量入库：补「后端 batch 接口」+「.xlsx 文件上传」

### 2.1 现有实现（确认可用，勿重写）

- 页面 [y-drugInbound.vue](ship-ui/src/views/backstage/y-drugInbound.vue#L148-L158)：粘贴解析 + 逐条 `reqDrugInbound` 串行提交，成功/失败分行汇总，失败行保留继续改。可行但量大时请求多、无原子性。

### 2.2 增强 A：后端批量入库接口（推荐，最实在）

新增 `POST /drug/inbound/batch`，事务内逐条走现有 `drugService.inbound` 同等校验与库存/台账逻辑，返回**逐条结果**（部分成功仍给每个 drugId 的成功/失败 message）。

- Controller（`DrugController`）：
  ```java
  @PostMapping("/inbound/batch")
  public Result inboundBatch(@RequestBody List<InboundLine> lines) {
      if (CollectionUtils.isEmpty(lines)) return Result.error("入库明细不能为空");
      return drugService.inboundBatch(lines);
  }
  ```
- DTO `InboundLine`：`drugId, quantity, batchNo, expireDate, note, supplierId`。
- Service `inboundBatch`：`@Transactional`，循环调用现有 `inbound` 校验（药品存在、数量>0）+ `drugStock` 累加 + `drug_record` 台账；任一条失败 → 记入该行结果，不抛全盘回滚（除非你希望强一致，则整体回滚并返回首错）。返回 `Result(code=1, 成功N条/失败N条, 失败明细List)`。

### 2.3 增强 B：支持 .xlsx 文件上传解析（对齐「Excel 批量入库」字面体验）

在 `y-drugInbound.vue` 顶部加「上传 Excel」按钮 +「下载模板」：

- 方案一（后端解析）：`POST /drug/inbound/parse` 接收 MultipartFile(.xlsx/.xls)，用 EasyExcel/POI 读 5 列（药品名/数量/批号/有效期/备注），回传给前端复用现有表格进行匹配→确认→提交。优点：模板列规整、可做并发。
- 方案二（前端解析）：引入 SheetJS(`xlsx`)，`read` 后映射到 `rows`。实现更轻，但校验逻辑在前端。

重建模板（表头：药品名 | 数量 | 批号 | 有效期 | 备注），前端「下载模板」用现有 `exportCsv` 或服务端模板文件。

### 2.4 前端 submit 改为批量

`submitAll` 复用现有表格：
```ts
const res = await reqDrugInboundBatch(validRows.map(r => ({drugId: r.drugId!, quantity: r.quantity, batchNo: r.batchNo || undefined, expireDate: r.expireDate || undefined, note: r.note || undefined})))
```
`DrugApi.ts` 新增 `reqDrugInboundBatch = (data) => reqApi.post('drug/inbound/batch', data)`。

### 2.5 验收

1. 上传含 10 条正确行的 .xlsx → 全部入库，`drug_stock` 与 `drug_record` 逐条写入正确；
2. 混入 1 条 药品不存在/数量<=0 → 成功 9 条、该行返回明确错误，库不写坏；事务语义符合预期；
3. 粘贴/上传两条路径并存且行为一致；原有单条 `reqDrugInbound` 无回归。

---

## 3. 交付与验证规范（生产标准，沿用 P0）

- **隔离提交**：P1-1 与 P1-2 分两个 commit；SQL 若涉及（本功能基本无新表，`drug_record` 已存在）只需确保无 Schema 变更。
- **回滚底线**：batch 接口只做「循环复用现有 inbound 逻辑」，不重构原单条 `inbound` 校验/扣库存；前端逐条路径保留为备选。
- **命令**：后端 `mvn -q compile` + 重启；前端 `npm run build`（TS 类型校验）。
- **依赖确认**：html2pdf.js / xlsx(SheetJS) / EasyExcel 均为新依赖，需先在 `package.json` / `pom.xml` 确认未引入再新增；获批后再装。

---

## 4. 一句话交接给 codebuddy

「打印多模板和粘贴批量入库**已经做了**，别重做」。真正的缺口就两处补强：①给打印加**真实 PDF 下载**按钮 + 一个**黑白简化模板**；②给入库加**后端 batch 接口** + 可选 **.xlsx 文件上传**。改动集中在 `d-recordPrint.vue` / `d-appoint.vue` / `y-drugInbound.vue` 与 `DrugController` / `DrugService`。