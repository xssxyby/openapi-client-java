package net.frontnode.openapi.Event;

import com.alibaba.fastjson.JSONObject;

/**
 * Created by zhongwh on 16/1/21.
 */
public interface IEventHandle {

    void dealMessage(JSONObject json);
}
