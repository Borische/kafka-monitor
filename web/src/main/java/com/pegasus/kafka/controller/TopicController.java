package com.pegasus.kafka.controller;

import com.pegasus.kafka.common.constant.Constants;
import com.pegasus.kafka.common.response.Result;
import com.pegasus.kafka.entity.vo.KafkaTopicPartitionVo;
import com.pegasus.kafka.entity.vo.KafkaTopicVo;
import com.pegasus.kafka.entity.vo.MBeanVo;
import com.pegasus.kafka.service.kafka.KafkaBrokerService;
import com.pegasus.kafka.service.kafka.KafkaTopicService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.pegasus.kafka.controller.TopicController.PREFIX;

/**
 * The controller for providing the ability of creating, modifying and deleting the topics.
 * <p>
 * *****************************************************************
 * Name               Action            Time          Description  *
 * Ning.Zhang       Initialize         11/7/2019      Initialize   *
 * *****************************************************************
 */
@Controller
@RequestMapping(PREFIX)
public class TopicController {
    public static final String PREFIX = "topic";
    private final KafkaTopicService kafkaTopicService;
    private final KafkaBrokerService kafkaBrokerService;


    public TopicController(KafkaTopicService kafkaTopicService, KafkaBrokerService kafkaBrokerService) {
        this.kafkaTopicService = kafkaTopicService;
        this.kafkaBrokerService = kafkaBrokerService;
    }

    @GetMapping("tolist")
    public String toList() {
        return String.format("%s/list", PREFIX);
    }

    @GetMapping("toadd")
    public String toAdd(Model model) {
        int brokerSize = 1;
        try {
            brokerSize = kafkaBrokerService.listAllBrokers().size();
        } catch (Exception ignored) {
        }
        model.addAttribute("brokerSize", brokerSize);
        return String.format("%s/add", PREFIX);
    }

    @GetMapping("toedit")
    public String toAdd(Model model, @RequestParam(name = "topicName", required = true) String topicName) throws Exception {
        topicName = topicName.trim();
        List<KafkaTopicVo> topicInfoList = kafkaTopicService.listTopics(topicName, KafkaTopicService.SearchType.EQUALS, false, true, false, false, false, false);
        if (topicInfoList != null && topicInfoList.size() > 0) {
            KafkaTopicVo topicVo = topicInfoList.get(0);
            List<KafkaTopicPartitionVo> topicDetails = kafkaTopicService.listTopicDetails(topicName);
            for (KafkaTopicPartitionVo topicDetail : topicDetails) {
                if (topicDetail.getReplicas() != null) {
                    model.addAttribute("replicasNum", topicDetail.getReplicas().size());
                    break;
                }
            }
            model.addAttribute("topicName", topicName);
            model.addAttribute("partitionNum", topicVo.getPartitionNum());
        }
        return String.format("%s/edit", PREFIX);
    }

    @GetMapping("todetail")
    public String toDetail(Model model, @RequestParam(name = "topicName", required = true) String topicName) {
        topicName = topicName.trim();
        model.addAttribute("topicName", topicName.trim());
        return String.format("%s/detail", PREFIX);
    }

    @GetMapping("tosendmsg")
    public String toSendMsg(Model model,
                            @RequestParam(name = "topicName", required = true) String topicName) {
        model.addAttribute("topicName", topicName.trim());
        return String.format("%s/sendmsg", PREFIX);
    }

    @PostMapping("list")
    @ResponseBody
    public Result<List<KafkaTopicVo>> list(@RequestParam(value = "topicName", required = false) String topicName,
                                           @RequestParam(value = "page", required = true) Integer pageNum,
                                           @RequestParam(value = "limit", required = true) Integer pageSize) throws Exception {
        if (!StringUtils.isEmpty(topicName)) {
            topicName = topicName.trim();
        }
        pageNum = Math.min(pageNum, Constants.MAX_PAGE_NUM);
        List<KafkaTopicVo> topicInfoList = kafkaTopicService.listTopics(topicName, KafkaTopicService.SearchType.LIKE, true, true, true, true, true, false);
        return Result.ok(topicInfoList.stream().skip(pageSize * (pageNum - 1))
                .limit(pageSize).sorted(Comparator.comparing(KafkaTopicVo::getTopicName)).collect(Collectors.toList()), topicInfoList.size());
    }

    @PostMapping("sendmsg")
    @ResponseBody
    public Result<?> sendMsg(@RequestParam(value = "topicName", required = true) String topicName,
                             @RequestParam(value = "content", required = true) String content) throws Exception {
        kafkaTopicService.sendMessage(topicName, content);
        return Result.ok();
    }

    @PostMapping("listTopicSize")
    @ResponseBody
    public Result<String> listTopicSize(@RequestParam(name = "topicName", required = true) String topicName) throws Exception {
        return Result.ok(kafkaTopicService.listTopicSize(topicName.trim()));
    }

    @PostMapping("listTopicMBean")
    @ResponseBody
    public Result<List<MBeanVo>> listTopicMBean(@RequestParam(name = "topicName", required = true) String topicName) throws Exception {
        return Result.ok(kafkaTopicService.listTopicMBean(topicName.trim()));
    }


    @PostMapping("listTopicDetails")
    @ResponseBody
    public Result<List<KafkaTopicPartitionVo>> listTopicDetails(@RequestParam(name = "topicName", required = true) String topicName) throws Exception {
        return Result.ok(kafkaTopicService.listTopicDetails(topicName.trim()));
    }

    @PostMapping("add")
    @ResponseBody
    public Result<?> add(@RequestParam(name = "topicName", required = true) String topicName,
                         @RequestParam(name = "partitionNumber", required = true) Integer partitionNumber,
                         @RequestParam(name = "replicationNumber", required = true) Integer replicationNumber) throws Exception {
        kafkaTopicService.add(topicName.trim(), partitionNumber, replicationNumber);
        return Result.ok();
    }

    @PostMapping("edit")
    @ResponseBody
    public Result<?> edit(@RequestParam(name = "topicName", required = true) String topicName,
                          @RequestParam(name = "partitionNumber", required = true) Integer partitionNumber) throws Exception {
        kafkaTopicService.edit(topicName.trim(), partitionNumber);
        return Result.ok();
    }

    @PostMapping("del")
    @ResponseBody
    public Result<?> del(@RequestParam(value = "topicName", required = true) String topicName) throws Exception {
        kafkaTopicService.delete(topicName.trim());
        return Result.ok();
    }
}
