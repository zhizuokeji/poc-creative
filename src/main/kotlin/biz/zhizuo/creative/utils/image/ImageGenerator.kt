package biz.zhizuo.creative.utils.image

/**
 * 图片生成器接口
 * 提供统一的图片生成抽象，支持多种实现方式（API获取、AI生成等）
 */
interface ImageGenerator {

    /**
     * 根据意图生成图片
     *
     * @param request 图片生成请求
     * @return 图片生成结果
     */
    fun generateImage(request: ImageGenerationRequest): ImageGenerationResult

    /**
     * 验证图片生成器是否可用
     *
     * @return 是否可用
     */
    fun validateAvailability(): Boolean

    /**
     * 获取图片生成器类型
     *
     * @return 生成器类型
     */
    fun getGeneratorType(): ImageGeneratorType
}

/**
 * 图片生成请求
 */
data class ImageGenerationRequest(
    /**
     * 图片意图描述（必需）
     */
    val intent: String,

    /**
     * 图片尺寸要求
     */
    val size: ImageSize = ImageSize.MEDIUM,

    /**
     * 图片风格
     */
    val style: ImageStyle = ImageStyle.PHOTO,

    /**
     * 图片方向
     */
    val orientation: ImageOrientation = ImageOrientation.HORIZONTAL,

    /**
     * 图片类别（可选）
     */
    val category: String? = null,

    /**
     * 关键词列表（可选，用于搜索优化）
     */
    val keywords: List<String> = emptyList(),

    /**
     * 最小宽度（像素）
     */
    val minWidth: Int = 640,

    /**
     * 最小高度（像素）
     */
    val minHeight: Int = 480,

    /**
     * 是否启用安全搜索
     */
    val safeSearch: Boolean = true,

    /**
     * 额外的生成参数
     */
    val additionalParams: Map<String, Any> = emptyMap(),
)

/**
 * 图片生成结果
 */
data class ImageGenerationResult(
    /**
     * 是否成功
     */
    val success: Boolean,

    /**
     * 图片信息（成功时）
     */
    val imageInfo: GeneratedImageInfo? = null,

    /**
     * 错误信息（失败时）
     */
    val errorMessage: String? = null,

    /**
     * 生成器类型
     */
    val generatorType: ImageGeneratorType,

    /**
     * 生成耗时（毫秒）
     */
    val durationMs: Long = 0,

    /**
     * 额外的元数据
     */
    val metadata: Map<String, Any> = emptyMap(),
) {
    companion object {
        fun success(
            imageInfo: GeneratedImageInfo,
            generatorType: ImageGeneratorType,
            durationMs: Long = 0,
            metadata: Map<String, Any> = emptyMap(),
        ) = ImageGenerationResult(
            success = true,
            imageInfo = imageInfo,
            generatorType = generatorType,
            durationMs = durationMs,
            metadata = metadata
        )

        fun failure(
            errorMessage: String,
            generatorType: ImageGeneratorType,
            durationMs: Long = 0,
        ) = ImageGenerationResult(
            success = false,
            errorMessage = errorMessage,
            generatorType = generatorType,
            durationMs = durationMs
        )
    }
}

/**
 * 生成的图片信息
 */
data class GeneratedImageInfo(
    /**
     * 图片唯一标识
     */
    val id: String,

    /**
     * 图片标题或描述
     */
    val title: String,

    /**
     * 图片标签
     */
    val tags: List<String>,

    /**
     * 预览图URL
     */
    val previewUrl: String,

    /**
     * 标准尺寸图片URL
     */
    val standardUrl: String,

    /**
     * 高清图片URL（可选）
     */
    val highResUrl: String? = null,

    /**
     * 图片宽度
     */
    val width: Int,

    /**
     * 图片高度
     */
    val height: Int,

    /**
     * 图片格式（jpg, png, webp等）
     */
    val format: String,

    /**
     * 图片来源信息
     */
    val source: ImageSource,

    /**
     * 额外的图片元数据
     */
    val metadata: Map<String, Any> = emptyMap(),
)

/**
 * 图片来源信息
 */
data class ImageSource(
    /**
     * 来源平台
     */
    val platform: String,

    /**
     * 作者信息（可选）
     */
    val author: String? = null,

    /**
     * 原始URL
     */
    val originalUrl: String? = null,

    /**
     * 许可证信息
     */
    val license: String? = null,
)

/**
 * 图片生成器类型
 */
enum class ImageGeneratorType(val displayName: String) {
    PIXABAY("Pixabay API"),
    AI_GENERATED("AI生成"),
    UNSPLASH("Unsplash API"),
    CUSTOM("自定义")
}

/**
 * 图片尺寸
 */
enum class ImageSize(val displayName: String, val width: Int, val height: Int) {
    SMALL("小尺寸", 400, 300),
    MEDIUM("中等尺寸", 800, 600),
    LARGE("大尺寸", 1200, 900),
    EXTRA_LARGE("超大尺寸", 1920, 1080)
}

/**
 * 图片风格
 */
enum class ImageStyle(val displayName: String, val apiValue: String) {
    PHOTO("照片", "photo"),
    ILLUSTRATION("插画", "illustration"),
    VECTOR("矢量图", "vector"),
    ALL("所有类型", "all")
}

/**
 * 图片方向
 */
enum class ImageOrientation(val displayName: String, val apiValue: String) {
    HORIZONTAL("横向", "horizontal"),
    VERTICAL("纵向", "vertical"),
    ALL("所有方向", "all")
}
