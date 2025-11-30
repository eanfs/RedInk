package com.redink.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 提示词模板工具类
 */
@Component
public class PromptTemplateManager {
    
    private static final Logger logger = LoggerFactory.getLogger(PromptTemplateManager.class);
    
    private static final String IMAGE_PROMPT_TEMPLATE = """
        请生成一张小红书风格的图文内容图片。
        【合规特别注意的】注意不要带有任何小红书的logo，不要有右下角的用户id以及logo
        【合规特别注意的】用户给到的参考图片里如果有水印和logo（尤其是注意右下角，左上角），请一定要去掉

        页面内容：{page_content}

        页面类型：{page_type}

        如果当前页面类型不是封面页的话，你要参考最后一张图片作为封面的样式

        后续生成风格要严格参考封面的风格，要保持风格统一。

        设计要求：

        1. 整体风格
        - 小红书爆款图文风格
        - 清新、精致、有设计感
        - 适合年轻人审美
        - 配色和谐，视觉吸引力强

        2. 文字排版
        - 文字清晰可读，字号适中
        - 重要信息突出显示
        - 排版美观，留白合理
        - 支持 emoji 和符号
        - 如果是封面，标题要大而醒目

        3. 视觉元素
        - 背景简洁但不单调
        - 可以有装饰性元素（如图标、插画）
        - 配色温暖或清新
        - 保持专业感

        4. 页面类型特殊要求

        [封面] 类型：
        - 标题占据主要位置，字号最大
        - 副标题居中或在标题下方
        - 整体设计要有吸引力和冲击力
        - 背景可以更丰富，有视觉焦点

        [内容] 类型：
        - 信息层次分明
        - 列表项清晰展示
        - 重点内容用颜色或粗体强调
        - 可以有小图标辅助说明

        [总结] 类型：
        - 总结性文字突出
        - 可以有勾选框或完成标志
        - 给人完成感和满足感
        - 鼓励性的视觉元素

        5. 技术规格
        - 竖版 3:4 比例（小红书标准）
        - 高清画质
        - 适合手机屏幕查看
        - 所有文字内容必须完整呈现
        - 【特别注意】无论是给到的图片还是参考文字，请仔细思考，让其符合正确的竖屏观看的排版，不能左右旋转或者是倒置。

        6. 整体风格一致性
        为确保所有页面风格统一，请参考完整的内容大纲和用户原始需求来确定：
        - 整体色调和配色方案
        - 设计风格（清新/科技/温暖/专业等）
        - 视觉元素的一致性
        - 排版布局的统一风格

        用户原始需求：
        {user_topic}

        完整内容大纲参考：
        ---
        {full_outline}
        ---

        请根据以上要求，生成一张精美的小红书风格图片。请直接给出图片，不要有任何手机边框，或者是白色留边。
        """;
    
    private static final String IMAGE_PROMPT_SHORT_TEMPLATE = """
        生成小红书风格竖版图片（3:4比例）。

        页面类型：{page_type}
        页面内容：{page_content}

        要求：清新精致、文字清晰、排版美观。
        """;
    
    private static final String OUTLINE_PROMPT_TEMPLATE = """
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

        ## 示例输出：

        [封面]
        标题：5分钟学会手冲咖啡☕
        副标题：新手也能做出咖啡店的味道
        背景：温馨的咖啡场景，一个家庭布局的咖啡角

        <page>
        [内容]
        第一步：准备器具

        必备工具：
        • 手冲壶（细嘴壶）
        • 滤杯和滤纸
        • 咖啡豆 15g
        • 热水 250ml（92-96℃）
        • 磨豆机
        • 电子秤

        配图建议：整齐摆放的咖啡器具

        <page>

        [内容]
        第二步：研磨咖啡豆

        研磨粗细度：中细研磨（像细砂糖）
        重量：15克
        新鲜度：建议现磨现冲

        小贴士💡：
        咖啡豆最好是烘焙后2周内的
        研磨后要在15分钟内冲泡完成

        配图建议：研磨咖啡豆的特写

        <page>

        [内容]
        第三步：闷蒸

        注水量：30ml（2倍咖啡粉重量）
        时间：30秒
        手法：从中心向外螺旋注水

        关键点⚠️：
        让所有咖啡粉都湿润
        不要注水太快

        配图建议：手冲壶注水的过程

        <page>

        [内容]
        第四步：分段萃取

        第二次注水：到120ml，用时1分钟
        第三次注水：到250ml，用时1分30秒
        总时间：2-2.5分钟

        配图建议：完整的冲泡过程

        <page>

        [总结]
        完成！享受你的手冲咖啡✨

        记住三个关键：
        ✅ 水温 92-96℃
        ✅ 粉水比 1:15
        ✅ 总时间 2-2.5分钟

        新手提示：
        前几次可能不完美
        多练习就会越来越好
        享受过程最重要！

        配图建议：一杯完成的手冲咖啡，温暖的场景

        ### 最后
        现在，请根据用户的主题生成大纲。记住：
        1. 严格使用 <page> 标签分割每一页
        2. 每页开头标注类型：[封面]、[内容]、[总结]
        3. 内容要详细、具体、专业、有价值。
        4. 适合制作成小红书图文
        5. 避免使用竖线符号 | （会与 markdown 表格冲突）

        【特别的！！注意】直接给出大纲内容（不要有任何多余的说明，也就是你直接从[封面]开始，不要有针对用户的回应对话），请输出：
        """;
    
    /**
     * 获取图片生成提示词模板
     */
    public String getImagePromptTemplate(boolean useShortPrompt) {
        return useShortPrompt ? IMAGE_PROMPT_SHORT_TEMPLATE : IMAGE_PROMPT_TEMPLATE;
    }
    
    /**
     * 获取大纲生成提示词模板
     */
    public String getOutlinePromptTemplate() {
        return OUTLINE_PROMPT_TEMPLATE;
    }
    
    /**
     * 构建图片生成提示词
     */
    public String buildImagePrompt(String pageContent, String pageType, String userTopic, String fullOutline) {
        String template = getImagePromptTemplate(false);
        return template
                .replace("{page_content}", pageContent != null ? pageContent : "")
                .replace("{page_type}", pageType != null ? pageType : "")
                .replace("{user_topic}", userTopic != null ? userTopic : "未提供")
                .replace("{full_outline}", fullOutline != null ? fullOutline : "");
    }
    
    /**
     * 构建短版图片生成提示词
     */
    public String buildImagePromptShort(String pageContent, String pageType) {
        String template = getImagePromptTemplate(true);
        return template
                .replace("{page_content}", pageContent != null ? pageContent : "")
                .replace("{page_type}", pageType != null ? pageType : "");
    }
    
    /**
     * 构建大纲生成提示词
     */
    public String buildOutlinePrompt(String topic) {
        String template = getOutlinePromptTemplate();
        return template.replace("{topic}", topic != null ? topic : "");
    }
    
    /**
     * 从文件加载模板（备用方案）
     */
    private String loadTemplateFromFile(String templatePath) {
        try {
            Path path = Paths.get(templatePath);
            if (Files.exists(path)) {
                return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            logger.warn("加载模板文件失败: {}", templatePath, e);
        }
        return null;
    }
}