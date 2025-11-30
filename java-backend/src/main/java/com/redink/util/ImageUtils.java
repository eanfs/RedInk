package com.redink.util;

import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * 图片工具类
 */
public class ImageUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageUtils.class);
    private static final int DEFAULT_MAX_SIZE_KB = 200;
    private static final int DEFAULT_QUALITY = 85;
    
    /**
     * 压缩图片到指定大小
     * @param imageData 原始图片数据
     * @param maxSizeKb 最大文件大小（KB）
     * @return 压缩后的图片数据
     */
    public static byte[] compressImage(byte[] imageData, int maxSizeKb) {
        if (imageData == null || imageData.length <= maxSizeKb * 1024) {
            return imageData;
        }
        
        try {
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));
            if (originalImage == null) {
                return imageData;
            }
            
            int quality = DEFAULT_QUALITY;
            byte[] compressedData;
            
            // 逐步降低质量直到满足大小要求
            while (quality >= 20) {
                compressedData = compressWithQuality(originalImage, quality);
                if (compressedData.length <= maxSizeKb * 1024) {
                    logger.info("图片压缩成功: {} -> {} KB (质量: {})", 
                        imageData.length / 1024, compressedData.length / 1024, quality);
                    return compressedData;
                }
                quality -= 5;
            }
            
            // 如果质量压缩不够，进一步缩小尺寸
            return compressWithResize(originalImage, maxSizeKb * 1024);
            
        } catch (Exception e) {
            logger.warn("图片压缩失败，返回原图: {}", e.getMessage());
            return imageData;
        }
    }
    
    /**
     * 使用指定质量压缩图片
     */
    private static byte[] compressWithQuality(BufferedImage image, int quality) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Thumbnails.of(image)
                .outputQuality(quality / 100.0)
                .outputFormat("JPEG")
                .toOutputStream(output);
        return output.toByteArray();
    }
    
    /**
     * 通过调整尺寸压缩图片
     */
    private static byte[] compressWithResize(BufferedImage originalImage, int targetSize) throws IOException {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        
        // 按比例缩小，直到满足大小要求
        while (true) {
            width = (int) (width * 0.9);
            height = (int) (height * 0.9);
            
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Thumbnails.of(originalImage)
                    .size(width, height)
                    .outputQuality(0.8)
                    .outputFormat("JPEG")
                    .toOutputStream(output);
            
            byte[] compressedData = output.toByteArray();
            if (compressedData.length <= targetSize || Math.max(width, height) <= 512) {
                return compressedData;
            }
        }
    }
    
    /**
     * 将图片转换为Base64字符串
     */
    public static String imageToBase64(byte[] imageData) {
        return Base64.getEncoder().encodeToString(imageData);
    }
    
    /**
     * 将Base64字符串转换为图片数据
     */
    public static byte[] base64ToImage(String base64String) {
        if (base64String.startsWith("data:image")) {
            base64String = base64String.split(",")[1];
        }
        return Base64.getDecoder().decode(base64String);
    }
    
    /**
     * 验证图片格式
     */
    public static boolean isImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            return false;
        }
        
        String contentType = file.getContentType();
        return contentType != null && (
            contentType.equals("image/jpeg") ||
            contentType.equals("image/png") ||
            contentType.equals("image/gif") ||
            contentType.equals("image/webp")
        );
    }
    
    /**
     * 获取图片尺寸
     */
    public static int[] getImageDimensions(byte[] imageData) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            if (image != null) {
                return new int[]{image.getWidth(), image.getHeight()};
            }
        } catch (Exception e) {
            logger.warn("获取图片尺寸失败: {}", e.getMessage());
        }
        return new int[]{0, 0};
    }
}