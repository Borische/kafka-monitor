package com.pegasus.kafka.entity.dto;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pegasus.kafka.common.constant.Constants;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = Constants.DATABASE_NAME + "." + "`sys_log_size`")
public class SysLogSize extends BaseDto {
    @TableField(value = "`topic_name`")
    private String topicName;

    @TableField(value = "`log_size`")
    private Long logSize;
}