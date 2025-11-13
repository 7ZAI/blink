package com.blink.base.entity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import jakarta.validation.constraints.NotNull;

import org.springframework.validation.annotation.Validated;

import static org.springframework.util.StringUtils.tokenizeToStringArray;

/**
 * 类复制来自 gateway中的同名类 为了转换json
 * @Author binblink
 */
@Validated
public class FilterDefinitionDO {

    @NotNull
    private String name;

    private Map<String, String> args = new LinkedHashMap<>();

    public FilterDefinitionDO() {
    }

    public FilterDefinitionDO(String text) {
        int eqIdx = text.indexOf('=');
        if (eqIdx <= 0) {
            setName(text);
            return;
        }
        setName(text.substring(0, eqIdx));

        String[] args = tokenizeToStringArray(text.substring(eqIdx + 1), ",");

        for (int i = 0; i < args.length; i++) {
            this.args.put("_genkey_" + i, args[i]);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getArgs() {
        return args;
    }

    public void setArgs(Map<String, String> args) {
        this.args = args;
    }

    public void addArg(String key, String value) {
        this.args.put(key, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FilterDefinitionDO that = (FilterDefinitionDO) o;
        return Objects.equals(name, that.name) && Objects.equals(args, that.args);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, args);
    }

    @Override
    public String toString() {
        String sb = "FilterDefinition{" + "name='" + name + '\'' +
                ", args=" + args +
                '}';
        return sb;
    }

}
