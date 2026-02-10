package com.blink.framework.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 环境变量工具类
 * 提供读取、验证环境变量的功能
 * 禁止使用程序写环境变量
 *
 * @author binblink
 */
@Slf4j
public class EnvReaderUtil {
    
    // 缓存读取的环境变量，避免重复读取
    private static final Map<String, String> ENV_CACHE = new ConcurrentHashMap<>();
    
    // 敏感关键词列表（用于脱敏）
    private static final Set<String> SENSITIVE_KEYS = Set.of(
        "PASSWORD", "SECRET", "KEY", "TOKEN", "CREDENTIAL", 
        "AUTH", "PRIVATE", "SALT", "CERTIFICATE", "JWT", "API_KEY",
        "ACCESS_KEY", "SECRET_KEY", "ENCRYPTION", "SIGNATURE"
    );
    
    // 私有构造函数，防止实例化
    private EnvReaderUtil() {
        throw new UnsupportedOperationException("工具类不需要实例化");
    }
    
    // ==================== 基础读取方法 ====================
    
    /**
     * 获取所有环境变量（不可修改）
     */
    public static Map<String, String> getAllEnvVariables() {
        return Collections.unmodifiableMap(System.getenv());
    }
    
    /**
     * 获取单个环境变量（无默认值）
     */
    public static String getEnv(String key) {
        return System.getenv(key);
    }
    
    /**
     * 获取单个环境变量（带默认值）
     */
    public static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 获取环境变量（从缓存读取，提高性能）
     */
    public static String getCachedEnv(String key) {
        return getCachedEnv(key, null);
    }
    
    public static String getCachedEnv(String key, String defaultValue) {
        return ENV_CACHE.computeIfAbsent(key, k -> getEnv(k, defaultValue));
    }
    
    /**
     * 清除环境变量缓存
     */
    public static void clearCache() {
        ENV_CACHE.clear();
    }
    
    // ==================== 条件查询方法 ====================
    
    /**
     * 获取所有以指定前缀开头的环境变量
     */
    public static Map<String, String> getEnvByPrefix(String prefix) {
        return System.getenv().entrySet().stream()
            .filter(entry -> entry.getKey().startsWith(prefix))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    
    /**
     * 获取所有包含指定关键词的环境变量
     */
    public static Map<String, String> getEnvContains(String keyword) {
        return System.getenv().entrySet().stream()
            .filter(entry -> entry.getKey().toLowerCase().contains(keyword.toLowerCase()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    
    /**
     * 检查环境变量是否存在
     */
    public static boolean exists(String key) {
        return System.getenv(key) != null;
    }
    
    /**
     * 检查环境变量是否存在且非空
     */
    public static boolean existsAndNotEmpty(String key) {
        String value = System.getenv(key);
        return value != null && !value.trim().isEmpty();
    }
    
    // ==================== 设置环境变量（当前进程） ====================
    
    /**
     * 设置当前进程的环境变量（通过反射）
     * 注意：这只影响当前 JVM 进程
     */
//    public static synchronized boolean setEnv(String key, String value) {
//        try {
//            Map<String, String> env = System.getenv();
//
//            // 获取不可修改的 map 内部的可修改 map
//            Class<?>[] classes = Collections.class.getDeclaredClasses();
//            for (Class<?> cl : classes) {
//                if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
//                    Field field = cl.getDeclaredField("m");
//                    field.setAccessible(true);
//
//                    // 获取实际的 map 对象
//                    Object obj = field.get(env);
//
//                    @SuppressWarnings("unchecked")
//                    Map<String, String> modifiableEnv = (Map<String, String>) obj;
//
//                    // 设置环境变量
//                    modifiableEnv.put(key, value);
//
//                    // 更新缓存
//                    ENV_CACHE.put(key, value);
//
//                    return true;
//                }
//            }
//        } catch (Exception e) {
//            System.err.println("设置环境变量失败: " + e.getMessage());
//        }
//        return false;
//    }
    
    /**
     * 批量设置环境变量
     */
//    public static void setEnvVariables(Map<String, String> variables) {
//        variables.forEach(EnvReadUtil::setEnv);
//    }
    
    /**
     * 删除环境变量
     */
//    public static synchronized boolean removeEnv(String key) {
//        try {
//            Map<String, String> env = System.getenv();
//
//            // 获取不可修改的 map 内部的可修改 map
//            Class<?>[] classes = Collections.class.getDeclaredClasses();
//            for (Class<?> cl : classes) {
//                if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
//                    Field field = cl.getDeclaredField("m");
//                    field.setAccessible(true);
//
//                    // 获取实际的 map 对象
//                    Object obj = field.get(env);
//
//                    @SuppressWarnings("unchecked")
//                    Map<String, String> modifiableEnv = (Map<String, String>) obj;
//
//                    // 删除环境变量
//                    modifiableEnv.remove(key);
//
//                    // 清除缓存
//                    ENV_CACHE.remove(key);
//
//                    return true;
//                }
//            }
//        } catch (Exception e) {
//            System.err.println("删除环境变量失败: " + e.getMessage());
//        }
//        return false;
//    }
    
    // ==================== 验证方法 ====================
    
    /**
     * 验证环境变量格式
     */
    public static boolean validateEnv(String key, Function<String, Boolean> validator) {
        String value = getEnv(key);
        if (value == null) {
            return false;
        }
        return validator.apply(value);
    }
    
    /**
     * 验证环境变量是否为有效URL
     */
    public static boolean isValidUrl(String key) {
        return validateEnv(key, value -> 
            value.matches("^(https?|ftp)://[^\\s/$.?#].[^\\s]*$"));
    }
    
    /**
     * 验证环境变量是否为有效端口
     */
    public static boolean isValidPort(String key) {
        return validateEnv(key, value -> {
            try {
                int port = Integer.parseInt(value);
                return port > 0 && port <= 65535;
            } catch (NumberFormatException e) {
                return false;
            }
        });
    }
    
    /**
     * 验证环境变量是否为布尔值
     */
    public static boolean isBoolean(String key) {
        return validateEnv(key, value -> 
            value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false"));
    }
    
    /**
     * 验证必需的环境变量是否存在
     */
    public static void validateRequired(String... requiredKeys) {
        List<String> missingKeys = new ArrayList<>();
        
        for (String key : requiredKeys) {
            if (!existsAndNotEmpty(key)) {
                missingKeys.add(key);
            }
        }
        
        if (!missingKeys.isEmpty()) {
            throw new IllegalStateException("缺少必需的环境变量: " + missingKeys);
        }
    }
    
    // ==================== 系统属性相关 ====================
    
    /**
     * 获取系统属性
     */
    public static String getProperty(String key) {
        return System.getProperty(key);
    }
    
    /**
     * 获取系统属性（带默认值）
     */
    public static String getProperty(String key, String defaultValue) {
        return System.getProperty(key, defaultValue);
    }
    
    /**
     * 设置系统属性
     */
    public static void setProperty(String key, String value) {
        System.setProperty(key, value);
    }
    
    /**
     * 获取环境变量或系统属性（优先级：系统属性 > 环境变量）
     */
    public static String getEnvOrProperty(String key) {
        return getEnvOrProperty(key, null);
    }
    
    public static String getEnvOrProperty(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }
        return getEnv(key, defaultValue);
    }
    
    // ==================== 文件操作 ====================
    
    /**
     * 导出环境变量到文件
     */
    public static void exportToFile(String filePath) throws IOException {
        exportToFile(filePath, false);
    }
    
    public static void exportToFile(String filePath, boolean includeSensitive) throws IOException {
        Path path = Paths.get(filePath);
        
        StringBuilder content = new StringBuilder();
        content.append("# 环境变量导出文件\n");
        content.append("# 生成时间: ").append(new Date()).append("\n");
        content.append("# 操作系统: ").append(System.getProperty("os.name")).append("\n\n");
        
        List<Map.Entry<String, String>> entries = new ArrayList<>(System.getenv().entrySet());
        entries.sort(Map.Entry.comparingByKey());
        
        for (Map.Entry<String, String> entry : entries) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            // 敏感信息脱敏
            if (!includeSensitive && isSensitiveKey(key)) {
                value = maskSensitiveValue(value);
            }
            
            content.append(key).append("=").append(value).append("\n");
        }
        
        Files.write(path, content.toString().getBytes(), StandardOpenOption.CREATE);
    }
    
    /**
     * 从文件加载环境变量（设置为当前进程的环境变量）
     */
//    public static void loadFromFile(String filePath) throws IOException {
//        Path path = Paths.get(filePath);
//
//        if (!Files.exists(path)) {
//            throw new FileNotFoundException("文件不存在: " + filePath);
//        }
//
//        List<String> lines = Files.readAllLines(path);
//
//        for (String line : lines) {
//            line = line.trim();
//
//            // 跳过注释和空行
//            if (line.isEmpty() || line.startsWith("#")) {
//                continue;
//            }
//
//            int equalsIndex = line.indexOf('=');
//            if (equalsIndex > 0) {
//                String key = line.substring(0, equalsIndex).trim();
//                String value = line.substring(equalsIndex + 1).trim();
//
//                // 去除可能的引号
//                if (value.startsWith("\"") && value.endsWith("\"")) {
//                    value = value.substring(1, value.length() - 1);
//                } else if (value.startsWith("'") && value.endsWith("'")) {
//                    value = value.substring(1, value.length() - 1);
//                }
//
//                setEnv(key, value);
//            }
//        }
//    }
    
    // ==================== 敏感信息处理 ====================
    
    /**
     * 判断是否为敏感键
     */
    public static boolean isSensitiveKey(String key) {
        String upperKey = key.toUpperCase();
        return SENSITIVE_KEYS.stream().anyMatch(upperKey::contains);
    }
    
    /**
     * 脱敏处理
     */
    public static String maskSensitiveValue(String value) {
        if (value == null || value.length() <= 4) {
            return "***";
        }
        
        int maskLength = Math.max(3, value.length() / 3);
        int start = Math.min(2, value.length() - maskLength - 2);
        int end = start + maskLength;
        
        if (end >= value.length()) {
            return "***";
        }
        
        return value.substring(0, start) + 
               "*".repeat(maskLength) + 
               value.substring(end);
    }
    
    /**
     * 获取脱敏后的环境变量
     */
    public static String getMaskedEnv(String key) {
        String value = getEnv(key);
        if (value == null) {
            return null;
        }
        
        if (isSensitiveKey(key)) {
            return maskSensitiveValue(value);
        }
        return value;
    }
    
    /**
     * 获取脱敏后的所有环境变量
     */
    public static Map<String, String> getAllMaskedEnvVariables() {
        Map<String, String> result = new TreeMap<>();
        
        System.getenv().forEach((key, value) -> {
            if (isSensitiveKey(key)) {
                result.put(key, maskSensitiveValue(value));
            } else {
                result.put(key, value);
            }
        });
        
        return result;
    }
    
    // ==================== 环境信息 ====================
    
    /**
     * 获取操作系统信息
     */
    public static String getOSInfo() {
        return System.getProperty("os.name") + " " + 
               System.getProperty("os.version") + " " + 
               System.getProperty("os.arch");
    }
    
    /**
     * 获取 Java 运行时信息
     */
    public static String getJavaInfo() {
        return System.getProperty("java.version") + " (" + 
               System.getProperty("java.vendor") + ")";
    }
    
    /**
     * 获取用户信息
     */
    public static String getUserInfo() {
        return System.getProperty("user.name") + "@" + 
               System.getProperty("user.home");
    }
    
    /**
     * 打印环境摘要
     */
    public static void printEnvSummary() {
        System.out.println("=== 环境变量摘要 ===");
        System.out.println("操作系统: " + getOSInfo());
        System.out.println("Java版本: " + getJavaInfo());
        System.out.println("用户信息: " + getUserInfo());
        System.out.println("环境变量数量: " + System.getenv().size());
        System.out.println("敏感变量数量: " + 
            System.getenv().keySet().stream()
                .filter(EnvReaderUtil::isSensitiveKey)
                .count());
    }
    
    // ==================== 转换方法 ====================
    
    /**
     * 转换为 Properties 对象
     */
    public static Properties toProperties() {
        Properties properties = new Properties();
        System.getenv().forEach(properties::setProperty);
        return properties;
    }
    
    /**
     * 转换为 Map 对象
     */
    public static Map<String, String> toMap() {
        return new HashMap<>(System.getenv());
    }
    
    /**
     * 转换为 JSON 字符串
     */
    public static String toJson() {
        StringBuilder json = new StringBuilder("{\n");
        
        List<Map.Entry<String, String>> entries = new ArrayList<>(System.getenv().entrySet());
        entries.sort(Map.Entry.comparingByKey());
        
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, String> entry = entries.get(i);
            String key = entry.getKey();
            String value = entry.getValue();
            
            if (isSensitiveKey(key)) {
                value = maskSensitiveValue(value);
            }
            
            json.append("  \"").append(key).append("\": \"").append(value).append("\"");
            
            if (i < entries.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        
        json.append("}");
        return json.toString();
    }
    
    // ==================== 命令行参数相关 ====================
    
    /**
     * 解析命令行参数为 Map
     */
    public static Map<String, String> parseArgs(String[] args) {
        Map<String, String> argsMap = new HashMap<>();
        
        for (String arg : args) {
            if (arg.startsWith("--")) {
                String[] parts = arg.substring(2).split("=", 2);
                if (parts.length == 2) {
                    argsMap.put(parts[0], parts[1]);
                } else {
                    argsMap.put(parts[0], "true");
                }
            } else if (arg.startsWith("-D")) {
                String[] parts = arg.substring(2).split("=", 2);
                if (parts.length == 2) {
                    setProperty(parts[0], parts[1]);
                }
            }
        }
        
        return argsMap;
    }
    
    // ==================== 子进程环境变量 ====================
    
    /**
     * 创建带有自定义环境变量的 ProcessBuilder
     */
    public static ProcessBuilder createProcessBuilder(String command, 
                                                     Map<String, String> extraEnv) {
        ProcessBuilder processBuilder = new ProcessBuilder(command.split("\\s+"));
        
        if (extraEnv != null && !extraEnv.isEmpty()) {
            Map<String, String> env = processBuilder.environment();
            env.putAll(extraEnv);
        }
        
        return processBuilder;
    }
    
    /**
     * 执行命令并返回结果
     */
    public static String executeCommand(String command, 
                                       Map<String, String> envVars) 
            throws IOException, InterruptedException {
        
        ProcessBuilder processBuilder = new ProcessBuilder(command.split("\\s+"));
        
        if (envVars != null && !envVars.isEmpty()) {
            Map<String, String> env = processBuilder.environment();
            env.putAll(envVars);
        }
        
        Process process = processBuilder.start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        int exitCode = process.waitFor();
        
        if (exitCode == 0) {
            return output.toString().trim();
        } else {
            throw new RuntimeException("命令执行失败，退出码: " + exitCode);
        }
    }
    
    // ==================== 实用方法 ====================
    
    /**
     * 获取环境变量，如果不存在则抛出异常
     */
    public static String requireEnv(String key) {
        String value = getEnv(key);
        if (value == null) {
            throw new IllegalStateException("必需的环境变量未设置: " + key);
        }
        return value;
    }
    
    /**
     * 获取环境变量，如果不存在则从 Supplier 获取
     */
    public static String getEnvOrDefault(String key, java.util.function.Supplier<String> supplier) {
        String value = getEnv(key);
        return value != null ? value : supplier.get();
    }
    
    /**
     * 获取环境变量并转换为整数
     */
    public static int getEnvAsInt(String key, int defaultValue) {
        String value = getEnv(key);
        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                // 忽略格式错误，返回默认值
            }
        }
        return defaultValue;
    }
    
    /**
     * 获取环境变量并转换为布尔值
     */
    public static boolean getEnvAsBoolean(String key, boolean defaultValue) {
        String value = getEnv(key);
        if (value != null) {
            return Boolean.parseBoolean(value.trim()) || 
                   value.equalsIgnoreCase("yes") || 
                   value.equalsIgnoreCase("y") || 
                   value.equals("1");
        }
        return defaultValue;
    }
    
    /**
     * 获取环境变量并转换为列表
     */
    public static List<String> getEnvAsList(String key, String delimiter) {
        String value = getEnv(key);
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(value.split(delimiter));
    }
    
    /**
     * 获取环境变量并转换为 Map
     */
    public static Map<String, String> getEnvAsMap(String key, String entryDelimiter, 
                                                 String kvDelimiter) {
        String value = getEnv(key);
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<String, String> result = new HashMap<>();
        String[] entries = value.split(entryDelimiter);
        
        for (String entry : entries) {
            String[] parts = entry.split(kvDelimiter, 2);
            if (parts.length == 2) {
                result.put(parts[0].trim(), parts[1].trim());
            }
        }
        
        return result;
    }
}
