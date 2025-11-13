
## 功能 响应式网关
 ### 动态路由
 ### 认证与权限校验
 ### 多渠道对接
 ### 混合加解密 加签验签
 ### 统一报文组装
 ### 动态调整参数配置
 ### 性能参数监控

gateway 路由刷新事件执行流程
```java
    // 完整的路由加载流程
    CachingRouteLocator.refresh()
    ↓
    CompositeRouteLocator.getRoutes()
    ↓
    RouteDefinitionRouteLocator.getRoutes()
    ↓ 从各个 RouteDefinitionLocator 获取路由定义
    List<RouteDefinition>
    ↓ 转换为 Route 对象
    List<Route>
    ↓ 缓存起来供路由匹配使用
    Cached Routes
```


关于路由的order 配置属性相关知识
order 属性是 Spring Cloud Gateway 中控制路由匹配顺序的关键机制：

✅ 数值越小，优先级越高

✅ 决定了请求被哪个路由处理的顺序

✅ 对于构建复杂路由策略至关重要

✅ 合理的设置可以优化性能和确保正确的请求转发

应用场景
```yaml

#1. 精确路径优先于通配路径
routes:
  - id: specific_route
    uri: lb://specific-service
    predicates:
      - Path=/api/v1/orders/{id}
    order: 1  # 高优先级：精确匹配

  - id: general_route
    uri: lb://general-service
    predicates:
      - Path=/api/v1/orders/**
    order: 2  # 低优先级：通配匹配
#2. 特定条件的路由优先
routes:
  - id: header_based_route
    uri: lb://premium-service
    predicates:
      - Path=/api/**
      - Header=X-Premium, true
    order: -1  # 最高优先级：有特定Header的请求
    
  - id: normal_route
    uri: lb://normal-service
    predicates:
      - Path=/api/**
    order: 0   # 普通优先级：普通请求
 #3. 版本控制路由
routes:
  - id: v2_route
    uri: lb://v2-service
    predicates:
      - Path=/api/v2/**
    order: 1  # 新版本优先

  - id: v1_route
    uri: lb://v1-service
    predicates:
      - Path=/api/v1/**
    order: 2  # 旧版本次之

  - id: legacy_route
    uri: lb://legacy-service
    predicates:
      - Path=/api/**
    order: 3  # 无版本标识的兜底
```

一个 `StreamMessageListenerContainer` 实例**可以绑定多个 Stream**。你可以在同一个容器实例上多次调用 `receive` 或 `receiveAutoAck` 方法，来监听不同的 Redis Stream。

下面这个表格汇总了两种绑定方式的主要区别：

| 特性维度         | 单Stream绑定                              | 多Stream绑定                                                                 |
| ---------------- | ----------------------------------------- | ---------------------------------------------------------------------------- |
| **绑定数量**     | 一个容器仅监听一个Stream                  | 一个容器可监听多个Stream                                             |
| **资源利用**     | 相对独立，可能资源利用率不高              | **共享线程池和配置**，资源利用更高效                                   |
| **代码结构**     | 简单直接                                  | 集中管理，结构更清晰                                                           |
| **配置隔离性**   | 高，各容器配置独立                        | 中，所有监听共享容器级配置（如序列化器、错误处理器）                     |
| **管理复杂度**   | 容器实例多时管理成本高                    | 单一容器实例，管理更方便                                                       |

### 🔧 如何绑定多个Stream

在同一个 `StreamMessageListenerContainer` 实例中，你可以通过多次调用 `receiveAutoAck` 或 `receive` 方法，为每个目标Stream创建相应的监听。

以下是一个代码示例，展示了如何在一个容器中绑定两个不同的Stream：

```java
@Bean
public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer(
        RedisConnectionFactory redisConnectionFactory) {
    
    // 创建容器配置选项
    StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options = 
        StreamMessageListenerContainerOptions.builder()
            .batchSize(10) // 一次性最多拉取多少条消息
            .pollTimeout(Duration.ofSeconds(2)) // 超时时间
            .build();
    
    // 创建监听容器
    StreamMessageListenerContainer<String, MapRecord<String, String, String>> container = 
        StreamMessageListenerContainer.create(redisConnectionFactory, options);
    
    // 为第一个Stream "stream1" 配置并注册监听
    StreamMessageListenerContainer.StreamReadRequest<String> stream1Request = 
        StreamMessageListenerContainer.StreamReadRequest
            .builder(StreamOffset.create("stream1", ReadOffset.lastConsumed()))
            .consumer(Consumer.from("group1", "consumer1"))
            .autoAcknowledge(true) // 自动确认
            .build();
    
    StreamListener<String, MapRecord<String, String, String>> listener1 = message -> {
        // 处理 stream1 的消息
        System.out.println("From stream1: " + message.getValue());
    };
    container.register(stream1Request, listener1);
    
    // 为第二个Stream "stream2" 配置并注册监听
    StreamMessageListenerContainer.StreamReadRequest<String> stream2Request = 
        StreamMessageListenerContainer.StreamReadRequest
            .builder(StreamOffset.create("stream2", ReadOffset.lastConsumed()))
            .consumer(Consumer.from("group2", "consumer2"))
            .autoAcknowledge(false) // 手动确认
            .build();
    
    StreamListener<String, MapRecord<String, String, String>> listener2 = message -> {
        // 处理 stream2 的消息
        System.out.println("From stream2: " + message.getValue());
        // 手动确认消息
        message.acknowledge();
    };
    container.register(stream2Request, listener2);
    
    // 启动容器，开始监听所有注册的Stream
    container.start();
    
    return container;
}
```

### 💡 多Stream绑定的优势与注意事项

- **提升资源效率**：多Stream绑定**共享容器级别的配置**（如线程池、序列化器、错误处理器），减少了为每个Stream创建独立容器带来的资源开销。
- **集中管理**：通过单一容器实例管理多个Stream监听，简化了应用的启动和停止逻辑。
- **配置的全局性**：需要留意，在 `StreamMessageListenerContainerOptions` 中设置的 **`batchSize`、`pollTimeout` 等配置会应用于该容器监听的所有Stream**。
- **错误处理**：配置一个全局的 `errorHandler` 很重要，它能捕获所有监听器处理消息时可能抛出的未处理异常，避免因单个Stream的消息处理异常影响其他Stream的监听。
- **序列化一致性**：确保生产者与消费者使用**相同或兼容的序列化器**，以免消息解析失败。例如，若使用不同的`RedisTemplate`（如`RedisTemplate<String, Object>`与`StringRedisTemplate`），可能因序列化器不同（JDK序列化 vs String序列化）而引起乱码或消费失败。

### ⚖️ 选择绑定方式的考虑

- **优先考虑多Stream绑定**：在大多数需要监听多个Stream的场景下，使用单个容器绑定多个Stream是**更推荐的做法**，因为它更资源友好，架构也更清晰。
- **考虑单Stream绑定的情况**：只有当不同的Stream监听需要**完全独立、互不影响**的配置（例如，需要使用不同的序列化方式，或者对线程池有特殊隔离要求）时，才考虑为每个Stream创建独立的 `StreamMessageListenerContainer` 实例。

总而言之，**`StreamMessageListenerContainer` 的一个实例完全可以绑定多个 Stream**。通常来说，这样做是更高效和可维护的选择。

希望这些信息能帮助你更好地设计和实现你的消息监听模块。如果你在具体配置中遇到其他难题，欢迎随时提出。

测试脚本

apifox 前置脚本 包含 加签 加密功能

```javascript
// Apifox 前置脚本 - 使用 jsrsasign 进行 RSA 加密
// 从环境变量获取必要的密钥信息和加密开关
// 引入 CryptoJS 库
const CryptoJS = require('crypto-js');
const rsa = require('jsrsasign');
// Apifox 前置脚本 - 使用 jsrsasign 进行 RSA 加密，添加 x-blink-key 和 x-blink-iv 请求头
// 从环境变量获取必要的密钥信息和加密开关
const appKey = pm.variables.get("appKey");
const appSecret = pm.variables.get("appSecret");
const systemPublicKey = pm.variables.get("systemPublickey"); // 用于 RSA 加密的公钥
const channelPublicKey = pm.variables.get("channelPublickey");
const channelSecretKey = pm.variables.get("channelSecretkey");
const encryptionSwitch = pm.variables.get("encryptionSwitch") || "1"; // 默认关闭加密 "0"开启 "1"关闭

// 验证必要的环境变量是否已设置
if (!appKey || !appSecret) {
    console.error("错误: 请确保 appKey 和 appSecret 环境变量已设置");
    throw new Error("缺少必要的环境变量: appKey 或 appSecret");
}

// 生成当前时间戳（毫秒级，与 Java 的 System.currentTimeMillis() 一致）
const timestamp = Date.now().toString();

// 生成 UUID 作为 nonce
const nonce = generateUUID();

// 获取请求体
let requestBody = {};
try {
    if (pm.request.body && pm.request.body.raw) {
        const rawBody = pm.request.body.raw;
        // 如果请求体是 JSON 字符串，则解析
        if (rawBody.trim().startsWith('{')) {
            requestBody = JSON.parse(rawBody);
        }
    }
} catch (e) {
    console.warn("请求体不是有效的 JSON，将使用空对象");
}

// 处理加密和签名
let sign, encryptedRequestBody,aesKey,iv64;
if (encryptionSwitch === "0") {
    // 开启加密
    console.log("加密开关: 开启");
    try {
        // 生成 AES 密钥和 IV
        encryptRequestBody(requestBody);
        sign = generateSignatureWithEncryption(timestamp, nonce);

    } catch (error) {
        console.error("加密过程失败:", error.message);
        throw error;
    }
} else {
    // 关闭加密
    console.log("加密开关: 关闭");
    sign = generateSignatureWithoutEncryption(timestamp, nonce, requestBody);
}

// 添加基础请求头
pm.request.headers.upsert({
    key: "x-blink-timestamp",
    value: timestamp
});

pm.request.headers.upsert({
    key: "x-blink-nonce",
    value: nonce
});

pm.request.headers.upsert({
    key: "x-blink-sign",
    value: sign
});

pm.request.headers.upsert({
    key: "x-blink-appKey",
    value: appKey
});

pm.request.headers.upsert({
    key: "x-encryption-switch",
    value: encryptionSwitch
});



// 在控制台输出调试信息
console.log("生成的请求头参数:");
console.log(`x-blink-timestamp: ${timestamp}`);
console.log(`x-blink-nonce: ${nonce}`);
console.log(`x-blink-sign: ${sign}`);
console.log(`appKey: ${appKey}`);
console.log(`appSecret: ${appSecret}`);
console.log(`x-encryption-switch: ${encryptionSwitch}`);


/**
 * 生成 UUID v4
 * @returns {string} UUID 字符串
 */
function generateUUID() {
    try {
        // 使用兼容性更好的方法生成 UUID
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
            const r = Math.random() * 16 | 0;
            const v = c == 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
    } catch (error) {
        console.error("生成 UUID 失败:", error);
        // 备用方案：使用时间戳 + 随机数
        return timestamp + Math.random().toString(36).substring(2, 15);
    }
}

/**
 * 使用 AES-GCM + RSA 加密请求体
 * @param {object} requestBody - 原始请求体
 * @returns {object} 加密后的请求体
 */
function encryptRequestBody(requestBody) {
    try {
        console.log("开始 AES + RSA 加密流程");

        // 使用 AES 加密请求体
        const requestBodyStr = JSON.stringify(requestBody);
        const encryptedData = aesEncrypt(requestBodyStr);
        console.log("AES 加密完成");
        
        // 使用 RSA 加密 AES 密钥
        const encryptedAesKey = rsaEncrypt();
        
        pm.request.headers.upsert({
            key: "x-blink-key",
            value: encryptedAesKey
        });

       
        console.log("RSA 加密完成");

        // 返回加密后的数据结构
         return encryptedData;

    } catch (error) {
        console.error("加密请求体失败:", error);
        throw error;
    }
}


/**
 * AES-GCM 加密
 * @param {string} data - 要加密的数据
 * @returns {ArrayBuffer} 加密后的数据
 */
function aesEncrypt(data) {
    try {
        // 将数据转换为 WordArray
        // 随机生成 256位 (32字节) 的 AES 密钥
        const keyWordArray = CryptoJS.lib.WordArray.random(32);
        // 生成16字节IV
        const ivWordArray = CryptoJS.lib.WordArray.random(16);
        // 转换为Base64格式（推荐，更安全且无乱码）
        aesKey = keyWordArray.toString(CryptoJS.enc.Base64);
        iv64 = ivWordArray.toString(CryptoJS.enc.Base64);
        
        // AES 加密
        const encryptedTxt = CryptoJS.AES.encrypt(data, keyWordArray, {
            iv: ivWordArray,
            mode: CryptoJS.mode.CBC,
            padding: CryptoJS.pad.Pkcs7
        }).toString();

        console.log("原始aesKey:{}",aesKey);
        console.log("原始iv64:{}",iv64);
        // 使用 CryptoJS 进行 AES-GCM 加密
       
        pm.request.body.update(encryptedTxt);
        pm.request.headers.upsert({
            key: "x-blink-iv",
            value: iv64
        });
        
        return encryptedTxt;

    } catch (error) {
        console.error("AES-GCM 加密失败:", error);
        throw error;
    }
   
}

/**
 * 使用 jsrsasign 进行 RSA 加密
 * @returns {string} Base64 编码的加密数据
 */
function rsaEncrypt() {
    
    try {
        // 获取 RSA 公钥 (优先使用系统公钥，其次渠道公钥)
        const rsaPublicKey = convertBase64ToPem(systemPublicKey, 'PUBLIC');;
        if (!rsaPublicKey) {
            throw new Error("未找到 RSA 公钥，请设置 systemPublickey 或 channelPublickey 环境变量");
        }
        console.log("使用 jsrsasign 进行 RSA 加密");
        const pubKeyObj = rsa.KEYUTIL.getKey(rsaPublicKey);
        
        // 使用 OAEP with SHA-256 填充进行加密 
        // RSAOAEP 算法对应 OAEP with SHA-1 and MGF1 with SHA-1
        // 对于 OAEPWithSHA-256AndMGF1Padding，可能需要使用特定的参数配置
        const encrypted = rsa.KJUR.crypto.Cipher.encrypt(aesKey, pubKeyObj,'RSAOAEP256');
        // 将加密结果转换为 Base64
        const encryptedB64 = rsa.hextob64(encrypted);
        
        console.log("RSA 加密成功，加密数据", encryptedB64);
        return encryptedB64;

    } catch (error) {
        console.error("RSA 加密失败:", error);
        throw error;
    }
}

/**
 * 开启加密时的签名生成
 * @param {string} timestamp - 时间戳
 * @param {string} nonce - 随机数
 * @returns {string} 签名
 */
function generateSignatureWithEncryption(timestamp, nonce) {
    try {
        // 构建签名字符串 - 使用加密后的请求体
        const signString = buildSignStringWithEncryption(timestamp, nonce);

        console.log("加密模式 - 签名字符串:", signString);


        // 使用 CryptoJS 进行 HmacSHA256 签名
        const hash = CryptoJS.HmacSHA256(signString, appSecret); // 使用 HMAC-SHA256 算法加密
        const signature = CryptoJS.enc.Base64.stringify(hash); // 转换为 Base64 编码

        console.log("加密模式 - 生成的签名:", signature);
        return signature;

    } catch (error) {
        console.error("签名生成失败:", error);
        throw error;
    }
}

/**
 * 关闭加密时的签名生成
 * @param {string} timestamp - 时间戳
 * @param {string} nonce - 随机数
 * @param {object} requestBody - 原始请求体
 * @returns {string} 签名
 */
function generateSignatureWithoutEncryption(timestamp, nonce, requestBody) {
    try {
        // 构建签名字符串 - 将未加密的 JSON 值添加到签名参数中
        const signString = buildSignStringWithoutEncryption(timestamp, nonce, requestBody);

        console.log("非加密模式 - 签名字符串:", signString);

        // 使用 CryptoJS 进行 HmacSHA256 签名
        const hash = CryptoJS.HmacSHA256(signString, appSecret); // 使用 HMAC-SHA256 算法加密
        const signature = CryptoJS.enc.Base64.stringify(hash); // 转换为 Base64 编码

        console.log("非加密模式 - 生成的签名:", signature);
        return signature;

    } catch (error) {
        console.error("签名生成失败:", error);
        throw error;
    }
}

/**
 * 构建加密模式下的签名字符串
 * @param {string} timestamp - 时间戳
 * @param {string} nonce - 随机数
 * @returns {string} 用于签名的字符串
 */
function buildSignStringWithEncryption(timestamp, nonce) {
    const params = {
        appKey: appKey,
        timeStamp: timestamp,
        loginName: 'test1',
        nonce: nonce

    };

    // encryptedKey: encryptedRequestBody.encryptedKey,
    //     encryptedData: encryptedRequestBody.encryptedData

    // 按参数名排序并拼接
    const sortedKeys = Object.keys(params).sort();
    const signParts = sortedKeys.map(key => `${key}=${params[key]}`);
    const signString = signParts.join('&');

    return signString;
}

/**
 * 构建非加密模式下的签名字符串
 * @param {string} timestamp - 时间戳
 * @param {string} nonce - 随机数
 * @param {object} requestBody - 原始请求体
 * @returns {string} 用于签名的字符串
 */
function buildSignStringWithoutEncryption(timestamp, nonce, requestBody) {
    // 基础参数
    const params = {
        appKey: appKey,
        timeStamp: timestamp,
        loginName: 'test1',
        nonce: nonce
    };
    const requestBodyStr = JSON.stringify(requestBody);
    // 将请求体的键值对添加到参数中
    // Object.keys(requestBody).forEach(key => {
    //     params[key] = requestBody[key];
    // });
    //
    // // 添加其他可选参数
    // if (systemPublicKey) {
    //     params.systemPublicKey = systemPublicKey;
    // }
    // if (channelPublicKey) {
    //     params.channelPublicKey = channelPublicKey;
    // }
    //
    // 按参数名排序并拼接
    const sortedKeys = Object.keys(params).sort();
    const signParts = sortedKeys.map(key => `${key}=${params[key]}`);
    const signString = signParts.join('&');

    const result = requestBodyStr + '&' + signString;
    return result;
}

function stringToBase64(str) {
    // 先将字符串转为 UTF-8 字节，再转为 Base64
    const utf8Bytes = new TextEncoder().encode(str);
    const binaryString = String.fromCharCode(...utf8Bytes);
    return btoa(binaryString);
}

// Base64 → 字符串
function base64ToString(base64) {
    const binaryString = atob(base64);
    const bytes = new Uint8Array(binaryString.length);
    for (let i = 0; i < binaryString.length; i++) {
        bytes[i] = binaryString.charCodeAt(i);
    }
    return new TextDecoder('utf-8').decode(bytes);
}

function convertBase64ToPem(base64Key, keyType = 'PUBLIC') {
    // 移除所有空白字符和PEM头尾（如果存在）
    let cleanKey = base64Key
        .replace(/\s+/g, '')
        .replace(/-----BEGIN[\w\s]+KEY-----/g, '')
        .replace(/-----END[\w\s]+KEY-----/g, '');

    // 验证Base64格式
    if (!/^[A-Za-z0-9+/]*={0,2}$/.test(cleanKey)) {
        throw new Error('Invalid Base64 format');
    }

    // 构建PEM格式
    const header = `-----BEGIN ${keyType} KEY-----\n`;
    const footer = `\n-----END ${keyType} KEY-----`;

    // 每64字符换行
    const formattedKey = cleanKey.match(/.{1,64}/g).join('\n');

    return header + formattedKey + footer;
}
```


apifox 后置脚本 解密 验签
```javascript
// // 获取 JSON 格式的请求返回数据
// var jsonData = pm.response.json();
const CryptoJS = require('crypto-js');
const rsa = require('jsrsasign');

// 获取响应数据
const headers = pm.response.headers;
const encryptedBody = pm.response.text();

// 从环境变量获取配置
const appKey = pm.variables.get("appKey");
const appSecret = pm.variables.get("appSecret");
const systemPublicKey = pm.variables.get("systemPublickey");
const channelPublicKey = pm.variables.get("channelPublickey");
const channelSecretKey = pm.variables.get("channelSecretkey");
const encryptionSwitch = pm.variables.get("encryptionSwitch") || "1";

// 获取响应头
const signature = headers.get("x-blink-sign");
const timestamp = headers.get("x-blink-timestamp");
const nonce = headers.get("x-blink-nonce");
const encryptedKey = headers.get("x-blink-key");
const encryptedIV = headers.get("x-blink-iv");

console.log("=== 开始解密响应 ===");
console.log("signature:", signature);
console.log("timestamp:", timestamp);
console.log("nonce:", nonce);
console.log("encryptedKey:", encryptedKey ? encryptedKey.substring(0, 50) + "..." : "null");
console.log("encryptedIV:", encryptedIV ? encryptedIV.substring(0, 30) + "..." : "null");
console.log("encryptedBody length:", encryptedBody.length);
console.log("encryptionSwitch:", encryptionSwitch);

try {
    // 检查加密开关
    if (encryptionSwitch === "1") {
        console.log("🔓 加密开关关闭，直接返回原始响应");
        return;
    }

    // 验证必要参数
    if (!encryptedKey || !encryptedIV || !encryptedBody) {
        throw new Error("缺少必要的加密参数");
    }

    if (!channelSecretKey) {
        throw new Error("未配置 channelSecretKey");
    }

    // 1. RSA 解密 AES 密钥和 IV
    console.log("🔑 开始RSA解密AES密钥材料...");
    const { aesKey, iv } = decryptAESMaterialsWithRSA(encryptedKey, encryptedIV, channelSecretKey);
    console.log("✅ RSA解密成功");

    // 2. AES 解密响应体
    console.log("🔓 开始AES解密响应体...");
    const decryptedData = decryptBodyWithAES(encryptedBody, aesKey, iv);
    console.log("✅ AES解密成功");
    console.log("解密后数据长度:", decryptedData.length);

    // 3. 验证签名（可选）
    if (signature && timestamp && nonce && appSecret) {
        console.log("🔍 开始验证签名...");
        const isSignatureValid = verifySignature(decryptedData, signature, timestamp, nonce, appSecret);
        if (!isSignatureValid) {
            console.warn("⚠️ 签名验证失败，但继续处理数据");
        } else {
            console.log("✅ 签名验证成功");
        }
    } else {
        console.log("⏭️ 跳过签名验证（缺少必要参数）");
    }

    // 4. 解析 JSON 并设置环境变量
    let parsedData;
    try {
        parsedData = JSON.parse(decryptedData);
        console.log("✅ JSON解析成功 {}",parsedData);
      
        // 更新响应体以便查看
        // pm.response.json(parsedData);
        // 设置新的响应体
        pm.response.setBody(parsedData);
       
    } catch (jsonError) {
        console.log(jsonError);
        console.log("⚠️ 响应不是JSON格式，保存为文本");
        parsedData = decryptedData;
    }

    // 保存到环境变量
    // pm.environment.set("decrypted_response", decryptedData);
    // pm.environment.set("decrypted_json", JSON.stringify(parsedData, null, 2));
    // pm.environment.set("response_timestamp", timestamp);
    // pm.environment.set("response_nonce", nonce);
    // pm.environment.set("last_decryption_time", new Date().toISOString());

    console.log("🎉 解密完成！");
    console.log("解密结果:", typeof parsedData === "string" ? parsedData.substring(0, 200) + "..." : parsedData);

} catch (error) {
    console.error("❌ 解密失败:", error.message);
    console.error(error);
    
    // 保存错误信息
    pm.environment.set("decryption_error", error.message);
    pm.environment.set("decryption_stack", error.stack);
    
    // 如果解密失败，保持原始响应
    pm.environment.set("decrypted_response", encryptedBody);
}

/**
 * RSA 解密 AES 密钥材料
 */
function decryptAESMaterialsWithRSA(encryptedKeyBase64, encryptedIVBase64, privateKeyPem) {
    try {
        // 由于 Apifox 环境限制，我们使用 CryptoJS 结合一些技巧
        // 注意：完整的 RSA 解密需要 Node.js crypto 模块，在 Apifox 中可能受限
        // 这里提供一个基于 CryptoJS 的简化版本
        
        // console.log("RSA解密 - 加密密钥长度:", encryptedKeyBase64.length);
        // console.log("RSA解密 - 加密IV长度:", encryptedIVBase64.length);
        
        // 在实际环境中，这里应该使用真正的 RSA 解密
        // 由于环境限制，这里模拟解密过程
        
        // 方法1: 如果服务器使用预共享密钥，可以直接使用
        // 方法2: 使用 CryptoJS 的简化 RSA（如果支持）
        
        // 这里我们假设 channelSecretKey 实际上是 AES 密钥的 Base64
      
        let aesKeyBase64, ivBase64;

        privateKeyPem = convertBase64ToPem(privateKeyPem);

        // console.log("privateKeyPem:{}",privateKeyPem);
        
        if (privateKeyPem && privateKeyPem.includes("BEGIN")) {
            // 如果是 PEM 格式的密钥，尝试使用 jsrsasign（如果可用）
            try {
                if (typeof rsa !== 'undefined') {
                    // 使用 jsrsasign 进行 RSA 解密
                    const key = rsa.KEYUTIL.getKey(privateKeyPem);
                    const plaintextHex = rsa.b64tohex(encryptedKeyBase64);
                    // 解密 AES 密钥
                   const encryptedHex = rsa.KJUR.crypto.Cipher.decrypt(plaintextHex, key,"RSAOAEP256");
                   console.log(encryptedHex); 
                   aesKeyBase64 = encryptedHex;
                    
                 console.log("使用 jsrsasign RSA 解密");
                }
            } catch (rsaError) {
                console.warn("jsrsasign RSA 解密失败:", rsaError.message);
                
            }
        }
        
        console.log("AES密钥aesKeyBase64:", aesKeyBase64);
        console.log("IV长度:", encryptedIVBase64);
        
        return {
            aesKey: aesKeyBase64,
            iv: encryptedIVBase64
        };
        
    } catch (error) {
        console.error("RSA解密失败:", error);
        throw new Error("RSA解密失败: " + error.message);
    }
}

/**
 * AES 解密响应体
 */
function decryptBodyWithAES(encryptedBodyBase64, aesKeyBase64, ivBase64) {
    try {
        console.log("AES解密 - 输入数据:", encryptedBodyBase64);
        console.log("AES解密 - 密钥:", aesKeyBase64);
        console.log("AES解密 - IV:", ivBase64);
        
        // 解析 Base64 数据
        const encryptedData = CryptoJS.enc.Base64.parse(encryptedBodyBase64);
        const key = CryptoJS.enc.Base64.parse(aesKeyBase64);
        const iv = CryptoJS.enc.Base64.parse(ivBase64);
        
        // console.log("AES解密 - 加密数据字节数:", encryptedData.sigBytes);
        // console.log("AES解密 - 密钥字节数:", key.sigBytes);
        // console.log("AES解密 - IV字节数:", iv.sigBytes);
        
        // AES-CBC 解密
        const decrypted = CryptoJS.AES.decrypt(
            { ciphertext: encryptedData },
            key,
            { 
                iv: iv,
                mode: CryptoJS.mode.CBC,
                padding: CryptoJS.pad.Pkcs7
            }
        );

        // console.log("decrypted:",decrypted);

        
        // 转换为 UTF-8 字符串
        const decryptedText = decrypted.toString(CryptoJS.enc.Utf8);
        
        if (!decryptedText) {
            console.error("AES解密得到空结果");
            console.log("decrypted对象:", decrypted);
            throw new Error("AES解密后得到空数据");
        }
        
        return decryptedText;
        
    } catch (error) {
        console.error("AES解密失败:", error);
        throw new Error("AES解密失败: " + error.message);
    }
}

function convertBase64ToPem(base64Key, keyType = 'PRIVATE') {
    // 移除所有空白字符和PEM头尾（如果存在）
    let cleanKey = base64Key
        .replace(/\s+/g, '')
        .replace(/-----BEGIN[\w\s]+KEY-----/g, '')
        .replace(/-----END[\w\s]+KEY-----/g, '');

    // 验证Base64格式
    if (!/^[A-Za-z0-9+/]*={0,2}$/.test(cleanKey)) {
        throw new Error('Invalid Base64 format');
    }

    // 构建PEM格式
    const header = `-----BEGIN ${keyType} KEY-----\n`;
    const footer = `\n-----END ${keyType} KEY-----`;

    // 每64字符换行
    const formattedKey = cleanKey.match(/.{1,64}/g).join('\n');

    return header + formattedKey + footer;
}

/**
 * 验证 HMAC 签名
 */
function verifySignature(data, signature, timestamp, nonce, secret) {
    try {
        // 构建签名字符串（根据服务器端的规则）
        const dataToSign = timestamp + "\n" + nonce + "\n" + data;
        
        console.log("签名验证 - 数据长度:", data.length);
        console.log("签名验证 - 时间戳:", timestamp);
        console.log("签名验证 - Nonce:", nonce);
        console.log("签名验证 - 签名:", signature.substring(0, 50) + "...");
        
        // 计算 HMAC-SHA256
        const computedSignature = CryptoJS.HmacSHA256(dataToSign, secret);
        const computedSignatureBase64 = CryptoJS.enc.Base64.stringify(computedSignature);
        
        console.log("签名验证 - 计算签名:", computedSignatureBase64.substring(0, 50) + "...");
        
        // 安全比较签名
        const isValid = computedSignatureBase64 === signature;
        console.log("签名验证结果:", isValid);
        
        return isValid;
        
    } catch (error) {
        console.error("签名验证错误:", error);
        return false;
    }
}

/**
 * 辅助函数：Base64 解码
 */
function base64Decode(str) {
    try {
        const words = CryptoJS.enc.Base64.parse(str);
        return CryptoJS.enc.Utf8.stringify(words);
    } catch (error) {
        console.error("Base64解码失败:", error);
        return null;
    }
}

/**
 * 辅助函数：检查字符串是否为有效 Base64
 */
function isValidBase64(str) {
    try {
        return btoa(atob(str)) === str;
    } catch (error) {
        return false;
    }
}

console.log("=== 解密脚本执行完成 ===");


```