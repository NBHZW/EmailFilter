package com.zealsinger.interview_butler.ai

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import kotlinx.coroutines.runBlocking

class OpenRouterAI {
    fun getAnswer(question: String): String = runBlocking {
        val apiKey = "sk-f75e43bedf22426abd75c863c9239844"
        
        // 使用OpenRouter的DeepSeek模型
        val agent = AIAgent(
            executor = simpleOpenAIExecutor(apiKey),
            systemPrompt = "你是一个擅长于整理，提取和分析信息的助手，请利用我给你的信息按照我的要求进行整理，提取和分析，并输出整理后的内容",
            llmModel = LLModel(
                provider = LLMProvider.Alibaba,
                id = "qwen-flash", // 阿里云百炼平台的Flash模型ID
                capabilities = listOf(
                    LLMCapability.Completion,
                    LLMCapability.Tools,
                    // 根据需要添加其他能力
                ),
                contextLength = 131072, // Flash模型支持128K上下文
                maxOutputTokens = 8192  // 最大输出tokens
            )
        )
        
        // 发送消息并获取回复
        agent.run(question)
    }
}