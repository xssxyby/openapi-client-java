package net.frontnode.openapi;

import com.alibaba.fastjson.JSONArray;

import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhongwh on 16/1/20.
 */
public class YingmiEventClient extends YingmiApiClient implements Runnable {

    public static Integer afterEventId = null;

    public YingmiEventClient(String apiKey, String apiSecret, String keyStorePath, String keyStorePassword,
                             String trustStorePath, String trustStorePassword) {
        super(apiKey, apiSecret, keyStorePath, keyStorePassword,
                trustStorePath, trustStorePassword);
    }


    private String getEvent(Integer timeout, Integer afterEventId) {
        Map<String, String> params = new HashMap<>();
        if (timeout == null)
            timeout = 200;
        if (afterEventId != null)
            params.put("afterEventId", afterEventId.toString());
        params.put("timeout", timeout.toString());
        return get("/events/pollEvents", params);

    }

    public void run() {
        int errorCount = 0;
        while (true) {
            if (errorCount >= 10) {
                try {
                    Thread.sleep(15000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
            try {

                String returnMsg = getEvent(200, afterEventId);
                JSONArray jsonArray = JSONArray.parseArray("".equals(returnMsg) ? "{}" : returnMsg);

                for (int i = 0; i < jsonArray.size(); i++) {
                    System.out.println(jsonArray.get(i));
                    //todo 更新逻辑
                }
                errorCount = 0;
            } catch (Exception e) {
                errorCount++;
                e.printStackTrace();
            }
        }
    }

}
