package com.redink.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 配置管理类
 * 负责加载和管理系统配置
 */
@Component
public class ConfigManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    
    private final Environment environment;
    
    // 配置缓存
    private Map<String, Object> imageProvidersConfig;
    private Map<String, Object> textProvidersConfig;
    
    public ConfigManager(Environment environment) {
        this.environment = environment;
    }
    
    /**
     * 加载图片服务商配置
     */
    public Map<String, Object> loadImageProvidersConfig() {
        if (imageProvidersConfig != null) {
            return imageProvidersConfig;
        }
        
        Path configPath = Paths.get("image_providers.yaml");
        if (!Files.exists(configPath)) {
            logger.warn("图片配置文件不存在: {}, 使用默认配置", configPath);
            imageProvidersConfig = createDefaultImageConfig();
            return imageProvidersConfig;
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            imageProvidersConfig = mapper.readValue(configPath.toFile(), Map.class);
            logger.debug("图片配置加载成功: {}", imageProvidersConfig.keySet());
        } catch (IOException e) {
            logger.error("图片配置文件格式错误: {}", configPath, e);
            throw new IllegalArgumentException(
                "配置文件格式错误: image_providers.yaml\n" +
                "YAML 解析错误: " + e.getMessage() + "\n" +
                "解决方案：\n" +
                "1. 检查 YAML 缩进是否正确（使用空格，不要用Tab）\n" +
                "2. 检查引号是否配对\n" +
                "3. 使用在线 YAML 验证器检查格式"
            );
        }
        
        return imageProvidersConfig;
    }
    
    /**
     * 加载文本生成服务商配置
     */
    public Map<String, Object> loadTextProvidersConfig() {
        if (textProvidersConfig != null) {
            return textProvidersConfig;
        }
        
        Path configPath = Paths.get("text_providers.yaml");
        if (!Files.exists(configPath)) {
            logger.warn("文本配置文件不存在: {}, 使用默认配置", configPath);
            textProvidersConfig = createDefaultTextConfig();
            return textProvidersConfig;
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            textProvidersConfig = mapper.readValue(configPath.toFile(), Map.class);
            logger.debug("文本配置加载成功: {}", textProvidersConfig.keySet());
        } catch (IOException e) {
            logger.error("文本配置文件格式错误: {}", configPath, e);
            throw new IllegalArgumentException(
                "配置文件格式错误: text_providers.yaml\n" +
                "YAML 解析错误: " + e.getMessage() + "\n" +
                "解决方案：\n" +
                "1. 检查 YAML 缩进是否正确（使用空格，不要用Tab）\n" +
                "2. 检查引号是否配对\n" +
                "3. 使用在线 YAML 验证器检查格式"
            );
        }
        
        return textProvidersConfig;
    }
    
    /**
     * 获取激活的图片服务商
     */
    public String getActiveImageProvider() {
        Map<String, Object> config = loadImageProvidersConfig();
        return config.getOrDefault("active_provider", "google_genai").toString();
    }
    
    /**
     * 获取激活的文本服务商
     */
    public String getActiveTextProvider() {
        Map<String, Object> config = loadTextProvidersConfig();
        return config.getOrDefault("active_provider", "openai").toString();
    }
    
    /**
     * 获取服务商配置
     */
    public Map<String, Object> getProviderConfig(String type, String providerName) {
        Map<String, Object> config;
        Map<String, Object> providers;
        
        if ("image".equals(type)) {
            config = loadImageProvidersConfig();
        } else if ("text".equals(type)) {
            config = loadTextProvidersConfig();
        } else {
            return new HashMap<>();
        }
        
        providers = (Map<String, Object>) config.get("providers");
        if (providers == null) {
            providers = new HashMap<>();
        }
        
        return (Map<String, Object>) providers.get(providerName);
    }
    
    /**
     * 验证服务商配置
     */
    public void validateProviderConfig(String type, String providerName) {
        Map<String, Object> providerConfig = getProviderConfig(type, providerName);
        if (providerConfig == null) {
            throw new IllegalArgumentException(
                "未找到服务商配置: " + providerName + "\n" +
                "解决方案：\n" +
                "1. 在系统设置页面添加该服务商\n" +
                "2. 或修改 active_provider 为已存在的服务商\n" +
                "3. 检查 " + type + "_providers.yaml 文件"
            );
        }
        
        String apiKey = (String) providerConfig.get("api_key");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "服务商 " + providerName + " 未配置 API Key\n" +
                "解决方案：\n" +
                "1. 在系统设置页面编辑该服务商，填写 API Key\n" +
                "2. 或手动在 " + type + "_providers.yaml 中添加 api_key 字段"
            );
        }
        
        String providerType = (String) providerConfig.getOrDefault("type", providerName);
        if (providerType.startsWith("openai") || "image_api".equals(providerType)) {
            String baseUrl = (String) providerConfig.get("base_url");
            if (baseUrl == null || baseUrl.trim().isEmpty()) {
                throw new IllegalArgumentException(
                    "服务商 " + providerName + " 未配置 Base URL\n" +
                    "服务商类型 " + providerType + " 需要配置 base_url\n" +
                    "解决方案：在系统设置页面编辑该服务商，填写 Base URL"
                );
            }
        }
    }
    
    /**
     * 重新加载所有配置
     */
    public void reloadConfigs() {
        logger.info("重新加载所有配置...");
        imageProvidersConfig = null;
        textProvidersConfig = null;
    }
    
    /**
     * 创建默认图片配置
     */
    private Map<String, Object> createDefaultImageConfig() {
        Map<String, Object> defaultConfig = new HashMap<>();
        defaultConfig.put("active_provider", "google_genai");
        defaultConfig.put("providers", new HashMap<String, Object>());
        return defaultConfig;
    }
    
    /**
     * 创建默认文本配置
     */
    private Map<String, Object> createDefaultTextConfig() {
        Map<String, Object> defaultConfig = new HashMap<>();
        defaultConfig.put("active_provider", "google_gemini");
        defaultConfig.put("providers", new HashMap<String, Object>());
        return defaultConfig;
    }
    
    /**
     * 系统配置类
     */
    @Data
    public static class SystemConfig {
        private boolean debug = true;
        private String host = "0.0.0.0";
        private int port = 12398;
        private String[] corsOrigins = {"http://localhost:5173", "http://localhost:3000"};
        private String outputDir = "output";
    }
}