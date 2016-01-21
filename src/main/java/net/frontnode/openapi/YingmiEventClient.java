package net.frontnode.openapi;

import com.alibaba.fastjson.JSONArray;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhongwh on 16/1/20.
 */
public class YingmiEventClient extends YingmiApiClient implements Runnable {

    public static Integer afterEventId = null;

    private static Integer timeout = 200;

    private static boolean isRunning;

    public static void setIsRunning(boolean run) {
       isRunning = run;
    }

    public YingmiEventClient(String apiKey, String apiSecret, String keyStorePath, String keyStorePassword,
                             String trustStorePath, String trustStorePassword) {
        super(apiKey, apiSecret, keyStorePath, keyStorePassword,
                trustStorePath, trustStorePassword);

        isRunning = true;
    }


    private String getEvent() {
        Map<String, String> params = new HashMap<>();
        if (timeout == null)
            timeout = 200;
        if (afterEventId != null)
            params.put("afterEventId", afterEventId.toString());
        params.put("timeout", timeout.toString());
        return get("/events/pollEvents", params);

    }

    public void run() {
        while (isRunning) {
            try {
                Thread.sleep(timeout);

                String returnMsg = getEvent();
                JSONArray jsonArray = JSONArray.parseArray("".equals(returnMsg) ? "{}" : returnMsg);

                for (int i = 0; i < jsonArray.size(); i++) {
                    System.out.println(jsonArray.get(i));
                    //todo 更新逻辑
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
