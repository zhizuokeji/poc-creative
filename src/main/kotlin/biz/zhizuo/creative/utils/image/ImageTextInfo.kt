package biz.zhizuo.creative.utils.image

/**
 * 表示图片中单个文字块的信息。
 *
 * @property text 原始文字内容。
 * @property translated_text 翻译后的文字内容（中文）。
 * @property bounding_box 文字块的边界框坐标，格式为 [xmin, ymin, xmax, ymax]。
 * @property background_color 文字块的背景颜色，Hex 格式，例如 "#RRGGBB"。
 */
data class TextBlockInfo(
    val text: String,
    val translated_text: String,
    val bounding_box: List<Int>,
    val background_color: String
)

/**
 * 表示大模型对整个图片文字分析的结果。
 *
 * @property blocks 包含图片中所有识别出的文字块信息的列表。
 */
data class ImageTextAnalysisResult(
    val blocks: List<TextBlockInfo>
)
