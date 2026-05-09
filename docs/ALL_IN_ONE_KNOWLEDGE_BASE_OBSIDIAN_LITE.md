---
title: AI Agent 学习地图（精简版）
aliases:
  - AI Agent 学习蓝图 Lite
  - AI Agent Knowledge Map
tags:
  - ai-agent
  - llm
  - rag
  - function-calling
  - obsidian
  - learning-map
source: /docs
---

# AI Agent 学习地图（精简版）

> [!abstract] 使用方式
> 这是一份“提纲式总览”，用于快速建立全局认知。  
> 先看本页建立框架，再按文末路径回到各专题文档深挖。

---

## 1. 项目一句话定位

这是一个面向 Java 开发者的 AI Agent 学习工程：  
**以 Chat 为入口，以 RAG + Function Calling 为能力核心，以可观测性为治理闭环，最终演进到 Agent Loop。**

---

## 2. 全局架构（6 层）

1. **交互层（UI / API）**
   - 页面：聊天、成本看板、表单 Demo
   - 接口：对话、检索、评估、成本统计

2. **编排层（Controller / Service）**
   - 接收请求并路由到 Chat、RAG、Function、Cost 模块

3. **智能层（LLM Runtime）**
   - `ChatClient` 执行推理
   - Prompt 注入、对话历史注入、工具调用触发

4. **知识层（RAG）**
   - 文档加载、切分、Embedding、检索、重排序、策略管道

5. **业务数据层（Domain / Persistence）**
   - 贷款、还款、风险评估等领域模型与仓储

6. **治理层（Observability / Optimization）**
   - 调用日志、成本统计、缓存策略、定时任务、评估指标

---

## 3. 四条核心链路（必须吃透）

### A. Chat 链路
用户输入 -> 会话管理 -> Prompt 组装 -> LLM -> 响应输出

### B. Stream（SSE）链路
用户输入 -> 流式推理 -> 分片推送 -> 前端实时渲染

### C. RAG 链路
查询预处理 -> 多路检索 -> 精排 -> 上下文注入 -> 生成回答

### D. Function Calling 链路
意图识别 -> 工具选择 -> 函数执行 -> 结构化结果 -> 自然语言回答

---

## 4. 能力模块地图（从基础到进阶）

### [M1] 基础对话（Chat Basics）
- 核心：`ChatClient`、消息角色、会话历史、SSE
- 目标：先把“稳定对话 + 多轮上下文”跑通

### [M2] 多模型接入（Multi-LLM）
- 核心：Provider 抽象、Profile 切换
- 目标：模型可替换，业务代码不感知底层供应商

### [M3] Prompt 工程（Prompt Engineering）
- 核心：系统提示词、模板外部化、热加载
- 目标：低成本提升回答质量和稳定性

### [M4] 工具调用（Function Calling）
- 核心：函数定义、参数 schema、结果结构化
- 目标：从“会聊”升级到“会做事”

### [M5] RAG 基础
- 核心：文档入库、Embedding、向量检索、阈值与 TopK
- 目标：让回答有事实依据，减少幻觉

### [M6] RAG 进阶
- 核心：Query Rewrite、Query Expansion、BM25、Hybrid、Rerank、Pipeline
- 目标：提升召回率、精确率和排序质量

### [M7] 成本与可观测性
- 核心：AOP 日志、Token 成本、缓存、多维统计
- 目标：把 Demo 变成可运营系统

### [M8] Agent Loop（下一阶段）
- 核心：Plan -> Act -> Observe -> Reflect -> Respond
- 目标：形成可扩展的自主执行闭环

---

## 5. 技术点速记（高频面试/实战）

1. **Spring AI 抽象价值**：统一不同 LLM 的调用方式
2. **Prompt 外部化价值**：快速迭代，不改代码逻辑
3. **RAG 本质**：检索系统 + 生成系统的组合
4. **Function Calling 本质**：LLM 决策，后端执行
5. **混合检索本质**：语义匹配（向量）+ 关键词匹配（BM25）互补
6. **Rerank 本质**：先粗召回，再精排序
7. **可观测性本质**：没有指标就无法优化

---

## 6. 常见误区（避坑）

- 只关注模型，不做上下文和会话管理
- 只做向量检索，不做评估与回归
- 函数返回长文本而非结构化数据
- 没有 traceId / 调用日志，排障困难
- 不做成本统计，后期优化无抓手
- 直接追求多 Agent，忽略单 Agent MVP

---

## 7. 学习与实践节奏（4 周）

### 第 1 周：打基础
- 快速启动 + 架构理解
- 聊天与流式响应跑通

### 第 2 周：提质量
- 多模型切换
- Prompt 管理与 A/B 对比

### 第 3 周：上能力
- Function Calling
- RAG 基础

### 第 4 周：做优化
- RAG 进阶策略
- 成本治理与可观测性

---

## 8. 当前项目成熟度定位

> [!success] 你现在的状态
> 已具备：Chat + RAG + Function + Cost 的完整学习闭环，  
> 可以正式进入 Agent Loop MVP 实现阶段。

建议下一步：
1. 建立 `agent` 包和最小闭环（Plan/Act/Respond）
2. 接入 `rag.search` + `loan.query` 两个工具
3. 为 Agent 引入 trace 和评估样本集

---

## 9. 文档导航（精简阅读顺序）

1. [[00-ai-agent-system-blueprint]]
2. [[01-quick-start]]
3. [[02-chat-basics]]
4. [[03-multi-llm]]
5. [[04-prompt-engineering]]
6. [[05-function-calling]]
7. [[06-rag-basics]]
8. [[07-rag-advanced]]
9. [[08-cost-and-observability]]
10. [[10-agent-loop-design]]
11. [[AI_LEARNING_PATH]]

---

## 10. 一页总结

AI 应用工程的核心不是“单点技术”，而是“系统协同”：
- **Chat** 提供交互入口
- **RAG** 提供知识依据
- **Function** 提供执行能力
- **Observability** 提供优化抓手
- **Agent Loop** 提供自主闭环

把这条主线吃透，你就具备了从“会用大模型”到“会做 AI 系统”的关键跨越。
