package ec.nexo.customer.infrastructure.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

@Configuration
class WebConfig {

    /**
     * ETag en la experiencia: la app guarda el último layout y envía {@code If-None-Match};
     * si nada cambió recibe un 304 vacío y sigue usando su caché.
     */
    @Bean
    FilterRegistrationBean<ShallowEtagHeaderFilter> experienceEtagFilter() {
        var registration = new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        registration.addUrlPatterns("/experience/*");
        return registration;
    }
}
