package biz.zhizuo.creative.utils.simple

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * JSON处理功能测试
 */
class JsonProcessingTest {

    private val logger = LoggerFactory.getLogger(JsonProcessingTest::class.java)

    @Test
    fun `测试OutlineContent的JSON序列化和反序列化`() {
        logger.info("测试OutlineContent的JSON序列化和反序列化")

        val outline = OutlineContent(
            title = "测试文章标题",
            description = "测试文章描述",
            structure = "总-分-总型",
            outline = OutlineStructure(
                introduction = OutlineSection(
                    title = "引言",
                    keyPoints = listOf("要点1", "要点2"),
                    estimatedWords = 200,
                    purpose = "建立背景"
                ),
                mainSections = listOf(
                    OutlineSection(
                        title = "主要章节",
                        keyPoints = listOf("核心要点1", "核心要点2"),
                        estimatedWords = 600,
                        importance = "high"
                    )
                ),
                conclusion = OutlineSection(
                    title = "结论",
                    keyPoints = listOf("总结要点1", "总结要点2"),
                    estimatedWords = 200,
                    purpose = "总结和展望"
                )
            ),
            totalEstimatedWords = 1000,
            readingTime = "3-5分钟"
        )

        // 序列化为JSON
        val json = JsonYamlProcessor.toJson(outline)
        assertNotNull(json)
        assertTrue(json.contains("测试文章标题"))
        logger.info("JSON序列化成功，长度: ${json.length}")

        // 反序列化为对象
        val parsedOutline = JsonYamlProcessor.parseOutlineContent(json)
        assertEquals(outline.title, parsedOutline.title)
        assertEquals(outline.description, parsedOutline.description)
        assertEquals(outline.totalEstimatedWords, parsedOutline.totalEstimatedWords)
        logger.info("JSON反序列化成功")

        // 序列化为YAML
        val yaml = JsonYamlProcessor.toYaml(outline)
        assertNotNull(yaml)
        assertTrue(yaml.contains("测试文章标题"))
        logger.info("YAML序列化成功，长度: ${yaml.length}")
    }

    @Test
    fun `测试ArticleContent的JSON序列化和反序列化`() {
        logger.info("测试ArticleContent的JSON序列化和反序列化")

        val article = ArticleContent(
            title = "测试文章",
            description = "测试文章描述",
            metadata = ArticleMetadata(
                author = "测试作者",
                estimatedReadingTime = 5,
                wordCount = 1000,
                tags = listOf("测试", "JSON"),
                category = "技术",
                optimizations = listOf(
                    OptimizationRecord(
                        type = "语言优化",
                        category = "表达",
                        description = "优化了表达方式",
                        reason = "提高可读性"
                    )
                )
            ),
            sections = listOf(
                ArticleSection(
                    title = "引言",
                    content = "这是引言内容",
                    level = 1,
                    order = 1
                ),
                ArticleSection(
                    title = "主要内容",
                    content = "这是主要内容",
                    imageDescription = "相关配图描述",
                    imagePath = "images/test.jpg",
                    level = 2,
                    order = 2
                )
            )
        )

        // 序列化为JSON
        val json = JsonYamlProcessor.toJson(article)
        assertNotNull(json)
        assertTrue(json.contains("测试文章"))
        logger.info("JSON序列化成功，长度: ${json.length}")

        // 反序列化为对象
        val parsedArticle = JsonYamlProcessor.parseArticleContent(json)
        assertEquals(article.title, parsedArticle.title)
        assertEquals(article.sections.size, parsedArticle.sections.size)
        assertEquals(article.metadata.optimizations.size, parsedArticle.metadata.optimizations.size)
        logger.info("JSON反序列化成功")

        // 序列化为YAML
        val yaml = JsonYamlProcessor.toYaml(article)
        assertNotNull(yaml)
        assertTrue(yaml.contains("测试文章"))
        logger.info("YAML序列化成功，长度: ${yaml.length}")
    }

    @Test
    fun `测试Markdown生成`() {
        logger.info("测试Markdown生成")

        val article = ArticleContent(
            title = "测试Markdown生成",
            description = "测试Markdown生成功能",
            metadata = ArticleMetadata(
                author = "测试作者",
                createdAt = "2025-06-28T10:00:00Z",
                estimatedReadingTime = 5,
                wordCount = 1000,
                tags = listOf("测试", "Markdown"),
                category = "技术"
            ),
            sections = listOf(
                ArticleSection(
                    title = "",
                    content = "这是引言内容，没有标题。",
                    imagePath = "images/intro.jpg",
                    level = 1,
                    order = 1
                ),
                ArticleSection(
                    title = "主要章节",
                    content = "这是主要章节的内容。",
                    imageDescription = "主要章节配图",
                    imagePath = "images/main.jpg",
                    level = 2,
                    order = 2
                ),
                ArticleSection(
                    title = "结论",
                    content = "这是结论部分的内容。",
                    level = 2,
                    order = 3
                )
            )
        )

        val markdown = MarkdownGenerator.generateMarkdown(article)
        assertNotNull(markdown)

        // 验证Front Matter
        assertTrue(markdown.contains("---"))
        assertTrue(markdown.contains("title: \"测试Markdown生成\""))
        assertTrue(markdown.contains("author: \"测试作者\""))
        assertTrue(markdown.contains("tags:"))

        // 验证内容结构
        assertTrue(markdown.contains("# 测试Markdown生成"))
        assertTrue(markdown.contains("## 主要章节"))
        assertTrue(markdown.contains("!["))
        assertTrue(markdown.contains("images/intro.jpg"))
        assertTrue(markdown.contains("images/main.jpg"))

        logger.info("Markdown生成成功，长度: ${markdown.length}")
        logger.info("生成的Markdown前200字符: ${markdown.take(200)}")
    }

    @Test
    fun `测试JSON Schema验证`() {
        logger.info("测试JSON Schema验证")

        // 测试有效的outline JSON
        val validOutlineJson = """
        {
          "title": "测试标题",
          "description": "测试描述",
          "structure": "总-分-总型",
          "outline": {
            "introduction": {
              "title": "引言",
              "keyPoints": ["要点1", "要点2"],
              "estimatedWords": 200,
              "purpose": "建立背景"
            },
            "mainSections": [
              {
                "title": "主要章节",
                "keyPoints": ["核心要点1"],
                "estimatedWords": 600,
                "importance": "high"
              }
            ],
            "conclusion": {
              "title": "结论",
              "keyPoints": ["总结要点1"],
              "estimatedWords": 200,
              "purpose": "总结"
            }
          },
          "totalEstimatedWords": 1000,
          "readingTime": "3-5分钟"
        }
        """.trimIndent()

        val outlineResult = JsonSchemaValidator.validateOutlineJson(validOutlineJson)
        assertTrue(outlineResult.isValid, "有效的outline JSON应该通过验证")
        logger.info("Outline JSON验证通过")

        // 测试有效的article JSON
        val validArticleJson = """
        {
          "title": "测试文章",
          "description": "测试描述",
          "metadata": {
            "wordCount": 1000,
            "estimatedReadingTime": 5,
            "tags": ["测试"]
          },
          "sections": [
            {
              "content": "这是测试内容，长度足够进行验证测试。",
              "level": 2,
              "order": 1
            }
          ]
        }
        """.trimIndent()

        val articleResult = JsonSchemaValidator.validateArticleJson(validArticleJson)
        assertTrue(articleResult.isValid, "有效的article JSON应该通过验证")
        logger.info("Article JSON验证通过")

        // 测试无效的JSON格式
        val invalidJson = "{ invalid json }"
        val formatResult = JsonSchemaValidator.validateJsonFormat(invalidJson)
        assertTrue(!formatResult.isValid, "无效的JSON格式应该验证失败")
        logger.info("无效JSON格式验证正确失败")
    }

    @Test
    fun `测试字数统计和阅读时间估算`() {
        logger.info("测试字数统计和阅读时间估算")

        val text = "这是一个测试文本，包含中文字符和 English words. 用于测试字数统计功能。"
        val wordCount = JsonYamlProcessor.calculateWordCount(text)
        assertTrue(wordCount > 0, "字数应该大于0")
        logger.info("字数统计结果: $wordCount")

        val readingTime = JsonYamlProcessor.estimateReadingTime(wordCount)
        assertTrue(readingTime > 0, "阅读时间应该大于0")
        logger.info("阅读时间估算: $readingTime 分钟")

        // 测试较长文本
        val longText = "这是一个很长的测试文本。".repeat(100)
        val longWordCount = JsonYamlProcessor.calculateWordCount(longText)
        val longReadingTime = JsonYamlProcessor.estimateReadingTime(longWordCount)
        assertTrue(longReadingTime > readingTime, "更长的文本应该需要更多阅读时间")
        logger.info("长文本字数: $longWordCount, 阅读时间: $longReadingTime 分钟")
    }
}
