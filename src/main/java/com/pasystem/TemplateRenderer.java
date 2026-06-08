package com.pasystem;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.loader.ClasspathLoader;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.io.StringWriter;
import java.util.Map;

public class TemplateRenderer {

    private final PebbleEngine engine;

    public TemplateRenderer(String basePath) {
        ClasspathLoader loader = new ClasspathLoader();
        loader.setPrefix(basePath);   // e.g. "templates"
        loader.setSuffix(".html");

        this.engine = new PebbleEngine.Builder()
                .loader(loader)
                .build();
    }

    public String render(String templateName, Map<String, Object> context) {
        try {
            PebbleTemplate template = engine.getTemplate(templateName);

            StringWriter writer = new StringWriter();
            template.evaluate(writer, context);

            return writer.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to render template: " + templateName, e);
        }
    }
}