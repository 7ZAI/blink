package com.blink.redis;

import com.blink.framework.redis.id.IdGenerator;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;


/**
 * jmeter测试用
 *
 * @Author binblink
 */

@RequestMapping(("/api"))
@RestController
public class GeneratorIdTestController {

    @Resource
    private IdGenerator idGenerator;

    @GetMapping("/test")
    public Result generate() {
        return new Result("test",idGenerator.generateId("test"));
    }

    public static class Result{

        private String key;
        private Long id;

        public Result(String key, Long id) {
            this.key = key;
            this.id = id;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }
}
