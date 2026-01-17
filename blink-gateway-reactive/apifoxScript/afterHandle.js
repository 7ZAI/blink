// apifox 后置脚本 解密 验签
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