package com.quizme;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

public class UnitTestUtils {

    @NonNull
    public static AppProperties initAppProperties() throws IOException {
        var loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> propertySources =
                loader.load("test", new ClassPathResource("application-test.yaml"));

        Binder binder = new Binder(ConfigurationPropertySources.from(propertySources));
        return binder.bind("app", AppProperties.class).get();
    }
}
