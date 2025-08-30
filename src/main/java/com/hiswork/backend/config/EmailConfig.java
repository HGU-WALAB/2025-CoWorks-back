package com.hiswork.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;

@Configuration
public class EmailConfig {

    @Bean
    public SpringResourceTemplateResolver emailTemplateResolver() {
        SpringResourceTemplateResolver r = new SpringResourceTemplateResolver();
        r.setPrefix("classpath:/mail-templates/");
        r.setSuffix(".html");
        r.setTemplateMode("HTML");
        r.setCharacterEncoding("UTF-8");
        r.setCacheable(true);
        r.setOrder(1);
        return r;
    }

    @Bean
    public SpringTemplateEngine templateEngine(SpringResourceTemplateResolver emailTemplateResolver) {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.addTemplateResolver(emailTemplateResolver);
        return engine;
    }
}
