package biz.zhizuo.creative.utils

import biz.zhizuo.creative.utils.image.ImageTextAnalysisResult
import biz.zhizuo.translation.image.ImageTranslator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.content.Media
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import java.io.File
import kotlin.test.Test

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class CreativeUtilsApplicationTests {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    lateinit var chatModel: VertexAiGeminiChatModel

    @Autowired
    lateinit var objectMapper: ObjectMapper // Spring Boot should provide a configured one

    @Test
    fun `翻译图片`() {
        val inputImagePath = "src/test/resources/image.jpg"
        val outputImagePath = "build/image_cn.jpg" // Output to build directory

        val imageFile = File(inputImagePath)

        val imageResource = ByteArrayResource(imageFile.readBytes())

        val instructionText = """
            Detect the all of the texts in the image. The bounding_box should be [ymin, xmin, ymax, xmax] normalized to 0-1000.
            
            对于每个文字块，请提供以下信息，并以JSON格式返回一个包含对象列表的根对象（键为 "blocks"）：
            1.  `text`: 原始文字内容。
            2.  `translated_text`: 将原始文字翻译成简体中文后的内容。如果原文已经是中文，则保持不变。
            3.  `bounding_box`: 文字块在图片中的边界框，表示为四个整数的数组 `[xmin, ymin, xmax, ymax]`，代表左上角和右下角的像素坐标。
            4.  `background_color`: 文字块主要背景的十六进制颜色代码 (例如, "#RRGGBB")。

            确保JSON格式严格符合要求。例如:
            {
              "blocks": [
                {
                  "text": "Example",
                  "translated_text": "例子",
                  "bounding_box": [10, 20, 100, 50],
                  "background_color": "#FFFFFF"
                }
              ]
            }
        """.trimIndent()

        val userMessage =
            UserMessage.builder().text(instructionText).media(Media(MediaType.IMAGE_JPEG, imageResource)).build()

        val prompt = Prompt(userMessage)
        logger.info("向大模型发送图片分析请求...")
//        val chatResponse = chatModel.call(prompt)
//        val llmResponseContent = extractJson(chatResponse.result.output.text!!)
//        File("src/test/resources/image.json").writeText(llmResponseContent)
        val llmResponseContent = File("src/test/resources/image.json").readText()
        logger.info("大模型响应内容:\n$llmResponseContent")

        try {
            val imageTextAnalysisResult = objectMapper.readValue<ImageTextAnalysisResult>(llmResponseContent)

            val imageTranslator = ImageTranslator()
            imageTranslator.processImageWithTextAnalysis(inputImagePath, outputImagePath, imageTextAnalysisResult)

            logger.info("图片翻译处理完成。输出图片位于: $outputImagePath")
            logger.info("请确保您已在 application-e2e.yaml (或相应 profile 的配置文件) 中配置了多模态大模型 (如 OpenAI GPT-4o, Ollama LLaVA等) 的 Spring AI 相关属性。")

        } catch (e: Exception) {
            logger.error("解析大模型响应或处理图片时出错: ${e.message}", e)
            // 可以选择抛出异常使测试失败
            // throw e
        }
    }

    private fun extractJson(text: String): String {
        return text.trim().replace(Regex("^.*```json\n([\\s\\S]*)```$"), "$1")
    }
}
