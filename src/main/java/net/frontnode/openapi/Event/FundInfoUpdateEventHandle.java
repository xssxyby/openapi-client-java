package net.frontnode.openapi.Event;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by zhongwh on 16/1/21.
 */
public class FundInfoUpdateEventHandle implements IEventHandle {

    private Logger logger = LoggerFactory.getLogger(FundInfoUpdateEventHandle.class);


    public void dealMessage(JSONObject json){
        //todo deal event message
        System.out.println(json);
    }

}
