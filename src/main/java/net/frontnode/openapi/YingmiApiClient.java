package net.frontnode.openapi;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;

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
public class YingmiApiClient {

    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    private String host = "api-test.frontnode.net";

    private String apiKey;

    private String apiSecret;

    private HttpClient httpClient;

    private String keyStorePath;

    private String keyStorePassword;

    private String trustStorePath;

    private String trustStorePassword;

    public YingmiApiClient(String apiKey, String apiSecret, String keyStorePath, String keyStorePassword,
                           String trustStorePath, String trustStorePassword) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;
        this.trustStorePath = trustStorePath;
        this.trustStorePassword = trustStorePassword;

        // load key store
        try {

            SSLContext context = SSLContexts.custom()
                    .loadKeyMaterial(new File(this.keyStorePath),
                            this.keyStorePassword.toCharArray(),
                            this.keyStorePassword.toCharArray())
                    .loadTrustMaterial(new File(this.trustStorePath),
                            this.trustStorePassword.toCharArray())
                    .build();
            SSLConnectionSocketFactory sf = new SSLConnectionSocketFactory(
                    context,
                    new String[] {"TLSv1.2"},
                    null,
                    SSLConnectionSocketFactory.getDefaultHostnameVerifier());

            httpClient = HttpClients.custom().setSSLSocketFactory(sf).build();

        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | UnrecoverableKeyException | KeyManagementException e) {
            e.printStackTrace();
        }
    }

    public String getFundsSearchInfo() {
        try {
            return get("/product/getFundsSearchInfo", new HashMap<>());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    String get(String path, Map<String, String> params) throws URISyntaxException, IOException {
        String basePath = "/v1";
        URIBuilder builder = new URIBuilder().setScheme("https")
                .setHost(host)
                .setPath(basePath + path);

        addRequiredParams("GET", path, params, apiKey, apiSecret);

        for (String key: params.keySet()) {
            builder.setParameter(key, params.get(key).toString());
        }

        URI uri = builder.build();

        HttpGet httpGet = new HttpGet(uri);
        HttpResponse resp = httpClient.execute(httpGet);
        if (resp.getStatusLine().getStatusCode() >= 300) {
            throw new RuntimeException("Something wrong: " + resp.getStatusLine().toString());
        }
        BufferedReader input = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[1000];
        int count;
        while ((count= input.read(buf)) > 0) {
            sb.append(buf, 0, count);
        }
        return sb.toString();
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
            SecretKeySpec secret = new SecretKeySpec(apiSecret.getBytes(), HMAC_SHA1_ALGORITHM);
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

    public static void main(String[] args) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {

        Options options = new Options();
        options.addOption("keystore", true, "path of key store");
        options.addOption("kp", true, "key store password");
        options.addOption("truststore", true, "path of trust store");
        options.addOption("tp", true, "trust store password");
        options.addOption("key", true, "api key");
        options.addOption("secret", true, "api secret");
        options.addOption("h", "help", false, "show usage");

        CommandLineParser commandLineParser = new DefaultParser();
        CommandLine commandLine = null;
        try {
            commandLine = commandLineParser.parse(options, args);
        } catch (ParseException e) {
            showUsage(options);
            System.exit(-1);
        }

        if (commandLine.hasOption("h")) {
            showUsage(options);
            System.exit(0);
        }

        Map<String, String> params = new HashMap<>();
        String[] paramNames = new String[]{"keystore", "kp", "truststore", "tp", "key", "secret"};
        for (String paramName: paramNames) {
            if (commandLine.hasOption(paramName)) {
                params.put(paramName, commandLine.getOptionValue(paramName));
            } else {
                System.err.println("缺少必要参数" + paramName);
                showUsage(options);
                System.exit(-1);
            }
        }

        YingmiApiClient ac = new YingmiApiClient(
                params.get("key"),
                params.get("secret"),
                params.get("keystore"),
                params.get("kp"),
                params.get("truststore"),
                params.get("tp"));

        // invoke the api
        String fundsSearchInfo = ac.getFundsSearchInfo();
        System.out.println(fundsSearchInfo);
    }

    private static void showUsage(Options options) {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("openapi-client",  options);
    }
}
