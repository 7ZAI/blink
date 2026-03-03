package com.blink.framework.common.crypt;

/**
 * @Author binblink
 * @Date 2026/2/18
 */
public abstract class Cryptor {

    abstract String decrypt(String data) throws Exception;

    abstract String encrypt(String data) throws Exception;
}
