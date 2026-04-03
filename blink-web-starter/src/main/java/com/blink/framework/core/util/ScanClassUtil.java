package com.blink.framework.core.util;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author binblink
 */
public class ScanClassUtil {


    public  static List<String> getClassNameByScanAnnotation(String basePackage,Class<? extends Annotation> clazz) {

        // 创建一个 ClassPathScanningCandidateComponentProvider 实例
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        // 添加一个 TypeFilter 来包含带有 @Service 注解的类
        scanner.addIncludeFilter(new AnnotationTypeFilter(clazz));
        // 执行扫描并获取结果
        Set<BeanDefinition> beanDefinitions = scanner.findCandidateComponents(basePackage);

        // 注意：上面的 findCandidateComponents 方法实际上返回的是 ClassPathBeanDefinitionScanner.BeanDefinitionHolder 的集合
        // 这里为了简化示例，我假设返回的是 BeanDefinition 的集合，但实际情况可能需要你进行一些转换
        List<String> result = new ArrayList<>();
        // 遍历结果（注意：这里只是示例，实际上你需要处理 BeanDefinitionHolder 集合）
        for (BeanDefinition beanDefinition : beanDefinitions) {
            // 这里只是打印 bean 的类名作为示例
            // 在实际应用中，你可能需要将这些 beanDefinition 添加到 Spring 应用上下文中
            result.add(beanDefinition.getBeanClassName());
        }
        return result;
    }



}
