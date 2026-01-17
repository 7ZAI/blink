// Apifox 前置脚本  包含 加签 加密功能
// - 使用 jsrsasign 进行 RSA 加密
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