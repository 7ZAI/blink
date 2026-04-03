package com.blink.framework.common.utils;

import com.github.therapi.runtimejavadoc.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 读取注释工具类 只能读当前工程包下的类
 * 用于读取错误码类常量doc 自动生成sql 入库
 */
public class JavaDocCommentUtil {

    // formatters are reusable and thread-safe
    private static final CommentFormatter formatter = new CommentFormatter();

    private static String format(Comment c) {
        return formatter.format(c);
    }

    /**
     * 获取类的属性注释
     *
     * @param fullyQualifiedClassName 类的全限定名
     * @return map key 字段名 value 注释信息
     * @throws IOException
     */
    public static Map<String, String> getFiledComment(String fullyQualifiedClassName) throws IOException {

        ClassJavadoc classDoc = RuntimeJavadoc.getJavadoc(fullyQualifiedClassName);
        Map<String, String> map = new HashMap<>();
        // optionally skip absent documentation
        if (classDoc.isEmpty()) {
            System.out.println("no documentation for " + fullyQualifiedClassName);
        }

        for (FieldJavadoc field : classDoc.getFields()) {

            System.out.println(field.getName() + "field comment: " + field.getComment());
            map.put(field.getName(), field.getComment().toString());
        }

        return map;
    }

    /**
     * 根据错误类源码的注释和属性值 生成插入sql
     *
     * @param fullyQualifiedClassName 类的全限定名
     * @return List<String> sql语句集合
     * @throws Exception
     */
    public static List<String> getMsgCoeInsertSQL(String fullyQualifiedClassName) throws Exception {

        List<String> list = new ArrayList<>();
        Map<String, String> map = getFiledComment(fullyQualifiedClassName);
        Class clazz = Class.forName(fullyQualifiedClassName);

        Field[] fields = clazz.getFields();

        for (Field f : fields) {
            f.setAccessible(true);
            String comment = map.get(f.getName());
            String code = (String) f.get(null);
            String sql = "INSERT INTO blink.sys_msg_code ( msg_code, msg_info, msg_type, msg_lang) VALUES( '" + code + "', '" + comment + "', 'E', 'zh_cn');";
            System.out.println(sql);
            list.add(sql);
        }

        return list;
    }

    //例子
    private static void printJavadoc(String fullyQualifiedClassName) throws IOException {
        ClassJavadoc classDoc = RuntimeJavadoc.getJavadoc(fullyQualifiedClassName);
        // optionally skip absent documentation
        if (classDoc.isEmpty()) {
            System.out.println("no documentation for " + fullyQualifiedClassName);
            return;
        }

        System.out.println(classDoc.getName());
        System.out.println(format(classDoc.getComment()));
        System.out.println();


        // @see tags
        for (SeeAlsoJavadoc see : classDoc.getSeeAlso()) {
            System.out.println("See also: " + see.getLink());
        }
        // miscellaneous and custom javadoc tags (@author, etc.)
        for (OtherJavadoc other : classDoc.getOther()) {
            System.out.println(other.getName() + ": " + format(other.getComment()));
        }

        System.out.println();
        System.out.println("CONSTRUCTORS");
        for (MethodJavadoc methodDoc : classDoc.getConstructors()) {
            printMethodJavadoc(methodDoc);
        }

        System.out.println();
        System.out.println("METHODS");
        for (MethodJavadoc methodDoc : classDoc.getMethods()) {
            printMethodJavadoc(methodDoc);
        }
    }

    private static void printMethodJavadoc(MethodJavadoc methodDoc) {
        System.out.println(methodDoc.getName() + methodDoc.getParamTypes());
        System.out.println(format(methodDoc.getComment()));

        if (!methodDoc.isConstructor()) {
            System.out.println("  returns " + format(methodDoc.getReturns()));
        }

        for (SeeAlsoJavadoc see : methodDoc.getSeeAlso()) {
            System.out.println("  See also: " + see.getLink());
        }
        for (OtherJavadoc other : methodDoc.getOther()) {
            System.out.println("  " + other.getName() + ": "
                    + format(other.getComment()));
        }
        for (ParamJavadoc paramDoc : methodDoc.getParams()) {
            System.out.println("  param " + paramDoc.getName() + " "
                    + format(paramDoc.getComment()));
        }
        for (ThrowsJavadoc throwsDoc : methodDoc.getThrows()) {
            System.out.println("  throws " + throwsDoc.getName() + " "
                    + format(throwsDoc.getComment()));
        }
        System.out.println();
    }

    public static void main(String[] args) throws Exception {
        getMsgCoeInsertSQL("com.blink.base.constant.BaseAppConstant");
    }

}
