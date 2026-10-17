# P2 优化方案 · MixPet 宠物医院管理系统 —— AI 辅助诊疗（MVP）

> 写给 codebuddy 的执行文档。P2 与 P0/P1 不同：**这是全新功能**，本项目当前无任何 AI 相关代码（`assistant` 命中项仅为手术"助手"字段）。
> 对标参考《小弗AI辅助诊疗教程》。本文先给 MVP 接口抽象 + 前端 UI + 合规留痕，**模型接入做 Provider 抽象，Demo 用假数据**，上线阶段再接真模型（如 DeepSeek）。

---

## 0. 现状核对

| 项 | 结论 |
|---|---|
| 是否已有 AI 相关实现 | ❌ 无（无 OpenAI/DeepSeek/智能诊疗类代码） |
| 是否已有病历聚合数据 | ✅ 已有 `RecordFullVo`（主诉/现病史/体征/诊断/检验异常/影像报告） |
| 是否已有宠物病史侧栏 | ✅ `d-appoint.vue` 已有「写病历时参考右侧病史历史」 |
| AI 能力 | 🆕 全新，需从 0 设计 |

---

## 1. 需求目标（来自参考教程）

AI 辅助诊疗是**基于病历已有信息**的增强，不是独立问答，用于：

1. **鉴别诊断**：按主诉/现病史/体征给出发病可能性排序的鉴别诊断清单；
2. **检查建议**：提示对当前病历最有价值的下一步检查（检验/影像）；
3. **指标解读**：结合实验室多项指标异常做关联解读（不只是单个指标超限）；
4. **治疗方案参考**：给出治疗方向/用药/护理的参考预案；
5. **宠主交代 + 复诊建议**：生成向宠主解释病情的文案与复诊计划。

**触发时机**：就诊信息（主诉/现病史/症状体征/化验）基本采集后由医生主动点击触发；同一就诊避免重复调用。

---

## 2. 整体设计

```
[医生] → 接诊弹窗 d-appoint.vue「AI 辅助」按钮
            │ 组装上下文(POST Body)
            ▼
   /ai/assist  (AiController)
            │
            ▼
   AiService.assist(ctx)  ──→  AiProvider 接口（Provider 抽象）
                                       ├─ MockAiProvider（MVP，返回演示 JSON，不联网）
                                       └─ DeepSeekProvider（上线期，可用 JDK HttpClient + jackson）
            │
            ▼
   解析为 AiAdviceVo（结构化 JSON）
            ▼
   成功后写 telemetry 表 ai_advice（留痕/防重复）
            ▼
   返回前端抽屉渲染（含「仅供参考」横幅 + 「一键填入」）
```

---

## 3. 后端（MVP 骨架）

### 3.1 数据表（增量 SQL：`db-backup/feature_20261017_ai_advice.sql`）

```sql
CREATE TABLE IF NOT EXISTS `ai_advice` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `record_id`   BIGINT       NULL COMMENT '关联病历',
  `provider`    VARCHAR(32)  NOT NULL DEFAULT 'mock' COMMENT '模型提供方 mock/deepseek',
  `request_hash`VARCHAR(64)  NULL COMMENT '入参摘要hash,用于防重复',
  `advice_json` TEXT         NULL COMMENT '结构化建议JSON',
  `doctor`      BIGINT       NULL COMMENT '触发医生',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  PRIMARY KEY (`id`),
  KEY `idx_record` (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI辅助诊疗留痕';
```

### 3.2 请求/响应模型

`AiContext`（入参）：主诉 `chiefComplaint`、现病史 `presentIllness`、既往史 `pastHistory`、**体征**（`weight/temperature/heartRate/respiratoryRate/bloodPressure`）、体格检查 `physicalExam`、已有诊断 `diagnosis`、**检验异常项** `labAbnormal: [{itemName, resultValue, unit, refRange}]`、影像报告 `imageReports: [text]`、宠物信息 `pet: {species, breed, sex, age, chip}`、医院名/医生名（用于措辞）。

`AiAdviceVo`（出参，结构化）：
```java
public class AiAdviceVo {
  private List<Pri> differentials;   // 鉴别诊断：{cond, prob, basis}
  private List<String> examSuggest;  // 建议检查
  private List<String> indexExplain; // 指标解读
  private List<String> treatRef;     // 治疗参考
  private String ownerNote;          // 向宠主说明文案
  private String revisitSuggest;     // 复诊建议
  public static class Pri { String cond; String prob; String basis; }
}
```

### 3.3 Provider 抽象 + Controller

```java
public interface AiProvider {
  String name();
  AiAdviceVo assist(AiContext ctx);   // 抛业务异常时由上层转为 Result.error
}

@Component
public class MockAiProvider implements AiProvider {
  public String name(){ return "mock"; }
  public AiAdviceVo assist(AiContext ctx){
    // MVP：基于 ctx 简单规则返回示例 JSON，无网络依赖，保证 CI/验收可跑
  }
}
// 上线期再补 DeepSeekProvider：JDK HttpClient POST /chat/completions，JSONPath 解析
```

```java
@RestController @RequestMapping("/ai")
public class AiController {
  @PostMapping("/assist")
  public Result assist(@RequestParam Integer recordId){
    // 1) getRecordFull(recordId) → AiService.buildContext(full)
    // 2) request_hash 命中 ai_advice 则直接返回缓存（防重复 & 省成本）
    // 3) provider.assist(ctx) → 写 ai_advice 留痕 → 返回 AiAdviceVo
    // 4) 失败返回 Result.error(友好文案)，不走全盘报错
  }
}
```

**安全**：模型 apiKey 读环境变量 `AI_API_KEY`（`application.yml` 不写死、不提交）；对输出 Content-Type 校验；异常降级为 mock，避免接口不可用时医生被卡。

---

## 4. 前端（d-appoint.vue 右侧抽屉）

1. 接诊弹窗工具栏加「AI 辅助」按钮 + `AiAdvice` 数据、loading；
2. 右侧 drawer：顶部 `el-alert type="warning"` **「本内容由 AI 生成，仅供参考，不作为诊断依据」**；
3. 用 `el-descriptions`/可折叠分组渲染：
   - 鉴别诊断表（条件/概率/依据）；
   - 建议检查（`tag` 列表）；
   - 指标解读（逐条）；
   - 治疗参考（checklist）；
   - 宠主说明文案 + 复诊建议（`textarea`，可编辑）；
4. **「一键填入」**：把 `ownerNote/revisitSuggest/treatRef` 回填到 `recordForm.advice(医嘱)` / `recordForm.revisitDate` 等对应字段（**医生确认后才生效，不自动覆盖已填内容**）；
5. 加`requesting`禁用态，避免连点；失败 `ElMessage.error`。

前端模型 `AiAdviceVo.ts` 与后端字段一一对应。

---

## 5. 合规与安全（必须）

- **「仅供参考，不作为诊断依据」**：每个进入入口 + 抽屉顶部都要有文案；`ai_advice` 表留痕（谁触发、何时、输入摘要、provider、生成内容）满足可追溯；
- **不接诊决策结构化影响**：AI 不写库、不改病情/处方，只有医生「一键填入」并经确认才进表单；
- apiKey 不入库、不日志；Provider 与业务解耦，方便替换模型与开关（可用 yml `ai.enabled` 一键关闭）。

---

## 6. 分阶段落地（建议）

| 阶段 | 内容 | 预估 |
|---|---|---|
| **Phase 1（MVP，本轮）** | 表 + `AiContext/AiAdviceVo` + `AiProvider` 接口 + `MockAiProvider` + Controller(带 hash 防重复 + 留痕) + 前端抽屉 UI + 一键填入 + 合规横幅 | 一次 commit 可交付 |
| **Phase 2（可选，另排期）** | 接 `DeepSeekProvider`（真实模型）、调 prompt 模板（鉴别/检查/解读/治疗/交代/复诊分节 JSON 输出）、加"复制全文/下载留档"、可对 `recordId` 直接生成宠物主与打印同款 PDF 版本 | 建议 P2 之后 |

> 建议本轮**只做 Phase 1**，保证「接口抽象 + 前端 UI + 合规留痕」闭环先跑通，模型随时可换。

---

## 7. 验收与交付规范

- **隔离提交**：只提交一个 P2 commit；SQL 先落 `db-backup/feature_20261017_ai_advice.sql` 再改代码。
- **必测**：mock provider 下 `/ai/assist?recordId=xxx` 返回结构化 JSON 并写留痕；同一 record 二次调用命中 hash 不再重复生成；前端抽屉渲染 + 一键填入正确、且不覆盖已填内容；关闭 `ai.enabled` 前端按钮置灰。
- **命令**：后端 `mvn -q compile` + 重启；前端 `npm run build`；用一条已有完整病历（含检验异常）作演示数据。
- **回滚底线**：AI 为旁路功能，停用开关绝不阻塞正常接诊/开方/结算。

---

## 8. 一句话交接给 codebuddy

「P2 是**新功能**，做**接口抽象的 MVP**即可」：后端建 `ai_advice` 表 + `AiProvider` 接口 + **Mock 实现**（模型接入留到 Phase 2）+ `/ai/assist` 接口（防重复 + 留痕）；前端接诊弹窗加右侧「AI 辅助」抽屉（渲染结构化建议 + 合规横幅 + 一键填入）。**本轮不接真实模型**，只保证抽象和链路闭环。