package com.blink.framework.redis.config.prop;

import cn.hutool.core.util.StrUtil;
import com.blink.framework.redis.id.IdGenerator;
import com.blink.framework.redis.id.IdGeneratorConstant;
import com.blink.framework.redis.id.SeqGenerator;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@ConfigurationProperties(prefix = "blink.redis")
@Slf4j
public class BlinkRedisProperties {

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

    public static class IdGenerator {

        private Map<String, SeqParam> seqParam = new HashMap<>();

        private String luaPath;

        private String luaScript;

        public Integer getkeySteps(String key) {
            SeqParam seqParam = this.getSeqParam().get(key);

            Integer defaultStep = IdGeneratorConstant.DEFAULT_STEP;
            if (Objects.isNull(seqParam)) {
                return defaultStep;
            }
            return seqParam.getStep();
        }

        public Map<String, SeqParam> getSeqParam() {
            return seqParam;
        }

        public void setSeqParam(Map<String, SeqParam> seqParam) {
            this.seqParam = seqParam;
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

        public static class SeqParam {
            private Integer step = IdGeneratorConstant.DEFAULT_STEP;
            private Double fetchPercent = IdGeneratorConstant.EIGHTY_PERCENT;

            public Integer getStep() {
                return step;
            }

            public void setStep(Integer step) {
                this.step = step;
            }

            public Double getFetchPercent() {
                return fetchPercent;
            }

            public void setFetchPercent(Double fetchPercent) {
                this.fetchPercent = fetchPercent;
            }
        }
    }
}
