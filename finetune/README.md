# Fine-tuning Demo Pipeline

这个目录用于学习“微调/二次训练”的最小实践流程。它不会直接训练模型，而是先建立微调前最重要的三个习惯：

1. 明确模型应该学会的行为。
2. 把业务样例整理成可训练的数据格式。
3. 在训练前先做数据校验和基线评测。

## 目录结构

```text
finetune/
├── data/
│   ├── raw/
│   │   └── loan_assistant_examples.jsonl
│   ├── eval/
│   │   └── eval_prompts.jsonl
│   └── processed/
│       ├── train.jsonl
│       └── validation.jsonl
└── scripts/
    ├── build_sft_dataset.py
    ├── validate_sft_dataset.py
    └── baseline_eval_template.py
```

`processed/` 目录由脚本生成，不需要手工维护。

## 数据格式

原始样例位于 `data/raw/loan_assistant_examples.jsonl`。每一行是一个 JSON 对象：

```json
{
  "id": "loan_tool_003",
  "tags": ["tool-boundary", "customer-data"],
  "system": "你是一个合规、谨慎、专业的金融贷款助手。",
  "user": "帮我查一下 LN001 这笔贷款还剩多少钱没还。",
  "assistant": "这属于具体贷款数据查询，不能凭空判断..."
}
```

转换后的 SFT 样例采用通用 chat JSONL 格式：

```json
{
  "id": "loan_tool_003",
  "messages": [
    {"role": "system", "content": "..."},
    {"role": "user", "content": "..."},
    {"role": "assistant", "content": "..."}
  ],
  "metadata": {
    "source": "loan_assistant_examples",
    "tags": ["tool-boundary", "customer-data"]
  }
}
```

这个格式可以继续适配云厂商 fine-tuning API，也可以作为本地 LoRA/QLoRA 训练前的数据源。

## 运行步骤

### 1. 构建 SFT 数据集

```bash
python3 finetune/scripts/build_sft_dataset.py
```

输出：

```text
finetune/data/processed/train.jsonl
finetune/data/processed/validation.jsonl
```

默认使用最后 20% 样例作为验证集，保证每次运行切分结果一致。

### 2. 校验数据质量

```bash
python3 finetune/scripts/validate_sft_dataset.py
```

校验内容包括：

- JSONL 格式是否正确。
- 每条样例是否包含 `system -> user -> assistant` 三段消息。
- 内容是否为空。
- 角色分布和长度统计。
- 用户问题与助手回答是否重复。
- 是否出现疑似手机号、身份证号、API key 等敏感内容。

### 3. 跑基线评测模板

默认 dry run，不调用任何模型服务：

```bash
python3 finetune/scripts/baseline_eval_template.py
```

如果后续要调用 OpenAI-compatible API：

```bash
export OPENAI_API_KEY=your-key
export OPENAI_BASE_URL=https://api.example.com/v1
export OPENAI_CHAT_MODEL=your-model
python3 finetune/scripts/baseline_eval_template.py --call-api
```

输出文件：

```text
finetune/data/eval/baseline_results.jsonl
```

微调后可以用相同的 `eval_prompts.jsonl` 再跑一次，把两个结果放在一起人工比较。

## 和当前 Spring AI 项目的关系

当前项目的主链路仍然是 Spring Boot + Spring AI 推理应用：

```text
Controller -> Service -> ChatClient -> ChatModel
```

微调实验目录位于这条链路之外。它负责准备未来可能使用的 fine-tuned model。等模型训练完成后，才需要回到 `application-openai.yml` 或模型提供方配置中切换模型名称。

因此本目录先不修改 Java 运行代码，也不要求 GPU、PyTorch 或真实 API Key。

## 继续扩展

完成本目录的实验后，可以继续选择：

- 接云端 fine-tuning API：上传 `train.jsonl` 和 `validation.jsonl`，训练完成后在应用配置中切换模型。
- 接本地 LoRA/QLoRA：引入 `transformers`、`datasets`、`peft`，训练 adapter 并通过本地推理服务暴露 OpenAI-compatible API。
- 增加人工评测表：对 `baseline_results.jsonl` 逐条打分，比较微调前后的格式遵循率、拒答正确率和幻觉率。
