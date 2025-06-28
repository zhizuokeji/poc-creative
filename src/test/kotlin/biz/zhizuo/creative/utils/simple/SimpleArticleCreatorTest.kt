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
    fun `测试单个阶段执行`() {
        logger.info("开始测试单个阶段执行")

        // 1. 测试创意阶段
        val expandedIdea = articleCreator.executeCreativeStage(
            "人工智能对教育的影响",
            "智能教育"
        )
        assertNotNull(expandedIdea)
        assertTrue(expandedIdea.isNotBlank())
        logger.info("创意阶段输出: $expandedIdea")

        // 2. 测试选题阶段
        val selectedTitle = articleCreator.executeTopicStage(expandedIdea)
        assertNotNull(selectedTitle)
        assertTrue(selectedTitle.isNotBlank())
        logger.info("选题阶段输出: $selectedTitle")

        // 3. 测试构思阶段
        val outline = articleCreator.executeOutlineStage(selectedTitle)
        assertNotNull(outline)
        assertTrue(outline.isNotBlank())
        logger.info("构思阶段输出: $outline")

        // 4. 测试写作阶段
        val article = articleCreator.executeWritingStage(outline)
        assertNotNull(article)
        assertTrue(article.isNotBlank())
        assertTrue(article.contains("# "), "应该包含标题")
        logger.info("写作阶段输出长度: ${article.length} 字符")
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
