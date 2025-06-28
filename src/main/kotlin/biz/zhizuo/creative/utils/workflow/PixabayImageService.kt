package biz.zhizuo.creative.utils.workflow

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

/**
 * Pixabay图片服务
 * 集成Pixabay API进行图片搜索、下载和处理
 */
@Service
class PixabayImageService(
    private val restClientBuilder: RestClient.Builder,
) {

    private val logger = LoggerFactory.getLogger(PixabayImageService::class.java)

    @Value("\${pixabay.api.key:}")
    private lateinit var apiKey: String

    @Value("\${pixabay.api.url:https://pixabay.com/api/}")
    private lateinit var apiUrl: String

    private val client by lazy {
        restClientBuilder
            .baseUrl(apiUrl)
            .build()
    }

    companion object {
        const val DEFAULT_PER_PAGE = 20
        const val DEFAULT_MIN_WIDTH = 640
        const val DEFAULT_MIN_HEIGHT = 480
    }

    /**
     * 搜索图片
     */
    fun searchImages(request: ImageSearchRequest): ImageSearchResponse {
        if (apiKey.isBlank()) {
            logger.error("Pixabay API密钥未配置")
            throw IllegalStateException("Pixabay API密钥未配置")
        }

        logger.info("搜索图片: query=${request.query}, category=${request.category}")

        return try {
            val apiResponse = client.get()
                .uri { uriBuilder ->
                    uriBuilder
                        .queryParam("key", apiKey)
                        .queryParam("q", request.query)
                        .queryParam("image_type", request.imageType)
                        .queryParam("orientation", request.orientation)
                        .queryParam("category", request.category)
                        .queryParam("min_width", request.minWidth)
                        .queryParam("min_height", request.minHeight)
                        .queryParam("per_page", request.perPage)
                        .queryParam("safesearch", "true")
                        .queryParam("order", "popular")
                        .build()
                }
                .retrieve()
                .body(PixabayApiResponse::class.java)
                ?: throw RuntimeException("API响应为空")

            val response = ImageSearchResponse(
                query = request.query,
                totalHits = apiResponse.totalHits,
                images = apiResponse.hits.map { hit ->
                    ImageInfo(
                        id = hit.id,
                        tags = hit.tags,
                        previewURL = hit.previewURL,
                        webformatURL = hit.webformatURL,
                        largeImageURL = hit.largeImageURL,
                        views = hit.views,
                        downloads = hit.downloads,
                        likes = hit.likes,
                        user = hit.user,
                        imageWidth = hit.imageWidth,
                        imageHeight = hit.imageHeight
                    )
                }
            )

            logger.info("图片搜索成功: query=${request.query}, 找到${response.images.size}张图片")
            response
        } catch (e: RestClientException) {
            logger.error("图片搜索失败: query=${request.query}", e)
            throw RuntimeException("图片搜索失败: ${e.message}", e)
        }
    }

    /**
     * 根据关键词和类型搜索最佳图片
     */
    fun findBestImage(
        keywords: String,
        imageType: String = "photo",
        category: String? = null,
    ): ImageInfo? {
        val request = ImageSearchRequest(
            query = keywords,
            imageType = imageType,
            category = category,
            perPage = 10 // 只获取前10张，选择最佳的
        )

        val response = searchImages(request)
        val bestImage = if (response.images.isNotEmpty()) {
            // 选择最受欢迎的图片（综合考虑浏览量、下载量、点赞数）
            response.images.maxByOrNull { image ->
                (image.views * 0.3 + image.downloads * 0.4 + image.likes * 0.3).toInt()
            }
        } else {
            null
        }

        if (bestImage != null) {
            logger.info("找到最佳图片: id=${bestImage.id}, tags=${bestImage.tags}")
        } else {
            logger.warn("未找到合适的图片: keywords=$keywords")
        }

        return bestImage
    }

    /**
     * 验证API连接
     */
    fun validateConnection(): Boolean {
        if (apiKey.isBlank()) {
            return false
        }

        return try {
            searchImages(
                ImageSearchRequest(
                    query = "test",
                    perPage = 3
                )
            )
            logger.info("Pixabay API连接验证成功")
            true
        } catch (e: Exception) {
            logger.error("Pixabay API连接验证失败", e)
            false
        }
    }
}

/**
 * 图片搜索请求
 */
data class ImageSearchRequest(
    val query: String,
    val imageType: String = "photo", // "all", "photo", "illustration", "vector"
    val orientation: String = "all", // "all", "horizontal", "vertical"
    val category: String? = null,
    val minWidth: Int = PixabayImageService.DEFAULT_MIN_WIDTH,
    val minHeight: Int = PixabayImageService.DEFAULT_MIN_HEIGHT,
    val perPage: Int = PixabayImageService.DEFAULT_PER_PAGE,
)

/**
 * 图片搜索响应
 */
data class ImageSearchResponse(
    val query: String,
    val totalHits: Int,
    val images: List<ImageInfo>,
)

/**
 * 图片信息
 */
data class ImageInfo(
    val id: Long,
    val tags: String,
    val previewURL: String,
    val webformatURL: String,
    val largeImageURL: String,
    val views: Int,
    val downloads: Int,
    val likes: Int,
    val user: String,
    val imageWidth: Int,
    val imageHeight: Int,
)

/**
 * Pixabay API响应
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class PixabayApiResponse(
    val total: Int,
    val totalHits: Int,
    val hits: List<PixabayImageHit>,
)

/**
 * Pixabay图片数据
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class PixabayImageHit(
    val id: Long,
    val tags: String,
    val previewURL: String,
    val webformatURL: String,
    val largeImageURL: String,
    val views: Int,
    val downloads: Int,
    val likes: Int,
    val user: String,
    @JsonProperty("imageWidth") val imageWidth: Int,
    @JsonProperty("imageHeight") val imageHeight: Int,
)
