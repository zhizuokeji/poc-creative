package biz.zhizuo.creative.utils.simple

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 简化文章生成器测试
 */
@SpringBootTest
class SimpleArticleCreatorTest {

    @Autowired
    private lateinit var articleCreator: SimpleArticleCreator

    private val logger = LoggerFactory.getLogger(SimpleArticleCreatorTest::class.java)

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

        val outline = articleCreator.executeOutlineStage(mockSelectedTitle)

        assertNotNull(outline, "构思阶段输出不能为空")
        assertTrue(outline.isNotBlank(), "构思阶段输出不能为空白")
        assertTrue(outline.length > 100, "大纲应该足够详细")

        logger.info("构思阶段测试通过")
        logger.info("大纲长度: ${outline.length} 字符")
        logger.info("大纲内容: ${outline.take(300)}...")
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "ENABLE_AI_TEST", matches = "true")
    fun `测试4_写作阶段`() {
        logger.info("开始测试写作阶段")

        // 使用模拟的构思阶段输出
        val mockOutline = """
        # 人工智能重塑教育：机遇与挑战并存的未来之路

        ## 一、引言
        - AI技术发展现状
        - 教育领域的变革需求

        ## 二、AI在教育中的应用现状
        - 个性化学习平台
        - 智能教学助手
        - 自动化评估系统

        ## 三、机遇分析
        - 提高教学效率
        - 个性化教育实现
        - 教育资源优化配置

        ## 四、挑战与风险
        - 技术依赖问题
        - 数据隐私保护
        - 教师角色转变

        ## 五、未来展望
        - 技术发展趋势
        - 教育模式创新
        - 政策建议
        """.trimIndent()

        val article = articleCreator.executeWritingStage(mockOutline)

        assertNotNull(article, "写作阶段输出不能为空")
        assertTrue(article.isNotBlank(), "写作阶段输出不能为空白")
        assertTrue(article.contains("# "), "文章应该包含标题")
        assertTrue(article.length > 500, "文章应该有足够的内容")

        logger.info("写作阶段测试通过")
        logger.info("文章长度: ${article.length} 字符")
        logger.info("文章开头: ${article.take(200)}...")
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "ENABLE_AI_TEST", matches = "true")
    fun `测试5_优化阶段`() {
        logger.info("开始测试优化阶段")

        // 使用模拟的写作阶段输出
        val mockArticle = """
        # 人工智能重塑教育：机遇与挑战并存的未来之路

        ## 引言

        人工智能技术的快速发展正在深刻改变着各个行业，教育领域也不例外。从个性化学习到智能教学，AI技术为教育带来了前所未有的机遇。

        ## AI在教育中的应用

        目前，AI技术在教育领域的应用主要体现在以下几个方面：

        1. **个性化学习平台**：根据学生的学习习惯和能力水平，提供定制化的学习内容。
        2. **智能教学助手**：协助教师进行课程设计和学生管理。
        3. **自动化评估**：通过AI算法对学生作业和考试进行快速评分。

        ## 结论

        AI技术在教育领域的应用前景广阔，但也需要谨慎应对相关挑战。
        """.trimIndent()

        val optimizedArticle = articleCreator.executeOptimizationStage(mockArticle)

        assertNotNull(optimizedArticle, "优化阶段输出不能为空")
        assertTrue(optimizedArticle.isNotBlank(), "优化阶段输出不能为空白")
        assertTrue(optimizedArticle.contains("# "), "优化后的文章应该包含标题")
        assertTrue(optimizedArticle.length >= mockArticle.length * 0.8, "优化后的文章不应该过度缩减")

        logger.info("优化阶段测试通过")
        logger.info("优化前长度: ${mockArticle.length} 字符")
        logger.info("优化后长度: ${optimizedArticle.length} 字符")
        logger.info("优化后开头: ${optimizedArticle.take(200)}...")
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "ENABLE_AI_TEST", matches = "true")
    fun `测试6_配图规划阶段`() {
        logger.info("开始测试配图规划阶段")

        // 使用模拟的优化阶段输出
        val mockOptimizedArticle = """
        # 人工智能重塑教育：机遇与挑战并存的未来之路

        ## 引言

        人工智能技术的快速发展正在深刻改变着各个行业，教育领域也不例外。从个性化学习到智能教学，AI技术为教育带来了前所未有的机遇。

        ## AI在教育中的应用现状

        目前，AI技术在教育领域的应用主要体现在以下几个方面：

        ### 个性化学习平台
        根据学生的学习习惯和能力水平，AI系统能够提供定制化的学习内容和学习路径。

        ### 智能教学助手
        AI助手可以协助教师进行课程设计、学生管理和教学效果分析。

        ## 未来展望

        随着技术的不断进步，AI在教育领域的应用将更加深入和广泛。
        """.trimIndent()

        val articleWithImagePlans = articleCreator.executeImagePlanningStage(mockOptimizedArticle)

        assertNotNull(articleWithImagePlans, "配图规划阶段输出不能为空")
        assertTrue(articleWithImagePlans.isNotBlank(), "配图规划阶段输出不能为空白")
        assertTrue(articleWithImagePlans.contains("# "), "配图规划后的文章应该包含标题")
        assertTrue(articleWithImagePlans.contains("<!--"), "应该包含配图注释")

        logger.info("配图规划阶段测试通过")
        logger.info("规划后长度: ${articleWithImagePlans.length} 字符")
        logger.info("配图注释数量: ${articleWithImagePlans.split("<!--").size - 1}")
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "ENABLE_AI_TEST", matches = "true")
    fun `测试7_配图执行阶段`() {
        logger.info("开始测试配图执行阶段")

        // 使用模拟的配图规划阶段输出（包含配图注释）
        val mockArticleWithPlans = """
        # 人工智能重塑教育：机遇与挑战并存的未来之路

        ## 引言

        人工智能技术的快速发展正在深刻改变着各个行业，教育领域也不例外。

        <!-- 配图点ID: intro-ai-education 类型: illustration 描述: 人工智能与教育结合的概念图 关键词: artificial intelligence, education, technology, future 风格: illustration -->

        ## AI在教育中的应用现状

        目前，AI技术在教育领域的应用主要体现在个性化学习、智能教学助手等方面。

        <!-- 配图点ID: ai-applications 类型: photo 描述: 学生使用AI学习平台的场景 关键词: students, AI platform, learning, computer, classroom 风格: photo -->

        ## 未来展望

        随着技术的不断进步，AI在教育领域的应用将更加深入和广泛。
        """.trimIndent()

        val articleWithImages = articleCreator.executeImageExecutionStage(mockArticleWithPlans)

        assertNotNull(articleWithImages, "配图执行阶段输出不能为空")
        assertTrue(articleWithImages.isNotBlank(), "配图执行阶段输出不能为空白")
        assertTrue(articleWithImages.contains("# "), "配图执行后的文章应该包含标题")
        // 注意：由于图片生成可能失败，我们不强制要求包含图片链接

        logger.info("配图执行阶段测试通过")
        logger.info("执行后长度: ${articleWithImages.length} 字符")

        // 检查是否有图片链接生成
        val imageLinks = articleWithImages.split("![").size - 1
        logger.info("生成的图片链接数量: $imageLinks")
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "ENABLE_AI_TEST", matches = "true")
    fun `测试8_完成阶段`() {
        logger.info("开始测试完成阶段")

        // 使用模拟的配图执行阶段输出
        val mockArticleWithImages = """
        # 人工智能重塑教育：机遇与挑战并存的未来之路

        ## 引言

        人工智能技术的快速发展正在深刻改变着各个行业，教育领域也不例外。

        <!-- 配图点ID: intro-ai-education 类型: illustration 描述: 人工智能与教育结合的概念图 关键词: artificial intelligence, education, technology, future 风格: illustration -->
        ![人工智能与教育结合的概念图](https://example.com/ai-education.jpg)

        ## AI在教育中的应用现状

        目前，AI技术在教育领域的应用主要体现在个性化学习、智能教学助手等方面。

        <!-- 配图点ID: ai-applications 类型: photo 描述: 学生使用AI学习平台的场景 关键词: students, AI platform, learning, computer, classroom 风格: photo -->
        ![学生使用AI学习平台的场景](https://example.com/students-ai.jpg)

        ## 结论

        AI技术为教育带来了巨大的机遇，同时也需要我们谨慎应对相关挑战。
        """.trimIndent()

        val finalArticle = articleCreator.executeCompletionStage(mockArticleWithImages)

        assertNotNull(finalArticle, "完成阶段输出不能为空")
        assertTrue(finalArticle.isNotBlank(), "完成阶段输出不能为空白")
        assertTrue(finalArticle.contains("# "), "最终文章应该包含标题")
        assertTrue(finalArticle.length > 200, "最终文章应该有足够的内容")

        logger.info("完成阶段测试通过")
        logger.info("最终文章长度: ${finalArticle.length} 字符")
        logger.info("最终文章开头: ${finalArticle.take(200)}...")
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
}
