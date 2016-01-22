package net.frontnode.openapi.service;

import net.frontnode.openapi.YingmiApiClient;
import net.frontnode.openapi.model.Account;

/**
 * Created by zhongwh on 16/1/21.
 */
public class AccountService extends YingmiApiClient {

    public String begin(Object object) {
        Account account = (Account)object;
        return post("/account/createAccount", account.asParamsMap());
    }


    public AccountService(String apiKey, String apiSecret, String keyStorePath, String keyStorePassword,
                          String trustStorePath, String trustStorePassword) {
        super(apiKey, apiSecret, keyStorePath, keyStorePassword,
                trustStorePath, trustStorePassword);

    }


}
