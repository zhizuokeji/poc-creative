package biz.zhizuo.creative.utils.simple

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory

/**
 * JSON Schema验证器
 * 用于验证各阶段输出的JSON格式是否符合规范
 */
object JsonSchemaValidator {

    private val logger = LoggerFactory.getLogger(JsonSchemaValidator::class.java)
    private val objectMapper = ObjectMapper()

    /**
     * 验证结果
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList(),
        val warnings: List<String> = emptyList(),
    )

    /**
     * 验证构思阶段的JSON输出
     */
    fun validateOutlineJson(jsonString: String): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        try {
            val outline = JsonYamlProcessor.parseOutlineContent(jsonString)

            // 验证必填字段
            if (outline.title.isBlank()) {
                errors.add("title字段不能为空")
            }
            if (outline.description.isBlank()) {
                errors.add("description字段不能为空")
            }
            if (outline.structure.isBlank()) {
                errors.add("structure字段不能为空")
            }
            if (outline.totalEstimatedWords <= 0) {
                errors.add("totalEstimatedWords必须大于0")
            }

            // 验证大纲结构
            validateOutlineSection(outline.outline.introduction, "introduction", errors, warnings)

            if (outline.outline.mainSections.isEmpty()) {
                errors.add("mainSections不能为空")
            } else {
                outline.outline.mainSections.forEachIndexed { index, section ->
                    validateOutlineSection(section, "mainSections[$index]", errors, warnings)
                }
            }

            validateOutlineSection(outline.outline.conclusion, "conclusion", errors, warnings)

        } catch (e: Exception) {
            errors.add("JSON格式错误: ${e.message}")
        }

        return ValidationResult(errors.isEmpty(), errors, warnings)
    }

    /**
     * 验证文章内容的JSON输出
     */
    fun validateArticleJson(jsonString: String): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        try {
            val article = JsonYamlProcessor.parseArticleContent(jsonString)

            // 验证必填字段
            if (article.title.isBlank()) {
                errors.add("title字段不能为空")
            }
            if (article.description.isBlank()) {
                errors.add("description字段不能为空")
            }
            if (article.sections.isEmpty()) {
                errors.add("sections不能为空")
            }

            // 验证章节
            article.sections.forEachIndexed { index, section ->
                validateArticleSection(section, index, errors, warnings)
            }

            // 验证元数据
            validateMetadata(article.metadata, errors, warnings)

        } catch (e: Exception) {
            errors.add("JSON格式错误: ${e.message}")
        }

        return ValidationResult(errors.isEmpty(), errors, warnings)
    }

    /**
     * 验证大纲章节
     */
    private fun validateOutlineSection(
        section: OutlineSection,
        sectionName: String,
        errors: MutableList<String>,
        warnings: MutableList<String>,
    ) {
        if (section.title.isBlank()) {
            warnings.add("$sectionName 的title为空")
        }
        if (section.keyPoints.isEmpty()) {
            errors.add("$sectionName 的keyPoints不能为空")
        }
        if (section.estimatedWords <= 0) {
            errors.add("$sectionName 的estimatedWords必须大于0")
        }

        // 验证子章节
        section.subsections.forEachIndexed { index, subsection ->
            if (subsection.title.isBlank()) {
                warnings.add("$sectionName.subsections[$index] 的title为空")
            }
            if (subsection.keyPoints.isEmpty()) {
                errors.add("$sectionName.subsections[$index] 的keyPoints不能为空")
            }
            if (subsection.estimatedWords <= 0) {
                errors.add("$sectionName.subsections[$index] 的estimatedWords必须大于0")
            }
        }
    }

    /**
     * 验证文章章节
     */
    private fun validateArticleSection(
        section: ArticleSection,
        index: Int,
        errors: MutableList<String>,
        warnings: MutableList<String>,
    ) {
        if (section.content.isBlank()) {
            errors.add("sections[$index] 的content不能为空")
        }
        if (section.order <= 0) {
            errors.add("sections[$index] 的order必须大于0")
        }
        if (section.level < 1 || section.level > 6) {
            errors.add("sections[$index] 的level必须在1-6之间")
        }

        // 检查内容长度
        if (section.content.length < 50) {
            warnings.add("sections[$index] 的content可能过短（少于50字符）")
        }

        // 检查配图描述
        if (!section.imageDescription.isNullOrBlank() && section.imageDescription!!.length < 10) {
            warnings.add("sections[$index] 的imageDescription可能过短")
        }
    }

    /**
     * 验证元数据
     */
    private fun validateMetadata(
        metadata: ArticleMetadata,
        errors: MutableList<String>,
        warnings: MutableList<String>,
    ) {
        // 验证字数统计
        if (metadata.wordCount != null && metadata.wordCount!! <= 0) {
            errors.add("metadata.wordCount必须大于0")
        }

        // 验证阅读时间
        if (metadata.estimatedReadingTime != null && metadata.estimatedReadingTime!! <= 0) {
            errors.add("metadata.estimatedReadingTime必须大于0")
        }

        // 验证语言代码
        if (metadata.language.isBlank()) {
            warnings.add("metadata.language为空，建议设置为zh-CN")
        }

        // 验证标签
        if (metadata.tags.isEmpty()) {
            warnings.add("metadata.tags为空，建议添加相关标签")
        }

        // 验证优化记录
        metadata.optimizations.forEachIndexed { index, optimization ->
            if (optimization.type.isBlank()) {
                errors.add("metadata.optimizations[$index].type不能为空")
            }
            if (optimization.description.isBlank()) {
                errors.add("metadata.optimizations[$index].description不能为空")
            }
            if (optimization.reason.isBlank()) {
                errors.add("metadata.optimizations[$index].reason不能为空")
            }
        }
    }

    /**
     * 验证JSON字符串的基本格式
     */
    fun validateJsonFormat(jsonString: String): ValidationResult {
        val errors = mutableListOf<String>()

        try {
            objectMapper.readTree(jsonString)
        } catch (e: Exception) {
            errors.add("无效的JSON格式: ${e.message}")
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * 验证并记录结果
     */
    fun validateAndLog(jsonString: String, stage: String, expectedType: String): Boolean {
        val formatResult = validateJsonFormat(jsonString)
        if (!formatResult.isValid) {
            logger.error("${stage}阶段JSON格式验证失败: ${formatResult.errors}")
            return false
        }

        val validationResult = when (expectedType) {
            "outline" -> validateOutlineJson(jsonString)
            "article" -> validateArticleJson(jsonString)
            else -> ValidationResult(false, listOf("未知的验证类型: $expectedType"))
        }

        if (!validationResult.isValid) {
            logger.error("${stage}阶段数据验证失败: ${validationResult.errors}")
            return false
        }

        if (validationResult.warnings.isNotEmpty()) {
            logger.warn("${stage}阶段数据验证警告: ${validationResult.warnings}")
        }

        logger.info("${stage}阶段数据验证通过")
        return true
    }
}
