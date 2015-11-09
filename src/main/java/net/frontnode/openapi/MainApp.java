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
 * @author jiankuan
 *         9/11/15.
 */
public class MainApp {

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
        List<FundSearchInfo> funds = ac.getFundsSearchInfo();
        for (FundSearchInfo fund: funds) {
            System.out.println(fund.fundCode);
            System.out.println(fund.fundName);
            System.out.println("============");
        }
        System.out.println(String.format("总共%d只基金", funds.size()));
    }

    private static void showUsage(Options options) {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("openapi-client",  options);
    }
}
