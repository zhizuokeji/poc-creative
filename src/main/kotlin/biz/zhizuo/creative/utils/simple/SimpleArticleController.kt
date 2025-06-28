package biz.zhizuo.creative.utils.simple

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

/**
 * 简化的文章生成控制器
 */
@RestController
@RequestMapping("/api/simple/articles")
class SimpleArticleController(
    private val articleCreator: SimpleArticleCreator,
) {

    private val logger = LoggerFactory.getLogger(SimpleArticleController::class.java)

    /**
     * 生成完整文章
     */
    @PostMapping("/create")
    fun createArticle(@RequestBody request: ArticleRequest): ArticleResult {
        logger.info("收到文章生成请求: ${request.originalIdea}")

        return try {
            articleCreator.createArticle(request)
        } catch (e: Exception) {
            logger.error("文章生成失败", e)
            throw e
        }
    }

    /**
     * 执行单个阶段（用于调试）
     */
    @PostMapping("/stages/{stageName}")
    fun executeStage(
        @PathVariable stageName: String,
        @RequestBody input: String,
    ): StageOutput {
        logger.info("执行单个阶段: $stageName")

        val output = when (stageName) {
            "creative" -> {
                val request = parseCreativeInput(input)
                articleCreator.executeCreativeStage(request.originalIdea, request.themeKeyword)
            }

            "topic" -> articleCreator.executeTopicStage(input)
            "outline" -> articleCreator.executeOutlineStage(input)
            "writing" -> articleCreator.executeWritingStage(input)
            "optimization" -> articleCreator.executeOptimizationStage(input)
            "image_planning" -> articleCreator.executeImagePlanningStage(input)
            "image_execution" -> articleCreator.executeImageExecutionStage(input)
            "completion" -> articleCreator.executeCompletionStage(input)
            else -> throw IllegalArgumentException("未知的阶段: $stageName")
        }

        return StageOutput(
            stageName = stageName,
            input = input,
            output = output
        )
    }

    /**
     * 解析创意阶段的输入
     */
    private fun parseCreativeInput(input: String): ArticleRequest {
        // 简单解析，实际可以用JSON
        val lines = input.lines()
        var originalIdea = ""
        var themeKeyword: String? = null

        for (line in lines) {
            when {
                line.startsWith("用户想法：") -> originalIdea = line.substring(5).trim()
                line.startsWith("主题词：") -> themeKeyword = line.substring(4).trim()
            }
        }

        return ArticleRequest(originalIdea, themeKeyword)
    }
}
