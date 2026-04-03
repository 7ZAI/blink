package com.blink.framework.common.utils;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.ipv4.IPv4Address;
import inet.ipaddr.ipv6.IPv6Address;

/**
 * 基于 inet.ipaddr 库的 IP 工具类
 * 支持 IPv4/IPv6 格式校验、网段归属判断、IP 特性（回环/私有/公网）判断等
 */
public class IPAddressUtils {

    // ===================== 基础格式校验 =====================
    /**
     * 判断字符串是否为合法的 IPv4 地址（纯地址，不含网段前缀）
     * @param ip 待校验的 IP 字符串
     * @return true=合法 IPv4，false=非法
     */
    public static boolean isIPv4Valid(String ip) {
        if (isEmpty(ip)) {
            return false;
        }
        IPAddressString ipStr = new IPAddressString(ip);
        // 仅校验纯 IPv4 地址（排除网段前缀如 /24、范围如 192.168.1.1-10 等）
        return ipStr.isIPv4() && !ipStr.getAddress().isMultiple() && !ipStr.getAddress().isPrefixed();
    }

    /**
     * 判断字符串是否为合法的 IPv6 地址（纯地址，不含网段前缀）
     * @param ip 待校验的 IP 字符串
     * @return true=合法 IPv6，false=非法
     */
    public static boolean isIPv6Valid(String ip) {
        if (isEmpty(ip)) {
            return false;
        }
        IPAddressString ipStr = new IPAddressString(ip);
        // 仅校验纯 IPv6 地址（排除网段前缀如 /64、范围如 2001::1-2001::10 等）
        return ipStr.isIPv6() && !ipStr.getAddress().isMultiple() && !ipStr.getAddress().isPrefixed();
    }

    /**
     * 获取 IP 版本（4=IPv4，6=IPv6，0=非法 IP）
     * @param ip 待判断的 IP 字符串
     * @return 4/6/0
     */
    public static int getIPVersion(String ip) {
        if (isEmpty(ip)) {
            return 0;
        }
        IPAddressString ipStr = new IPAddressString(ip);
        IPAddress address = ipStr.getAddress();
        if (address.isIPv4()) {
            return 4;
        } else if (address.isIPv6()) {
            return 6;
        }
        return 0;
    }

    // ===================== 网段归属判断 =====================
    /**
     * 判断 IP 是否在指定网段中（支持 IPv4/IPv6 网段，如 192.168.1.0/24、2001::/64）
     * @param ip 待判断的 IP（纯地址，如 192.168.1.1、2001::1）
     * @param network 网段字符串（如 192.168.1.0/24、2001::/64）
     * @return true=在网段中，false=不在/参数非法
     */
    public static boolean isIpInNetwork(String ip, String network) {
        if (isEmpty(ip) || isEmpty(network)) {
            return false;
        }
        IPAddressString ipStr = new IPAddressString(ip);
        IPAddressString networkStr = new IPAddressString(network);

        // 校验 IP 版本与网段版本一致
        IPAddress ipAddress = ipStr.getAddress();
        IPAddress networkAddress = networkStr.getAddress();

        if (!ipAddress.getIPVersion().equals(networkAddress.getIPVersion())) {
            return false;
        }

        // 判断 IP 是否包含在网段中
        return networkAddress.contains(ipAddress);
    }

    // ===================== 常规 IP 特性判断 =====================
    /**
     * 判断 IP 是否为回环地址（IPv4：127.0.0.0/8；IPv6：::1/128）
     * @param ip 待判断的 IP 字符串
     * @return true=回环地址，false=非回环/非法 IP
     */
    public static boolean isLoopbackIP(String ip) {
        if (isEmpty(ip)) {
            return false;
        }
        IPAddressString ipStr = new IPAddressString(ip);
        return ipStr.getAddress().isLoopback();
    }

    /**
     * 判断 IP 是否为私有（内网）IP
     * IPv4 私有段：10.0.0.0/8、172.16.0.0/12、192.168.0.0/16
     * IPv6 私有段：fc00::/7（唯一本地地址）
     * @param ip 待判断的 IP 字符串
     * @return true=私有 IP，false=公网/非法 IP
     */
    public static boolean isPrivateIP(String ip) {
        if (isEmpty(ip)) {
            return false;
        }
        IPAddressString ipStr = new IPAddressString(ip);
        IPAddress address = ipStr.getAddress();
        if (address.isIPv4()) {
            return ((IPv4Address) address).isPrivate();
        } else if (address.isIPv6()) {
            return ((IPv6Address) address).isLocal();
        }
        return false;
    }

    /**
     * 判断 IP 是否为公网 IP（非私有 + 非回环 + 合法 IP）
     * @param ip 待判断的 IP 字符串
     * @return true=公网 IP，false=私有/回环/非法 IP
     */
    public static boolean isPublicIP(String ip) {
        int version = getIPVersion(ip);
        if (version == 0) {
            return false;
        }
        return !isPrivateIP(ip) && !isLoopbackIP(ip);
    }

    // ===================== 格式转换/辅助方法 =====================
    /**
     * 获取 IP 的规范字符串（统一格式，如 192.168.001.001 → 192.168.1.1；2001:0db8::0001 → 2001:db8::1）
     * @param ip 原始 IP 字符串
     * @return 规范格式 IP，非法 IP 返回 null
     */
    public static String getNormalizedIP(String ip) {
        if (isEmpty(ip)) {
            return null;
        }
        IPAddressString ipStr = new IPAddressString(ip);
        return ipStr.getAddress().toNormalizedString();
    }

    /**
     * 获取 IP 网段的前缀长度（如 192.168.1.1/24 返回 24；纯 IP 返回 -1；非法返回 -2）
     * @param ipOrNetwork IP/网段字符串（如 192.168.1.1、192.168.1.0/24）
     * @return 前缀长度 / -1（纯 IP） / -2（非法）
     */
    public static int getNetworkPrefix(String ipOrNetwork) {
        if (isEmpty(ipOrNetwork)) {
            return -2;
        }
        IPAddressString ipStr = new IPAddressString(ipOrNetwork);
        IPAddress address = ipStr.getAddress();
        Integer prefix = address.getNetworkPrefixLength();
        return prefix == null ? -1 : prefix;
    }

    /**
     * 将 IP 转换为字节数组（IPv4 返回 4 字节，IPv6 返回 16 字节，非法返回 null）
     * @param ip 待转换的 IP 字符串
     * @return 字节数组 / null
     */
    public static byte[] getIPBytes(String ip) {
        if (isEmpty(ip)) {
            return null;
        }
        IPAddressString ipStr = new IPAddressString(ip);
        return ipStr.getAddress().getBytes();
    }

    // ===================== 私有辅助方法 =====================
    /**
     * 空值/空串判断
     */
    private static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    // ===================== 测试用例 =====================
    public static void main(String[] args) {
        // 1. 格式校验测试
        System.out.println("===== 格式校验 =====");
        System.out.println("192.168.1.1 是否合法 IPv4：" + isIPv4Valid("192.168.1.1")); // true
        System.out.println("2001:db8::1 是否合法 IPv6：" + isIPv6Valid("2001:db8::1")); // true
        System.out.println("192.168.1.256 是否合法 IPv4：" + isIPv4Valid("192.168.1.256")); // false
        System.out.println("::1 的 IP 版本：" + getIPVersion("::1")); // 6

        // 2. 网段归属判断
        System.out.println("\n===== 网段归属 =====");
        System.out.println("192.168.1.10 是否在 192.168.1.0/24 中：" + isIpInNetwork("192.168.1.10", "192.168.1.0/24")); // true
        System.out.println("2001:db8::10 是否在 2001:db8::/64 中：" + isIpInNetwork("2001:db8::10", "2001:db8::/64")); // true
        System.out.println("10.0.0.1 是否在 192.168.1.0/24 中：" + isIpInNetwork("10.0.0.1", "192.168.1.0/24")); // false

        // 3. 特性判断
        System.out.println("\n===== 特性判断 =====");
        System.out.println("127.0.0.1 是否回环地址：" + isLoopbackIP("127.0.0.1")); // true
        System.out.println("192.168.1.1 是否私有 IP：" + isPrivateIP("192.168.1.1")); // true
        System.out.println("8.8.8.8 是否公网 IP：" + isPublicIP("8.8.8.8")); // true

        // 4. 格式转换
        System.out.println("\n===== 格式转换 =====");
        System.out.println("192.168.001.001 的规范格式：" + getNormalizedIP("192.168.001.001")); // 192.168.1.1
        System.out.println("192.168.1.0/24 的前缀长度：" + getNetworkPrefix("192.168.1.0/24")); // 24
        System.out.println("192.168.1.1 的字节数组长度：" + getIPBytes("192.168.1.1").length); // 4
    }
}