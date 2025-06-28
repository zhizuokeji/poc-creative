package biz.zhizuo.creative.utils.simple

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 文章内容数据结构 - 统一的JSON格式
 */
data class ArticleContent(
    val title: String,
    val description: String,
    val metadata: ArticleMetadata = ArticleMetadata(),
    val sections: List<ArticleSection> = emptyList(),
)

/**
 * 文章元数据
 */
data class ArticleMetadata(
    val author: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val estimatedReadingTime: Int? = null,
    val wordCount: Int? = null,
    val tags: List<String> = emptyList(),
    val category: String? = null,
    val language: String = "zh-CN",
    val seoKeywords: List<String> = emptyList(),
    val seoDescription: String? = null,
    val optimizations: List<OptimizationRecord> = emptyList(),
)

/**
 * 文章章节
 */
data class ArticleSection(
    val title: String? = null,
    val content: String,
    val imageDescription: String? = null,
    val imagePath: String? = null,
    val level: Int = 2,
    val order: Int,
)

/**
 * 优化记录
 */
data class OptimizationRecord(
    val type: String,
    val category: String,
    val description: String,
    val section: String? = null,
    val reason: String,
)

/**
 * 构思阶段输出数据结构
 */
data class OutlineContent(
    val title: String,
    val description: String,
    val structure: String,
    val outline: OutlineStructure,
    val totalEstimatedWords: Int,
    val readingTime: String,
)

/**
 * 大纲结构
 */
data class OutlineStructure(
    val introduction: OutlineSection,
    val mainSections: List<OutlineSection>,
    val conclusion: OutlineSection,
)

/**
 * 大纲章节
 */
data class OutlineSection(
    val title: String,
    val keyPoints: List<String>,
    val estimatedWords: Int,
    val purpose: String? = null,
    val importance: String? = null,
    val hooks: List<String> = emptyList(),
    val supportingElements: List<String> = emptyList(),
    val subsections: List<OutlineSubsection> = emptyList(),
    val callToAction: String? = null,
)

/**
 * 大纲子章节
 */
data class OutlineSubsection(
    val title: String,
    val keyPoints: List<String>,
    val estimatedWords: Int,
)

/**
 * JSON/YAML 处理工具类
 */
object JsonYamlProcessor {

    private val jsonMapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
    }

    private val yamlMapper = ObjectMapper(YAMLFactory()).apply {
        registerModule(KotlinModule.Builder().build())
    }

    /**
     * 将JSON字符串解析为ArticleContent对象
     */
    fun parseArticleContent(jsonString: String): ArticleContent {
        return jsonMapper.readValue(jsonString)
    }

    /**
     * 将JSON字符串解析为OutlineContent对象
     */
    fun parseOutlineContent(jsonString: String): OutlineContent {
        return jsonMapper.readValue(jsonString)
    }

    /**
     * 将对象序列化为JSON字符串
     */
    fun toJson(obj: Any): String {
        return jsonMapper.writeValueAsString(obj)
    }

    /**
     * 将对象序列化为YAML字符串
     */
    fun toYaml(obj: Any): String {
        return yamlMapper.writeValueAsString(obj)
    }

    /**
     * 从YAML字符串解析为ArticleContent对象
     */
    fun parseArticleContentFromYaml(yamlString: String): ArticleContent {
        return yamlMapper.readValue(yamlString)
    }

    /**
     * 从YAML字符串解析为OutlineContent对象
     */
    fun parseOutlineContentFromYaml(yamlString: String): OutlineContent {
        return yamlMapper.readValue(yamlString)
    }

    /**
     * 生成当前时间的ISO格式字符串
     */
    fun getCurrentIsoTime(): String {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z"
    }

    /**
     * 计算文本的字数（中文字符和英文单词）
     */
    fun calculateWordCount(text: String): Int {
        // 简单的字数统计：中文字符 + 英文单词
        val chineseChars = text.count { it.toString().matches(Regex("[\u4e00-\u9fa5]")) }
        val englishWords = text.split(Regex("\\s+")).count { it.matches(Regex("[a-zA-Z]+")) }
        return chineseChars + englishWords
    }

    /**
     * 根据字数估算阅读时间（分钟）
     */
    fun estimateReadingTime(wordCount: Int): Int {
        // 中文阅读速度约为每分钟300-400字，这里取350字
        return (wordCount / 350.0).toInt().coerceAtLeast(1)
    }
}

/**
 * Markdown生成器
 */
object MarkdownGenerator {

    /**
     * 将ArticleContent转换为Markdown格式
     */
    fun generateMarkdown(content: ArticleContent): String {
        val sb = StringBuilder()

        // 生成Front Matter
        sb.appendLine("---")
        sb.appendLine("title: \"${content.title}\"")
        sb.appendLine("description: \"${content.description}\"")

        content.metadata.author?.let { sb.appendLine("author: \"$it\"") }
        content.metadata.createdAt?.let { sb.appendLine("createdAt: \"$it\"") }
        content.metadata.updatedAt?.let { sb.appendLine("updatedAt: \"$it\"") }
        content.metadata.estimatedReadingTime?.let { sb.appendLine("estimatedReadingTime: $it") }
        content.metadata.wordCount?.let { sb.appendLine("wordCount: $it") }

        if (content.metadata.tags.isNotEmpty()) {
            sb.appendLine("tags:")
            content.metadata.tags.forEach { sb.appendLine("  - \"$it\"") }
        }

        content.metadata.category?.let { sb.appendLine("category: \"$it\"") }
        sb.appendLine("language: \"${content.metadata.language}\"")

        if (content.metadata.seoKeywords.isNotEmpty()) {
            sb.appendLine("seoKeywords:")
            content.metadata.seoKeywords.forEach { sb.appendLine("  - \"$it\"") }
        }

        content.metadata.seoDescription?.let { sb.appendLine("seoDescription: \"$it\"") }
        sb.appendLine("---")
        sb.appendLine()

        // 生成文章标题
        sb.appendLine("# ${content.title}")
        sb.appendLine()

        // 生成章节内容
        content.sections.sortedBy { it.order }.forEach { section ->
            // 添加章节标题（如果有）
            if (!section.title.isNullOrBlank()) {
                val headerPrefix = "#".repeat(section.level)
                sb.appendLine("$headerPrefix ${section.title}")
                sb.appendLine()
            }

            // 添加图片（如果有）
            if (!section.imagePath.isNullOrBlank()) {
                val altText = section.imageDescription ?: section.title ?: "配图"
                sb.appendLine("![$altText](${section.imagePath})")
                sb.appendLine()
            }

            // 添加章节内容
            sb.appendLine(section.content)
            sb.appendLine()
        }

        return sb.toString().trim()
    }
}
