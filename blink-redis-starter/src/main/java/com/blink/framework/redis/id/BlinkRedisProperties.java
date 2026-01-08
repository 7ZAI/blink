package com.blink.framework.redis.id;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


/**
 * 属性配置类
 *
 * @author binblink
 */
@ConfigurationProperties(prefix = "blink.redis")
@Slf4j
public class BlinkRedisProperties {


    /**
     * 是否启用本地缓存 caffeine实现
     */
    private Boolean enableLocalCache = false;


    private IdGenerator idGenerator = new IdGenerator();


    @PostConstruct
    public void init() throws IOException {

        if (StrUtil.isBlank(this.idGenerator.luaPath)) {
            this.idGenerator.luaPath = IdGeneratorConstant.DEFAULT_IDGEN_LUA_FILE_PATH;
        }

        StringBuilder content = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(SeqGenerator.class.getClassLoader().
                        getResourceAsStream(this.idGenerator.luaPath))));
        while (true) {
            String str = null;
            if ((str = br.readLine()) == null) {
                break;
            }
            content.append(str).append(System.getProperty("line.separator"));
        }

        this.idGenerator.luaScript = content.toString();
        if (log.isDebugEnabled()) {
            log.debug("Load IdGen lua script {} ~~~ {}", this.idGenerator.luaPath, this.idGenerator.luaScript);
        }
    }

    public IdGenerator getIdGenerator() {
        return idGenerator;
    }

    public void setIdGenerator(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }


    public Boolean getEnableLocalCache() {
        return enableLocalCache;
    }

    public void setEnableLocalCache(Boolean enableLocalCache) {
        this.enableLocalCache = enableLocalCache;
    }

    /**
     * IdGenerator 配置类
     *
     */
    public static class IdGenerator {

        /**
         * 配置key 每次生成id的增量
         * <p>
         * <key,deltaValue> [{"xxxx": 1},{"aaa":1000}]
         */
        private Map<String, Integer> keySteps = new HashMap<>();

        /**
         * luaScript 脚本路径
         */
        private String luaPath;

        private String luaScript;

        private Double fetchPercent;


        public Integer getKeySteps(String key) {
            //是否有配置
            Integer delta = this.getKeySteps().get(key);

            if (Objects.isNull(delta)) {
                delta = IdGeneratorConstant.DEFAULT_STEP;
            }

            return delta;
        }

        public Double getFetchPercent() {
            return fetchPercent;
        }

        public void setFetchPercent(Double fetchPercent) {
            this.fetchPercent = fetchPercent;
        }

        public Map<String, Integer> getKeySteps() {
            return keySteps;
        }

        public void setKeySteps(Map<String, Integer> keySteps) {
            this.keySteps = keySteps;
        }

        public String getLuaPath() {
            return luaPath;
        }

        public void setLuaPath(String luaPath) {
            this.luaPath = luaPath;
        }

        public String getLuaScript() {
            return this.luaScript;
        }

        public void setLuaScript(String luaScript) {
            this.luaScript = luaScript;
        }
    }


}
