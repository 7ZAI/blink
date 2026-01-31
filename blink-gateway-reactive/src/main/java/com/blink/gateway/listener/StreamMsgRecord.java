package com.blink.gateway.listener;

import com.blink.framework.redis.mq.StreamMessage;

/**
 * @Author binblink
 */
public class StreamMsgRecord {

    private String id;

    private String streamKey;

    private String groupName;

    private StreamMessage<?> streamMessage;

    private Boolean handledResult = false;

    public StreamMsgRecord() {
    }

    public StreamMsgRecord(String id, String streamKey, String groupName, StreamMessage<?> streamMessage) {
        this.id = id;
        this.streamKey = streamKey;
        this.groupName = groupName;
        this.streamMessage = streamMessage;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public StreamMessage<?> getStreamMessage() {
        return streamMessage;
    }

    public void setStreamMessage(StreamMessage<?> streamMessage) {
        this.streamMessage = streamMessage;
    }

    public String getStreamKey() {
        return streamKey;
    }

    public void setStreamKey(String streamKey) {
        this.streamKey = streamKey;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Boolean getHandledResult() {
        return handledResult;
    }

    public void setHandledResult(Boolean handledResult) {
        this.handledResult = handledResult;
    }
}
