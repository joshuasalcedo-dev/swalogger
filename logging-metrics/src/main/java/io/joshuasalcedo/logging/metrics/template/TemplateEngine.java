package io.joshuasalcedo.logging.metrics.template;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * FreeMarker template engine wrapper for generating metrics exports
 */
public class TemplateEngine {
    
    private static final String TEMPLATES_PATH = "/templates";
    private static TemplateEngine instance;
    private final Configuration cfg;
    
    private TemplateEngine() {
        cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setClassForTemplateLoading(getClass(), TEMPLATES_PATH);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);
        cfg.setFallbackOnNullLoopVariable(false);
    }
    
    public static synchronized TemplateEngine getInstance() {
        if (instance == null) {
            instance = new TemplateEngine();
        }
        return instance;
    }
    
    /**
     * Process a template with the given data model
     * 
     * @param templateName Name of the template file (e.g., "metrics.json.ftl")
     * @param dataModel Data model to be used in template processing
     * @return Processed template as string
     * @throws IOException If template cannot be loaded
     * @throws TemplateException If template processing fails
     */
    public String processTemplate(String templateName, Map<String, Object> dataModel) 
            throws IOException, TemplateException {
        Template template = cfg.getTemplate(templateName);
        StringWriter out = new StringWriter();
        template.process(dataModel, out);
        return out.toString();
    }
    
    /**
     * Process a template with the given data model and write to the provided Writer
     * 
     * @param templateName Name of the template file (e.g., "metrics.html.ftl")
     * @param dataModel Data model to be used in template processing
     * @param out Writer to output the processed template
     * @throws IOException If template cannot be loaded
     * @throws TemplateException If template processing fails
     */
    public void processTemplate(String templateName, Map<String, Object> dataModel, Writer out) 
            throws IOException, TemplateException {
        Template template = cfg.getTemplate(templateName);
        template.process(dataModel, out);
    }
    
    /**
     * Create a base data model with common variables
     * 
     * @return Base data model with system information and metadata
     */
    public Map<String, Object> createBaseDataModel() {
        Map<String, Object> dataModel = new HashMap<>();
        
        // Add timestamp
        dataModel.put("exportTimestamp", java.time.LocalDateTime.now().toString());
        
        // Add framework version
        dataModel.put("frameworkVersion", "1.0.0-SNAPSHOT");
        
        // Add system information
        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("javaVersion", System.getProperty("java.version"));
        systemInfo.put("javaVendor", System.getProperty("java.vendor"));
        systemInfo.put("osName", System.getProperty("os.name"));
        systemInfo.put("osVersion", System.getProperty("os.version"));
        systemInfo.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        systemInfo.put("maxMemory", Runtime.getRuntime().maxMemory());
        systemInfo.put("totalMemory", Runtime.getRuntime().totalMemory());
        systemInfo.put("freeMemory", Runtime.getRuntime().freeMemory());
        
        dataModel.put("systemInfo", systemInfo);
        
        return dataModel;
    }
    
    /**
     * Get the FreeMarker configuration for advanced usage
     * 
     * @return FreeMarker Configuration instance
     */
    public Configuration getConfiguration() {
        return cfg;
    }
}