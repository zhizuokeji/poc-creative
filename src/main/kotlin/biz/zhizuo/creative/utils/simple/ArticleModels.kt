package biz.zhizuo.creative.utils.simple

/**
 * 文章生成请求
 */
data class ArticleRequest(
    val originalIdea: String,           // 原始想法
    val themeKeyword: String? = null,    // 主题词（可选）
)

/**
 * 文章生成结果
 */
data class ArticleResult(
    val title: String,                  // 文章标题
    val content: String,                // 文章内容（Markdown格式）
    val filePath: String,               // 保存的文件路径
    val imagesDir: String,               // 图片目录路径
)

/**
 * 阶段执行结果
 */
data class StageOutput(
    val stageName: String,              // 阶段名称
    val input: String,                  // 输入内容
    val output: String,                  // 输出内容
)
