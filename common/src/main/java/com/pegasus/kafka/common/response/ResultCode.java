package com.pegasus.kafka.common.response;

public enum ResultCode {
    SUCCESS(0, true, "success"),
    ERROR(1, false, "error"),
    ZOOKEEPER_CONFIG_IS_NULL(2, false, "zookeeper的地址配置为空，请在application.yml或application.properties中通过kafka.monitor.zookeeper配置zookeeper的地址，多个地址用逗号分隔，例:192.168.182.128:2181,192.168.182.129:2181,192.168.182.130:2181"),
    KAFKA_NOT_RUNNING(3, false, "KAFKA可能没有启动，请先启动kafka");
    private int code;
    private String description;
    private Boolean success;

    ResultCode(int code, Boolean success, String description) {
        this.code = code;
        this.success = success;
        this.description = description;
    }

    public static ResultCode get(Integer code) {
        for (ResultCode item : ResultCode.values()) {
            if (item.getCode() == code) {
                return item;
            }
        }
        return null;
    }

    public static ResultCode get(String description) {
        for (ResultCode item : ResultCode.values()) {
            if (item.getDescription().equals(description)) {
                return item;
            }
        }
        return null;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getSuccess() {
        return this.success;
    }
}