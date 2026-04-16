package com.blink.base.constants;

/**
 * 定时任务状态常量
 *
 * @author binblink
 */
public interface JobStatusConstant {

    /**
     * 任务状态 - 暂停
     */
    Byte PAUSED = 0;

    /**
     * 任务状态 - 正常
     */
    Byte NORMAL = 1;

    /**
     * 执行状态 - 执行中
     */
    Byte RUNNING = 0;

    /**
     * 执行状态 - 成功
     */
    Byte SUCCESS = 1;

    /**
     * 执行状态 - 失败
     */
    Byte FAILED = 2;
}