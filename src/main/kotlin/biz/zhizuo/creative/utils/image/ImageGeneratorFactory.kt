package biz.zhizuo.creative.utils.image

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * 图片生成器工厂
 * 管理和提供不同类型的图片生成器实例
 */
@Component
class ImageGeneratorFactory(
    private val availableGenerators: List<ImageGenerator>,
) {

    private val logger = LoggerFactory.getLogger(ImageGeneratorFactory::class.java)

    @Value("\${image.generator.default-type:PIXABAY}")
    private lateinit var defaultGeneratorType: String

    @Value("\${image.generator.fallback-enabled:true}")
    private var fallbackEnabled: Boolean = true

    @Value("\${image.generator.priority-order:PIXABAY,AI_GENERATED}")
    private lateinit var priorityOrder: String

    /**
     * 获取默认图片生成器
     */
    fun getDefaultGenerator(): ImageGenerator {
        val defaultType = try {
            ImageGeneratorType.valueOf(defaultGeneratorType)
        } catch (e: IllegalArgumentException) {
            logger.warn("无效的默认生成器类型: $defaultGeneratorType，使用 PIXABAY")
            ImageGeneratorType.PIXABAY
        }

        return try {
            getGenerator(defaultType)
        } catch (error: Exception) {
            if (fallbackEnabled) {
                getFirstAvailableGenerator()
            } else {
                throw RuntimeException("默认图片生成器不可用且未启用回退机制")
            }
        }
    }

    /**
     * 获取指定类型的图片生成器
     */
    fun getGenerator(type: ImageGeneratorType): ImageGenerator {
        val generator = availableGenerators.find { it.getGeneratorType() == type }
            ?: throw RuntimeException("找不到类型为 $type 的生成器")

        val available = generator.validateAvailability()
        if (!available) {
            throw RuntimeException("生成器 $type 不可用")
        }

        return generator
    }

    /**
     * 获取第一个可用的图片生成器
     */
    fun getFirstAvailableGenerator(): ImageGenerator {
        val orderedGenerators = getOrderedGenerators()

        for (generator in orderedGenerators) {
            if (generator.validateAvailability()) {
                logger.info("选择图片生成器: ${generator.getGeneratorType()}")
                return generator
            }
        }

        logger.error("没有可用的图片生成器")
        throw RuntimeException("没有可用的图片生成器")
    }

    /**
     * 获取所有可用的图片生成器
     */
    fun getAllAvailableGenerators(): List<ImageGenerator> {
        return availableGenerators.filter { generator ->
            val available = generator.validateAvailability()
            if (available) {
                logger.debug("可用的图片生成器: ${generator.getGeneratorType()}")
            }
            available
        }
    }

    /**
     * 智能选择图片生成器
     * 根据请求特征选择最适合的生成器
     */
    fun selectBestGenerator(request: ImageGenerationRequest): ImageGenerator {
        val generators = getAllAvailableGenerators()
        if (generators.isEmpty()) {
            throw RuntimeException("没有可用的图片生成器")
        }

        // 根据请求特征选择最佳生成器
        return selectGeneratorByRequest(generators, request)
    }

    /**
     * 使用回退策略生成图片
     * 如果首选生成器失败，自动尝试其他可用生成器
     */
    fun generateWithFallback(request: ImageGenerationRequest): ImageGenerationResult {
        val primaryGenerator = selectBestGenerator(request)
        return generateWithFallbackChain(request, listOf(primaryGenerator))
    }

    /**
     * 获取工厂状态信息
     */
    fun getFactoryStatus(): ImageGeneratorFactoryStatus {
        val availableGenerators = getAllAvailableGenerators()
        val generatorStatuses = this.availableGenerators.map { generator ->
            GeneratorStatus(
                type = generator.getGeneratorType(),
                available = availableGenerators.contains(generator)
            )
        }

        return ImageGeneratorFactoryStatus(
            totalGenerators = this.availableGenerators.size,
            availableGenerators = availableGenerators.size,
            defaultType = try {
                ImageGeneratorType.valueOf(defaultGeneratorType)
            } catch (e: IllegalArgumentException) {
                ImageGeneratorType.PIXABAY
            },
            fallbackEnabled = fallbackEnabled,
            generatorStatuses = generatorStatuses
        )
    }

    /**
     * 根据优先级顺序排列生成器
     */
    private fun getOrderedGenerators(): List<ImageGenerator> {
        val priorityTypes = priorityOrder.split(",")
            .mapNotNull { typeName ->
                try {
                    ImageGeneratorType.valueOf(typeName.trim())
                } catch (e: IllegalArgumentException) {
                    logger.warn("无效的生成器类型: $typeName")
                    null
                }
            }

        val orderedGenerators = mutableListOf<ImageGenerator>()

        // 按优先级添加生成器
        priorityTypes.forEach { type ->
            availableGenerators.find { it.getGeneratorType() == type }?.let {
                orderedGenerators.add(it)
            }
        }

        // 添加剩余的生成器
        availableGenerators.forEach { generator ->
            if (!orderedGenerators.contains(generator)) {
                orderedGenerators.add(generator)
            }
        }

        return orderedGenerators
    }

    /**
     * 根据请求特征选择生成器
     */
    private fun selectGeneratorByRequest(
        generators: List<ImageGenerator>,
        request: ImageGenerationRequest,
    ): ImageGenerator {
        // 简单的选择逻辑，可以根据需要扩展
        return when {
            // 如果需要特定风格或创意内容，优先使用AI生成
            request.style == ImageStyle.ILLUSTRATION ||
                    request.style == ImageStyle.VECTOR ||
                    request.intent.contains("创意") ||
                    request.intent.contains("抽象") -> {
                generators.find { it.getGeneratorType() == ImageGeneratorType.AI_GENERATED }
                    ?: generators.first()
            }

            // 其他情况优先使用Pixabay
            else -> {
                generators.find { it.getGeneratorType() == ImageGeneratorType.PIXABAY }
                    ?: generators.first()
            }
        }
    }

    /**
     * 使用回退链生成图片
     */
    private fun generateWithFallbackChain(
        request: ImageGenerationRequest,
        generators: List<ImageGenerator>,
    ): ImageGenerationResult {
        if (generators.isEmpty()) {
            return ImageGenerationResult.failure(
                errorMessage = "没有可用的图片生成器",
                generatorType = ImageGeneratorType.CUSTOM
            )
        }

        for (generator in generators) {
            try {
                val result = generator.generateImage(request)
                if (result.success) {
                    return result
                } else {
                    logger.warn("生成器 ${generator.getGeneratorType()} 失败，尝试下一个")
                }
            } catch (e: Exception) {
                logger.warn("生成器 ${generator.getGeneratorType()} 异常，尝试下一个: ${e.message}")
            }
        }

        return ImageGenerationResult.failure(
            errorMessage = "所有图片生成器都失败了",
            generatorType = ImageGeneratorType.CUSTOM
        )
    }
}

/**
 * 图片生成器工厂状态
 */
data class ImageGeneratorFactoryStatus(
    val totalGenerators: Int,
    val availableGenerators: Int,
    val defaultType: ImageGeneratorType,
    val fallbackEnabled: Boolean,
    val generatorStatuses: List<GeneratorStatus>,
)

/**
 * 生成器状态
 */
data class GeneratorStatus(
    val type: ImageGeneratorType,
    val available: Boolean,
)
