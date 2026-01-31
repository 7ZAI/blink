package com.blink.base.controller;


import com.blink.base.dto.req.AddSysConfigReqDTO;
import com.blink.base.dto.req.DeleteSysConfigReqDTO;
import com.blink.base.dto.req.UpdateSysConfigReqDTO;
import com.blink.base.dto.req.QuerySysConfigReqDTO;
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

/**
 *
 *  单元测试
 *  参数配置表 管理API
 *
 * @author blink
 * @since 2025-09-05
 */
@SpringBootTest
@AutoConfigureMockMvc
class SysConfigControllerTest {

    @Resource
    private MockMvc mockMvc;

    /**
     * 测试
     * 新增参数配置表
     *
     */
    @Test
    void testSaveSysConfig() throws Exception {
        AddSysConfigReqDTO reqParam = new AddSysConfigReqDTO();

        MvcResult result = perform("/sysConfig/saveSysConfig", reqParam);
        ResponseDTO response = JacksonUtil.fromJson(result.getResponse().getContentAsString(), ResponseDTO.class);
        Assertions.assertEquals(SysConstant.SUCCESS_CODE, response.getMsgCode());
    }
    /**
     * 测试
     * 删除参数配置表
     *
     */
    @Test
    void testDeleteSysConfig() throws Exception {

        DeleteSysConfigReqDTO reqParam = new DeleteSysConfigReqDTO();

        MvcResult result = perform("/sysConfig/deleteSysConfig", reqParam);
        ResponseDTO response = JacksonUtil.fromJson(result.getResponse().getContentAsString(), ResponseDTO.class);
        Assertions.assertEquals(SysConstant.SUCCESS_CODE, response.getMsgCode());
    }

    /**
     * 测试
     * 更新参数配置表
     *
     */
    @Test
    void testModifySysConfig() throws Exception {

        UpdateSysConfigReqDTO reqParam = new UpdateSysConfigReqDTO();

        MvcResult result = perform("/sysConfig/modifySysConfig", reqParam);
        ResponseDTO response = JacksonUtil.fromJson(result.getResponse().getContentAsString(), ResponseDTO.class);
        Assertions.assertEquals(SysConstant.SUCCESS_CODE, response.getMsgCode());
    }

    /**
     * 测试
     * 根据查询条件查询参数配置表列表
     *
     */
    @Test
    void testGetSysConfigList() throws Exception {

        QuerySysConfigReqDTO reqParam = new QuerySysConfigReqDTO();

        MvcResult result = perform("/sysConfig/getSysConfigList", reqParam);
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
