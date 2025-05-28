package biz.zhizuo.translation.image

import biz.zhizuo.creative.utils.image.ImageTextAnalysisResult
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class ImageTranslator {

    /**
     * 根据提供的大模型分析结果，在图片上绘制翻译后的文字。
     *
     * @param inputImagePath 输入图片路径。
     * @param outputImagePath 输出图片路径。
     * @param imageTextAnalysisResult 大模型对图片文字的分析结果。
     */
    fun processImageWithTextAnalysis(
        inputImagePath: String,
        outputImagePath: String,
        imageTextAnalysisResult: ImageTextAnalysisResult
    ) {
        val imageFile = File(inputImagePath)
        if (!imageFile.exists()) {
            println("错误：输入图片文件不存在: $inputImagePath")
            return
        }

        val image: BufferedImage = ImageIO.read(imageFile)
        val graphics = image.createGraphics()

        // 开启抗锯齿，使文字更平滑
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        for (block in imageTextAnalysisResult.blocks) {
            val (text, translatedText, boundingBox, backgroundColorHex) = block
            val y = boundingBox[0] * image.height / 1000
            val x = boundingBox[1] * image.width / 1000
            val yMax = boundingBox[2] * image.height / 1000
            val xMax = boundingBox[3] * image.width / 1000

            val width = xMax - x;
            val height = yMax - y;

            // 1. 填充背景色
            try {
                val backgroundColor = Color.decode(backgroundColorHex)
                graphics.color = backgroundColor
                graphics.fillRect(x, y, width, height)
            } catch (e: NumberFormatException) {
                println("警告：无效的背景颜色代码 '${backgroundColorHex}' 对于文本 '${text}'。使用默认白色。")
                graphics.color = Color.WHITE
                graphics.fillRect(x, y, width, height)
            }

            // 2. 居中写入翻译结果
            graphics.color = Color.BLACK // 假设文字颜色为黑色
            
            var fontSize = height / 2 
            if (fontSize < 10) fontSize = 10 
            var font = Font("SansSerif", Font.PLAIN, fontSize)
            graphics.font = font
            
            var stringWidth = graphics.fontMetrics.stringWidth(translatedText)
            while (stringWidth > width && fontSize > 10) {
                fontSize--
                font = Font("SansSerif", Font.PLAIN, fontSize)
                graphics.font = font
                stringWidth = graphics.fontMetrics.stringWidth(translatedText)
            }
            
            val textX = x + (width - stringWidth) / 2
            val textY = y + (height - graphics.fontMetrics.height) / 2 + graphics.fontMetrics.ascent

            graphics.drawString(translatedText, textX, textY)
            println("在图片上处理文本: '$text' -> '$translatedText' at [${boundingBox.joinToString(",")}] with background $backgroundColorHex")
        }

        graphics.dispose()

        try {
            val outputFile = File(outputImagePath)
            if (!outputFile.parentFile.exists()) {
                outputFile.parentFile.mkdirs()
            }
            ImageIO.write(image, "jpg", outputFile)
            println("处理后的图片已保存到: $outputImagePath")
        } catch (e: Exception) {
            println("错误：保存图片失败: ${e.message}")
            e.printStackTrace()
        }
    }
}
