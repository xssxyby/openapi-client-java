package net.frontnode.openapi.service;

import net.frontnode.openapi.YingmiApiClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhongwh on 16/1/21.
 */
public class FundFeeService extends YingmiApiClient {


    public String begin(Object fundCode) {
        Map<String, String> params = new HashMap<>();
        params.put("fundCode", (String)fundCode);
        return get("/product/getFundFee", new HashMap<String, String>());

    }

    public FundFeeService(String apiKey, String apiSecret, String keyStorePath, String keyStorePassword,
                          String trustStorePath, String trustStorePassword) {
        super(apiKey, apiSecret, keyStorePath, keyStorePassword,
                trustStorePath, trustStorePassword);

    }

}
