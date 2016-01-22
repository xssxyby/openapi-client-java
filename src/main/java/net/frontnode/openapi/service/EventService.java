package net.frontnode.openapi.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import net.frontnode.openapi.Event.FundInfoUpdateEventHandle;
import net.frontnode.openapi.Event.IEventHandle;
import net.frontnode.openapi.YingmiApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhongwh on 16/1/20.
 */
public class EventService extends YingmiApiClient implements Runnable {

    private Logger logger = LoggerFactory.getLogger(EventService.class);


    public static Integer afterEventId = null;

    private static Integer timeout = 200;

    private volatile boolean isRunning;

    public void setIsRunning(boolean run) {
       isRunning = run;
    }

    public EventService(String apiKey, String apiSecret, String keyStorePath, String keyStorePassword,
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

    IEventHandle dealInterface;

    public void run() {

        while (isRunning) {
            try {
                Thread.sleep(timeout);

                String returnMsg = getEvent();
                JSONArray jsonArray = JSONArray.parseArray("".equals(returnMsg) ? "{}" : returnMsg);

                for (int i = 0; i < jsonArray.size(); i++) {

                    JSONObject jsonObject = (JSONObject) jsonArray.get(i);

                    logger.debug("接受信息内容：{}", jsonArray.get(i));

                    if ("FUND_INFO_UPDATE".equals(jsonObject.get("type")))
                        dealInterface = new FundInfoUpdateEventHandle();
                    //todo other event type

                    dealInterface.dealMessage((JSONObject) jsonObject.get("content"));

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public String begin(Object object) {

        Thread yingmiClientThread = new Thread(this);

        yingmiClientThread.start();

        logger.debug("开始监听Event....");

        try {
            while (true) {

                BufferedReader strin = new BufferedReader(new InputStreamReader(System.in));
                logger.debug("输入'X'结束监听：");
                String str = strin.readLine();

                if ("X".equals(str.toUpperCase())) {
                    setIsRunning(false);
                    break;
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return null;
    }

}
