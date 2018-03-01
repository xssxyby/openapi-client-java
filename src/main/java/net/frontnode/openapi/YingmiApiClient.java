package net.frontnode.openapi;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.frontnode.openapi.model.Account;
import net.frontnode.openapi.model.FundSearchInfo;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.*;
import java.security.cert.CertificateException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author jiankuan
 *         21/10/2015.
 */
@SuppressWarnings("unused")
public class YingmiApiClient {

    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    private String host;

    private String apiKey;

    private String apiSecret;

    private HttpClient httpClient;

    final private String keyStoreType;
    final private String keyStorePath;
    final private String keyStorePassword;

    OutputStream out = null;
    InputStream in = null;

//    private String trustStorePath;
//    private String trustStorePassword;

    private Logger logger = LoggerFactory.getLogger(YingmiApiClient.class);

    private ObjectMapper objectMapper = new ObjectMapper();

    // a handy simplified version
    public YingmiApiClient(String host, String apiKey, String apiSecret) {
        this(
                host,
                apiKey,
                apiSecret,
                System.getProperty("javax.net.ssl.keyStoreType", "jks"),
                System.getProperty("javax.net.ssl.keyStore"),
                System.getProperty("javax.net.ssl.keyStorePassword")
        );
    }

    public YingmiApiClient(String host, String apiKey, String apiSecret, String keyStoreType, String keyStorePath, String keyStorePassword) {
        this.host = host;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.keyStoreType = keyStoreType;
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;

        try {

            // load client certificate
            KeyStore ks = KeyStore.getInstance(this.keyStoreType);
            ks.load(new FileInputStream(new File(this.keyStorePath)), this.keyStorePassword.toCharArray());

            // Not calling loadTrustStore() in order to use the settings in $JAVA_OPTS implicitly
            // -Djavax.net.ssl.trustStore=/path/to/app-trust-store
            // -Djavax.net.ssl.trustStorePassword=password

            SSLContext context = SSLContexts.custom()
                    .loadKeyMaterial(ks, this.keyStorePassword.toCharArray())
                    .build();

            SSLConnectionSocketFactory sf = new SSLConnectionSocketFactory(
                    context,
                    new String[]{"TLSv1.2"},
                    null,
                    SSLConnectionSocketFactory.getDefaultHostnameVerifier());

            httpClient = HttpClients.custom().setSSLSocketFactory(sf).build();

        } catch (
                KeyStoreException |
                        NoSuchAlgorithmException |
                        CertificateException |
                        IOException |
                        UnrecoverableKeyException |
                        KeyManagementException
                        e) {
            e.printStackTrace();
        }
    }

    public List<FundSearchInfo> getFundsSearchInfo() {
        String json = get("/product/getFundsSearchInfo", new HashMap<String, String>());
        JavaType javaType = objectMapper.getTypeFactory().constructCollectionType(List.class, FundSearchInfo.class);
        try {
            List<FundSearchInfo> result = objectMapper.readValue(json, javaType);
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getFundFee(String fundCode) {
        Map<String, String> params = new HashMap<>();
        params.put("fundCode", fundCode);
        return get("/product/getFundFee", new HashMap<String, String>());

    }

    void getOrderFile(String fileName) {
        downloadFile("/file/" + fileName, new HashMap<String, String>(), "下载本地地址");
    }

    public String createAccount(Account account) {
        return post("/account/createAccount", account.asParamsMap());
    }

    String get(String path, Map<String, String> params) {
        String basePath = "/v1";
        URIBuilder builder = new URIBuilder().setScheme("https")
                .setHost(host)
                .setPath(basePath + path);

        addRequiredParams("GET", path, params, apiKey, apiSecret);

        for (String key : params.keySet()) {
            builder.setParameter(key, params.get(key).toString());
        }

        try {

            URI uri = builder.build();

            HttpGet httpGet = new HttpGet(uri);
            HttpResponse resp = httpClient.execute(httpGet);
            if (resp.getStatusLine().getStatusCode() >= 300) {
                System.err.println("Something wrong: " + resp.getStatusLine().toString());
            }
            BufferedReader input = new BufferedReader(new InputStreamReader(resp.getEntity().getContent(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[1000];
            int count;
            while ((count = input.read(buf)) > 0) {
                sb.append(buf, 0, count);
            }
            return sb.toString();
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    String post(String path, Map<String, String> params) {
        String basePath = "/v1";
        URIBuilder builder = new URIBuilder().setScheme("https")
                .setHost(host)
                .setPath(basePath + path);
        // clear the params with empty value
        Map<String, String> trimmedParams = new HashMap<>();
        for (String key : params.keySet()) {
            if (params.get(key) != null) {
                trimmedParams.put(key, params.get(key));
            }
        }
        addRequiredParams("POST", path, trimmedParams, apiKey, apiSecret);

        try {
            URI uri = builder.build();
            RequestBuilder requestBuilder = RequestBuilder.post(uri);
            List<NameValuePair> kvs = new ArrayList<>();
            for (String key : trimmedParams.keySet()) {
                kvs.add(new BasicNameValuePair(key, trimmedParams.get(key)));
            }
            requestBuilder.setEntity(new UrlEncodedFormEntity(kvs, "UTF-8"));
            HttpUriRequest request = requestBuilder.build();
            HttpResponse resp = httpClient.execute(request);
            if (resp.getStatusLine().getStatusCode() >= 300) {
                System.err.println("Something wrong: " + resp.getStatusLine().toString());
            }
            BufferedReader input = new BufferedReader(new InputStreamReader(resp.getEntity().getContent(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[1000];
            int count;
            while ((count = input.read(buf)) > 0) {
                sb.append(buf, 0, count);
            }
            return sb.toString();
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }


    void downloadFile(String path, Map<String, String> params, String localFileName) {
        String basePath = "/v1";
        URIBuilder builder = new URIBuilder().setScheme("https")
                .setHost(host)
                .setPath(basePath + path);

        addRequiredParams("GET", path, params, apiKey, apiSecret);
        for (String key : params.keySet()) {
            builder.setParameter(key, params.get(key).toString());
        }

        try {
            URI uri = builder.build();

            HttpGet httpGet = new HttpGet(uri);
            HttpResponse resp = httpClient.execute(httpGet);
            if (resp.getStatusLine().getStatusCode() >= 300) {
                throw new RuntimeException("Something wrong: " + resp.getStatusLine().toString());
            }
            HttpEntity entity = resp.getEntity();
            in = entity.getContent();

            long length = entity.getContentLength();
            if (length <= 0) {
                throw new RuntimeException("下载文件不存在!");
            }

            File file = new File(localFileName);
            if (!file.exists()) {
                file.createNewFile();
            }

            out = new FileOutputStream(file);
            byte[] buffer = new byte[4096];
            int readLength = 0;
            while ((readLength = in.read(buffer)) > 0) {
                byte[] bytes = new byte[readLength];
                System.arraycopy(buffer, 0, bytes, 0, readLength);
                out.write(bytes);
            }
            out.flush();
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    void addRequiredParams(String method, String path, Map<String, String> params, String apiKey, String apiSecret) {
        params.put("key", apiKey);
        params.put("sigVer", "1");
        String ts = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS").format(LocalDateTime.now());
        params.put("ts", ts);
        params.put("nonce", RandomStringUtils.randomAlphanumeric(16));
        String sig = getSig(method, path, apiSecret, params);
        params.put("sig", sig);
    }

    String getSig(String method, String path, String apiSecret, Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        Set<String> keySet = new TreeSet<>(params.keySet());
        for (String key : keySet) {
            String value = params.get(key);
            if (value == null) {
                continue;
            }
            sb.append(key);
            sb.append("=");
            sb.append(params.get(key));
            sb.append("&");
        }
        sb.setLength(sb.length() - 1); // trim the last "&"
        String unifiedString = method.toUpperCase() + ":" + path + ":" + sb.toString();
        logger.debug("unified string: " + unifiedString);

        // calc hmac sha1
        try {
            SecretKeySpec secret = new SecretKeySpec(apiSecret.getBytes(), HMAC_SHA1_ALGORITHM);
            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(secret);
            byte[] hmac = mac.doFinal(unifiedString.getBytes()); // UTF8

            // base64 encode the hmac
            String sig = Base64.getEncoder().encodeToString(hmac);
            logger.debug("signature: " + sig);
            return sig;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }
}
