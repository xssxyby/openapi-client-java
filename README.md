盈米Openopi Java客户端样例代码
===================================

本文介绍如何使用Java访问盈米openapi。本样例基于Java 1.7, HttpClient 4.5版本编写。

在与盈米联系联系确认要接入openapi后，盈米会提供一组文件／key用于接入。

* **yingmi-openapi-root-ca.crt** － 盈米openapi的根证书，用于接口客户端验证盈米的服务器
* **openapi-[环境]-cert-[商户名].crt** － 客户端证书文件，用于盈米服务器验证客户端
* **openapi-[环境]-cert-[商户名].key** － 客户端证书文件的秘钥文件
* **api key** － 一个长字符串，用于唯一标志接入商户
* **api secret**  一个长字符串，用于产生请求签名

本项目提供了一段简单的代码来使用这些信息访问盈米openapi。

> * 注意，切勿直接将该代码用于生产，因为样例代码不考虑如何处理如断连、日志、异步访问等问题。请根据自身需要开发SDK。
> * 注意，盈米开放接口需要使用TLSv1.1或者TLSv1.2协议。而JDK6或者更老的版本不支持这两个协议。所以**必须使用JDK1.7以上版本开发SDK**。本例子假设使用JDK1.7。

# 1. 配置SSL证书

盈米openapi采用双向SSL校验，因此客户端需要：

* yingmi-openapi-root-ca.crt － 盈米openapi的根证书，用于接口客户端验证盈米的服务器
* openapi-[环境]-cert-[商户名].crt － 客户端证书文件，用于盈米服务器验证客户端
* openapi-[环境]-cert-[商户名].key － 客户端证书文件的秘钥文件

其中“环境”可能是`test`或者`prod`分别对应测试环境和生产环境。商户名是唯一的商户名称。（下文举例使用`test`，商户名用`foo`）.

在Java中，根据JSSE的规范，证书需要先导入到truststore／keystore文件，才能被Java识别和使用。

步骤为：

## 1.1 盈米openapi根证书（root ca）到truststore

```
keytool -import -keystore truststore.jks -file path/to/yingmi-openapi-root-ca.crt -alias yingmica
```
命令行会提示输入一个truststore的密码。请记下这个密码，下面配置会用到。

命令行还会提示“是否要信任该证书”，输入“Y”，并回车确认。

成功后，该命令会产生一个名称为"truststore.jks"的文件。

## 1.2 导入客户端证书到keystore

keystore不直接支持导入crt/key文件。所以首先先用openssl命令将证书转换为pkcs12格式的文件。p12文件可以同时包括证书和秘钥。

```
openssl pkcs12 -export -in openapi-test-cert-foo.crt -inkey openapi-test-cert-foo.key > foo.p12
```

此时会提示指定导出的p12文件密码。请记下这个密码，下一步会用到。

然后导入到keystore文件中

```
keytool -importkeystore -destkeystore keystore.jks -srckeystore foo.p12 -srcstoretype pkcs12
```
这步首先会要求你指定产生的keystore的密码。请记下这个密码，下面配置会用到。

然后会要求你输入上一步指定的p12文件的密码，请输入之。

**注意**，本样例代码假设你的p12密码与keystore密码相同。

# 2. 使用apiKey和apiSecret

盈米会提供商户一组apiKey和apiSecret用于产生请求签名。该算法详见//TODO。如下是一个Java产生签名的样例代码：

```java
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

# 3. 发送请求

发送请求时请确保

* 上面SSL配置产生的SSLConnectionSocketFactory在起作用
* 每个请求要添加必要的`key`, `ts`, `nonce`, `sigVer`等参数
* 每个请求计算正确的请求签名，并以`sig`参数的形式发给服务器端

盈米服务器会校验证书和请求签名。如果一切通过，会发送正确的结果。

# 使用本样例代码

假设

* keystore路径为"keystore.jks"
* truststore路径为"truststore.jks"
* 密码均为123456
* api key为abcdefg
* api secret为ABCDEFG

则使用以下命令调用盈米openapi的getFundsSearchInfo接口。

```
git clone git@github.com:yingmi/openapi-client-java.git
cd openapi-client-java
mvn clean package
java -jar target/openapi-client-1.0-SNAPSHOT-jar-with-dependencies.jar \
    -keystore keystore.jks \
    -kp 123456 \
    -truststore truststore.jks \
    -tp 123456 \
    -key abcefg \
    -secret ABCDEFG
```
该接口会返回一个包含多个基金基本信息的JSON文本。

使用

```
java -jar target/openapi-client-1.0-SNAPSHOT-jar-with-dependencies.jar -h
```

可以查看使用帮助.





