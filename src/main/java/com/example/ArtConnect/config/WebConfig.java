package com.example.ArtConnect.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import com.example.ArtConnect.security.CurrentUserArgumentResolver;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String[] frontendOrigins;
    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    /**
     * app.frontend.origin is a comma-separated list of allowed browser origins. Patterns are
     * allowed, e.g. "http://localhost:[*]" for any local port, or "https://*.example.com".
     */
    public WebConfig(@Value("${app.frontend.origin:http://localhost:[*],http://127.0.0.1:[*]}") String frontendOrigins,
                     CurrentUserArgumentResolver currentUserArgumentResolver) {
        this.frontendOrigins = Arrays.stream(frontendOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
        this.currentUserArgumentResolver = currentUserArgumentResolver;
    }

    /** Only needed when the React app is hosted on a different origin than this API. */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(frontendOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
    }

    /**
     * Serves the built React app from classpath:/static. Unknown non-API paths without a file
     * extension fall back to index.html so client-side routes like /artworks/42 work on reload.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) return requested;
                        if (resourcePath.startsWith("api/") || resourcePath.contains(".")) return null;
                        Resource index = location.createRelative("index.html");
                        return index.exists() && index.isReadable() ? index : null;
                    }
                });
    }
}
