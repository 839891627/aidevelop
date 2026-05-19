# 11 - Fine-tuning 基础：从 RAG 应用到二次训练

## 1. 为什么在这个项目之后学习微调

前面的章节已经覆盖了大模型应用开发的主要推理时能力：

- **Prompt 工程**：通过系统提示词和 few-shot 示例约束模型行为。
- **RAG**：把企业知识库检索结果放进上下文，让模型基于事实回答。
- **Function Calling**：让模型调用贷款查询、还款查询、风险评估等业务工具。
- **Agent Loop**：把计划、工具调用、反思和最终回答串成多步任务流程。

这些能力都发生在**推理时**。模型本身的参数没有变化，只是我们在每次调用时提供了更多上下文、规则和工具。

Fine-tuning（微调）属于**训练时**能力。它用一批示例数据继续训练已有模型，让模型更稳定地学会某种回答风格、任务格式、行业表达或行为边界。

## 2. 一句话区分 Prompt、RAG、Agent 和 Fine-tuning

| 技术 | 改变什么 | 不改变什么 | 适合解决的问题 |
|------|----------|------------|----------------|
| Prompt | 单次请求的指令和约束 | 模型参数 | 角色、格式、简单规则 |
| RAG | 单次请求可见的外部知识 | 模型参数 | 私有知识、实时知识、可追溯问答 |
| Function Calling | 模型可调用的业务能力 | 模型参数 | 查数据库、算风险、执行动作 |
| Agent | 多步任务的执行流程 | 模型参数 | 规划、工具编排、复杂任务 |
| Fine-tuning | 模型参数或适配器权重 | 运行时知识库 | 稳定风格、领域表达、固定任务模式 |

本项目的 `AiModelConfig` 负责组装 `ChatClient`、系统提示词、工具和 Advisor。这说明当前代码主要在做推理时增强：

```text
ChatModel -> ChatClient -> System Prompt -> Tools -> Advisors -> Response
```

微调发生在这条链路之前：

```text
Base Model + Training Dataset -> Fine-tuned Model -> ChatModel -> ChatClient
```

## 3. 微调不是给模型“塞知识库”

这是学习微调时最容易混淆的点。

如果问题是“最新贷款产品利率是多少”“客户 CUST001 当前逾期几天”“某份合同第 5 条写了什么”，应该优先使用 RAG 或工具调用。因为这些信息会变化，需要可追溯，也不适合烘焙进模型参数。

如果问题是“希望模型永远按客服口吻回答”“希望风险解释固定包含风险点、依据、建议三段”“希望模型学会公司内部的问答格式和拒答边界”，才更接近微调的价值。

可以用一个判断规则：

```text
事实会频繁变化 -> RAG / 工具
行为模式需要稳定 -> Prompt / Fine-tuning
Prompt 很长且仍不稳定 -> 考虑 Fine-tuning
```

## 4. 常见微调类型

### 4.1 SFT：监督微调

SFT（Supervised Fine-tuning）是最常见的入门微调方式。数据通常是一组“用户输入 -> 理想回答”的样例。

Chat 模型常见格式：

```json
{
  "messages": [
    {"role": "system", "content": "你是一个合规、谨慎的金融贷款助手。"},
    {"role": "user", "content": "客户逾期 20 天该怎么处理？"},
    {"role": "assistant", "content": "该客户处于 M1 阶段，建议先电话提醒..."}
  ]
}
```

SFT 的核心不是样本越多越好，而是样本是否稳定、覆盖是否合理、理想回答是否真的代表你想要的行为。

### 4.2 LoRA / QLoRA：低成本适配

LoRA 不直接更新大模型的全部参数，而是在部分层旁边训练很小的适配器权重。训练完成后，基础模型保持不变，额外加载 LoRA adapter。

QLoRA 在 LoRA 基础上进一步使用量化技术，降低显存占用，适合本地实验。

可以把它理解成：

```text
完整微调：修改整本教材
LoRA：给教材贴一组可拆卸的专业批注
```

### 4.3 Preference Tuning、DPO、RLHF

这类方法不只告诉模型“标准答案是什么”，还告诉模型“两种回答中哪个更好”。它们更适合优化主观偏好、安全边界、对齐风格。

入门阶段先掌握 SFT，再理解偏好优化即可。

### 4.4 Embedding 模型微调

Embedding 模型微调不是让 Chat 模型更会回答，而是让向量空间更符合你的领域检索需求。例如在金融贷款语料中，让“M1 阶段”和“逾期 1-30 天”更接近。

本项目目前使用 Ollama `nomic-embed-text` 做 RAG embedding。如果未来发现检索召回长期受领域术语影响，可以再考虑 embedding 模型微调。

## 5. 微调项目的最小流水线

一个健康的微调实验通常不是从训练脚本开始，而是从评测和数据开始：

```text
定义目标 -> 准备样例 -> 校验数据 -> 跑基线 -> 训练 -> 对比评测 -> 决定是否上线
```

本项目新增的 `finetune/` 目录只做前三步和评测模板：

1. `data/raw/loan_assistant_examples.jsonl`：原始业务样例。
2. `scripts/build_sft_dataset.py`：转换为 chat SFT JSONL。
3. `scripts/validate_sft_dataset.py`：检查格式、长度、重复和潜在泄漏。
4. `data/eval/eval_prompts.jsonl`：固定评测问题。
5. `scripts/baseline_eval_template.py`：后续接云端或本地模型时的评测模板。

这样做的原因是：微调失败大多不是训练命令写错，而是数据目标不清、样本质量差、没有基线评测。

## 6. 数据设计：贷款助手应该学什么

以当前项目的金融贷款助手为例，微调样本应该优先覆盖以下行为：

| 行为 | 示例目标 |
|------|----------|
| 回答结构稳定 | 风险解释固定包含“风险判断、依据、建议动作” |
| 语气稳定 | 专业、克制、不夸大、不承诺审批结果 |
| 边界清晰 | 不编造客户数据，不替代人工审批 |
| 工具意识 | 涉及具体贷款编号时提醒需要查询系统 |
| RAG 意识 | 涉及制度条款时说明应以知识库或正式制度为准 |

不要把大量具体客户信息写进训练集。真实客户数据应该通过数据库工具或 RAG 获取，而不是放进模型参数。

## 7. 评测思维：先有基线，再谈微调

本项目已有 `RagEvaluationService`，它用 Recall、Precision、F1、MRR、NDCG 评估检索效果。微调评测也要有同样的思想：先定义固定问题，再比较改动前后的表现。

微调评测常见维度：

- **格式遵循率**：是否稳定按指定结构回答。
- **拒答正确率**：遇到越权、未知、隐私问题时是否拒绝。
- **事实幻觉率**：是否编造客户编号、审批结果、制度条款。
- **领域表达一致性**：是否使用正确术语解释风险。
- **人工偏好评分**：业务人员更偏好哪个回答。

如果没有评测集，就无法判断微调到底是提升了模型，还是只是让模型在训练样本上背答案。

## 8. 什么时候不该微调

以下情况通常不建议优先微调：

1. 只是要加入新知识。优先 RAG。
2. 只是要查实时数据。优先 Function Calling。
3. 只是回答格式偶尔不稳。先优化 Prompt 和 few-shot。
4. 数据少、脏、目标不清。先做数据治理。
5. 没有评测集。先建立 baseline eval。

## 9. 动手实验

### 实验 1：构建 SFT 数据集

```bash
python3 finetune/scripts/build_sft_dataset.py
```

生成：

```text
finetune/data/processed/train.jsonl
finetune/data/processed/validation.jsonl
```

### 实验 2：校验数据集

```bash
python3 finetune/scripts/validate_sft_dataset.py
```

观察输出中的样本数量、角色分布、长度统计、重复样本和潜在泄漏提示。

### 实验 3：阅读评测模板

```bash
python3 finetune/scripts/baseline_eval_template.py --help
```

这个脚本默认不要求你配置 API Key。后续如果要接云端 fine-tuning 或本地 LoRA，可以把相同的 eval prompts 用在微调前后，比较模型行为变化。

## 10. 下一步方向

掌握本章后，可以继续选择两条路线：

- **云端 API 微调**：把 `train.jsonl` 上传到支持 fine-tuning 的 OpenAI-compatible 平台，训练完成后在 `application-openai.yml` 中切换模型名。
- **本地 LoRA/QLoRA**：引入 Python、Transformers、PEFT、datasets 和可用 GPU，训练 adapter，再通过本地推理服务暴露 OpenAI-compatible API。

无论选择哪条路线，都应该保持同一个原则：

```text
先明确目标和评测，再准备数据，最后才训练。
```
