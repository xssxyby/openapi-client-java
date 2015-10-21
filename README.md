Yingmi Open Api Java Client Sample
===================

本文介绍如何使用Java访问yingmi openapi。

# 配置SSL证书

盈米openapi采用双向SSL校验，盈米会给每个接入商发放3个文件：

* yingmi-openapi-root-ca.crt － 盈米openapi的根证书，用于接口客户端验证盈米的服务器
* openapi-[环境]-cert-[商户名].crt － 客户端证书文件，用于盈米服务器验证客户端
* openapi-[环境]-cert-[商户名].key － 客户端证书文件的秘钥文件

其中“环境”可能是`test`或者`prod`分别对应测试环境和生产环境。商户名是唯一的商户名称。（下文举例环境使用`test`，商户名用`foo`）.

在Java中，根据JCE的规范，证书需要先配置truststore／keystore文件。总体的配置步骤是：
1. 导入根证书到truststore中；
2. 导入客户端证书到keystore中；
3. 启动程序，程序使用配置的truststore／keystore。

## 步骤1，导入根证书到truststore

```
keytool -import -keystore truststore -file path/to/yingmi-openapi-root-ca.crt -alias yingmica
```
命令行会提示输入一个truststore的密码。请记下这个密码，下面配置会用到。

成功后，该命令会产生一个名称为"truststore"的文件。你可以任意命名这个文件。

## 步骤2，导入客户端证书到keystore

keystore不直接支持crt格式的文件。所以首先先用openssl命令将证书转换为pkcs12格式的文件。

```
openssl pkcs12 -export -in openapi-test-cert-foo.crt -inkey openapi-test-cert-foo.key > foo.p12
```

此时会提示指定导出的p12文件密码。请记下这个密码，下一步会用到。

然后导入到keystore文件中

```
keytool -importkeystore -destkeystore keystore -srckeystore foo.p12 -srcstoretype pkcs12
```
这步首先会要求你指定产生的keystore的密码。请记下这个密码，下面配置会用到。

然后会要求你输入上一步指定的p12文件的密码，请输入之。

## 步骤3，配置你的程序

你已经得到的两个文件"truststore"和"keystore"。在启动你的Java程序前，配置如下System Properties:

```
-Djavax.net.ssl.keyStoreType=jks
-Djavax.net.ssl.keyStore=keystore
-Djavax.net.ssl.keyStorePassword=[keystore的密码]
-Djavax.net.ssl.trustStoreType=jks
-Djavax.net.ssl.trustStore=juaicai-truststore
-Djavax.net.ssl.trustStorePassword=[truststore的密码]
```

然后启动程序。Java默认的SSLSocketFactory实例就会加载这个这些配置。

此外可能有各种定制化的配置，例如合并truststore和keystore到一个文件；手工加载key文件，并创建SSLSocketFactory示例等。这里不再赘述。

# 配置apiKey和apiSecret

盈米会提供商户一组apiKey和apiSecret用于产生请求签名。给算法详见接口文档。如下是一个Java产生签名的样例代码：

```
String getSig(String method, String path, String apiSecret, Map<String, String> params) {
    StringBuilder sb = new StringBuilder();
    Set<String> keySet = new TreeSet<String>(params.keySet());
    for (String key: keySet) {
        sb.append(key);
        sb.append("=");
        sb.append(params.get(key));
        sb.append("&");
    }
    sb.setLength(sb.length() - 1); // trim the last "&"
    String unifiedString = method.toUpperCase() + ":" + path + ":" + sb.toString();

    // calc hmac sha1
    try {
        SecretKeySpec secret = new SecretKeySpec(apiSecret.getBytes(), "HmacSHA1");
        Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
        mac.init(secret);
        byte[] hmac = mac.doFinal(unifiedString.getBytes()); // UTF8

        // base64 encode the hmac
        String sig = Base64.getEncoder().encodeToString(hmac);
        return sig;
    } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
    } catch (InvalidKeyException e) {
        e.printStackTrace();
    }

    return null;
}
```

# 发送请求

发送请求时请确保

* 上面SSL配置产生的SSLSocketFactory在起作用
* 每个请求要添加必要的`key`, `ts`, `nonce`, `sigVer`等参数
* 每个请求计算正确的请求签名，并以`sig`参数的形式发给服务器端


盈米服务器会校验证书和请求签名。如果一切通过，会发送正确的结果。





