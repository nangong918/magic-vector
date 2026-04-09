package com.demo.config;

import com.demo.component.interceptor.AuthTokenInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AuthRouteProperties.class)
public class AuthInterceptorConfig implements WebMvcConfigurer {

    private final AuthTokenInterceptor authTokenInterceptor;
    private final AuthRouteProperties authRouteProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        var registration = registry.addInterceptor(authTokenInterceptor);
        if (!CollectionUtils.isEmpty(authRouteProperties.getIncludePaths())) {
            registration.addPathPatterns(authRouteProperties.getIncludePaths());
        }
        if (!CollectionUtils.isEmpty(authRouteProperties.getExcludePaths())) {
            registration.excludePathPatterns(authRouteProperties.getExcludePaths());
        }
    }
}
