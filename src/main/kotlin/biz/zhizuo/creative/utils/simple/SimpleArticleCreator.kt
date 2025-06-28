package biz.zhizuo.creative.utils.simple

import biz.zhizuo.creative.utils.image.ImageGenerationRequest
import biz.zhizuo.creative.utils.image.ImageGeneratorFactory
import biz.zhizuo.creative.utils.image.ImageSize
import biz.zhizuo.creative.utils.image.ImageStyle
import biz.zhizuo.creative.utils.workflow.ImageProcessor
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

/**
 * 简化的文章生成器
 * 直接由代码驱动8个步骤的流程，支持新的JSON数据格式
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
        private const val TEST_OUTPUT_DIR = "workspace/test-outputs"
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
        val outlineJson = executeOutlineStage(selectedTitle)

        // 4. 写作阶段
        val articleJson = executeWritingStage(outlineJson)

        // 5. 优化阶段
        val optimizedArticleJson = executeOptimizationStage(articleJson)

        // 6. 配图规划阶段
        val articleWithImagePlansJson = executeImagePlanningStage(optimizedArticleJson)

        // 7. 配图执行阶段（程序自动化）
        val articleWithImagesJson = executeImageExecutionStage(articleWithImagePlansJson)

        // 8. 完成阶段（程序自动化）
        val finalMarkdown = executeCompletionStage(articleWithImagesJson)

        // 保存文章文件
        val result = saveArticleFile(finalMarkdown)

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
     * 3. 构思阶段 - 生成文章大纲（JSON格式）
     */
    fun executeOutlineStage(selectedTitle: String): String {
        logger.info("执行构思阶段")

        val systemPrompt = promptLoader.loadPrompt("outline")
        val jsonOutput = callAI(systemPrompt, selectedTitle)

        // 验证JSON格式
        if (!JsonSchemaValidator.validateAndLog(jsonOutput, "构思", "outline")) {
            throw RuntimeException("构思阶段输出JSON格式验证失败")
        }

        // 保存YAML格式用于测试和人工检查
        saveStageOutputAsYaml("outline", jsonOutput)

        return jsonOutput
    }

    /**
     * 4. 写作阶段 - 生成完整文章（JSON格式）
     */
    fun executeWritingStage(outlineJson: String): String {
        logger.info("执行写作阶段")

        val systemPrompt = promptLoader.loadPrompt("writing")
        val jsonOutput = callAI(systemPrompt, outlineJson)

        // 验证JSON格式
        if (!JsonSchemaValidator.validateAndLog(jsonOutput, "写作", "article")) {
            throw RuntimeException("写作阶段输出JSON格式验证失败")
        }

        // 保存YAML格式用于测试和人工检查
        saveStageOutputAsYaml("writing", jsonOutput)

        return jsonOutput
    }

    /**
     * 5. 优化阶段 - 润色文章内容（JSON格式）
     */
    fun executeOptimizationStage(articleJson: String): String {
        logger.info("执行优化阶段")

        val systemPrompt = promptLoader.loadPrompt("optimization")
        val jsonOutput = callAI(systemPrompt, articleJson)

        // 验证JSON格式
        if (!JsonSchemaValidator.validateAndLog(jsonOutput, "优化", "article")) {
            throw RuntimeException("优化阶段输出JSON格式验证失败")
        }

        // 保存YAML格式用于测试和人工检查
        saveStageOutputAsYaml("optimization", jsonOutput)

        return jsonOutput
    }

    /**
     * 6. 配图规划阶段 - 设计配图方案（JSON格式）
     */
    fun executeImagePlanningStage(articleJson: String): String {
        logger.info("执行配图规划阶段")

        val systemPrompt = promptLoader.loadPrompt("image_planning")
        val jsonOutput = callAI(systemPrompt, articleJson)

        // 验证JSON格式
        if (!JsonSchemaValidator.validateAndLog(jsonOutput, "配图规划", "article")) {
            throw RuntimeException("配图规划阶段输出JSON格式验证失败")
        }

        // 保存YAML格式用于测试和人工检查
        saveStageOutputAsYaml("image_planning", jsonOutput)

        return jsonOutput
    }

    /**
     * 7. 配图执行阶段 - 程序自动化图片处理
     */
    fun executeImageExecutionStage(articleWithPlansJson: String): String {
        logger.info("执行配图执行阶段（程序自动化）")

        try {
            // 解析JSON数据
            val articleContent = JsonYamlProcessor.parseArticleContent(articleWithPlansJson)

            // 处理图片生成和下载
            val updatedSections = articleContent.sections.map { section ->
                if (!section.imageDescription.isNullOrBlank()) {
                    // 生成图片并获取路径
                    val imagePath = generateAndSaveImage(section.imageDescription!!, section.order)
                    section.copy(imagePath = imagePath)
                } else {
                    section
                }
            }

            // 更新文章内容
            val updatedContent = articleContent.copy(sections = updatedSections)
            val jsonOutput = JsonYamlProcessor.toJson(updatedContent)

            // 保存YAML格式用于测试和人工检查
            saveStageOutputAsYaml("image_execution", jsonOutput)

            return jsonOutput

        } catch (e: Exception) {
            logger.error("配图执行阶段失败", e)
            throw RuntimeException("配图执行阶段失败: ${e.message}", e)
        }
    }

    /**
     * 8. 完成阶段 - 程序自动化生成最终Markdown
     */
    fun executeCompletionStage(articleWithImagesJson: String): String {
        logger.info("执行完成阶段（程序自动化）")

        try {
            // 解析JSON数据
            val articleContent = JsonYamlProcessor.parseArticleContent(articleWithImagesJson)

            // 补充元数据
            val currentTime = JsonYamlProcessor.getCurrentIsoTime()
            val totalWordCount = articleContent.sections.sumOf {
                JsonYamlProcessor.calculateWordCount(it.content)
            }
            val estimatedTime = JsonYamlProcessor.estimateReadingTime(totalWordCount)

            val updatedMetadata = articleContent.metadata.copy(
                createdAt = currentTime,
                updatedAt = currentTime,
                wordCount = totalWordCount,
                estimatedReadingTime = estimatedTime
            )

            val finalContent = articleContent.copy(metadata = updatedMetadata)

            // 生成Markdown
            val markdown = MarkdownGenerator.generateMarkdown(finalContent)

            // 保存YAML格式用于测试和人工检查
            val finalJson = JsonYamlProcessor.toJson(finalContent)
            saveStageOutputAsYaml("completion", finalJson)

            return markdown

        } catch (e: Exception) {
            logger.error("完成阶段失败", e)
            throw RuntimeException("完成阶段失败: ${e.message}", e)
        }
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
     * 生成并保存图片
     */
    private fun generateAndSaveImage(imageDescription: String, sectionOrder: Int): String? {
        try {
            // 从描述中提取关键词
            val keywords = extractKeywordsFromDescription(imageDescription)

            // 使用图片生成器搜索图片
            val imageGenerator = imageGeneratorFactory.getDefaultGenerator()
            val request = ImageGenerationRequest(
                intent = imageDescription,
                keywords = keywords,
                style = determineImageStyle(imageDescription),
                size = ImageSize.MEDIUM
            )

            val generationResult = imageGenerator.generateImage(request)

            if (generationResult.success && generationResult.imageInfo != null) {
                val imageInfo = generationResult.imageInfo!!

                // 生成有意义的文件名
                val fileName = "section${sectionOrder}-image.jpg"
                val imagePath = "images/$fileName"

                logger.info("成功生成图片: $imageDescription -> ${imageInfo.standardUrl}")
                return imagePath
            } else {
                logger.warn("图片生成失败: $imageDescription - ${generationResult.errorMessage}")
                return null
            }
        } catch (e: Exception) {
            logger.error("生成图片失败: $imageDescription", e)
            return null
        }
    }

    /**
     * 从描述中提取关键词
     */
    private fun extractKeywordsFromDescription(description: String): List<String> {
        // 简单的关键词提取逻辑，可以根据需要改进
        val commonWords =
            setOf("的", "和", "与", "或", "在", "是", "有", "为", "以", "及", "等", "包含", "采用", "使用")
        return description.split("[，,、\\s]+".toRegex())
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length > 1 && !commonWords.contains(it) }
            .take(5) // 最多取5个关键词
    }

    /**
     * 根据描述确定图片风格
     */
    private fun determineImageStyle(description: String): ImageStyle {
        return when {
            description.contains("概念图") || description.contains("示意图") -> ImageStyle.ILLUSTRATION
            description.contains("矢量") || description.contains("图标") -> ImageStyle.VECTOR
            else -> ImageStyle.PHOTO
        }
    }

    /**
     * 保存阶段输出为YAML格式
     */
    private fun saveStageOutputAsYaml(stageName: String, jsonOutput: String) {
        try {
            // 创建测试输出目录
            val testOutputDir = Paths.get(TEST_OUTPUT_DIR)
            Files.createDirectories(testOutputDir)

            // 解析JSON并转换为YAML
            val yamlContent = when (stageName) {
                "outline" -> {
                    val outline = JsonYamlProcessor.parseOutlineContent(jsonOutput)
                    JsonYamlProcessor.toYaml(outline)
                }

                else -> {
                    val article = JsonYamlProcessor.parseArticleContent(jsonOutput)
                    JsonYamlProcessor.toYaml(article)
                }
            }

            // 保存YAML文件
            val timestamp = System.currentTimeMillis()
            val yamlFile = testOutputDir.resolve("${stageName}_${timestamp}.yaml")
            Files.writeString(yamlFile, yamlContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

            logger.info("已保存${stageName}阶段输出为YAML: $yamlFile")

        } catch (e: Exception) {
            logger.warn("保存${stageName}阶段YAML输出失败", e)
        }
    }

    /**
     * 保存文章文件
     */
    private fun saveArticleFile(markdownContent: String): ArticleResult {
        // 从内容中提取标题
        val title = extractTitleFromMarkdown(markdownContent)

        // 创建文章目录
        val articlesDir = Paths.get(WORKSPACE_ARTICLES_DIR)
        Files.createDirectories(articlesDir)

        val articleDir = articlesDir.resolve(title)
        Files.createDirectories(articleDir)

        // 创建图片目录
        val imagesDir = imageProcessor.createImageDirectory(articleDir)

        // 保存文章文件
        val articleFilePath = articleDir.resolve("$title.md")
        Files.writeString(
            articleFilePath,
            markdownContent,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        )

        logger.info("文章文件已保存: $articleFilePath")

        return ArticleResult(
            title = title,
            content = markdownContent,
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
