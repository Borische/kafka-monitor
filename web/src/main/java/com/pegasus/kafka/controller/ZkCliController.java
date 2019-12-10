package com.pegasus.kafka.controller;

import com.pegasus.kafka.common.response.Result;
import com.pegasus.kafka.common.utils.ZooKeeperKpiUtils;
import com.pegasus.kafka.entity.vo.ZooKeeperVo;
import com.pegasus.kafka.service.core.KafkaZkService;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static com.pegasus.kafka.controller.ZkCliController.PREFIX;

/**
 * The controller for providing the zookeeper's client.
 * <p>
 * *****************************************************************
 * Name               Action            Time          Description  *
 * Ning.Zhang       Initialize         11/7/2019      Initialize   *
 * *****************************************************************
 */
@Controller
@RequestMapping(PREFIX)
public class ZkCliController {
    public static final String PREFIX = "zkCli";
    private final KafkaZkService kafkaZkService;

    public ZkCliController(KafkaZkService kafkaZkService) {
        this.kafkaZkService = kafkaZkService;
    }

    @GetMapping("tolist")
    public String toList() {
        return String.format("%s/list", PREFIX);
    }

    @PostMapping("zkInfo")
    @ResponseBody
    public Result<String> zkInfo() {
        try {
            List<ZooKeeperVo> zooKeeperVoList = kafkaZkService.listZooKeeperCluster();
            if (zooKeeperVoList.size() > 0) {
                ZooKeeperVo zooKeeperInfo = zooKeeperVoList.get(0);
                ZooKeeperKpiUtils.ZooKeeperKpi zooKeeperKpi = ZooKeeperKpiUtils.listKpi(zooKeeperInfo.getHost(), Integer.parseInt(zooKeeperInfo.getPort()));
                if (!StringUtils.isEmpty(zooKeeperKpi.getZkNumAliveConnections())) {
                    List<String> result = zooKeeperVoList.stream().map(p -> String.format("%s:%s", p.getHost(), p.getPort())).collect(Collectors.toList());
                    return Result.ok(result.toString());
                }
            }
        } catch (Exception ignored) {
        }
        return Result.error();
    }

    @PostMapping("execute")
    @ResponseBody
    public Result<String> execute(@RequestParam(name = "command", required = true) String command,
                                  @RequestParam(name = "type", required = true) String type) throws Exception {
        try {
            return Result.ok(kafkaZkService.execute(command, type));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

}