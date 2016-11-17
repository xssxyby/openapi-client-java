盈米Openopi Java客户端样例代码
===================================

本文介绍如何使用Java访问盈米openapi。本样例基于Java 1.7, HttpClient 4.5版本编写。

在与盈米联系联系确认要接入openapi后，盈米会提供一组文件／key用于接入。

* **openapi-[环境]-cert-[商户名].crt** － 客户端证书文件，用于盈米服务器验证客户端
* **openapi-[环境]-cert-[商户名].key** － 客户端证书文件的秘钥文件
* **openapi-[环境]-cert-[商户名].p12** － 客户端证书文件的p12格式文件
* **api key** － 一个长字符串，用于唯一标志接入商户
* **api secret**  一个长字符串，用于产生请求签名

本项目提供了一段简单的代码来使用这些信息访问盈米openapi。

> * 注意，切勿直接将该代码用于生产，因为样例代码不考虑如何处理如断连、日志、异步访问等问题。请根据自身需要开发SDK。
> * 注意，盈米开放接口需要使用TLSv1.1或者TLSv1.2协议。所以**建议使用JDK1.7以上版本开发SDK**。对于JDK6，其老版本只支持SSLv3.0等具有巨大安全风险的SSL协议，本系统不支持。如果必须使用JDK6，请升级到JDK6u121. 该JDK6版本支持了TLSv1.2。
> 本例子使用JDK1.7编写。

# 1. 配置SSL证书

盈米openapi采用双向SSL校验，因此客户端在建立TLS连接过程中需要使用客户端证书

* openapi-[环境]-cert-[商户号].crt － PEM格式的客户端证书文件，用于盈米服务器验证客户端 (Java 应用不使用 )
* openapi-[环境]-cert-[商户号].key － PEM格式的客户端证书文件的私钥文件 (Java 应用不使用)
* openapi-[环境]-cert-[商户号].p12 － PKCS12 格式的客户端证书文件(含私钥), 默认密码为 “123456”

其中“环境”可能是`test`或者`prod`分别对应测试环境和生产环境。商户名是唯一的商户名称。（下文举例使用`test`，商户名用`0000`）.

在JAVA环境下客户端证书需要配置为keystore才可以使用。keystore可以采用PKCS12格式（.p12)。为此我们需要将crt／key文件转换成PKCS12格式。

> 此步骤先安装opnessl命令，在Windows环境比较麻烦。为此，盈米直接在发布证书时会直接发一个p12文件，可以直接使用，并且忽略该步骤。

```bash
openssl pkcs12 -export -in openapi-test-cert-foo.crt -inkey openapi-test-cert-foo.key > foo.p12
```

此时会提示指定导出的p12文件密码。请记下这个密码，下一步会用到。盈米提供的p12文件的密码统一为123456。

**备注 － 关于根证书**

> 由于历史原因, 盈米 OpenAPI 开发环境 https://api-test.frontnode.net 使用了盈米自签发的服务器证书, 该自签发证书已经被废弃,与生产环境一样使用由公开的证书签发机构签发的服务器证书。新的证书在大多数操作系统和JRE中都被认可，所以不再需要配置盈米根证书。


# 2. 使用apiKey和apiSecret

盈米会提供商户一组apiKey和apiSecret用于产生请求签名。该算法详见[这里](https://github.com/yingmi/openapi-docs/blob/master/oepnapi公共参数和校验.md#请求签名产生方法)。如下是一个Java产生签名的样例代码：

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

* 客户端证书的路径为 "openapi-test-cert-0000.p12"
* 客户端证书的密码为 "123456"
* truststore路径为 "cacerts"
* truststore密码为 "changeit"
* api key为abcdefg
* api secret为ABCDEFG

则使用以下命令调用盈米openapi的getFundsSearchInfo接口。

```
git clone git@github.com:yingmi/openapi-client-java.git
cd openapi-client-java
mvn clean package
java \
	-Djavax.net.ssl.keyStoreType=pkcs12 \
	-Djavax.net.ssl.keyStore=openapi-test-cert-0000.p12 \
	-Djavax.net.ssl.keyStorePassword=123456 \
    -jar target/openapi-client-1.0-SNAPSHOT-jar-with-dependencies.jar \
    -host api-test.frontnode.net \
    -key abcefg \
    -secret ABCDEFG
```
该接口会连接到"https://api-test.frontnode.net"测试环境，返回一个包含多个基金基本信息的JSON文本。

如要连接到生产环境，则需要明确指出要连接的主机名，并且配合生产环境使用的证书、api key和api secret。

```
java \
	-Djavax.net.ssl.keyStoreType=pkcs12 \
	-Djavax.net.ssl.keyStore=openapi-prod-cert-0000.p12 \
	-Djavax.net.ssl.keyStorePassword=123456 \
	-jar target/openapi-client-1.0-SNAPSHOT-jar-with-dependencies.jar \
    -host api.yingmi.cn \
    -key abcefg \
    -secret ABCDEFG
```

使用

```
java -jar target/openapi-client-1.0-SNAPSHOT-jar-with-dependencies.jar -h
```

可以查看使用帮助.
