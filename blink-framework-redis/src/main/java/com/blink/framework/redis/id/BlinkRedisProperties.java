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
     * 是否启用分布式锁
     */
    private Boolean enableRedisson = false;

    /**
     * 是否启用本地缓存 caffeine实现
     */
    private Boolean enableLocalCache = false;

    /**
     * redis的模式 只有有两种sync 阻塞式  reactive响应式
     *  默认 sync
     */
    private String mode = "sync";


    private IdGenerator idGenerator = new IdGenerator();


    @PostConstruct
    public void init() throws IOException {

        if(StrUtil.isBlank(this.idGenerator.luaPath)){
            this.idGenerator.luaPath = IdGeneratorConstant.DEFAULT_IDGEN_LUA_FILE_PATH;
        }

        StringBuilder content = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(
                SeqGenerator.class.getClassLoader().
                        getResourceAsStream(this.idGenerator.luaPath)));
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

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public IdGenerator getIdGenerator() {
        return idGenerator;
    }

    public void setIdGenerator(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    public Boolean getEnableRedisson() {
        return enableRedisson;
    }

    public void setEnableRedisson(Boolean enableRedisson) {
        this.enableRedisson = enableRedisson;
    }

    public Boolean getEnableLocalCache() {
        return enableLocalCache;
    }

    public void setEnableLocalCache(Boolean enableLocalCache) {
        this.enableLocalCache = enableLocalCache;
    }

    public static class IdGenerator{

        /**
         * 配置key 每次生成id的增量
         *
         * <key,deltaValue> [{"xxxx": 1},{"aaa":1000}]
         */
        private Map<String, Integer> keySteps = new HashMap<>();

        /**
         * luaScript 脚本路径
         *
         */
        private String luaPath;

        private String luaScript;


        public Integer getKeySteps(String key) {
            //是否有配置
            Integer delta = this.getKeySteps().get(key);

            if (Objects.isNull(delta)) {
                delta = IdGeneratorConstant.DEFAULT_STEP;
            }

            return delta;
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
