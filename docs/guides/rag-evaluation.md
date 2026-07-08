# RAG 效果评估指南

本文档说明如何使用 Java-native 的 RAGAS-like 评估流程，对当前项目的统一 RAG 链路做离线效果回归。

## 1. 为什么不用 Python RAGAS

RAGAS 官方生态主要在 Python，适合已经有 Python 数据评估平台的团队。本项目主栈是 Spring Boot + Spring AI，生产 RAG 入口已经统一为 `RagFacade`，因此默认采用 Java-native 方案：

- 评估代码直接复用 `RagFacade.retrieve()`，避免绕过生产检索链路。
- 评测集、导出、评分和报告都能在 Maven/JUnit/CI 体系内运行。
- 不额外引入 LangChain4j 或 Python 运行时。

当前实现提供的是 RAGAS-like 确定性基线指标。若后续需要 LLM-as-judge，可在 `RagasLikeMetricCalculator` 之上接入 Spring AI `Evaluator` 或自定义 judge prompt。

## 2. 文件结构

```text
eval/ragas/
├── datasets/
│   └── financial_rag_eval.jsonl
└── output/
    ├── questions_answers_contexts.jsonl
    └── summary.md
```

核心 Java 类：

| 类 | 说明 |
|---|---|
| `RagasEvalSample` | 评测集样本：`id/question/ground_truth/reference_contexts` |
| `RagasEvaluationExporter` | 调用 `RagFacade`，导出 `question/answer/contexts/ground_truth` |
| `RagasLikeMetricCalculator` | 计算 `context_precision/context_recall/faithfulness` 等指标 |
| `RagasEvalReportWriter` | 生成 Markdown 汇总报告 |
| `RagasEvaluationRunner` | `eval` profile 下的离线评估入口 |

## 3. 评测集格式

评测集使用 JSONL，每行一条样本：

```json
{"id":"loan_001","question":"普通客户、黄金客户和白金客户分别怎么划分？","ground_truth":"普通客户为历史借款次数小于 5 次，黄金客户为 5 到 10 次以内，白金客户为历史借款次数大于等于 10 次。","reference_contexts":["普通客户：历史借款次数 < 5 次。黄金客户：5 <= 历史借款次数 < 10 次。白金客户：历史借款次数 >= 10 次。"]}
```

字段说明：

- `question`：评估问题。
- `ground_truth`：期望答案，用于回答正确性和相关性评估。
- `reference_contexts`：人工标注的关键证据，用于检索召回评估。

## 4. 运行评估

先确保 MySQL、Milvus 和模型环境变量可用，并且知识库已完成向量化入库。

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=openai,eval
```

默认输出：

- `eval/ragas/output/questions_answers_contexts.jsonl`
- `eval/ragas/output/summary.md`

常用覆盖参数：

```bash
RAGAS_TOP_K=3 \
RAGAS_GENERATE_ANSWER=true \
mvn spring-boot:run -Dspring-boot.run.profiles=openai,eval
```

如果只想评估检索质量，不调用 Chat 模型生成答案：

```bash
RAGAS_GENERATE_ANSWER=false \
mvn spring-boot:run -Dspring-boot.run.profiles=openai,eval
```

## 5. 配置项

配置位于 `app.ragas.*`：

| 配置 | 默认值 | 说明 |
|---|---|---|
| `dataset-path` | `eval/ragas/datasets/financial_rag_eval.jsonl` | 评测集路径 |
| `output-jsonl` | `eval/ragas/output/questions_answers_contexts.jsonl` | 明细输出路径 |
| `output-summary` | `eval/ragas/output/summary.md` | Markdown 汇总报告路径 |
| `top-k` | `5` | 每条问题检索文档数 |
| `similarity-threshold` | `0.2` | 评估请求保留参数；当前生产 pipeline 尚未完整透传 |
| `generate-answer` | `true` | 是否调用 Chat 模型生成答案 |

## 6. 指标含义

| 指标 | 含义 |
|---|---|
| `context_precision` | 检索到的上下文中，有多少与问题、黄金答案或参考证据相关 |
| `context_recall` | 人工标注的参考证据被检索结果覆盖的比例 |
| `context_relevancy` | 最相关上下文与评测目标的相似度 |
| `faithfulness` | 回答内容被检索上下文覆盖的比例 |
| `answer_relevancy` | 回答与问题/上下文的相关程度 |
| `answer_correctness` | 回答与黄金答案的相似度 |
| `overall` | 以上指标的简单平均 |

这些指标是确定性文本基线，不等同于 Python RAGAS 的 LLM judge 结果。它们适合做配置变更前后的相对比较，例如对比 `topK=3/5/8`、是否启用 rerank、是否启用 query expansion。

## 7. 验证命令

评估模块的纯单元测试不启动 Spring 上下文、不连接数据库、不调用 LLM：

```bash
mvn test -Dtest=RagasEvalJsonlSupportTest,RagasLikeMetricCalculatorTest,RagasEvaluationExporterTest,RagasEvalReportWriterTest
```
