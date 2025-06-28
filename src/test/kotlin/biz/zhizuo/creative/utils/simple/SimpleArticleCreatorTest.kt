package biz.zhizuo.creative.utils.simple

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 简化文章生成器测试 - 支持新的8阶段JSON工作流
 */
@SpringBootTest
class SimpleArticleCreatorTest {

    @Autowired
    private lateinit var articleCreator: SimpleArticleCreator

    private val logger = LoggerFactory.getLogger(SimpleArticleCreatorTest::class.java)

    private val yamlMapper = ObjectMapper(YAMLFactory()).apply {
        registerModule(KotlinModule.Builder().build())
    }

    companion object {
        private const val TEST_OUTPUT_DIR = "workspace/test-outputs"
    }

    @BeforeEach
    fun setup() {
        // 确保测试输出目录存在
        Files.createDirectories(Paths.get(TEST_OUTPUT_DIR))
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "ENABLE_AI_TEST", matches = "true")
    fun `测试完整文章生成流程`() {
        logger.info("开始测试完整文章生成流程")

        val request = ArticleRequest(
            originalIdea = "探讨远程工作的优缺点",
            themeKeyword = "未来工作模式"
        )

        // 执行完整流程
        val result = articleCreator.createArticle(request)

        // 验证结果
        assertNotNull(result.title, "文章标题不能为空")
        assertNotNull(result.content, "文章内容不能为空")
        assertNotNull(result.filePath, "文件路径不能为空")
        assertNotNull(result.imagesDir, "图片目录不能为空")

        // 验证文件确实被创建
        val filePath = Paths.get(result.filePath)
        assertTrue(Files.exists(filePath), "文章文件应该存在")

        val imagesDirPath = Paths.get(result.imagesDir)
        assertTrue(Files.exists(imagesDirPath), "图片目录应该存在")

        // 验证内容格式
        assertTrue(result.content.contains("# "), "文章应该包含标题")
        assertTrue(result.content.length > 100, "文章内容应该足够长")

        logger.info("文章生成成功:")
        logger.info("标题: ${result.title}")
        logger.info("文件路径: ${result.filePath}")
        logger.info("图片目录: ${result.imagesDir}")
        logger.info("内容长度: ${result.content.length} 字符")
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "ENABLE_AI_TEST", matches = "true")
    fun `测试1_创意阶段`() {
        logger.info("开始测试创意阶段")

        val expandedIdea = articleCreator.executeCreativeStage(
            "人工智能对教育的影响",
            "智能教育"
        )

        assertNotNull(expandedIdea, "创意阶段输出不能为空")
        assertTrue(expandedIdea.isNotBlank(), "创意阶段输出不能为空白")
        assertTrue(expandedIdea.length > 50, "创意阶段输出应该足够详细")

        logger.info("创意阶段测试通过")
        logger.info("输出长度: ${expandedIdea.length} 字符")
        logger.info("输出内容: ${expandedIdea.take(200)}...")
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "ENABLE_AI_TEST", matches = "true")
    fun `测试2_选题阶段`() {
        logger.info("开始测试选题阶段")

        // 使用模拟的创意阶段输出
        val mockExpandedIdea = """
        基于"人工智能对教育的影响"这个主题，我们可以从多个角度来探讨：
        1. AI技术在个性化学习中的应用
        2. 智能教学系统对传统教育模式的冲击
        3. AI辅助教师提高教学效率的可能性
        4. 学生数据隐私和AI教育伦理问题
        5. 未来教育发展趋势预测
        """.trimIndent()

        val selectedTitle = articleCreator.executeTopicStage(mockExpandedIdea)

        assertNotNull(selectedTitle, "选题阶段输出不能为空")
        assertTrue(selectedTitle.isNotBlank(), "选题阶段输出不能为空白")
        assertTrue(selectedTitle.length > 10, "标题应该有合适的长度")

        logger.info("选题阶段测试通过")
        logger.info("生成的标题: $selectedTitle")
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "ENABLE_AI_TEST", matches = "true")
    fun `测试3_构思阶段`() {
        logger.info("开始测试构思阶段")

        // 使用模拟的选题阶段输出
        val mockSelectedTitle = "人工智能重塑教育：机遇与挑战并存的未来之路"

        val outlineJson = articleCreator.executeOutlineStage(mockSelectedTitle)

        // 验证JSON输出
        assertNotNull(outlineJson, "构思阶段输出不能为空")
        assertTrue(outlineJson.isNotBlank(), "构思阶段输出不能为空白")

        // 验证JSON格式
        try {
            val outline = JsonYamlProcessor.parseOutlineContent(outlineJson)
            assertNotNull(outline.title, "大纲标题不能为空")
            assertNotNull(outline.description, "大纲描述不能为空")
            assertNotNull(outline.outline, "大纲结构不能为空")
            assertTrue(outline.totalEstimatedWords > 0, "预估字数应该大于0")

            // 保存YAML格式用于人工检查
            saveTestOutput("outline", outline)

            logger.info("构思阶段测试通过")
            logger.info("大纲标题: ${outline.title}")
            logger.info("预估字数: ${outline.totalEstimatedWords}")
            logger.info("阅读时间: ${outline.readingTime}")

        } catch (e: Exception) {
            logger.error("JSON解析失败", e)
            throw AssertionError("构思阶段输出不是有效的JSON格式: ${e.message}")
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "ENABLE_AI_TEST", matches = "true")
    fun `测试4_写作阶段`() {
        logger.info("开始测试写作阶段")

        // 使用模拟的构思阶段JSON输出
        val mockOutlineJson = """
        {
          "title": "人工智能重塑教育：机遇与挑战并存的未来之路",
          "description": "全面解析人工智能技术如何深度融入我们的教育场景",
          "structure": "总-分-总型",
          "outline": {
            "introduction": {
              "title": "AI时代已经到来",
              "keyPoints": ["AI技术的普及现状", "对教育的影响概述"],
              "estimatedWords": 300,
              "purpose": "建立背景，激发兴趣"
            },
            "mainSections": [
              {
                "title": "AI在教育中的应用现状",
                "keyPoints": ["个性化学习平台", "智能教学助手", "自动化评估系统"],
                "estimatedWords": 600,
                "importance": "high"
              },
              {
                "title": "机遇与挑战分析",
                "keyPoints": ["提高教学效率", "数据隐私保护", "教师角色转变"],
                "estimatedWords": 800,
                "importance": "high"
              }
            ],
            "conclusion": {
              "title": "未来展望",
              "keyPoints": ["技术发展趋势", "教育模式创新"],
              "estimatedWords": 300,
              "purpose": "总结和展望"
            }
          },
          "totalEstimatedWords": 2000,
          "readingTime": "6-8分钟"
        }
        """.trimIndent()

        val articleJson = articleCreator.executeWritingStage(mockOutlineJson)

        // 验证JSON输出
        assertNotNull(articleJson, "写作阶段输出不能为空")
        assertTrue(articleJson.isNotBlank(), "写作阶段输出不能为空白")

        // 验证JSON格式
        try {
            val article = JsonYamlProcessor.parseArticleContent(articleJson)
            assertNotNull(article.title, "文章标题不能为空")
            assertNotNull(article.description, "文章描述不能为空")
            assertTrue(article.sections.isNotEmpty(), "文章应该包含章节")

            // 验证章节结构
            article.sections.forEach { section ->
                assertNotNull(section.content, "章节内容不能为空")
                assertTrue(section.content.isNotBlank(), "章节内容不能为空白")
                assertTrue(section.order > 0, "章节顺序应该大于0")
            }

            // 保存YAML格式用于人工检查
            saveTestOutput("writing", article)

            logger.info("写作阶段测试通过")
            logger.info("文章标题: ${article.title}")
            logger.info("章节数量: ${article.sections.size}")
            logger.info("总字数: ${article.metadata.wordCount ?: 0}")

        } catch (e: Exception) {
            logger.error("JSON解析失败", e)
            throw AssertionError("写作阶段输出不是有效的JSON格式: ${e.message}")
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "ENABLE_AI_TEST", matches = "true")
    fun `测试5_优化阶段`() {
        logger.info("开始测试优化阶段")

        // 使用模拟的写作阶段JSON输出
        val mockArticleJson = """
        {
          "title": "人工智能重塑教育：机遇与挑战并存的未来之路",
          "description": "全面解析人工智能技术如何深度融入我们的教育场景",
          "metadata": {
            "estimatedReadingTime": 8,
            "wordCount": 2000,
            "tags": ["人工智能", "教育", "技术"],
            "category": "科技教育"
          },
          "sections": [
            {
              "title": "",
              "content": "人工智能技术的快速发展正在深刻改变着各个行业，教育领域也不例外。从个性化学习到智能教学，AI技术为教育带来了前所未有的机遇。",
              "level": 1,
              "order": 1
            },
            {
              "title": "AI在教育中的应用",
              "content": "目前，AI技术在教育领域的应用主要体现在个性化学习平台、智能教学助手、自动化评估等方面。这些应用正在逐步改变传统的教学模式。",
              "level": 2,
              "order": 2
            },
            {
              "title": "结论",
              "content": "AI技术在教育领域的应用前景广阔，但也需要谨慎应对相关挑战。",
              "level": 2,
              "order": 3
            }
          ]
        }
        """.trimIndent()

        val optimizedArticleJson = articleCreator.executeOptimizationStage(mockArticleJson)

        // 验证JSON输出
        assertNotNull(optimizedArticleJson, "优化阶段输出不能为空")
        assertTrue(optimizedArticleJson.isNotBlank(), "优化阶段输出不能为空白")

        // 验证JSON格式
        try {
            val optimizedArticle = JsonYamlProcessor.parseArticleContent(optimizedArticleJson)
            assertNotNull(optimizedArticle.title, "优化后文章标题不能为空")
            assertTrue(optimizedArticle.sections.isNotEmpty(), "优化后文章应该包含章节")

            // 验证优化记录
            assertTrue(optimizedArticle.metadata.optimizations.isNotEmpty(), "应该包含优化记录")
            optimizedArticle.metadata.optimizations.forEach { optimization ->
                assertNotNull(optimization.type, "优化类型不能为空")
                assertNotNull(optimization.description, "优化描述不能为空")
            }

            // 保存YAML格式用于人工检查
            saveTestOutput("optimization", optimizedArticle)

            logger.info("优化阶段测试通过")
            logger.info("优化记录数量: ${optimizedArticle.metadata.optimizations.size}")
            logger.info("优化后字数: ${optimizedArticle.metadata.wordCount ?: 0}")

        } catch (e: Exception) {
            logger.error("JSON解析失败", e)
            throw AssertionError("优化阶段输出不是有效的JSON格式: ${e.message}")
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "ENABLE_AI_TEST", matches = "true")
    fun `测试6_配图规划阶段`() {
        logger.info("开始测试配图规划阶段")

        // 使用模拟的优化阶段JSON输出
        val mockOptimizedArticleJson = """
        {
          "title": "人工智能重塑教育：机遇与挑战并存的未来之路",
          "description": "全面解析人工智能技术如何深度融入我们的教育场景",
          "metadata": {
            "estimatedReadingTime": 8,
            "wordCount": 2000,
            "tags": ["人工智能", "教育", "技术"],
            "category": "科技教育",
            "optimizations": [
              {
                "type": "语言优化",
                "category": "表达",
                "description": "优化了专业术语的表达方式",
                "reason": "提高可读性"
              }
            ]
          },
          "sections": [
            {
              "title": "",
              "content": "人工智能技术的快速发展正在深刻改变着各个行业，教育领域也不例外。从个性化学习到智能教学，AI技术为教育带来了前所未有的机遇。",
              "level": 1,
              "order": 1
            },
            {
              "title": "AI在教育中的应用现状",
              "content": "目前，AI技术在教育领域的应用主要体现在个性化学习平台、智能教学助手等方面。这些应用正在逐步改变传统的教学模式。",
              "level": 2,
              "order": 2
            },
            {
              "title": "未来展望",
              "content": "随着技术的不断进步，AI在教育领域的应用将更加深入和广泛。",
              "level": 2,
              "order": 3
            }
          ]
        }
        """.trimIndent()

        val articleWithImagePlansJson = articleCreator.executeImagePlanningStage(mockOptimizedArticleJson)

        // 验证JSON输出
        assertNotNull(articleWithImagePlansJson, "配图规划阶段输出不能为空")
        assertTrue(articleWithImagePlansJson.isNotBlank(), "配图规划阶段输出不能为空白")

        // 验证JSON格式和配图描述
        try {
            val article = JsonYamlProcessor.parseArticleContent(articleWithImagePlansJson)
            assertNotNull(article.title, "文章标题不能为空")
            assertTrue(article.sections.isNotEmpty(), "文章应该包含章节")

            // 检查配图描述是否已添加
            var imageDescriptionCount = 0
            article.sections.forEach { section ->
                if (!section.imageDescription.isNullOrBlank()) {
                    imageDescriptionCount++
                    assertTrue(section.imageDescription!!.length > 10, "配图描述应该足够详细")
                    logger.info("章节 ${section.order} 配图描述: ${section.imageDescription}")
                }
            }

            assertTrue(imageDescriptionCount > 0, "应该至少有一个章节包含配图描述")

            // 保存YAML格式用于人工检查
            saveTestOutput("image_planning", article)

            logger.info("配图规划阶段测试通过")
            logger.info("配图描述数量: $imageDescriptionCount")

        } catch (e: Exception) {
            logger.error("JSON解析失败", e)
            throw AssertionError("配图规划阶段输出不是有效的JSON格式: ${e.message}")
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "ENABLE_AI_TEST", matches = "true")
    fun `测试7_配图执行阶段`() {
        logger.info("开始测试配图执行阶段（程序自动化）")

        // 使用模拟的配图规划阶段JSON输出
        val mockArticleWithPlansJson = """
        {
          "title": "人工智能重塑教育：机遇与挑战并存的未来之路",
          "description": "全面解析人工智能技术如何深度融入我们的教育场景",
          "metadata": {
            "estimatedReadingTime": 8,
            "wordCount": 2000,
            "tags": ["人工智能", "教育", "技术"],
            "category": "科技教育"
          },
          "sections": [
            {
              "title": "",
              "content": "人工智能技术的快速发展正在深刻改变着各个行业，教育领域也不例外。",
              "imageDescription": "展示AI技术融入教育场景的概念图，包含智能设备、学习平台等元素",
              "level": 1,
              "order": 1
            },
            {
              "title": "AI在教育中的应用现状",
              "content": "目前，AI技术在教育领域的应用主要体现在个性化学习、智能教学助手等方面。",
              "imageDescription": "学生使用AI学习平台的真实场景照片，展示现代化教室环境",
              "level": 2,
              "order": 2
            }
          ]
        }
        """.trimIndent()

        val articleWithImagesJson = articleCreator.executeImageExecutionStage(mockArticleWithPlansJson)

        // 验证JSON输出
        assertNotNull(articleWithImagesJson, "配图执行阶段输出不能为空")
        assertTrue(articleWithImagesJson.isNotBlank(), "配图执行阶段输出不能为空白")

        // 验证JSON格式和图片路径
        try {
            val article = JsonYamlProcessor.parseArticleContent(articleWithImagesJson)
            assertNotNull(article.title, "文章标题不能为空")
            assertTrue(article.sections.isNotEmpty(), "文章应该包含章节")

            // 检查图片路径是否已添加
            var imagePathCount = 0
            article.sections.forEach { section ->
                if (!section.imageDescription.isNullOrBlank()) {
                    // 有配图描述的章节应该尝试添加图片路径
                    if (!section.imagePath.isNullOrBlank()) {
                        imagePathCount++
                        logger.info("章节 ${section.order} 已添加图片路径: ${section.imagePath}")
                    } else {
                        logger.warn("章节 ${section.order} 配图生成失败")
                    }
                }
            }

            // 保存YAML格式用于人工检查
            saveTestOutput("image_execution", article)

            logger.info("配图执行阶段测试通过")
            logger.info("成功生成图片路径数量: $imagePathCount")

        } catch (e: Exception) {
            logger.error("JSON解析失败", e)
            throw AssertionError("配图执行阶段输出不是有效的JSON格式: ${e.message}")
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "ENABLE_AI_TEST", matches = "true")
    fun `测试8_完成阶段`() {
        logger.info("开始测试完成阶段（程序自动化）")

        // 使用模拟的配图执行阶段JSON输出
        val mockArticleWithImagesJson = """
        {
          "title": "人工智能重塑教育：机遇与挑战并存的未来之路",
          "description": "全面解析人工智能技术如何深度融入我们的教育场景",
          "metadata": {
            "estimatedReadingTime": 8,
            "wordCount": 2000,
            "tags": ["人工智能", "教育", "技术"],
            "category": "科技教育"
          },
          "sections": [
            {
              "title": "",
              "content": "人工智能技术的快速发展正在深刻改变着各个行业，教育领域也不例外。从个性化学习到智能教学，AI技术为教育带来了前所未有的机遇。",
              "imageDescription": "展示AI技术融入教育场景的概念图",
              "imagePath": "images/section1-image.jpg",
              "level": 1,
              "order": 1
            },
            {
              "title": "AI在教育中的应用现状",
              "content": "目前，AI技术在教育领域的应用主要体现在个性化学习、智能教学助手等方面。这些应用正在逐步改变传统的教学模式。",
              "imageDescription": "学生使用AI学习平台的真实场景照片",
              "imagePath": "images/section2-image.jpg",
              "level": 2,
              "order": 2
            },
            {
              "title": "未来展望",
              "content": "随着技术的不断进步，AI在教育领域的应用将更加深入和广泛，为教育现代化提供强有力的支撑。",
              "level": 2,
              "order": 3
            }
          ]
        }
        """.trimIndent()

        val finalMarkdown = articleCreator.executeCompletionStage(mockArticleWithImagesJson)

        // 验证Markdown输出
        assertNotNull(finalMarkdown, "完成阶段输出不能为空")
        assertTrue(finalMarkdown.isNotBlank(), "完成阶段输出不能为空白")
        assertTrue(finalMarkdown.contains("---"), "最终文章应该包含Front Matter")
        assertTrue(finalMarkdown.contains("# "), "最终文章应该包含标题")
        assertTrue(finalMarkdown.contains("!["), "最终文章应该包含图片引用")

        // 验证Front Matter格式
        assertTrue(finalMarkdown.contains("title:"), "应该包含标题元数据")
        assertTrue(finalMarkdown.contains("description:"), "应该包含描述元数据")
        assertTrue(finalMarkdown.contains("createdAt:"), "应该包含创建时间")
        assertTrue(finalMarkdown.contains("wordCount:"), "应该包含字数统计")

        // 保存最终Markdown文件用于人工检查
        saveTestMarkdown("completion", finalMarkdown)

        logger.info("完成阶段测试通过")
        logger.info("最终Markdown长度: ${finalMarkdown.length} 字符")
        logger.info("包含图片数量: ${finalMarkdown.split("![").size - 1}")
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "ENABLE_AI_TEST", matches = "true")
    fun `测试不同主题的文章生成`() {
        logger.info("开始测试不同主题的文章生成")

        val testCases = listOf(
            ArticleRequest("如何提高工作效率", "时间管理"),
            ArticleRequest("健康饮食的重要性", "营养健康"),
            ArticleRequest("环保生活方式", "绿色生活")
        )

        testCases.forEach { request ->
            logger.info("测试主题: ${request.originalIdea}")

            val result = articleCreator.createArticle(request)

            assertNotNull(result.title)
            assertNotNull(result.content)
            assertTrue(Files.exists(Paths.get(result.filePath)))

            logger.info("生成文章: ${result.title}")
        }
    }

    @Test
    fun `测试系统提示词加载`() {
        logger.info("测试系统提示词加载")

        val promptLoader = SimplePromptLoader()
        val stageNames = promptLoader.getAllStageNames()

        assertTrue(stageNames.isNotEmpty(), "应该有阶段名称")
        assertTrue(stageNames.contains("creative"), "应该包含创意阶段")
        assertTrue(stageNames.contains("writing"), "应该包含写作阶段")

        // 测试加载每个阶段的提示词
        stageNames.forEach { stageName ->
            val prompt = promptLoader.loadPrompt(stageName)
            assertNotNull(prompt, "提示词不能为空: $stageName")
            assertTrue(prompt.isNotBlank(), "提示词不能为空白: $stageName")
            logger.info("$stageName 阶段提示词长度: ${prompt.length}")
        }
    }

    /**
     * 保存测试输出为YAML格式，便于人工检查
     */
    private fun saveTestOutput(stageName: String, content: Any) {
        try {
            val timestamp = System.currentTimeMillis()
            val fileName = "${stageName}_test_${timestamp}.yaml"
            val filePath = Paths.get(TEST_OUTPUT_DIR, fileName)

            val yamlContent = yamlMapper.writeValueAsString(content)
            Files.writeString(filePath, yamlContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

            logger.info("已保存${stageName}阶段测试输出: $filePath")
        } catch (e: Exception) {
            logger.warn("保存${stageName}阶段测试输出失败", e)
        }
    }

    /**
     * 保存Markdown文件，便于人工检查
     */
    private fun saveTestMarkdown(stageName: String, markdown: String) {
        try {
            val timestamp = System.currentTimeMillis()
            val fileName = "${stageName}_test_${timestamp}.md"
            val filePath = Paths.get(TEST_OUTPUT_DIR, fileName)

            Files.writeString(filePath, markdown, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

            logger.info("已保存${stageName}阶段Markdown输出: $filePath")
        } catch (e: Exception) {
            logger.warn("保存${stageName}阶段Markdown输出失败", e)
        }
    }

    /**
     * 验证JSON格式的有效性
     */
    private fun validateJsonFormat(jsonString: String, expectedType: String): Boolean {
        return try {
            when (expectedType) {
                "outline" -> {
                    JsonYamlProcessor.parseOutlineContent(jsonString)
                    true
                }

                "article" -> {
                    JsonYamlProcessor.parseArticleContent(jsonString)
                    true
                }

                else -> false
            }
        } catch (e: Exception) {
            logger.error("JSON格式验证失败: $expectedType", e)
            false
        }
    }
}
