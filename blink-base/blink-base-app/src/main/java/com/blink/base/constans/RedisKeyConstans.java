package com.blink.base.constans;

public interface RedisKeyConstans {


    String BASE_APP = "base-app:";

    String USER_TOKEN = "user:token:";

    String USER_TOKEN_OLD = "user:token:old:";

    String USER_INFO = "user:info:";

    String BLINK_PREFIX = "blink:";

    String CHANNEL_INFO = BLINK_PREFIX + "channel:";

    String URL_PERMISSION =  "permission:identity:";

    String GATEWAY_CONFIG_PREFIX = BLINK_PREFIX + "config:gateway:";

    String GATEWAY_DYNAMIC_ROUTES = BLINK_PREFIX +"gateway:routes";

    /**
     * gateway同步 stream key
     */
    String GATEWAY_STREAM_EVENT = BLINK_PREFIX + "stream:gateway:event";




}
