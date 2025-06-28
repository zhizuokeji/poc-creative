package biz.zhizuo.creative.utils.workflow

import org.imgscalr.Scalr
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

/**
 * 图片处理器
 * 处理图片下载、移动端尺寸优化、相对路径生成等功能
 */
@Component
class ImageProcessor(
    private val restClientBuilder: RestClient.Builder,
) {

    private val logger = LoggerFactory.getLogger(ImageProcessor::class.java)

    private val client by lazy {
        restClientBuilder.build()
    }

    companion object {
        private const val MOBILE_MAX_WIDTH = 800
        private const val MOBILE_MAX_HEIGHT = 600
        private val SUPPORTED_FORMATS = setOf("jpg", "jpeg", "png", "webp")
    }

    /**
     * 下载并处理图片
     */
    fun downloadAndProcessImage(
        imageInfo: ImageInfo,
        targetDirectory: Path,
        fileName: String? = null,
    ): ProcessedImageResult {
        val finalFileName = fileName ?: generateFileName(imageInfo)
        val targetPath = targetDirectory.resolve(finalFileName)

        logger.info("开始下载图片: ${imageInfo.webformatURL} -> ${targetPath}")

        return try {
            val imageBytes = downloadImage(imageInfo.webformatURL)
            val result = processImageForMobile(imageBytes, targetPath)

            logger.info("图片处理完成: ${result.filePath}, 原始尺寸: ${result.originalWidth}x${result.originalHeight}, 处理后尺寸: ${result.processedWidth}x${result.processedHeight}")
            result
        } catch (e: Exception) {
            logger.error("图片处理失败: ${imageInfo.webformatURL}", e)
            throw RuntimeException("图片处理失败: ${e.message}", e)
        }
    }

    /**
     * 下载图片
     */
    private fun downloadImage(imageUrl: String): ByteArray {
        return try {
            val bytes = client.get()
                .uri(imageUrl)
                .retrieve()
                .body(ByteArray::class.java)
                ?: throw RuntimeException("下载的图片数据为空")

            logger.debug("图片下载完成: ${imageUrl}, 大小: ${bytes.size} bytes")
            bytes
        } catch (e: RestClientException) {
            logger.error("图片下载失败: $imageUrl", e)
            throw RuntimeException("图片下载失败: ${e.message}", e)
        }
    }

    /**
     * 为移动端优化图片
     */
    private fun processImageForMobile(
        imageBytes: ByteArray,
        targetPath: Path,
    ): ProcessedImageResult {
        return try {
            // 确保目标目录存在
            Files.createDirectories(targetPath.parent)

            // 读取原始图片
            val originalImage = ImageIO.read(ByteArrayInputStream(imageBytes))
                ?: throw IllegalArgumentException("无法读取图片数据")

            val originalWidth = originalImage.width
            val originalHeight = originalImage.height

            // 判断是否需要缩放
            val needsResize = originalWidth > MOBILE_MAX_WIDTH || originalHeight > MOBILE_MAX_HEIGHT

            val processedImage = if (needsResize) {
                // 计算缩放比例，保持宽高比
                val widthRatio = MOBILE_MAX_WIDTH.toDouble() / originalWidth
                val heightRatio = MOBILE_MAX_HEIGHT.toDouble() / originalHeight
                val scaleFactor = minOf(widthRatio, heightRatio)

                val newWidth = (originalWidth * scaleFactor).toInt()
                val newHeight = (originalHeight * scaleFactor).toInt()

                logger.debug("缩放图片: ${originalWidth}x${originalHeight} -> ${newWidth}x${newHeight}")

                Scalr.resize(
                    originalImage,
                    Scalr.Method.QUALITY,
                    Scalr.Mode.FIT_EXACT,
                    newWidth,
                    newHeight
                )
            } else {
                logger.debug("图片尺寸合适，无需缩放: ${originalWidth}x${originalHeight}")
                originalImage
            }

            // 保存处理后的图片
            val format = getImageFormat(targetPath)
            ImageIO.write(processedImage, format, targetPath.toFile())

            ProcessedImageResult(
                filePath = targetPath,
                originalWidth = originalWidth,
                originalHeight = originalHeight,
                processedWidth = processedImage.width,
                processedHeight = processedImage.height,
                fileSize = Files.size(targetPath),
                format = format,
                wasResized = needsResize
            )
        } catch (e: Exception) {
            logger.error("图片处理失败: $targetPath", e)
            throw RuntimeException("图片处理失败: ${e.message}", e)
        }
    }

    /**
     * 生成文件名
     */
    private fun generateFileName(imageInfo: ImageInfo): String {
        val extension = getExtensionFromUrl(imageInfo.webformatURL)
        val sanitizedTags = imageInfo.tags
            .split(",")
            .take(3)
            .joinToString("-") { it.trim().replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5]"), "") }
            .take(50)

        return "image_${imageInfo.id}_${sanitizedTags}.${extension}"
    }

    /**
     * 从URL获取文件扩展名
     */
    private fun getExtensionFromUrl(url: String): String {
        val extension = url.substringAfterLast('.').substringBefore('?').lowercase()
        return if (extension in SUPPORTED_FORMATS) extension else "jpg"
    }

    /**
     * 获取图片格式
     */
    private fun getImageFormat(filePath: Path): String {
        val extension = filePath.fileName.toString().substringAfterLast('.').lowercase()
        return when (extension) {
            "png" -> "png"
            "webp" -> "webp"
            else -> "jpg"
        }
    }

    /**
     * 生成相对于Markdown文件的图片路径
     */
    fun generateRelativePath(
        markdownFilePath: Path,
        imagePath: Path,
    ): String {
        return try {
            val markdownDir = markdownFilePath.parent
            val relativePath = markdownDir.relativize(imagePath)
            relativePath.toString().replace('\\', '/') // 确保使用正斜杠
        } catch (e: Exception) {
            logger.warn("无法生成相对路径，使用绝对路径: ${e.message}")
            imagePath.toString().replace('\\', '/')
        }
    }

    /**
     * 创建图片目录
     */
    fun createImageDirectory(articleDirectory: Path): Path {
        val imageDir = articleDirectory.resolve("images")
        Files.createDirectories(imageDir)
        logger.debug("创建图片目录: ${imageDir}")
        return imageDir
    }

    /**
     * 验证图片文件
     */
    fun validateImageFile(filePath: Path): Boolean {
        return try {
            if (!Files.exists(filePath)) {
                logger.warn("图片文件不存在: ${filePath}")
                return false
            }

            val image = ImageIO.read(filePath.toFile())
            if (image == null) {
                logger.warn("无法读取图片文件: ${filePath}")
                return false
            }

            logger.debug("图片文件验证通过: ${filePath}, 尺寸: ${image.width}x${image.height}")
            true
        } catch (e: Exception) {
            logger.error("图片文件验证失败: ${filePath}", e)
            false
        }
    }

    /**
     * 清理临时文件
     */
    fun cleanupTempFiles(directory: Path) {
        try {
            Files.walk(directory)
                .filter { it.fileName.toString().startsWith("temp_") }
                .forEach { tempFile ->
                    try {
                        Files.deleteIfExists(tempFile)
                        logger.debug("清理临时文件: ${tempFile}")
                    } catch (e: Exception) {
                        logger.warn("清理临时文件失败: ${tempFile}", e)
                    }
                }
        } catch (e: Exception) {
            logger.error("清理临时文件时发生错误", e)
        }
    }
}

/**
 * 图片处理结果
 */
data class ProcessedImageResult(
    val filePath: Path,
    val originalWidth: Int,
    val originalHeight: Int,
    val processedWidth: Int,
    val processedHeight: Int,
    val fileSize: Long,
    val format: String,
    val wasResized: Boolean,
) {
    /**
     * 获取处理摘要
     */
    fun getSummary(): String {
        val resizeInfo = if (wasResized) {
            "已缩放从 ${originalWidth}x${originalHeight} 到 ${processedWidth}x${processedHeight}"
        } else {
            "未缩放 ${processedWidth}x${processedHeight}"
        }
        return "图片处理完成: ${filePath.fileName}, $resizeInfo, 文件大小: ${fileSize / 1024}KB"
    }
}
