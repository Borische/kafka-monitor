package com.pegasus.kafka.entity.dto;


import com.baomidou.mybatisplus.annotation.TableField;
import com.pegasus.kafka.common.utils.Common;
import com.pegasus.kafka.entity.vo.KafkaTopicRecordInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * The entity for dynamic table. Using for saving the topic's content.
 * One topic corresponds one table, the table's name is the name of topic.
 * <p>
 * *****************************************************************
 * Name               Action            Time          Description  *
 * Ning.Zhang       Initialize         11/7/2019      Initialize   *
 * *****************************************************************
 */
@EqualsAndHashCode(callSuper = false)
@Data
public class TopicRecord extends BaseDto {
    @TableField(value = "topic_name")
    private String topicName;

    @TableField(value = "partition_id")
    private Integer partitionId;

    @TableField(value = "offset")
    private Long offset;

    @TableField(value = "key")
    private String key;

    @TableField(value = "value")
    private String value;

    @TableField(value = "timestamp")
    private Date timestamp;

    public KafkaTopicRecordInfo toVo() {
        KafkaTopicRecordInfo result = new KafkaTopicRecordInfo();
        result.setTopicName(this.getTopicName());
        result.setPartitionId(this.getPartitionId().toString());
        result.setOffset(this.getOffset().toString());
        result.setKey(this.getKey());
        result.setCreateTime(Common.format(this.getTimestamp()));
        result.setValue(this.getValue());
        result.setTimestamp(this.getTimestamp().getTime());
        return result;
    }
}
