package biz.zhizuo.creative.utils.simple

import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Paths

/**
 * 简化的系统提示词加载器
 * 直接从文件读取，无缓存
 */
@Component
class SimplePromptLoader {

    companion object {
        private const val INSTRUCTIONS_DIR = "workspace/instructions"

        // 8个阶段的提示词文件名
        private val STAGE_FILES = mapOf(
            "creative" to "1. 创意阶段.md",
            "topic" to "2. 选题阶段.md",
            "outline" to "3. 构思阶段.md",
            "writing" to "4. 写作阶段.md",
            "optimization" to "5. 优化阶段.md",
            "image_planning" to "6. 配图计划阶段.md",
            "image_execution" to "7. 配图执行阶段.md",
            "completion" to "8. 完成阶段.md"
        )
    }

    /**
     * 加载指定阶段的系统提示词
     */
    fun loadPrompt(stageName: String): String {
        val fileName = STAGE_FILES[stageName]
            ?: throw IllegalArgumentException("未知的阶段: $stageName")

        val filePath = Paths.get(INSTRUCTIONS_DIR, fileName)

        if (!Files.exists(filePath)) {
            throw IllegalStateException("提示词文件不存在: $filePath")
        }

        return Files.readString(filePath)
    }

    /**
     * 获取所有阶段名称
     */
    fun getAllStageNames(): List<String> {
        return STAGE_FILES.keys.toList()
    }
}
