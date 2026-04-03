package com.blink.gateway.base.controller;

import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.service.CaptchaService;
import com.blink.gateway.base.dto.req.CheckCaptchaReq;
import com.blink.gateway.base.dto.req.GetCaptchaReq;
import com.blink.gateway.base.dto.vo.CaptchaCheckVO;
import com.blink.gateway.base.dto.vo.CaptchaVO;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.core.annotation.RateLimit;
import com.blink.framework.redis.component.RedisClient;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

/**
 * 验证码接口控制器
 * <p>
 * 提供验证码生成、校验等功能，支持滑块拼图和点选文字两种类型
 * </p>
 *
 * @author binblink
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/captcha")
@Validated
public class SysCaptchaController {

    @Resource
    private CaptchaService captchaService;

    @Resource
    private RedisClient redisClient;

    // 验证码验证状态缓存前缀
    private static final String CAPTCHA_VERIFIED_PREFIX = "captcha:verified:";

    private static final String[] CAPTCHA_TYPES = {"blockPuzzle"};  // 暂时只使用滑块拼图，点选文字需要字体配置
    private static final Random RANDOM = new Random();

    /**
     * 随机获取验证码类型
     * @return blockPuzzle 或 clickWord
     */
    private String getRandomCaptchaType() {
        return CAPTCHA_TYPES[RANDOM.nextInt(CAPTCHA_TYPES.length)];
    }

    /**
     * 获取验证码
     * <p>
     * 支持两种类型：
     * - blockPuzzle: 滑块拼图验证码
     * - clickWord: 点选文字验证码
     * 
     * 如果前端不指定类型，则随机返回一种验证码
     * </p>
     *
     * @param reqDto 请求参数
     * @return 验证码数据（包含图片base64、坐标等信息）
     */
    @PostMapping("/get")
    @RateLimit(name = "captchaGet", limitForPeriod = 30, limitRefreshPeriod = 1)
    public ResponseDTO<CaptchaVO> get(@RequestBody @Valid RequestDTO<GetCaptchaReq> reqDto) {
        GetCaptchaReq req = reqDto.getBody();

        // 如果前端未指定验证码类型，则随机选择一种
        String captchaType = req.getCaptchaType();
        if (captchaType == null || captchaType.isEmpty() || "default".equals(captchaType)) {
            captchaType = getRandomCaptchaType();
            log.info("Random captcha type selected: {}", captchaType);
        }

        // 使用全限定名避免类名冲突
        com.anji.captcha.model.vo.CaptchaVO captchaVO = new com.anji.captcha.model.vo.CaptchaVO();
        captchaVO.setCaptchaType(captchaType);
        captchaVO.setClientUid(req.getClientUid());
        captchaVO.setTs(req.getTs());

        ResponseModel responseModel = captchaService.get(captchaVO);

        CaptchaVO resultVO = new CaptchaVO();
        if (responseModel.isSuccess()) {
            Object repData = responseModel.getRepData();
            log.info("Captcha get response data type: {}", repData.getClass().getName());
            
            // 直接使用anji-captcha返回的VO
            if (repData instanceof com.anji.captcha.model.vo.CaptchaVO) {
                com.anji.captcha.model.vo.CaptchaVO anjiCaptchaVO = (com.anji.captcha.model.vo.CaptchaVO) repData;
                log.info("OriginalImageBase64 length: {}", 
                    anjiCaptchaVO.getOriginalImageBase64() != null ? anjiCaptchaVO.getOriginalImageBase64().length() : 0);
                log.info("JigsawImageBase64 length: {}", 
                    anjiCaptchaVO.getJigsawImageBase64() != null ? anjiCaptchaVO.getJigsawImageBase64().length() : 0);
                
                // 手动转换字段
                // anji-captcha使用token作为验证码ID
                String captchaId = anjiCaptchaVO.getCaptchaId();
                if (StrUtil.isBlank(captchaId)) {
                    captchaId = anjiCaptchaVO.getToken();
                }
                resultVO.setCaptchaId(captchaId);
                log.info("CaptchaId set to: {}", captchaId);
                
                resultVO.setCaptchaType(anjiCaptchaVO.getCaptchaType());
                log.info("CaptchaType set to: {}", anjiCaptchaVO.getCaptchaType());
                resultVO.setOriginalImageBase64(anjiCaptchaVO.getOriginalImageBase64());
                resultVO.setJigsawImageBase64(anjiCaptchaVO.getJigsawImageBase64());
                resultVO.setToken(anjiCaptchaVO.getToken());
                resultVO.setPointJson(anjiCaptchaVO.getPointJson());
                resultVO.setWordList(anjiCaptchaVO.getWordList());
            } else {
                BeanUtils.copyProperties(repData, resultVO);
            }
        }

        return ResponseDTO.newSuccessInstance(resultVO);
    }

    /**
     * 校验验证码
     * <p>
     * 用户完成验证码操作后，调用此接口进行校验
     * </p>
     *
     * @param reqDto 请求参数（包含验证码ID、坐标数据等）
     * @return 校验结果
     */
    @PostMapping("/check")
    @RateLimit(name = "captchaCheck", limitForPeriod = 20, limitRefreshPeriod = 1)
    public ResponseDTO<CaptchaCheckVO> check(@RequestBody @Valid RequestDTO<CheckCaptchaReq> reqDto) {
        CheckCaptchaReq req = reqDto.getBody();

        // 使用全限定名避免类名冲突
        com.anji.captcha.model.vo.CaptchaVO captchaVO = new com.anji.captcha.model.vo.CaptchaVO();
        // anji-captcha 使用 token 作为验证码标识
        captchaVO.setToken(req.getCaptchaId());
        captchaVO.setCaptchaType(req.getCaptchaType());
        captchaVO.setPointJson(req.getPointJson());
        captchaVO.setClientUid(req.getClientUid());
        captchaVO.setTs(req.getTs());

        log.info("Captcha check request - token: {}, captchaType: {}, pointJson: {}", 
            req.getCaptchaId(), req.getCaptchaType(), req.getPointJson());

        ResponseModel responseModel = captchaService.check(captchaVO);
        
        log.info("Captcha check response - success: {}, msg: {}", 
            responseModel.isSuccess(), responseModel.getRepMsg());

        CaptchaCheckVO checkVO = new CaptchaCheckVO();
        if (responseModel.isSuccess()) {
            checkVO.setResult(true);
            checkVO.setMsg("校验成功");
            Object repData = responseModel.getRepData();
            log.info("Captcha check repData type: {}, value: {}",
                repData != null ? repData.getClass().getName() : "null", repData);

            String captchaVerification = null;
            String captchaId = req.getCaptchaId();

            if (repData instanceof com.anji.captcha.model.vo.CaptchaVO) {
                com.anji.captcha.model.vo.CaptchaVO resultData = (com.anji.captcha.model.vo.CaptchaVO) repData;
                captchaVerification = resultData.getCaptchaVerification();
                if (resultData.getCaptchaId() != null) {
                    captchaId = resultData.getCaptchaId();
                }
            } else if (repData instanceof java.util.Map) {
                // 处理 repData 为 Map 的情况
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) repData;
                Object verification = dataMap.get("captchaVerification");
                if (verification != null) {
                    captchaVerification = verification.toString();
                }
                Object id = dataMap.get("captchaId");
                if (id != null) {
                    captchaId = id.toString();
                }
            }

            // 如果 captchaVerification 为空，使用 token 作为验证凭证
            if (StrUtil.isBlank(captchaVerification)) {
                // 使用 token 作为二次校验凭证
                captchaVerification = req.getCaptchaId();
                log.info("captchaVerification is null, using token as verification: {}", captchaVerification);
            }

            // 将验证成功的 token 存储到 Redis，有效期 5 分钟
            String verifiedKey = CAPTCHA_VERIFIED_PREFIX + captchaVerification;
            redisClient.setEx(verifiedKey, "1", 5 * 60);
            log.info("Captcha verified, token stored in Redis: {}", captchaVerification);

            checkVO.setCaptchaId(captchaId);
            checkVO.setCaptchaVerification(captchaVerification);
        } else {
            checkVO.setResult(false);
            checkVO.setMsg(responseModel.getRepMsg());
        }

        return ResponseDTO.newSuccessInstance(checkVO);
    }

    /**
     * 二次校验验证码
     * <p>
     * 用于关键业务场景的后台二次验证
     * </p>
     *
     * @param reqDto 请求参数（包含captchaVerification）
     * @return 校验结果
     */
    @PostMapping("/verify")
    @RateLimit(name = "captchaVerify", limitForPeriod = 10, limitRefreshPeriod = 1)
    public ResponseDTO<CaptchaCheckVO> verify(@RequestBody @Valid RequestDTO<CheckCaptchaReq> reqDto) {
        CheckCaptchaReq req = reqDto.getBody();

        // 使用全限定名避免类名冲突
        com.anji.captcha.model.vo.CaptchaVO captchaVO = new com.anji.captcha.model.vo.CaptchaVO();
        captchaVO.setCaptchaVerification(req.getPointJson());

        ResponseModel responseModel = captchaService.verification(captchaVO);

        CaptchaCheckVO checkVO = new CaptchaCheckVO();
        checkVO.setResult(responseModel.isSuccess());
        checkVO.setMsg(responseModel.getRepMsg());

        return ResponseDTO.newSuccessInstance(checkVO);
    }
}
