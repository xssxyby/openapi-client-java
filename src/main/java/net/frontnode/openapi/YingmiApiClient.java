package net.frontnode.openapi;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.frontnode.openapi.model.Account;
import net.frontnode.openapi.model.FundSearchInfo;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.*;
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
public abstract class YingmiApiClient {

    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    private String host = "api-test.frontnode.net";

    private String apiKey;

    private String apiSecret;

    private CloseableHttpClient httpClient;

    private String keyStorePath;

    private String keyStorePassword;

    private String trustStorePath;

    private String trustStorePassword;

    private Logger logger = LoggerFactory.getLogger(YingmiApiClient.class);


    private SSLConnectionSocketFactory sf;

    public YingmiApiClient(String apiKey, String apiSecret, String keyStorePath, String keyStorePassword,
                           String trustStorePath, String trustStorePassword) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;
        this.trustStorePath = trustStorePath;
        this.trustStorePassword = trustStorePassword;

        try {

            SSLContext context = SSLContexts.custom()
                    .loadKeyMaterial(new File(this.keyStorePath),
                            this.keyStorePassword.toCharArray(),
                            this.keyStorePassword.toCharArray())
                    .loadTrustMaterial(new File(this.trustStorePath),
                            this.trustStorePassword.toCharArray())
                    .build();
            sf = new SSLConnectionSocketFactory(
                    context,
                    new String[]{"TLSv1.2"},
                    null,
                    SSLConnectionSocketFactory.getDefaultHostnameVerifier());

        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | UnrecoverableKeyException | KeyManagementException e) {
            throw new RuntimeException(e.getMessage());

        }
    }


    private HttpClient createConnection() {
        httpClient = HttpClients.custom().setSSLSocketFactory(sf).build();
        return httpClient;
    }




    protected String get(String path, Map<String, String> params) {
        String basePath = "/v1";
        URIBuilder builder = new URIBuilder().setScheme("https")
                .setHost(host)
                .setPath(basePath + path);

        addRequiredParams("GET", path, params, apiKey, apiSecret);

        for (String key: params.keySet()) {
            builder.setParameter(key, params.get(key).toString());
        }

        try {

            URI uri = builder.build();

            HttpGet httpGet = new HttpGet(uri);
            HttpResponse resp = createConnection().execute(httpGet);
            if (resp.getStatusLine().getStatusCode() >= 300) {
                logger.error("Something wrong: " + resp.getStatusLine().toString());
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
        } finally {
            try {
                if (httpClient != null)
                    httpClient.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
        }

        }
    }

    protected String post(String path, Map<String, String> params) {
        String basePath = "/v1";
        URIBuilder builder = new URIBuilder().setScheme("https")
                .setHost(host)
                .setPath(basePath + path);
        // clear the params with empty value
        Map<String, String> trimmedParams = new HashMap<>();
        for (String key: params.keySet()) {
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
            HttpResponse resp = createConnection().execute(request);
            if (resp.getStatusLine().getStatusCode() >= 300) {
                logger.error("Something wrong: " + resp.getStatusLine().toString());
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
        } finally {
            try {
                if (httpClient != null)
                    httpClient.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
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
        for (String key: keySet) {
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



    public abstract String begin(Object objects);

}
