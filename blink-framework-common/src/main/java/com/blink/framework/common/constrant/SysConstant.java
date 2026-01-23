package com.blink.framework.common.constrant;

/**
 * 系统常量类
 * @author binblink
 * */
public interface SysConstant {


   String SUCCESS_CODE = "BLINK0000";

   String FAIL_CODE = "BLINK0001";

   String UTF_8 = "UTF-8";

   String TYPE = "Content-Type";

   String APPLICATION_JSON_UTF8_VALUE = "application/json;charset=UTF-8";

   /**----------------------------------------------自定义请求头常量-----------------------------------------------------------**/

   /**
    * 登入用户id
    *  custom request header
    */
   String X_BLINK_USRID = "x-blink-usrId";

   /**
    * 登入用户名
    */
   String X_BLINK_LOGINNAME = "x-blink-loginName";

   /**
    * 来源
    */
   String X_BLINK_SOURCE = "x-blink-source";

   /**
    * 是不是加密报文 0 否 1是
    */
   String X_BLINK_ENCRYPTED = "x-blink-encrypted";

   /**
    * 客户端ip地址
    */
   String X_BLINK_CLIENTIP = "x-blink-clientIp";

   /**
    * 渠道
    */
   String X_BLINK_CHANNEL = "x-blink-channel";
   /**
    * 请求id
    */
   String X_BLINK_REQUEST_ID = "x-blink-requestId";

   /**
    * 跟踪id
    */
   String X_BLINK_TRACE_ID = "x-blink-traceId";

   /**
    * 用户token
    */
   String X_BLINK_TOKEN = "x-blink-token";

   /**
    * 应用key
    */
   String X_BLINK_APPKEY = "x-blink-appKey";

   /**
    * key(16位随机数)
    */
   String X_BLINK_KEY = "x-blink-key";

   /**
    * 语言
    */
   String X_BLINK_LOCALE = "x-blink-locale";

   /**
    * 偏移量
    */
   String X_BLINK_IV = "x-blink-iv";
   /**
    * 签名
    */
   String X_BLINK_SIGN = "x-blink-sign";

   /**
    * 时间戳 必填
    */
   String X_BLINK_TIMESTAMP = "x-blink-timestamp";

   /**
    * 随机数 必填
    */
   String X_BLINK_NONCE = "x-blink-nonce";



}
