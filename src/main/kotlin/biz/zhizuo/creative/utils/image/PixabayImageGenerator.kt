package biz.zhizuo.creative.utils.image

import biz.zhizuo.creative.utils.workflow.ImageSearchRequest
import biz.zhizuo.creative.utils.workflow.PixabayImageService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import biz.zhizuo.creative.utils.workflow.ImageInfo as WorkflowImageInfo

/**
 * Pixabay 图片生成器实现
 * 基于 Pixabay API 搜索和获取现有图片
 */
@Component
class PixabayImageGenerator(
    private val pixabayImageService: PixabayImageService,
) : ImageGenerator {

    private val logger = LoggerFactory.getLogger(PixabayImageGenerator::class.java)

    override fun generateImage(request: ImageGenerationRequest): ImageGenerationResult {
        val startTime = Instant.now()

        logger.info("开始使用Pixabay生成图片: intent=${request.intent}")

        return try {
            val searchRequest = buildSearchRequest(request)
            val searchResponse = pixabayImageService.searchImages(searchRequest)

            if (searchResponse.images.isNotEmpty()) {
                // 选择最佳图片
                val bestImage = selectBestImage(searchResponse.images)
                val imageInfo = convertToGeneratedImageInfo(bestImage)
                val duration = Instant.now().toEpochMilli() - startTime.toEpochMilli()

                ImageGenerationResult.success(
                    imageInfo = imageInfo,
                    generatorType = ImageGeneratorType.PIXABAY,
                    durationMs = duration,
                    metadata = mapOf(
                        "totalResults" to searchResponse.totalHits,
                        "query" to searchResponse.query
                    )
                )
            } else {
                val duration = Instant.now().toEpochMilli() - startTime.toEpochMilli()
                ImageGenerationResult.failure(
                    errorMessage = "未找到符合条件的图片: ${request.intent}",
                    generatorType = ImageGeneratorType.PIXABAY,
                    durationMs = duration
                )
            }
        } catch (error: Exception) {
            val duration = Instant.now().toEpochMilli() - startTime.toEpochMilli()
            logger.error("Pixabay图片生成失败", error)
            ImageGenerationResult.failure(
                errorMessage = "Pixabay API调用失败: ${error.message}",
                generatorType = ImageGeneratorType.PIXABAY,
                durationMs = duration
            )
        }
    }

    override fun validateAvailability(): Boolean {
        val available = pixabayImageService.validateConnection()
        if (available) {
            logger.info("Pixabay图片生成器可用")
        } else {
            logger.warn("Pixabay图片生成器不可用")
        }
        return available
    }

    override fun getGeneratorType(): ImageGeneratorType {
        return ImageGeneratorType.PIXABAY
    }

    /**
     * 构建Pixabay搜索请求
     */
    private fun buildSearchRequest(request: ImageGenerationRequest): ImageSearchRequest {
        // 构建搜索查询
        val query = buildSearchQuery(request)

        return ImageSearchRequest(
            query = query,
            imageType = request.style.apiValue,
            orientation = request.orientation.apiValue,
            category = request.category,
            minWidth = maxOf(request.minWidth, request.size.width),
            minHeight = maxOf(request.minHeight, request.size.height),
            perPage = 20 // 获取更多选项以便选择最佳图片
        )
    }

    /**
     * 构建搜索查询字符串
     */
    private fun buildSearchQuery(request: ImageGenerationRequest): String {
        val queryParts = mutableListOf<String>()

        // 添加主要意图
        queryParts.add(request.intent)

        // 添加关键词
        if (request.keywords.isNotEmpty()) {
            queryParts.addAll(request.keywords)
        }

        // 限制查询长度（Pixabay API限制）
        val query = queryParts.joinToString(" ").take(100)

        logger.debug("构建的搜索查询: $query")
        return query
    }

    /**
     * 选择最佳图片
     * 综合考虑浏览量、下载量、点赞数和尺寸匹配度
     */
    private fun selectBestImage(images: List<WorkflowImageInfo>): WorkflowImageInfo {
        return images.maxByOrNull { image ->
            // 计算综合评分
            val popularityScore = (image.views * 0.3 + image.downloads * 0.4 + image.likes * 0.3)

            // 尺寸匹配度加分（优先选择合适尺寸的图片）
            val sizeScore = when {
                image.imageWidth >= 1200 && image.imageHeight >= 900 -> 1.2
                image.imageWidth >= 800 && image.imageHeight >= 600 -> 1.1
                else -> 1.0
            }

            popularityScore * sizeScore
        } ?: images.first()
    }

    /**
     * 转换为通用图片信息格式
     */
    private fun convertToGeneratedImageInfo(
        pixabayImage: WorkflowImageInfo,
    ): GeneratedImageInfo {
        return GeneratedImageInfo(
            id = pixabayImage.id.toString(),
            title = pixabayImage.tags.split(",").take(3).joinToString(", "),
            tags = pixabayImage.tags.split(",").map { it.trim() },
            previewUrl = pixabayImage.previewURL,
            standardUrl = pixabayImage.webformatURL,
            highResUrl = pixabayImage.largeImageURL,
            width = pixabayImage.imageWidth,
            height = pixabayImage.imageHeight,
            format = extractFormatFromUrl(pixabayImage.webformatURL),
            source = ImageSource(
                platform = "Pixabay",
                author = pixabayImage.user,
                originalUrl = pixabayImage.webformatURL,
                license = "Pixabay License"
            ),
            metadata = mapOf(
                "views" to pixabayImage.views,
                "downloads" to pixabayImage.downloads,
                "likes" to pixabayImage.likes,
                "pixabayId" to pixabayImage.id
            )
        )
    }

    /**
     * 从URL提取图片格式
     */
    private fun extractFormatFromUrl(url: String): String {
        return url.substringAfterLast(".").substringBefore("?").lowercase().let { ext ->
            when (ext) {
                "jpg", "jpeg" -> "jpg"
                "png" -> "png"
                "webp" -> "webp"
                else -> "jpg" // 默认格式
            }
        }
    }
}
