package com.blink.base.controller;

import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.service.CaptchaService;
import com.blink.base.dto.req.CheckCaptchaReq;
import com.blink.base.dto.req.GetCaptchaReq;
import com.blink.base.dto.vo.CaptchaCheckVO;
import com.blink.base.dto.vo.CaptchaVO;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.redis.component.RedisClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

/**
 * SysCaptchaController 单元测试类
 *
 * @author binblink
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SysCaptchaController 单元测试")
class SysCaptchaControllerTest {

    @Mock
    private CaptchaService captchaService;

    @Mock
    private RedisClient redisClient;

    @InjectMocks
    private SysCaptchaController sysCaptchaController;

    @BeforeEach
    void setUp() {
        // 初始化设置
    }

    @Nested
    @DisplayName("get 测试")
    class GetTests {

        @Test
        @DisplayName("获取验证码 - 滑块拼图类型")
        void testGet_BlockPuzzle() {
            GetCaptchaReq req = new GetCaptchaReq();
            req.setCaptchaType("blockPuzzle");

            RequestDTO<GetCaptchaReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            com.anji.captcha.model.vo.CaptchaVO anjiCaptchaVO = new com.anji.captcha.model.vo.CaptchaVO();
            anjiCaptchaVO.setCaptchaId("test_captcha_id");
            anjiCaptchaVO.setCaptchaType("blockPuzzle");
            anjiCaptchaVO.setOriginalImageBase64("base64_image");
            anjiCaptchaVO.setJigsawImageBase64("base64_jigsaw");

            ResponseModel responseModel = ResponseModel.successData(anjiCaptchaVO);

            when(captchaService.get(any())).thenReturn(responseModel);

            ResponseDTO<CaptchaVO> response = sysCaptchaController.get(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            verify(captchaService, times(1)).get(any());
        }

        @Test
        @DisplayName("获取验证码 - 默认类型随机选择")
        void testGet_DefaultType() {
            GetCaptchaReq req = new GetCaptchaReq();
            req.setCaptchaType("default");

            RequestDTO<GetCaptchaReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            com.anji.captcha.model.vo.CaptchaVO anjiCaptchaVO = new com.anji.captcha.model.vo.CaptchaVO();
            anjiCaptchaVO.setCaptchaId("test_captcha_id");
            anjiCaptchaVO.setCaptchaType("blockPuzzle");

            ResponseModel responseModel = ResponseModel.successData(anjiCaptchaVO);

            when(captchaService.get(any())).thenReturn(responseModel);

            ResponseDTO<CaptchaVO> response = sysCaptchaController.get(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(captchaService, times(1)).get(any());
        }

        @Test
        @DisplayName("获取验证码 - 空类型随机选择")
        void testGet_EmptyType() {
            GetCaptchaReq req = new GetCaptchaReq();
            req.setCaptchaType("");

            RequestDTO<GetCaptchaReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            com.anji.captcha.model.vo.CaptchaVO anjiCaptchaVO = new com.anji.captcha.model.vo.CaptchaVO();
            anjiCaptchaVO.setCaptchaId("test_captcha_id");
            anjiCaptchaVO.setCaptchaType("blockPuzzle");

            ResponseModel responseModel = ResponseModel.successData(anjiCaptchaVO);

            when(captchaService.get(any())).thenReturn(responseModel);

            ResponseDTO<CaptchaVO> response = sysCaptchaController.get(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(captchaService, times(1)).get(any());
        }

        @Test
        @DisplayName("获取验证码 - 失败场景")
        void testGet_Failed() {
            GetCaptchaReq req = new GetCaptchaReq();
            req.setCaptchaType("blockPuzzle");

            RequestDTO<GetCaptchaReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            ResponseModel responseModel = ResponseModel.errorMsg("获取验证码失败");

            when(captchaService.get(any())).thenReturn(responseModel);

            ResponseDTO<CaptchaVO> response = sysCaptchaController.get(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            verify(captchaService, times(1)).get(any());
        }
    }

    @Nested
    @DisplayName("check 测试")
    class CheckTests {

        @Test
        @DisplayName("校验验证码 - 成功场景")
        void testCheck_Success() {
            CheckCaptchaReq req = new CheckCaptchaReq();
            req.setCaptchaId("test_captcha_id");
            req.setCaptchaType("blockPuzzle");
            req.setPointJson("{\"x\":100}");

            RequestDTO<CheckCaptchaReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            com.anji.captcha.model.vo.CaptchaVO anjiCaptchaVO = new com.anji.captcha.model.vo.CaptchaVO();
            anjiCaptchaVO.setCaptchaVerification("verification_token");
            anjiCaptchaVO.setCaptchaId("test_captcha_id");

            ResponseModel responseModel = ResponseModel.successData(anjiCaptchaVO);

            when(captchaService.check(any())).thenReturn(responseModel);
            lenient().doNothing().when(redisClient).setEx(anyString(), anyString(), anyLong());

            ResponseDTO<CaptchaCheckVO> response = sysCaptchaController.check(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().getResult());
            verify(captchaService, times(1)).check(any());
            verify(redisClient, times(1)).setEx(anyString(), anyString(), anyLong());
        }

        @Test
        @DisplayName("校验验证码 - Map类型返回数据")
        void testCheck_MapResponse() {
            CheckCaptchaReq req = new CheckCaptchaReq();
            req.setCaptchaId("test_captcha_id");
            req.setCaptchaType("blockPuzzle");
            req.setPointJson("{\"x\":100}");

            RequestDTO<CheckCaptchaReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("captchaVerification", "verification_token");
            dataMap.put("captchaId", "test_captcha_id");

            ResponseModel responseModel = ResponseModel.successData(dataMap);

            when(captchaService.check(any())).thenReturn(responseModel);
            lenient().doNothing().when(redisClient).setEx(anyString(), anyString(), anyLong());

            ResponseDTO<CaptchaCheckVO> response = sysCaptchaController.check(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertTrue(response.getBody().getResult());
            verify(captchaService, times(1)).check(any());
        }

        @Test
        @DisplayName("校验验证码 - 失败场景")
        void testCheck_Failed() {
            CheckCaptchaReq req = new CheckCaptchaReq();
            req.setCaptchaId("test_captcha_id");
            req.setCaptchaType("blockPuzzle");
            req.setPointJson("{\"x\":50}");

            RequestDTO<CheckCaptchaReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            ResponseModel responseModel = ResponseModel.errorMsg("验证失败");

            when(captchaService.check(any())).thenReturn(responseModel);

            ResponseDTO<CaptchaCheckVO> response = sysCaptchaController.check(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            assertFalse(response.getBody().getResult());
            assertEquals("验证失败", response.getBody().getMsg());
            verify(captchaService, times(1)).check(any());
        }
    }

    @Nested
    @DisplayName("verify 测试")
    class VerifyTests {

        @Test
        @DisplayName("二次校验验证码 - 成功场景")
        void testVerify_Success() {
            CheckCaptchaReq req = new CheckCaptchaReq();
            req.setPointJson("verification_token");

            RequestDTO<CheckCaptchaReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            ResponseModel responseModel = ResponseModel.successMsg("校验成功");

            when(captchaService.verification(any())).thenReturn(responseModel);

            ResponseDTO<CaptchaCheckVO> response = sysCaptchaController.verify(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().getResult());
            assertEquals("校验成功", response.getBody().getMsg());
            verify(captchaService, times(1)).verification(any());
        }

        @Test
        @DisplayName("二次校验验证码 - 失败场景")
        void testVerify_Failed() {
            CheckCaptchaReq req = new CheckCaptchaReq();
            req.setPointJson("invalid_token");

            RequestDTO<CheckCaptchaReq> requestDTO = new RequestDTO<>();
            requestDTO.setBody(req);

            ResponseModel responseModel = ResponseModel.errorMsg("验证码已过期");

            when(captchaService.verification(any())).thenReturn(responseModel);

            ResponseDTO<CaptchaCheckVO> response = sysCaptchaController.verify(requestDTO);

            assertNotNull(response);
            assertEquals("BLINK0000", response.getMsgCode());
            assertNotNull(response.getBody());
            assertFalse(response.getBody().getResult());
            assertEquals("验证码已过期", response.getBody().getMsg());
            verify(captchaService, times(1)).verification(any());
        }
    }
}