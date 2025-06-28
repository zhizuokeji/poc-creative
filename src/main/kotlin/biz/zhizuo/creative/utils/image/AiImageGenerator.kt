package biz.zhizuo.creative.utils.image

import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * AI 图片生成器实现
 * 基于大模型生成图片（如 DALL-E、Midjourney 等）
 *
 * 注意：此实现需要额外的 Spring AI 图片生成依赖
 * 当前版本作为占位符实现，实际使用需要配置相应的图片生成服务
 */
@Component
@ConditionalOnProperty(name = ["image.generator.ai.enabled"], havingValue = "true", matchIfMissing = false)
class AiImageGenerator(
    private val chatClient: ChatClient,
) : ImageGenerator {

    private val logger = LoggerFactory.getLogger(AiImageGenerator::class.java)

    companion object {
        private const val PROMPT_OPTIMIZATION_SYSTEM_PROMPT = """
你是一个专业的AI图片生成提示词优化专家。你的任务是将用户的图片意图转换为高质量的图片生成提示词。

要求：
1. 提示词应该详细、具体、富有表现力
2. 包含风格、色彩、构图、光线等视觉元素
3. 使用英文输出，因为大多数图片生成模型对英文提示词响应更好
4. 避免包含人物面部特征的具体描述
5. 确保内容积极正面，符合安全准则

请将以下图片意图转换为优化的提示词：
"""
    }

    override fun generateImage(request: ImageGenerationRequest): ImageGenerationResult {
        val startTime = Instant.now()

        logger.info("开始使用AI生成图片: intent=${request.intent}")
        logger.warn("AI图片生成器当前为占位符实现，需要配置真实的图片生成服务")

        val duration = Instant.now().toEpochMilli() - startTime.toEpochMilli()

        return ImageGenerationResult.failure(
            errorMessage = "AI图片生成器当前为占位符实现，需要配置真实的图片生成服务",
            generatorType = ImageGeneratorType.AI_GENERATED,
            durationMs = duration
        )
    }

    override fun validateAvailability(): Boolean {
        // 当前实现为占位符，实际部署时需要配置真实的图片生成服务
        logger.warn("AI图片生成器当前为占位符实现，需要配置真实的图片生成服务")
        logger.warn("AI图片生成器不可用")
        return false
    }

    override fun getGeneratorType(): ImageGeneratorType {
        return ImageGeneratorType.AI_GENERATED
    }
}
