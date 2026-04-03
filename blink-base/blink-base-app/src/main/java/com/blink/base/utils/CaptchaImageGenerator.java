package com.blink.base.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * 验证码背景图片生成工具
 * 用于生成滑块拼图和点选文字验证码的背景图片
 *
 * @author binblink
 */
public class CaptchaImageGenerator {

    private static final Random RANDOM = new Random();
    private static final int WIDTH = 310;
    private static final int HEIGHT = 155;

    public static void main(String[] args) throws IOException {
        String outputPath = "src/main/resources/images";

        // 生成滑块拼图背景图
        generateJigsawImages(outputPath + "/jigsaw/original", 10);

        // 生成点选文字背景图
        generateClickWordImages(outputPath + "/pic-click", 10);
    }

    /**
     * 生成滑块拼图背景图
     */
    public static void generateJigsawImages(String outputDir, int count) throws IOException {
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        for (int i = 1; i <= count; i++) {
            BufferedImage image = generateJigsawBackground();
            File file = new File(outputDir + "/" + i + ".png");
            ImageIO.write(image, "png", file);
        }
    }

    /**
     * 生成点选文字背景图
     */
    public static void generateClickWordImages(String outputDir, int count) throws IOException {
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        for (int i = 1; i <= count; i++) {
            BufferedImage image = generateClickWordBackground();
            File file = new File(outputDir + "/" + i + ".png");
            ImageIO.write(image, "png", file);
        }
    }

    /**
     * 生成滑块拼图背景图
     * 特点：渐变背景 + 几何图形 + 纹理
     */
    private static BufferedImage generateJigsawBackground() {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // 启用抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // 1. 绘制渐变背景
        GradientPaint gradient = createRandomGradient();
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // 2. 添加纹理效果
        addTextureEffect(g2d);

        // 3. 绘制几何图形
        drawGeometricShapes(g2d);

        // 4. 添加噪点
        addNoise(image);

        g2d.dispose();
        return image;
    }

    /**
     * 生成点选文字背景图
     * 特点：复杂背景 + 干扰元素
     */
    private static BufferedImage generateClickWordBackground() {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // 1. 绘制渐变背景
        GradientPaint gradient = createRandomGradient();
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // 2. 添加波浪纹理
        addWaveTexture(g2d);

        // 3. 绘制干扰图形
        drawInterferenceShapes(g2d);

        // 4. 添加噪点
        addNoise(image);

        g2d.dispose();
        return image;
    }

    /**
     * 创建随机渐变色
     */
    private static GradientPaint createRandomGradient() {
        Color[] colorPairs = {
            new Color(135, 206, 250), new Color(70, 130, 180),   // 天蓝色系
            new Color(144, 238, 144), new Color(34, 139, 34),    // 绿色系
            new Color(255, 182, 193), new Color(219, 112, 147),  // 粉色系
            new Color(255, 218, 185), new Color(210, 105, 30),   // 橙色系
            new Color(230, 230, 250), new Color(123, 104, 238),  // 紫色系
            new Color(176, 224, 230), new Color(0, 139, 139),    // 青色系
            new Color(255, 250, 205), new Color(189, 183, 107),  // 黄色系
            new Color(255, 228, 225), new Color(178, 34, 34),    // 红色系
        };

        int index = RANDOM.nextInt(colorPairs.length / 2) * 2;
        Color startColor = colorPairs[index];
        Color endColor = colorPairs[index + 1];

        // 随机渐变方向
        int type = RANDOM.nextInt(3);
        switch (type) {
            case 0: // 水平渐变
                return new GradientPaint(0, 0, startColor, WIDTH, 0, endColor);
            case 1: // 垂直渐变
                return new GradientPaint(0, 0, startColor, 0, HEIGHT, endColor);
            default: // 对角渐变
                return new GradientPaint(0, 0, startColor, WIDTH, HEIGHT, endColor);
        }
    }

    /**
     * 添加纹理效果
     */
    private static void addTextureEffect(Graphics2D g2d) {
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
        
        for (int i = 0; i < 50; i++) {
            int x = RANDOM.nextInt(WIDTH);
            int y = RANDOM.nextInt(HEIGHT);
            int size = RANDOM.nextInt(20) + 5;
            
            g2d.setColor(new Color(RANDOM.nextInt(256), RANDOM.nextInt(256), RANDOM.nextInt(256)));
            g2d.fillOval(x, y, size, size);
        }
        
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    /**
     * 绘制几何图形
     */
    private static void drawGeometricShapes(Graphics2D g2d) {
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
        
        for (int i = 0; i < 8; i++) {
            int x = RANDOM.nextInt(WIDTH);
            int y = RANDOM.nextInt(HEIGHT);
            int size = RANDOM.nextInt(60) + 20;
            
            g2d.setColor(new Color(255, 255, 255, 100));
            g2d.setStroke(new BasicStroke(2));
            
            int shapeType = RANDOM.nextInt(4);
            switch (shapeType) {
                case 0: // 圆形
                    g2d.drawOval(x, y, size, size);
                    break;
                case 1: // 矩形
                    g2d.drawRect(x, y, size, size);
                    break;
                case 2: // 三角形
                    int[] xPoints = {x, x + size, x + size / 2};
                    int[] yPoints = {y + size, y + size, y};
                    g2d.drawPolygon(xPoints, yPoints, 3);
                    break;
                case 3: // 线条
                    g2d.drawLine(x, y, x + size, y + RANDOM.nextInt(60) - 30);
                    break;
            }
        }
        
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    /**
     * 添加波浪纹理
     */
    private static void addWaveTexture(Graphics2D g2d) {
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(1.5f));

        for (int wave = 0; wave < 5; wave++) {
            int startY = RANDOM.nextInt(HEIGHT);
            int amplitude = RANDOM.nextInt(20) + 10;
            int wavelength = RANDOM.nextInt(50) + 30;

            for (int x = 0; x < WIDTH; x++) {
                int y = startY + (int) (amplitude * Math.sin(2 * Math.PI * x / wavelength));
                if (x > 0) {
                    g2d.drawLine(x - 1, startY + (int) (amplitude * Math.sin(2 * Math.PI * (x - 1) / wavelength)), x, y);
                }
            }
        }

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    /**
     * 绘制干扰图形
     */
    private static void drawInterferenceShapes(Graphics2D g2d) {
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));

        for (int i = 0; i < 15; i++) {
            int x1 = RANDOM.nextInt(WIDTH);
            int y1 = RANDOM.nextInt(HEIGHT);
            int x2 = RANDOM.nextInt(WIDTH);
            int y2 = RANDOM.nextInt(HEIGHT);

            g2d.setColor(new Color(RANDOM.nextInt(256), RANDOM.nextInt(256), RANDOM.nextInt(256), 80));
            g2d.setStroke(new BasicStroke(RANDOM.nextInt(3) + 1));
            g2d.drawLine(x1, y1, x2, y2);
        }

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    /**
     * 添加噪点
     */
    private static void addNoise(BufferedImage image) {
        for (int i = 0; i < WIDTH * HEIGHT * 0.02; i++) {
            int x = RANDOM.nextInt(WIDTH);
            int y = RANDOM.nextInt(HEIGHT);
            int rgb = image.getRGB(x, y);
            
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;

            int noise = RANDOM.nextInt(30) - 15;
            r = Math.max(0, Math.min(255, r + noise));
            g = Math.max(0, Math.min(255, g + noise));
            b = Math.max(0, Math.min(255, b + noise));

            int newRgb = (r << 16) | (g << 8) | b;
            image.setRGB(x, y, newRgb);
        }
    }
}
