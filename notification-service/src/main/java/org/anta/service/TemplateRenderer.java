package org.anta.service;

import java.util.Map;

public interface TemplateRenderer {

    String render(String templateId, Map<String, Object> data);
}