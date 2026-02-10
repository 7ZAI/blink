package com.blink.base.controller;

import com.blink.base.constans.CommonConstans;
import com.blink.base.dto.req.*;
import com.blink.framework.common.constrant.SysConstant;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.utils.JacksonUtil;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class BlinkChannelControllerTest {

    @Resource
    private MockMvc mockMvc;


    @Test
    void testSaveBlinkChannel() throws Exception {
        AddBlinkChannelReqDTO reqParam = new AddBlinkChannelReqDTO();
        reqParam.setChannelName("ThirdParty");

        reqParam.setEnable(CommonConstans.SWITCH_OPEN);
        reqParam.setAuthoritySwitch(CommonConstans.SWITCH_CLOSE);
        reqParam.setEncryptionSwitch(CommonConstans.SWITCH_CLOSE);
        reqParam.setRelaUserId("1");

        MvcResult result = perform("/channel/saveChannel", reqParam);
        ResponseDTO response = JacksonUtil.fromJson(result.getResponse().getContentAsString(), ResponseDTO.class);
        Assertions.assertEquals(SysConstant.SUCCESS_CODE, response.getMsgCode());
    }

    @Test
    void testDeleteBlinkChannel() throws Exception {

        DeleteBlinkChannelReqDTO reqParam = new DeleteBlinkChannelReqDTO();

        reqParam.setDeleteId("fea35977-803d-46de-945c-e42916d95c05");


        MvcResult result = perform("/channel/deleteChannel", reqParam);
        ResponseDTO response = JacksonUtil.fromJson(result.getResponse().getContentAsString(), ResponseDTO.class);
        Assertions.assertEquals(SysConstant.SUCCESS_CODE, response.getMsgCode());
    }

    @Test
    void testModifyBlinkChannel() throws Exception {

        UpdateBlinkChannelReqDTO reqParam = new UpdateBlinkChannelReqDTO();

        reqParam.setChannelId("e4090c3738214a6d9a4fc80d59622e5f");
        reqParam.setChannelName("test");

        reqParam.setEnable(CommonConstans.SWITCH_CLOSE);
        reqParam.setAuthoritySwitch(CommonConstans.SWITCH_CLOSE);
        reqParam.setEncryptionSwitch(CommonConstans.SWITCH_CLOSE);

        MvcResult result = perform("/channel/modifyChannel", reqParam);
        ResponseDTO response = JacksonUtil.fromJson(result.getResponse().getContentAsString(), ResponseDTO.class);
        Assertions.assertEquals(SysConstant.SUCCESS_CODE, response.getMsgCode());
    }

    @Test
    void testGetBlinkChannelList() throws Exception {

        QueryBlinkChannelReqDTO reqParam = new QueryBlinkChannelReqDTO();

        MvcResult result = perform("/channel/getChannelList", reqParam);
        ResponseDTO response = JacksonUtil.fromJson(result.getResponse().getContentAsString(), ResponseDTO.class);
        Assertions.assertEquals(SysConstant.SUCCESS_CODE, response.getMsgCode());
    }


    @Test
    void testGetChannel() throws Exception {
        QueryOneChannelReqDTO reqParam = new QueryOneChannelReqDTO();
//        reqParam.setChannelId("e4090c3738214a6d9a4fc80d59622e5f");
        reqParam.setChannelName("Apple");

        MvcResult result = perform("/channel/getChannel", reqParam);
        ResponseDTO response = JacksonUtil.fromJson(result.getResponse().getContentAsString(), ResponseDTO.class);
        Assertions.assertEquals(SysConstant.SUCCESS_CODE, response.getMsgCode());
    }


    private <T> MvcResult perform(String url, T reqParam) throws Exception {

        RequestDTO reqDto = new RequestDTO<>();
        reqDto.setBody(reqParam);
        String param = JacksonUtil.toJson(reqDto);

        return mockMvc.perform(MockMvcRequestBuilders.post(url)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(param))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

    }


}
