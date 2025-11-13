package com.blink.base;

import com.alibaba.fastjson2.JSON;
import com.blink.base.dto.req.SysLoginReqDTO;
import com.blink.base.dto.vo.CaptchaVO;
import com.blink.framework.common.data.RequestDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

//@SpringBootTest
//@AutoConfigureMockMvc
public class AppTest {

    @Resource
    private MockMvc mockMvc;

    @Test
    public void test1() throws Exception {

        RequestDTO<SysLoginReqDTO> requestDTO = new RequestDTO<>();
        SysLoginReqDTO loginParam = new SysLoginReqDTO();
        CaptchaVO captchaVO = new CaptchaVO();

        loginParam.setUsername("test1");
        loginParam.setPassword("123456");
        loginParam.setCaptchaVO(captchaVO);
        requestDTO.setBody(loginParam);
        String param = JSON.toJSONString(requestDTO);

        System.out.println(param);

        mockMvc.perform(MockMvcRequestBuilders.post("/system/login")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(param))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print());
    }
}
