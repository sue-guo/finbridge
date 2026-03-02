package io.github.sueguo.finbridge.config;

import org.apache.camel.component.servlet.CamelHttpTransportServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CamelConfig {

    @Bean
    public ServletRegistrationBean<CamelHttpTransportServlet> camelServlet() {
        ServletRegistrationBean<CamelHttpTransportServlet> registration =
                new ServletRegistrationBean<>(new CamelHttpTransportServlet(), "/api/*");
        registration.setName("CamelServlet");
        return registration;
    }
}