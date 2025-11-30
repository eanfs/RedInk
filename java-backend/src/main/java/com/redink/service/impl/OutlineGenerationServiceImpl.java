package com.redink.service.impl;

import com.redink.config.ConfigManager;
import com.redink.model.OutlineResult;
import com.redink.model.Page;
import com.redink.service.OutlineGenerationService;
import com.redink.util.ImageUtils;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 大纲生成服务实现（简化版）
 */
@Service
public class OutlineGenerationServiceImpl implements OutlineGenerationService {
    
    private static final Logger logger = LoggerFactory.getLogger(OutlineGenerationServiceImpl.class);
    
    private final ConfigManager configManager;
    private final ChatModel openAiChatModel;

    // 提示词模板
    private static final String OUTLINE_PROMPT = """
        你是一个小红书内容创作专家。用户会给你一个要求以及说明，你需要生成一个适合小红书的图文内容大纲。

        用户的要求以及说明：
        {topic}

        要求：
        1. 第一页必须是吸引人的封面/标题页，包含标题和副标题
        2. 内容控制在 6-12 页（包括封面）（如果用户特别要求页数，以用户的要求为准，页数可以适当放宽到2-18页的范围）
        特别的. 如果用户在要求了某种特定语言风格的喜好，或者是否使用emoji等，则以客户的要求为准
        3. 每页内容简洁有力，适合配图展示
        4. 使用小红书风格的语言（亲切、有趣、实用）
        5. 可以适当使用 emoji 增加趣味性
        6. 内容要有实用价值，能解决用户问题或提供有用信息
        7. 最后一页可以是总结或行动呼吁

        输出格式（严格遵守）：
        - 用 <page> 标签分割每一页（重要：这是强制分隔符）
        - 每页第一行是页面类型标记：[封面]、[内容]、[总结]
        - 后面是该页的具体内容描述
        - 内容要具体、详细，方便后续生成图片
        - 避免在内容中使用 | 竖线符号（会与 markdown 表格冲突）

        现在，请根据用户的主题生成大纲。记住：
        1. 严格使用 <page> 标签分割每一页
        2. 每页开头标注类型：[封面]、[内容]、[总结]
        3. 内容要详细、具体、专业、有价值。
        4. 适合制作成小红书图文
        5. 避免使用竖线符号 | （会与 markdown 表格冲突）

        【特别的！！注意】直接给出大纲内容（不要有任何多余的说明，也就是你直接从[封面]开始，不要有针对用户的回应对话），请输出：
        """;
    
    public OutlineGenerationServiceImpl(ConfigManager configManager, ChatModel chatModel) {
        this.configManager = configManager;
        this.openAiChatModel = chatModel;
    }
    
    @Override
    public OutlineResult generateOutline(String topic, byte[][] images) {
        try {
            if (topic == null || topic.trim().isEmpty()) {
                return OutlineResult.builder()
                        .success(false)
                        .error("主题不能为空")
                        .build();
            }
            
            // 调用 AI 服务生成大纲
            String generatedOutline = generateOutlineWithAI(topic);
            List<Page> pages = parseOutline(generatedOutline);
            
            return OutlineResult.builder()
                    .success(true)
                    .outline(generatedOutline)
                    .pages(pages)
                    .hasImages(images != null && images.length > 0)
                    .build();
            
        } catch (Exception e) {
            String errorMessage = analyzeError(e.getMessage());
            return OutlineResult.builder()
                    .success(false)
                    .error(errorMessage)
                    .build();
        }
    }

    /**
     * 使用 AI 生成大纲
     */
    private String generateOutlineWithAI(String topic) {
        try {
            // 构建提示词
            String prompt = OUTLINE_PROMPT.replace("{topic}", topic);
            
            // 创建用户消息
            Message userMessage = new UserMessage(prompt);
            Prompt aiPrompt = new Prompt(userMessage);
            
            // 调用 AI 服务
            ChatResponse response = openAiChatModel.call(aiPrompt);
            
            if (response == null || response.getResult() == null) {
                logger.error("AI 服务返回空结果");
                return generateFallbackOutline(topic);
            }
            
            String generatedContent = response.getResult().getOutput().getText();
            if (generatedContent == null || generatedContent.trim().isEmpty()) {
                logger.error("AI 服务返回空内容");
                return generateFallbackOutline(topic);
            }
            
            logger.info("AI 生成大纲成功，主题: {}", topic);
            return generatedContent;
            
        } catch (Exception e) {
            logger.error("AI 大纲生成失败: {}", e.getMessage(), e);
            return generateFallbackOutline(topic);
        }
    }
    
    /**
     * 生成备用大纲（当 AI 服务失败时使用）
     */
    private String generateFallbackOutline(String topic) {
        logger.info("使用备用大纲生成策略，主题: {}", topic);
        return "<page>[封面] " + topic + "\n\n" +
               "<page>[内容] 为什么要学习" + topic + "？\n\n" +
               "<page>[内容] " + topic + "的核心知识点解析\n\n" +
               "<page>[内容] 实践应用与案例分析\n\n" +
               "<page>[内容] 常见问题解答\n\n" +
               "<page>[内容] 进阶学习路径建议\n\n" +
               "<page>[总结] 总结与行动计划";
    }

    /**
     * 解析大纲文本
     */
    private List<Page> parseOutline(String outlineText) {
        // 按 <page> 分割页面
        String[] pagesRaw = outlineText.split("<page>", -1);
        
        List<Page> pages = Arrays.stream(pagesRaw)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(this::parsePage)
                .collect(Collectors.toList());
        
        // 为每页分配索引
        for (int i = 0; i < pages.size(); i++) {
            pages.get(i).setIndex(i);
        }
        
        return pages;
    }
    
    /**
     * 解析单页内容
     */
    private Page parsePage(String pageText) {
        String type = "content";
        
        // 提取页面类型
        Pattern typePattern = Pattern.compile("\\[(\\S+)\\]");
        Matcher typeMatcher = typePattern.matcher(pageText);
        if (typeMatcher.find()) {
            String typeCn = typeMatcher.group(1);
            type = switch (typeCn) {
                case "封面" -> "cover";
                case "内容" -> "content";
                case "总结" -> "summary";
                default -> "content";
            };
        }
        
        // 清理页面内容
        String cleanedContent = pageText.replaceAll("\\[\\S+\\]", "").trim();
        
        return new Page(0, type, cleanedContent);
    }
    
    /**
     * 分析错误信息
     */
    private String analyzeError(String errorMessage) {
        return """
                大纲生成失败。
                错误详情: """ + errorMessage + """
                可能原因：
                1. 主题参数不正确
                2. 内部处理错误
                建议：检查输入参数和系统状态
                """;
    }
}
