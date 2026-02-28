package io.github.xiaotang.novartc.entity.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.xiaotang.novartc.enums.ExceptionEnum;
import io.github.xiaotang.novartc.util.JacksonUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Tang_J on 2025/3/2 2:59
 * @Description TODO
 **/
public class ResponseMsgBody {
    /**
     * 失败编码
     */
    public static final int FAIL_CODE = 404;
    /**
     * 成功编码
     */
    public static final int SUCCESS_CODE = 200;
    /**
     * 状态码
     */
    private int code;
    /**
     * 提示信息
     */
    private String msg;

    /**
     * 服务器要返回给浏览器的数据
     */
    private Map<String, Object> body = new HashMap<>();

    public static ResponseMsgBody success() {
        ResponseMsgBody rmb = new ResponseMsgBody();
        rmb.setCode(SUCCESS_CODE);
        rmb.setMsg("处理成功！");
        return rmb;
    }

    public static ResponseMsgBody fail() {
        ResponseMsgBody rmb = new ResponseMsgBody();
        rmb.setCode(FAIL_CODE);
        rmb.setMsg("处理失败！");
        return rmb;
    }

    public ResponseMsgBody addErr(int code, Object value) {
        this.getBody().put("errCode", code);
        return addErr(value);
    }

    public ResponseMsgBody addErr(Object value) {
        this.getBody().put("errMsg", value);
        return this;
    }

    public ResponseMsgBody addTokenErr() {
        this.getBody().put("tokenErr", FAIL_CODE);
        return this;
    }

    public ResponseMsgBody addMap(Map<String, Object> map) {
        this.getBody().putAll(map);
        return this;
    }

    public ResponseMsgBody add(String key, Object value) {
        this.getBody().put(key, value);
        return this;
    }

    public ResponseMsgBody addData(Object value) {
        this.getBody().put("data", value);
        return this;
    }

    public static ResponseMsgBody invalidRequestParam() {
        return ResponseMsgBody.fail().addErr(ExceptionEnum.INVALID_PARAM_ERROR.getValue(), ExceptionEnum.INVALID_PARAM_ERROR.getContent());
    }

    @JsonIgnore
    public Object getData() {
        return this.body.get("data");
    }

    public ResponseMsgBody addDataJson(Object dataObj) {
        try {
            String dataJson = JacksonUtil.toJson(dataObj);
            this.getBody().put("dataJson", dataJson);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    @JsonIgnore
    public String getDataJson() {
        return (String) this.getBody().get("dataJson");
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Map<String, Object> getBody() {
        return body;
    }
}
