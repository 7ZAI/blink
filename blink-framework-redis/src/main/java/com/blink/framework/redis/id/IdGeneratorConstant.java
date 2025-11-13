package com.blink.framework.redis.id;

/**
 * 生成ID用到的常量
 *
 * @author binblink
 */
public class IdGeneratorConstant {

    /**
     * Redis cluster对多key操作有限，要求命令中所有的key都属于一个slot，才可以被执行。
     * 比如在一个管道中多次执行多个key
     * 利用 hash tag {samekey}.xxx来使多个key落到同一个slot
     * 这里lua脚本只执行一个key 不影响
     */
    public static final String DEFAULT_SLOT_HASHTAG = "{IDGEN}." ;

    public static final String MSG_ID_KEY = "message:seq";

    public static final String REQ_ID_KEY = "request:seq";

    public static final String MQ_MSG_ID_KEY = "mq:message:seq";

    public static final String TRANCE_ID_KEY = "trance:seq";

    /**
     * 默认step值
     */
    public static final Integer DEFAULT_STEP = 1000;
    /**
     * lua脚本默认路径
     */
    public static final String DEFAULT_IDGEN_LUA_FILE_PATH = "lua/IdGen.lua";

    /**
     * 默认的最大值，18位 redis 虽然incr的最大值为Long.MAX 是19位的 这里取18位已经足够使用
     */
    public static final String DEFAULT_KEY_MAX_VALUE = "999999999999999999";

    /**
     * 最大长度位18，超过前面补0
     */
    public static final int DEFAULT_MAX_LENGTH = 18;

    /**
     * 普通长度位10
     */
    public static final int DEFAULT_LENGTH = 10;


    /**
     * 默认填充字符0
     */
    public static final char DEFAULT_SEQUENCE_PAD_CHAR = '0';


}
