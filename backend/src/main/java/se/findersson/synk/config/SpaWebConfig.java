package se.findersson.synk.config;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the SvelteKit SPA from classpath:/static/ (packaged by the frontend module).
 * Paths that don't match a static file fall back to index.html so client-side routing
 * works on page reload, except for /api/** which should 404 normally.
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    private static final Resource INDEX = new ClassPathResource("/static/index.html");

    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(final String resourcePath, final Resource location) throws IOException {
                        final Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        if (resourcePath.startsWith("api/") || !INDEX.exists()) {
                            return null;
                        }
                        return INDEX;
                    }
                });
    }

}
