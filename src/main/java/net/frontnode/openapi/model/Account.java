package net.frontnode.openapi.model;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jiankuan
 *         29/10/15.
 */
public class Account {
    public String accountName;

    public String identityType;

    public String identityNo;

    public String phone;

    public String paymentType;

    public String paymentNo;

    public String brokerUserId;

    public String password;

    public String riskGrade;

    public String email;

    public Map<String, String> asParamsMap() {
        Map<String, String> result = new HashMap<>();
        result.put("accountName", accountName);
        result.put("identityType", identityType);
        result.put("identityNo", identityNo);
        result.put("phone", phone);
        result.put("paymentType", paymentType);
        result.put("paymentNo", paymentNo);
        result.put("brokerUserId", brokerUserId);
        result.put("password", password);
        result.put("riskGrade", riskGrade);
        result.put("email", email);
        return result;
    }
}
