package biz.zhizuo.creative.utils.simple

import biz.zhizuo.creative.utils.image.ImageGeneratorFactory
import biz.zhizuo.creative.utils.workflow.ImageProcessor
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.regex.Pattern

/**
 * 简化的文章生成器
 * 直接由代码驱动8个步骤的流程，无复杂的工作流概念
 */
@Component
class SimpleArticleCreator(
    private val chatClient: ChatClient,
    private val promptLoader: SimplePromptLoader,
    private val imageGeneratorFactory: ImageGeneratorFactory,
    private val imageProcessor: ImageProcessor,
) {

    private val logger = LoggerFactory.getLogger(SimpleArticleCreator::class.java)

    companion object {
        private const val WORKSPACE_ARTICLES_DIR = "workspace/articles"
        private val IMAGE_COMMENT_PATTERN = Pattern.compile(
            "<!--\\s*配图点ID:\\s*([^\\n]+)\\s*类型:\\s*([^\\n]+)\\s*描述:\\s*([^\\n]+)\\s*关键词:\\s*([^\\n]+)\\s*风格:\\s*([^\\n]+)\\s*-->",
            Pattern.DOTALL
        )
    }

    /**
     * 生成完整文章 - 一键执行所有8个步骤
     */
    fun createArticle(request: ArticleRequest): ArticleResult {
        logger.info("开始生成文章，原始想法: ${request.originalIdea}")

        // 1. 创意阶段
        val expandedIdea = executeCreativeStage(request.originalIdea, request.themeKeyword)

        // 2. 选题阶段  
        val selectedTitle = executeTopicStage(expandedIdea)

        // 3. 构思阶段
        val outline = executeOutlineStage(selectedTitle)

        // 4. 写作阶段
        val article = executeWritingStage(outline)

        // 5. 优化阶段
        val optimizedArticle = executeOptimizationStage(article)

        // 6. 配图规划阶段
        val articleWithImagePlans = executeImagePlanningStage(optimizedArticle)

        // 7. 配图执行阶段
        val articleWithImages = executeImageExecutionStage(articleWithImagePlans)

        // 8. 完成阶段
        val finalArticle = executeCompletionStage(articleWithImages)

        // 保存文章文件
        val result = saveArticleFile(finalArticle)

        logger.info("文章生成完成: ${result.title}")
        return result
    }

    /**
     * 1. 创意阶段 - 扩展用户想法
     */
    fun executeCreativeStage(originalIdea: String, themeKeyword: String?): String {
        logger.info("执行创意阶段")

        val systemPrompt = promptLoader.loadPrompt("creative")
        val userInput = buildCreativeInput(originalIdea, themeKeyword)

        return callAI(systemPrompt, userInput)
    }

    /**
     * 2. 选题阶段 - 生成候选标题
     */
    fun executeTopicStage(expandedIdea: String): String {
        logger.info("执行选题阶段")

        val systemPrompt = promptLoader.loadPrompt("topic")
        return callAI(systemPrompt, expandedIdea)
    }

    /**
     * 3. 构思阶段 - 生成文章大纲
     */
    fun executeOutlineStage(selectedTitle: String): String {
        logger.info("执行构思阶段")

        val systemPrompt = promptLoader.loadPrompt("outline")
        return callAI(systemPrompt, selectedTitle)
    }

    /**
     * 4. 写作阶段 - 生成完整文章
     */
    fun executeWritingStage(outline: String): String {
        logger.info("执行写作阶段")

        val systemPrompt = promptLoader.loadPrompt("writing")
        return callAI(systemPrompt, outline)
    }

    /**
     * 5. 优化阶段 - 润色文章内容
     */
    fun executeOptimizationStage(article: String): String {
        logger.info("执行优化阶段")

        val systemPrompt = promptLoader.loadPrompt("optimization")
        return callAI(systemPrompt, article)
    }

    /**
     * 6. 配图规划阶段 - 设计配图方案
     */
    fun executeImagePlanningStage(article: String): String {
        logger.info("执行配图规划阶段")

        val systemPrompt = promptLoader.loadPrompt("image_planning")
        return callAI(systemPrompt, article)
    }

    /**
     * 7. 配图执行阶段 - 生成实际配图
     */
    fun executeImageExecutionStage(articleWithPlans: String): String {
        logger.info("执行配图执行阶段")

        val systemPrompt = promptLoader.loadPrompt("image_execution")
        val articleWithImageUrls = callAI(systemPrompt, articleWithPlans)

        // 处理图片生成
        return processImages(articleWithImageUrls)
    }

    /**
     * 8. 完成阶段 - 生成最终文章
     */
    fun executeCompletionStage(articleWithImages: String): String {
        logger.info("执行完成阶段")

        val systemPrompt = promptLoader.loadPrompt("completion")
        return callAI(systemPrompt, articleWithImages)
    }

    /**
     * 调用AI模型
     */
    private fun callAI(systemPrompt: String, userInput: String): String {
        return chatClient.prompt()
            .system(systemPrompt)
            .user(userInput)
            .call()
            .content() ?: ""
    }

    /**
     * 构建创意阶段的输入
     */
    private fun buildCreativeInput(originalIdea: String, themeKeyword: String?): String {
        return if (themeKeyword.isNullOrBlank()) {
            "用户想法：$originalIdea"
        } else {
            "用户想法：$originalIdea\n主题词：$themeKeyword"
        }
    }

    /**
     * 处理图片生成
     */
    private fun processImages(content: String): String {
        // 简化的图片处理逻辑
        return processImageComments(content)
    }

    /**
     * 处理图片注释，生成实际图片链接
     */
    private fun processImageComments(content: String): String {
        val matcher = IMAGE_COMMENT_PATTERN.matcher(content)
        var result = content

        while (matcher.find()) {
            val commentId = matcher.group(1)?.trim()
            val imageType = matcher.group(2)?.trim()
            val description = matcher.group(3)?.trim()
            val keywords = matcher.group(4)?.trim()
            val style = matcher.group(5)?.trim()

            if (keywords != null && description != null) {
                try {
                    // 使用图片生成器搜索图片
                    val imageGenerator = imageGeneratorFactory.getDefaultGenerator()
                    val request = biz.zhizuo.creative.utils.image.ImageGenerationRequest(
                        intent = description,
                        keywords = keywords.split(",").map { it.trim() },
                        style = when (style?.lowercase()) {
                            "illustration" -> biz.zhizuo.creative.utils.image.ImageStyle.ILLUSTRATION
                            "vector" -> biz.zhizuo.creative.utils.image.ImageStyle.VECTOR
                            else -> biz.zhizuo.creative.utils.image.ImageStyle.PHOTO
                        },
                        size = biz.zhizuo.creative.utils.image.ImageSize.MEDIUM
                    )

                    val generationResult = imageGenerator.generateImage(request)

                    if (generationResult.success && generationResult.imageInfo != null) {
                        val imageInfo = generationResult.imageInfo!!
                        val imageMarkdown = "![${description}](${imageInfo.standardUrl})"

                        // 在注释后添加图片链接
                        result = result.replace(
                            matcher.group(0),
                            "${matcher.group(0)}\n$imageMarkdown"
                        )

                        logger.info("成功生成图片: $description -> ${imageInfo.standardUrl}")
                    } else {
                        logger.warn("图片生成失败: $description - ${generationResult.errorMessage}")
                    }
                } catch (e: Exception) {
                    logger.error("处理图片注释失败: $description", e)
                }
            }
        }

        return result
    }

    /**
     * 保存文章文件
     */
    private fun saveArticleFile(content: String): ArticleResult {
        // 从内容中提取标题
        val title = extractTitleFromMarkdown(content)

        // 创建文章目录
        val articlesDir = Paths.get(WORKSPACE_ARTICLES_DIR)
        Files.createDirectories(articlesDir)

        val articleDir = articlesDir.resolve(title)
        Files.createDirectories(articleDir)

        // 创建图片目录
        val imagesDir = imageProcessor.createImageDirectory(articleDir)

        // 保存文章文件
        val articleFilePath = articleDir.resolve("$title.md")
        Files.writeString(articleFilePath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

        logger.info("文章文件已保存: $articleFilePath")

        return ArticleResult(
            title = title,
            content = content,
            filePath = articleFilePath.toString(),
            imagesDir = imagesDir.toString()
        )
    }

    /**
     * 从Markdown内容中提取标题
     */
    private fun extractTitleFromMarkdown(content: String): String {
        val lines = content.lines()
        for (line in lines) {
            if (line.startsWith("# ")) {
                return line.substring(2).trim()
            }
        }
        return "未命名文章_${System.currentTimeMillis()}"
    }
}
