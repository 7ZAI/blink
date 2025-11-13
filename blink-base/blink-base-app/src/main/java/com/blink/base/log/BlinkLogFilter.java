package com.blink.base.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class BlinkLogFilter extends Filter<ILoggingEvent> {

    public BlinkLogFilter(){

    }

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (event.getThreadName().startsWith("kont-task")) {
            return FilterReply.DENY;
        } else if (event.getThreadName().startsWith("Eureka-JerseyClient-Conn-Cleaner")) {
            return FilterReply.DENY;
        } else if (event.getThreadName().startsWith("NettyClientHandler")) {
            return FilterReply.DENY;}
        else if (event.getThreadName().startsWith("DiscoveryClient-CacheRefreshExecutor")) {
            return FilterReply.DENY;
        } else if (event.getThreadName().startsWith("DiscoveryClient-HeartbeatExecutor")) {
            return FilterReply.DENY;
        } else if (event.getThreadName().startsWith("PollingServerListUpdater-")) {
            return FilterReply.DENY;
        } else if (event.getThreadName().startsWith("NFLoadBalancer-PingTimer-")) {
            return FilterReply.DENY;
        } else if (event.getThreadName().startsWith("SimpleHostRoutingFilter.connectionManagerTimer")) {
            return FilterReply.DENY;
        } else if (event.getThreadName().startsWith("DiscoveryClient-InstanceInfoReplicator")) {
            return FilterReply.DENY;
        } else if ("com.netflix.discovery.DiscoveryClient".equals(event.getLoggerName())) {
            return FilterReply.DENY;
        } else if (event.getLoggerName().startsWith("com.netflix.discovery.shared")) {
            return FilterReply.DENY;
        } else if ("org.apache.http.headers".equals(event.getLoggerName())) {
            return FilterReply.DENY;
        } else {
            return "org.apache.http.wire".equals(event.getLoggerName()) ? FilterReply.DENY : FilterReply.ACCEPT;
        }
    }
}
