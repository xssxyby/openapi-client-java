package net.frontnode.openapi;

import net.frontnode.openapi.model.FundSearchInfo;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by zhongwh on 16/1/20.
 */

public class MainEventApp {

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

        YingmiEventClient ac = new YingmiEventClient(
                params.get("key"),
                params.get("secret"),
                params.get("keystore"),
                params.get("kp"),
                params.get("truststore"),
                params.get("tp"));

        // invoke the api
        ac.run();
        System.out.println("开始监听Event....");
    }

    private static void showUsage(Options options) {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("openapi-client",  options);
    }
}
