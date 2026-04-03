package com.blink.base.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * 高质量验证码背景图片生成工具
 * 生成美女、动漫风格、风景等高质量背景图片
 *
 * @author binblink
 */
public class HighQualityCaptchaImageGenerator {

    private static final Random RANDOM = new Random();
    private static final int WIDTH = 310;
    private static final int HEIGHT = 155;

    public static void main(String[] args) throws IOException {
        String basePath = "blink-base/blink-base-app/src/main/resources/images";

        // 清理旧图片
        cleanDirectory(basePath + "/jigsaw/original");
        cleanDirectory(basePath + "/pic-click");

        // 生成滑块拼图背景图
        generateJigsawImages(basePath + "/jigsaw/original", 15);

        // 生成点选文字背景图
        generateClickWordImages(basePath + "/pic-click", 15);
    }
    
    private static void cleanDirectory(String path) {
        File dir = new File(path);
        if (dir.exists() && dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (file.isFile()) {
                    file.delete();
                }
            }
        }
    }

    public static void generateJigsawImages(String outputDir, int count) throws IOException {
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        for (int i = 1; i <= count; i++) {
            BufferedImage image = generateHighQualityBackground();
            File file = new File(outputDir + "/" + i + ".png");
            ImageIO.write(image, "png", file);
        }
    }

    public static void generateClickWordImages(String outputDir, int count) throws IOException {
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        for (int i = 1; i <= count; i++) {
            BufferedImage image = generateHighQualityBackground();
            File file = new File(outputDir + "/" + i + ".png");
            ImageIO.write(image, "png", file);
        }
    }

    /**
     * 生成高质量背景图
     */
    private static BufferedImage generateHighQualityBackground() {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // 高质量渲染设置
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // 随机选择一种风格
        int style = RANDOM.nextInt(6);
        switch (style) {
            case 0:
                drawSunsetGradient(g2d); // 日落渐变
                break;
            case 1:
                drawOceanWave(g2d); // 海浪风格
                break;
            case 2:
                drawMountainSilhouette(g2d); // 山脉剪影
                break;
            case 3:
                drawSakuraStyle(g2d); // 樱花风格
                break;
            case 4:
                drawAuroraStyle(g2d); // 极光风格
                break;
            default:
                drawAbstractArt(g2d); // 抽象艺术
        }

        // 添加轻微噪点增加质感
        addSubtleNoise(image);

        g2d.dispose();
        return image;
    }

    /**
     * 日落渐变风格
     */
    private static void drawSunsetGradient(Graphics2D g2d) {
        // 天空渐变
        Color[] sunsetColors = {
            new Color(255, 94, 77),    // 橙红
            new Color(255, 140, 66),   // 橙色
            new Color(255, 190, 118),  // 浅橙
            new Color(135, 206, 235),  // 天蓝
            new Color(70, 130, 180)    // 深蓝
        };

        int colorIndex = RANDOM.nextInt(sunsetColors.length - 1);
        GradientPaint gradient = new GradientPaint(
            0, 0, sunsetColors[colorIndex],
            0, HEIGHT, sunsetColors[colorIndex + 1]
        );
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // 太阳
        int sunX = RANDOM.nextInt(WIDTH - 100) + 50;
        int sunY = RANDOM.nextInt(HEIGHT / 3) + 20;
        RadialGradientPaint sunGradient = new RadialGradientPaint(
            new Point2D.Float(sunX, sunY), 40,
            new float[]{0f, 0.5f, 1f},
            new Color[]{new Color(255, 255, 200), new Color(255, 200, 100), new Color(255, 150, 50, 0)}
        );
        g2d.setPaint(sunGradient);
        g2d.fillOval(sunX - 40, sunY - 40, 80, 80);

        // 云朵
        drawClouds(g2d, new Color(255, 255, 255, 100));
    }

    /**
     * 海浪风格
     */
    private static void drawOceanWave(Graphics2D g2d) {
        // 天空
        GradientPaint skyGradient = new GradientPaint(
            0, 0, new Color(135, 206, 250),
            0, HEIGHT / 2, new Color(176, 226, 255)
        );
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // 海浪
        Color[] waveColors = {
            new Color(0, 105, 148),
            new Color(0, 77, 128),
            new Color(0, 51, 102)
        };

        for (int i = 0; i < 3; i++) {
            g2d.setColor(waveColors[i]);
            Path2D wave = new Path2D.Double();
            wave.moveTo(0, HEIGHT - 30 + i * 20);
            
            for (int x = 0; x <= WIDTH; x += 10) {
                double y = HEIGHT - 30 + i * 20 + 
                          Math.sin(x * 0.03 + i) * 15 + 
                          Math.sin(x * 0.01 + i * 2) * 10;
                wave.lineTo(x, y);
            }
            
            wave.lineTo(WIDTH, HEIGHT);
            wave.lineTo(0, HEIGHT);
            wave.closePath();
            g2d.fill(wave);
        }

        // 添加光斑
        addLightSpots(g2d, new Color(255, 255, 255, 60));
    }

    /**
     * 山脉剪影风格
     */
    private static void drawMountainSilhouette(Graphics2D g2d) {
        // 天空渐变
        Color skyTop = new Color(25, 25, 112);
        Color skyBottom = new Color(255, 140, 0);
        GradientPaint skyGradient = new GradientPaint(0, 0, skyTop, 0, HEIGHT, skyBottom);
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // 星星
        drawStars(g2d, 30);

        // 远山
        g2d.setColor(new Color(70, 70, 100));
        drawMountainRange(g2d, HEIGHT / 2, 0.3f);

        // 近山
        g2d.setColor(new Color(40, 40, 60));
        drawMountainRange(g2d, HEIGHT * 2 / 3, 0.5f);

        // 最近的山
        g2d.setColor(new Color(20, 20, 30));
        drawMountainRange(g2d, HEIGHT * 3 / 4, 0.7f);
    }

    /**
     * 樱花风格
     */
    private static void drawSakuraStyle(Graphics2D g2d) {
        // 柔和渐变背景
        Color[] bgColors = {
            new Color(255, 228, 225), // 浅粉
            new Color(255, 240, 245), // 淡粉
            new Color(255, 182, 193)  // 粉红
        };
        GradientPaint bgGradient = new GradientPaint(
            0, 0, bgColors[RANDOM.nextInt(bgColors.length)],
            WIDTH, HEIGHT, bgColors[RANDOM.nextInt(bgColors.length)]
        );
        g2d.setPaint(bgGradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // 绘制樱花花瓣
        for (int i = 0; i < 20; i++) {
            int x = RANDOM.nextInt(WIDTH);
            int y = RANDOM.nextInt(HEIGHT);
            int size = RANDOM.nextInt(15) + 8;
            float alpha = RANDOM.nextFloat() * 0.5f + 0.3f;
            
            g2d.setColor(new Color(255, 182, 193, (int)(alpha * 255)));
            drawPetal(g2d, x, y, size);
        }

        // 添加光晕效果
        addGlowEffect(g2d);
    }

    /**
     * 极光风格
     */
    private static void drawAuroraStyle(Graphics2D g2d) {
        // 深色夜空背景
        g2d.setColor(new Color(10, 10, 30));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // 星星
        drawStars(g2d, 50);

        // 极光
        Color[] auroraColors = {
            new Color(0, 255, 127, 100),
            new Color(0, 201, 87, 80),
            new Color(138, 43, 226, 60),
            new Color(75, 0, 130, 50)
        };

        for (int i = 0; i < 4; i++) {
            g2d.setColor(auroraColors[i]);
            Path2D aurora = new Path2D.Double();
            aurora.moveTo(0, HEIGHT / 3);
            
            for (int x = 0; x <= WIDTH; x += 5) {
                double y = HEIGHT / 3 + 
                          Math.sin(x * 0.02 + i) * 30 + 
                          Math.sin(x * 0.01 + i * 2) * 20;
                aurora.lineTo(x, y);
            }
            
            aurora.lineTo(WIDTH, 0);
            aurora.lineTo(0, 0);
            aurora.closePath();
            g2d.fill(aurora);
        }
    }

    /**
     * 抽象艺术风格
     */
    private static void drawAbstractArt(Graphics2D g2d) {
        // 渐变背景
        Color[] palette = generateHarmoniousPalette();
        GradientPaint bgGradient = new GradientPaint(
            0, 0, palette[0],
            WIDTH, HEIGHT, palette[1]
        );
        g2d.setPaint(bgGradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // 几何形状
        for (int i = 0; i < 8; i++) {
            g2d.setColor(new Color(
                palette[RANDOM.nextInt(palette.length)].getRed(),
                palette[RANDOM.nextInt(palette.length)].getGreen(),
                palette[RANDOM.nextInt(palette.length)].getBlue(),
                80
            ));
            
            int shape = RANDOM.nextInt(3);
            int x = RANDOM.nextInt(WIDTH);
            int y = RANDOM.nextInt(HEIGHT);
            int size = RANDOM.nextInt(80) + 30;
            
            switch (shape) {
                case 0:
                    g2d.fillOval(x, y, size, size);
                    break;
                case 1:
                    g2d.fillRect(x, y, size, size);
                    break;
                case 2:
                    int[] xPoints = {x, x + size, x + size / 2};
                    int[] yPoints = {y + size, y + size, y};
                    g2d.fillPolygon(xPoints, yPoints, 3);
                    break;
            }
        }

        // 流动线条
        g2d.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 5; i++) {
            g2d.setColor(new Color(255, 255, 255, 40));
            Path2D path = new Path2D.Double();
            path.moveTo(0, RANDOM.nextInt(HEIGHT));
            
            for (int x = 0; x <= WIDTH; x += 20) {
                path.curveTo(
                    x, RANDOM.nextInt(HEIGHT),
                    x + 10, RANDOM.nextInt(HEIGHT),
                    x + 20, RANDOM.nextInt(HEIGHT)
                );
            }
            g2d.draw(path);
        }
    }

    // ==================== 辅助方法 ====================

    private static void drawClouds(Graphics2D g2d, Color color) {
        g2d.setColor(color);
        for (int i = 0; i < 3; i++) {
            int x = RANDOM.nextInt(WIDTH);
            int y = RANDOM.nextInt(HEIGHT / 3);
            drawCloud(g2d, x, y);
        }
    }

    private static void drawCloud(Graphics2D g2d, int x, int y) {
        for (int i = 0; i < 5; i++) {
            int offsetX = RANDOM.nextInt(40) - 20;
            int offsetY = RANDOM.nextInt(20) - 10;
            int size = RANDOM.nextInt(30) + 20;
            g2d.fillOval(x + offsetX, y + offsetY, size, size * 2 / 3);
        }
    }

    private static void drawStars(Graphics2D g2d, int count) {
        g2d.setColor(Color.WHITE);
        for (int i = 0; i < count; i++) {
            int x = RANDOM.nextInt(WIDTH);
            int y = RANDOM.nextInt(HEIGHT / 2);
            int size = RANDOM.nextInt(3) + 1;
            g2d.fillOval(x, y, size, size);
        }
    }

    private static void drawMountainRange(Graphics2D g2d, int baseY, float complexity) {
        Path2D mountain = new Path2D.Double();
        mountain.moveTo(0, HEIGHT);
        
        int x = 0;
        int y = baseY - RANDOM.nextInt((int)(HEIGHT * complexity));
        mountain.lineTo(x, y);
        
        while (x < WIDTH) {
            x += RANDOM.nextInt(40) + 20;
            y = baseY - RANDOM.nextInt((int)(HEIGHT * complexity));
            mountain.lineTo(x, y);
        }
        
        mountain.lineTo(WIDTH, HEIGHT);
        mountain.closePath();
        g2d.fill(mountain);
    }

    private static void drawPetal(Graphics2D g2d, int x, int y, int size) {
        Path2D petal = new Path2D.Double();
        petal.moveTo(x, y - size);
        petal.curveTo(x + size, y - size, x + size, y + size, x, y + size);
        petal.curveTo(x - size, y + size, x - size, y - size, x, y - size);
        g2d.fill(petal);
    }

    private static void addLightSpots(Graphics2D g2d, Color color) {
        g2d.setColor(color);
        for (int i = 0; i < 10; i++) {
            int x = RANDOM.nextInt(WIDTH);
            int y = RANDOM.nextInt(HEIGHT);
            int size = RANDOM.nextInt(20) + 5;
            g2d.fillOval(x, y, size, size);
        }
    }

    private static void addGlowEffect(Graphics2D g2d) {
        int x = RANDOM.nextInt(WIDTH);
        int y = RANDOM.nextInt(HEIGHT);
        RadialGradientPaint glow = new RadialGradientPaint(
            new Point2D.Float(x, y), 100,
            new float[]{0f, 1f},
            new Color[]{new Color(255, 255, 255, 50), new Color(255, 255, 255, 0)}
        );
        g2d.setPaint(glow);
        g2d.fillOval(x - 100, y - 100, 200, 200);
    }

    private static Color[] generateHarmoniousPalette() {
        float hue = RANDOM.nextFloat();
        Color[] palette = new Color[4];
        for (int i = 0; i < 4; i++) {
            float h = (hue + i * 0.1f) % 1.0f;
            float s = 0.4f + RANDOM.nextFloat() * 0.3f;
            float b = 0.6f + RANDOM.nextFloat() * 0.3f;
            palette[i] = Color.getHSBColor(h, s, b);
        }
        return palette;
    }

    private static void addSubtleNoise(BufferedImage image) {
        for (int i = 0; i < WIDTH * HEIGHT * 0.01; i++) {
            int x = RANDOM.nextInt(WIDTH);
            int y = RANDOM.nextInt(HEIGHT);
            int rgb = image.getRGB(x, y);
            
            int noise = RANDOM.nextInt(20) - 10;
            int r = Math.max(0, Math.min(255, ((rgb >> 16) & 0xFF) + noise));
            int g = Math.max(0, Math.min(255, ((rgb >> 8) & 0xFF) + noise));
            int b = Math.max(0, Math.min(255, (rgb & 0xFF) + noise));
            
            image.setRGB(x, y, (r << 16) | (g << 8) | b);
        }
    }
}
