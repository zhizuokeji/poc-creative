package biz.zhizuo.creative.utils.workflow

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.ai.chat.client.ChatClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.web.client.RestClient

/**
 * 工作流配置类
 * 配置文章生成工作流所需的Bean
 */
@Configuration
class WorkflowConfiguration {

    /**
     * 配置ObjectMapper，支持Kotlin和Java时间类型
     */
    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        return ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .registerModule(JavaTimeModule())
    }

    /**
     * 配置YAML ObjectMapper，用于YAML文件处理
     */
    @Bean("yamlObjectMapper")
    fun yamlObjectMapper(): ObjectMapper {
        return ObjectMapper(YAMLFactory())
            .registerModule(KotlinModule.Builder().build())
            .registerModule(JavaTimeModule())
    }

    /**
     * 配置RestClient.Builder，用于HTTP请求
     */
    @Bean
    fun restClientBuilder(): RestClient.Builder {
        return RestClient.builder()
    }

    /**
     * 配置ChatClient，用于与AI模型交互
     */
    @Bean
    fun chatClient(chatClientBuilder: ChatClient.Builder): ChatClient {
        return chatClientBuilder.build()
    }
}
