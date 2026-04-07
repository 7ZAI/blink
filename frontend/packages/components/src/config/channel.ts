/**
 * 渠道配置 - Apple渠道
 * 用于网关认证和加密通信
 */

export const channelConfig = {
  // 渠道标识
  appKey: 'b8366e81a1d21ceb035d09a6c5251587e77c4309',
  
  // 签名密钥（用于HMAC签名，不传输）
  appSecret: 'KLrmMSMBAb3f1bd7euUs8ks6W_mZVmAponfE1WKbjxI',
  
  // 渠道名称
  channelName: 'Apple',
  
  // 系统公钥（用于加密请求中的AES密钥）
  systemPublicKey: `MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5eIUJpegMRMzWbekf1IUxL9F3RiBYYK+eL2e6+ncymwrz/3t2CVUi+jEpcgPdX8tbKyvUYcJFzul4G6e0DCz/c5U9VAmsxfu9A11GESIOsNlEZeuhEJmzcD5nmgRcZCHzyI/t6z/8vyNUdq45bjLc2Ew5lvgmQgwAiTE6Y170ZuCvKe6+gtO6kUbsxcxY4jF82BltFFjGojc9+XFFWyu1yfNM8480F6gwLod9IQnfX+uuG/jYdtpCdgrtNFzDcTnbKBXrZNVzv8j0v27LuR+JvrABJyOKD4MbMKnA/VqxGSOhmyEBwtNYRaPCoovYqEplK+fDPrEk72ytJDZh5NXuwIDAQAB`,
  
  // 渠道私钥（用于解密响应中的AES密钥）
  channelPrivateKey: `MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQC6/6U9F1Ews4C8f82TDR9lQRXzDukJmZsGaGRj7oolQeqSjr+ng9PIlhBGZAScmuHTsu722/rRWi0ZGTZsQpG7fC884JuV3xPlNZWylXBmNM/IyuKbv3YOKq9r22LTwZkJrpu/fDIok/U9QoGuxvqRdXS/vvpF9gNJ2RWMq5ZY9SyHIVvi53pXB94oGCzUHC7ZFAy+LOCNa8bN8XavRHLK5STmQvgoONGjmEVAXuT/ycPWOa8ry01DvBZ1hGrteRbLRdEojk0BaKuIWiGPxnFtKpotmW52Y/5paaRYAqoPRh8UQBwXYs5FXC9qQjpodew/KjbmZMeDblJyw0pu8aABAgMBAAECggEACnm7xfpXwASuQjZeoSLruVO2E7xSwwjp2FK3clRbRM8L1v322pQrTwypCL3ITFruKlqBwdknlIbFXqc87JT7blH8oMpsV4RRrFYeIWJMaPgDE9qEa06EdzKzn7UrNMr0SciQTQp7Z4PdCdDUoaKKnY9uOjehSZNd3SCIxoKY6jlpJjFBa594j43d8iPJnjzQK6I2hBknsz93tGxWnu2CUZaY/ec1hm1zggKV172uyidtiKKO5lexOmHtPt00zC9Z/6yMUtG86PeYZeY0w38u1ZF5aSOZ/TWVRERnZq9Er+kNZXMkWKeSwyUmPvIXRfsLgTZ+Rh7cu6/z+8JX0nDkCQKBgQDBVmayEC/WIW7trO372vTYUikXcWfcObVpxBbqse4CADZ4UKYD/jQfb5FPjbrdmOcxjkw+i8rnW+60HK8Ch3i3MG5LDcv7PMU7lenq3pNU4GB0m1Yv13GKFea7d5Ify5q7JXdDJq+4NGr3nL5OX7G2pO2H4x+zxd9bSCSbRIBbfwKBgQD3m0tDQnRo0kXbVFIj0N3QkaEgpl8iDNUSKBz+9HMg8dpUsL9BCAPpDFJHHnm8Ymg9w7bN9AHqYt0rQIlJbz3oky4U3I5d2hRxeEbXUrJeLsXZJs8LDrrevmgEaeY9noWZT3woxXkQw1zgTxh6F2MukKk36yg+GP7hhiDr6knEfwKBgC65EKxrmQPopIQvzHuXGKDGikOuFml0UNE0oE0rntdA8ej1pIlGf5YxQi61k7lF4GlreCWNZwWug+tnGyxDTPsIaz8cIHWg5BFYU2V9UKGsb+L+Pz/kkv3J+S7I4LlqXQRpM3849DJSCJ+6c4tBORNonDbAvsKVSTsoHiCf111FAoGAM9nVXPRhpEHEVzXFLbLjgHW+pDOy4FKHnBHCYLGYGmILMokAxAGFsBvgbvw2yfhhk+2ULnTWodqqLhCJr1MYR8fqcnvtZEHy3gAt9ZFfwNFSpTK3UL147IQ7DBGRIX48w+odOfyAab4/iMmHeqnDH1Ez3n3kI3zggyGvazfY0skCgYAFb2mBF+iQM2D5/OXzdT2s0FqRalKeGKM18XbiwnyAdoedOr8b2wc+GRO99cOj4b25nrNQqwDfh3BLNc8lc6pCxB8IAAMecomSf7+Fht/niWJcaJlhZijfWB5ePq0zY58mYzNr5GeRz6daSXLdqH8EJNgJPKf8gaQWQ2e4O+lSPg==`,
  
  // JWT Token密钥（用于验证token）
  tokenSecret: 'Sq8tvt3Fd2d9Emwg0hhD1tOJBMA/G7Cv6FEvNiAtrPk=',
  
  // 是否启用加密（根据数据库配置 encryption_switch = 0 表示启用加密）
  encryptionEnabled: false,
  
  // 网关地址
  gatewayUrl: 'http://localhost:8002',
}

/**
 * 请求头常量
 */
export const HEADER_CONSTANTS = {
  X_BLINK_APPKEY: 'x-blink-appKey',
  X_BLINK_TOKEN: 'x-blink-token',
  X_BLINK_TIMESTAMP: 'x-blink-timestamp',
  X_BLINK_NONCE: 'x-blink-nonce',
  X_BLINK_SIGN: 'x-blink-sign',
  X_BLINK_KEY: 'x-blink-key',
  X_BLINK_IV: 'x-blink-iv',
  X_BLINK_LOCALE: 'x-blink-locale',
  X_BLINK_SOURCE: 'x-blink-source',
  X_BLINK_CLIENTIP: 'x-blink-clientIp',
  X_BLINK_USER_ID: 'x-blink-usrId',
  X_BLINK_LOGIN_NAME: 'x-blink-loginName',
}

export type ChannelConfig = typeof channelConfig
