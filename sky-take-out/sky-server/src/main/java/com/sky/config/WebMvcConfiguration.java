package com.sky.config;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import com.sky.json.JacksonObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Parameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.Collections;
import java.util.List;

/**
 * 配置类，注册web层相关组件
 */
@Configuration
@EnableKnife4j
@Slf4j
public class WebMvcConfiguration extends WebMvcConfigurationSupport {

    /**
     * 设置静态资源映射
     * @param registry
     */
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/swagger-ui/**").addResourceLocations("classpath:/META-INF/resources/webjars/springfox-swagger-ui/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    /**
     * 扩展Spring MVC框架的消息转化器
     * @param converters
     */
    protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("扩展消息转换器...");
        //创建一个消息转换器对象
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        //需要为消息转换器设置一个对象转换器，对象转换器可以将Java对象序列化为json数据
        converter.setObjectMapper(new JacksonObjectMapper());
        //将自己的消息转化器加入容器中
        converters.add(0,converter);
    }

    @Bean
    public Docket logisticsDocket(){
        log.info("准备生成智慧物流接口文档...");
        ApiInfo apiInfo = new ApiInfoBuilder()
                .title("智慧物流 IoT 平台接口文档")
                .version("0.1-starter")
                .description("智慧物流 IoT 平台 starter 接口文档")
                .build();

        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("智慧物流接口")
                .apiInfo(apiInfo)
                .globalOperationParameters(Collections.singletonList(authorizationHeader()))
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.sky.logistics.controller"))
                .paths(PathSelectors.ant("/api/v1/**"))
                .build();
    }

    private Parameter authorizationHeader() {
        return new ParameterBuilder()
                .name("Authorization")
                .description("登录后填写：Bearer <accessToken>")
                .modelRef(new ModelRef("string"))
                .parameterType("header")
                .required(false)
                .build();
    }
}
