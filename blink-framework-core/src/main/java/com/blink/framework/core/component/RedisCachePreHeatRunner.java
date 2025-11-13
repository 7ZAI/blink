package com.blink.framework.core.component;

import com.blink.framework.common.exception.BlinkErrorCodeEnum;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.utils.ApplicationContextUtil;
import com.blink.framework.core.annotation.PreHeatData;
import com.blink.framework.core.data.CoreConstant;
import com.blink.framework.core.entity.SysDataDictDO;
import com.blink.framework.core.entity.SysMsgInfoDO;
import com.blink.framework.core.mapper.SysDataDictMapper;
import com.blink.framework.core.mapper.SysMsgInfoMapper;
import com.blink.framework.core.util.ScanClassUtil;
import com.blink.framework.redis.component.CacheComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * redis在应用启动后 加载数据
 *
 * @author binblink
 */
@Component
@Slf4j
public class RedisCachePreHeatRunner implements ApplicationRunner {

    private final SysDataDictMapper sysDataDictMapper;
    private final SysMsgInfoMapper sysMsgInfoMapper;
    private final CacheComponent cacheComponent;

    public RedisCachePreHeatRunner(CacheComponent cacheComponent,SysMsgInfoMapper sysMsgInfoMapper,SysDataDictMapper sysDataDictMapper) {
        this.cacheComponent = cacheComponent;
        this.sysMsgInfoMapper = sysMsgInfoMapper;
        this.sysDataDictMapper = sysDataDictMapper;
    }

    /**
     * 正则截取最后一个。后的字符串
     */
    final Pattern pattern = Pattern.compile("[^.]*$");

    @Override
    public void run(ApplicationArguments args) throws BlinkException {

        preHeatDataByScanPackage();
        preHeatingDataDictCache();
        preHeatingMsgInfoCache();
    }

    /**
     *  预加载数据到redis中
     *  统一调用外部声明了@PreHeatData的类
     */
    private void preHeatDataByScanPackage(){

        try {
            List<String> classNames = ScanClassUtil.getClassNameByScanAnnotation("com.blink.*", PreHeatData.class);
            log.info("scanClass fund className with @PreHeatData :{}",classNames);
            for (String s : classNames) {

                Matcher matcher = pattern.matcher(s);
                if(matcher.find()){
                    String beanName  = matcher.group();
                    beanName = Character.toLowerCase(beanName.charAt(0)) + beanName.substring(1);
                    Object obj = ApplicationContextUtil.getBean(beanName);
                    Class clazz =  obj.getClass();
                    PreHeatData[] preHeatData = (PreHeatData[]) clazz.getAnnotationsByType(PreHeatData.class);
                    if (preHeatData[0].enable()) {
                        String methodName = preHeatData[0].method();
                        Method method = clazz.getMethod(methodName);
                        method.setAccessible(true);
                        method.invoke(obj);
                    }
                }
            }
        } catch (Exception e) {
            log.error("PreHeatData occurs error: {}",e.getMessage());
            throw new BlinkException(e, BlinkErrorCodeEnum.SYS_ERROR.getCode());
        }
    }



    /**
     * 初始化数据字典缓存
     */
    private void preHeatingDataDictCache() {
        log.info("-----------------------------------------preHeatingDataDictCache-----------------------------------------");
        cacheComponent.loadCacheFromDB(CoreConstant.DICT_KEY_PREFIX, () -> {
            List<SysDataDictDO> dictList = sysDataDictMapper.findAllDataDicts();
            return dictList.stream().collect(Collectors.toMap(dict -> CoreConstant.DICT_KEY_PREFIX + dict.getDictName()
                    , dict -> dict));
        });
    }

    /**
     * 初始化消息缓存
     */
    private void preHeatingMsgInfoCache() {

        log.info("-----------------------------------------preHeatingMsgInfoCache-----------------------------------------");
        cacheComponent.loadCacheFromDB(CoreConstant.MSG_INFO_KEY_PREFIX, () -> {

            List<SysMsgInfoDO> msgInfoList = sysMsgInfoMapper.findAllMsgInfo();

            return msgInfoList.stream().collect(Collectors.toMap(sysMsgInfoDO ->
                            CoreConstant.MSG_INFO_KEY_PREFIX + sysMsgInfoDO.getMsgLang() + ":" + sysMsgInfoDO.getMsgCode()
                    , SysMsgInfoDO::getMsgInfo));
        });
    }


    public void refreshDataDictCache() {
        preHeatingDataDictCache();
    }

    public void refreshMsgInfoCache() {
        preHeatingMsgInfoCache();
    }

}
