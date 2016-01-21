package net.frontnode.openapi.Event;

import com.alibaba.fastjson.JSONObject;

/**
 * Created by zhongwh on 16/1/21.
 */
public class DealFundInfoMessage  implements DealMessageInterface {

    public void dealMessage(JSONObject json){
        //todo deal event message
        System.out.println(json);
    }

}
