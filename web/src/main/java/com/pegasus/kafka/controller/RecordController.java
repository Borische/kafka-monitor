package com.pegasus.kafka.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pegasus.kafka.common.constant.Constants;
import com.pegasus.kafka.common.response.Result;
import com.pegasus.kafka.common.utils.Common;
import com.pegasus.kafka.entity.vo.*;
import com.pegasus.kafka.service.dto.TopicRecordService;
import com.pegasus.kafka.service.kafka.KafkaConsumerService;
import com.pegasus.kafka.service.kafka.KafkaTopicService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.pegasus.kafka.controller.RecordController.PREFIX;

/**
 * The controller for providing the trace ability for topics' records.
 * <p>
 * *****************************************************************
 * Name               Action            Time          Description  *
 * Ning.Zhang       Initialize         11/7/2019      Initialize   *
 * *****************************************************************
 */
@Controller
@RequestMapping(PREFIX)
public class RecordController {
    public static final String PREFIX = "record";
    private final TopicRecordService topicRecordService;
    private final KafkaTopicService kafkaTopicService;
    private final KafkaConsumerService kafkaConsumerService;

    public RecordController(KafkaTopicService kafkaTopicService, TopicRecordService topicRecordService, KafkaConsumerService kafkaConsumerService) {
        this.kafkaTopicService = kafkaTopicService;
        this.topicRecordService = topicRecordService;
        this.kafkaConsumerService = kafkaConsumerService;
    }

    @GetMapping("tolist")
    public String toList(Model model) throws Exception {
        List<KafkaTopicVo> kafkaTopicVoList = kafkaTopicService.listTopics(false, false, false, true, false, false);
        model.addAttribute("topics", kafkaTopicVoList.stream().sorted(Comparator.comparing(KafkaTopicVo::getTopicName)).collect(Collectors.toList()));
        model.addAttribute("savingDays", Constants.SAVING_DAYS);
        return String.format("%s/list", PREFIX);
    }

    @GetMapping("tomsgdetail")
    public String toMsgDetail(Model model,
                              @RequestParam(value = "topicName", required = true) String topicName,
                              @RequestParam(value = "partitionId", required = true) Integer partitionId,
                              @RequestParam(value = "offset", required = true) Long offset,
                              @RequestParam(value = "key", required = true) String key,
                              @RequestParam(value = "createTime", required = true) Date createTime) {
        String recordValue = topicRecordService.findRecordValue(topicName, partitionId, offset);
        model.addAttribute("topicName", topicName);
        model.addAttribute("partitionId", partitionId);
        model.addAttribute("offset", offset);
        model.addAttribute("key", key);
        model.addAttribute("createTime", Common.format(createTime));
        model.addAttribute("value", recordValue);
        return String.format("%s/msgdetail", PREFIX);
    }

    @GetMapping("toconsumerdetail")
    public String toConsumerDetail(Model model,
                                   @RequestParam(name = "topicName", required = true, defaultValue = "") String topicName,
                                   @RequestParam(name = "partitionId", required = false) Integer partitionId,
                                   @RequestParam(name = "offset", required = false, defaultValue = "") Long offset) {
        model.addAttribute("topicName", topicName);
        model.addAttribute("partitionId", partitionId);
        model.addAttribute("offset", offset);
        return String.format("%s/consumerdetail", PREFIX);
    }

    @PostMapping("listTopicPartitions")
    @ResponseBody
    public Result<List<KafkaTopicPartitionVo>> listTopicPartitions(@RequestParam(name = "topicName", required = true) String topicName) throws Exception {
        return Result.ok(kafkaTopicService.listTopicDetails(topicName));
    }

    @PostMapping("list")
    @ResponseBody
    public Result<List<KafkaTopicRecordVo>> list(@RequestParam(name = "topicName", required = false, defaultValue = "") String topicName,
                                                 @RequestParam(name = "partitionId", required = false) Integer partitionId,
                                                 @RequestParam(name = "key", required = false, defaultValue = "") String key,
                                                 @RequestParam(name = "createTimeRange", required = false, defaultValue = "") String createTimeRange,
                                                 @RequestParam(value = "page", required = true) Integer pageNum,
                                                 @RequestParam(value = "limit", required = true) Integer pageSize) throws Exception {
        topicName = topicName.trim();
        key = key.trim();
        createTimeRange = createTimeRange.trim();
        pageNum = Math.min(pageNum, Constants.MAX_PAGE_NUM);
        if (StringUtils.isEmpty(topicName)) {
            return Result.ok();
        }

        IPage page = new Page(pageNum, pageSize);

        Common.TimeRange timeRange = Common.splitTime(createTimeRange);
        Date from = timeRange.getStart(), to = timeRange.getEnd();
        try {
            return Result.ok(kafkaTopicService.listMessages(page, topicName, partitionId, key, from, to), page.getTotal());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.ok();
        }
    }

    @PostMapping("resend")
    @ResponseBody
    public Result<?> resend(@RequestParam(name = "topicName", required = true, defaultValue = "") String topicName,
                            @RequestParam(name = "key", required = true, defaultValue = "") String key,
                            @RequestParam(name = "value", required = true, defaultValue = "") String value) throws Exception {
        topicName = topicName.trim();
        key = key.trim();
        value = value.trim();
        if (StringUtils.isEmpty(topicName)) {
            return Result.error("主题不能为空");
        }
        kafkaTopicService.sendMessage(topicName, key, value);
        return Result.ok();
    }

    @PostMapping("listTopicConsumers")
    @ResponseBody
    public Result<List<KafkaRecordConsumeVo>> listTopicConsumers(@RequestParam(name = "topicName", required = true, defaultValue = "") String topicName,
                                                                 @RequestParam(name = "partitionId", required = false) Integer partitionId,
                                                                 @RequestParam(name = "offset", required = false, defaultValue = "") Long offset) throws Exception {

        List<KafkaConsumerVo> allConsumers = kafkaConsumerService.listKafkaConsumers();
        List<KafkaConsumerVo> kafkaConsumerVoList = allConsumers.stream().filter(p -> p.getTopicNames().contains(topicName)).collect(Collectors.toList());

        List<KafkaRecordConsumeVo> result = new ArrayList<>(kafkaConsumerVoList.size());

        for (KafkaConsumerVo kafkaConsumerVo : kafkaConsumerVoList) {
            List<OffsetVo> offsetVoList = kafkaConsumerService.listOffsetVo(kafkaConsumerVo.getGroupId(), topicName);
            Optional<OffsetVo> first = offsetVoList.stream().filter(p -> p.getPartitionId().equals(partitionId)).findFirst();
            if (first.isPresent()) {
                OffsetVo offsetVo = first.get();
                KafkaRecordConsumeVo recordConsumeVo = new KafkaRecordConsumeVo();
                recordConsumeVo.setGroupId(kafkaConsumerVo.getGroupId());
                recordConsumeVo.setIsConsume((offsetVo.getOffset() >= offset) ? 1 : 0);
                result.add(recordConsumeVo);
            }
        }
        return Result.ok(result);
    }
}