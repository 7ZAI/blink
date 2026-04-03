/**
 * 加密工具类
 * 使用 Web Crypto API 实现加密功能
 * RSA-OAEP-SHA-256 与后端 RSAUtils 一致
 */

/**
 * Base64 工具类
 * 处理标准 Base64 和 URL 安全 Base64 的转换
 */
export class Base64Util {
  /**
   * 标准Base64编码
   */
  static encode(buffer: ArrayBuffer): string {
    const bytes = new Uint8Array(buffer)
    let binary = ''
    for (let i = 0; i < bytes.byteLength; i++) {
      binary += String.fromCharCode(bytes[i]!)
    }
    return btoa(binary)
  }

  /**
   * 标准Base64解码
   */
  static decode(base64: string): ArrayBuffer {
    // 处理URL传输时可能的问题：将空格替换回+
    const normalized = base64.replace(/ /g, '+')
    const binary = atob(normalized)
    const bytes = new Uint8Array(binary.length)
    for (let i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i)
    }
    return bytes.buffer
  }

  /**
   * Uint8Array转Base64
   */
  static fromUint8Array(array: Uint8Array): string {
    let binary = ''
    for (let i = 0; i < array.length; i++) {
      binary += String.fromCharCode(array[i]!)
    }
    return btoa(binary)
  }

  /**
   * Base64转Uint8Array
   */
  static toUint8Array(base64: string): Uint8Array {
    // 处理URL传输时可能的问题：将空格替换回+
    const normalized = base64.replace(/ /g, '+')
    const binary = atob(normalized)
    const bytes = new Uint8Array(binary.length)
    for (let i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i)
    }
    return bytes
  }
}

/**
 * AES工具类
 * 使用 Web Crypto API 实现
 */
export class AESUtil {
  /**
   * 生成随机AES密钥（256位 = 32字节）
   */
  static async generateKey(): Promise<CryptoKey> {
    return await crypto.subtle.generateKey(
      { name: 'AES-CBC', length: 256 },
      true,
      ['encrypt', 'decrypt']
    )
  }

  /**
   * 生成随机IV（16字节）
   */
  static generateIV(): Uint8Array {
    return crypto.getRandomValues(new Uint8Array(16))
  }

  /**
   * AES-CBC加密
   */
  static async encrypt(plainText: string, key: CryptoKey, iv: Uint8Array): Promise<string> {
    const encoder = new TextEncoder()
    const data = encoder.encode(plainText)

    const encrypted = await crypto.subtle.encrypt(
      { name: 'AES-CBC', iv: iv as BufferSource },
      key,
      data as BufferSource
    )

    return Base64Util.encode(encrypted)
  }

  /**
   * AES-CBC解密
   */
  static async decrypt(cipherText: string, key: CryptoKey, iv: Uint8Array): Promise<string> {
    const data = Base64Util.decode(cipherText)

    const decrypted = await crypto.subtle.decrypt(
      { name: 'AES-CBC', iv: iv as BufferSource },
      key,
      data as BufferSource
    )

    const decoder = new TextDecoder()
    return decoder.decode(decrypted)
  }

  /**
   * 从Base64字符串导入AES密钥
   */
  static async importKeyFromBase64(keyBase64: string): Promise<CryptoKey> {
    const keyData = Base64Util.decode(keyBase64)
    return await crypto.subtle.importKey(
      'raw',
      keyData,
      { name: 'AES-CBC' },
      true,
      ['encrypt', 'decrypt']
    )
  }

  /**
   * 导出AES密钥为Base64字符串
   */
  static async exportKeyToBase64(key: CryptoKey): Promise<string> {
    const exported = await crypto.subtle.exportKey('raw', key)
    return Base64Util.encode(exported)
  }
}

/**
 * RSA工具类
 * 使用 Web Crypto API 实现 RSA-OAEP-SHA-256
 * 与后端 RSAUtils 一致
 */
export class RSAUtil {
  /**
   * RSA-OAEP-SHA-256 公钥加密
   */
  static async encrypt(plainText: string, publicKeyBase64: string): Promise<string> {
    const publicKey = await this.importPublicKey(publicKeyBase64)
    
    const encoder = new TextEncoder()
    const data = encoder.encode(plainText)
    
    const encrypted = await crypto.subtle.encrypt(
      {
        name: 'RSA-OAEP',
      } as any,
      publicKey,
      data
    )
    
    return Base64Util.encode(encrypted)
  }

  /**
   * RSA-OAEP-SHA-256 私钥解密
   */
  static async decrypt(cipherTextBase64: string, privateKeyBase64: string): Promise<string> {
    const privateKey = await this.importPrivateKey(privateKeyBase64)
    
    const data = Base64Util.decode(cipherTextBase64)
    
    const decrypted = await crypto.subtle.decrypt(
      {
        name: 'RSA-OAEP',
      } as any,
      privateKey,
      data as BufferSource
    )
    
    const decoder = new TextDecoder()
    return decoder.decode(decrypted)
  }

  /**
   * 导入公钥（SPKI格式）
   */
  private static async importPublicKey(publicKeyBase64: string): Promise<CryptoKey> {
    const keyData = Base64Util.decode(publicKeyBase64)
    return await crypto.subtle.importKey(
      'spki',
      keyData,
      {
        name: 'RSA-OAEP',
        hash: 'SHA-256',
      },
      true,
      ['encrypt']
    )
  }

  /**
   * 导入私钥（PKCS8格式）
   */
  private static async importPrivateKey(privateKeyBase64: string): Promise<CryptoKey> {
    const keyData = Base64Util.decode(privateKeyBase64)
    return await crypto.subtle.importKey(
      'pkcs8',
      keyData,
      {
        name: 'RSA-OAEP',
        hash: 'SHA-256',
      },
      true,
      ['decrypt']
    )
  }
}

/**
 * HMAC签名工具类
 * 使用 Web Crypto API 实现
 */
export class HMACUtil {
  /**
   * HMAC-SHA256签名
   */
  static async sign(
    data: string,
    secret: string,
    params: { timestamp: string; nonce: string; appKey: string }
  ): Promise<string> {
    const signStr = this.buildSignString(data, params)
    const encoder = new TextEncoder()
    const keyData = encoder.encode(secret)
    const messageData = encoder.encode(signStr)

    const key = await crypto.subtle.importKey(
      'raw',
      keyData,
      { name: 'HMAC', hash: 'SHA-256' },
      false,
      ['sign']
    )

    const signature = await crypto.subtle.sign('HMAC', key, messageData)
    return Base64Util.encode(signature)
  }

  /**
   * 验证签名
   */
  static async verify(
    data: string,
    secret: string,
    sign: string,
    params: { timestamp: string; nonce: string; appKey: string }
  ): Promise<boolean> {
    const expectedSign = await this.sign(data, secret, params)
    return expectedSign === sign
  }

  /**
   * 构建签名字符串
   * 格式: data + timestamp + nonce + appKey
   */
  private static buildSignString(
    data: string,
    params: { timestamp: string; nonce: string; appKey: string }
  ): string {
    return `${data}${params.timestamp}${params.nonce}${params.appKey}`
  }
}

/**
 * 生成UUID v4
 */
export function generateUUID(): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID()
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

/**
 * 生成时间戳（毫秒）
 */
export function generateTimestamp(): string {
  return Date.now().toString()
}

/**
 * 加密请求数据
 */
export async function encryptRequest(
  body: string,
  systemPublicKey: string
): Promise<{ encryptedBody: string; encryptedKey: string; iv: string }> {
  const aesKey = await AESUtil.generateKey()
  const iv = AESUtil.generateIV()

  const encryptedBody = await AESUtil.encrypt(body, aesKey, iv)
  const keyBase64 = await AESUtil.exportKeyToBase64(aesKey)
  const encryptedKey = await RSAUtil.encrypt(keyBase64, systemPublicKey)

  return {
    encryptedBody,
    encryptedKey,
    iv: Base64Util.fromUint8Array(iv),
  }
}

/**
 * 解密响应数据
 */
export async function decryptResponse(
  encryptedBody: string,
  encryptedKey: string,
  iv: string,
  channelPrivateKey: string
): Promise<string> {
  const keyBase64 = await RSAUtil.decrypt(encryptedKey, channelPrivateKey)
  const aesKey = await AESUtil.importKeyFromBase64(keyBase64)
  const ivArray = Base64Util.toUint8Array(iv)

  return await AESUtil.decrypt(encryptedBody, aesKey, ivArray)
}
