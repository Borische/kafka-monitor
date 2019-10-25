package com.pegasus.kafka.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonSerialize
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class KafkaTopicInfo implements Serializable {
    private String topicName;
    private Integer consumerStatus;
    private Long lag;
    private Long logSize;
    private Integer partitionNum;
    private String partitionIndex;
    private String createTime;
    private String modifyTime;
    private Long createTimeLong;
    private Long modifyTimeLong;
    private String error;

    public KafkaTopicInfo(String topicName, Integer consumerStatus) {
        this.topicName = topicName;
        this.consumerStatus = consumerStatus;
    }

    public KafkaTopicInfo() {

    }


}
