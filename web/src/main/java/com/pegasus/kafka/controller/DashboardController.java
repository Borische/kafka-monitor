package com.pegasus.kafka.controller;

import com.pegasus.kafka.common.annotation.TranRead;
import com.pegasus.kafka.common.ehcache.EhcacheService;
import com.pegasus.kafka.common.response.Result;
import com.pegasus.kafka.common.utils.Common;
import com.pegasus.kafka.entity.dto.SysLag;
import com.pegasus.kafka.entity.dto.SysLogSize;
import com.pegasus.kafka.entity.echarts.LineInfo;
import com.pegasus.kafka.service.dto.SysLagService;
import com.pegasus.kafka.service.dto.SysLogSizeService;
import com.pegasus.kafka.service.kafka.KafkaConsumerService;
import com.pegasus.kafka.service.kafka.KafkaTopicService;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.pegasus.kafka.controller.DashboardController.PREFIX;

/**
 * The controller for providing the ability of dashboard.
 * <p>
 * *****************************************************************
 * Name               Action            Time          Description  *
 * Ning.Zhang       Initialize         11/7/2019      Initialize   *
 * *****************************************************************
 */
@Controller
@RequestMapping(PREFIX)
public class DashboardController {
    public static final String PREFIX = "dashboard";
    private final KafkaConsumerService kafkaConsumerService;
    private final KafkaTopicService kafkaTopicService;
    private final SysLagService sysLagService;
    private final SysLogSizeService sysLogSizeService;
    private final EhcacheService ehcacheService;

    public DashboardController(KafkaConsumerService kafkaConsumerService, KafkaTopicService kafkaTopicService, SysLagService sysLagService, SysLogSizeService sysLogSizeService, EhcacheService ehcacheService) {
        this.kafkaConsumerService = kafkaConsumerService;
        this.kafkaTopicService = kafkaTopicService;
        this.sysLagService = sysLagService;
        this.sysLogSizeService = sysLogSizeService;
        this.ehcacheService = ehcacheService;
    }

    @GetMapping("index")
    public String index(Model model) throws Exception {
        model.addAttribute("consumers", kafkaConsumerService.listKafkaConsumers());
        model.addAttribute("topics", kafkaTopicService.listTopics(false, false, false, false, false));
        return String.format("%s/index", PREFIX);
    }

    @PostMapping("getTopicChart")
    @ResponseBody
    public Result<LineInfo> getTopicChart(@RequestParam(name = "topicName", required = true) String topicName,
                                          @RequestParam(name = "createTimeRange", required = true) String createTimeRange) throws ParseException {
        String key = String.format("DashboardController::getTopicChart:%s:%s", topicName, createTimeRange);
        LineInfo cache = ehcacheService.get(key);
        if (cache != null) {
            return Result.ok(cache);
        }

        topicName = topicName.trim();
        createTimeRange = createTimeRange.trim();
        if (StringUtils.isEmpty(createTimeRange)) {
            return Result.ok();
        }
        LineInfo result = new LineInfo();
        Common.TimeRange timeRange = Common.splitTime(createTimeRange);
        Date from = timeRange.getStart(), to = timeRange.getEnd();
        from = DateUtils.addMinutes(from, -1);
        List<SysLogSize> sysLogSizeList = sysLogSizeService.listByTopicName(topicName.equals("所有主题") ? null : topicName, from, to);

        List<String> topicNames = sysLogSizeList.stream().map(SysLogSize::getTopicName).distinct().collect(Collectors.toList());
        List<String> times = sysLogSizeList.stream().map(p -> Common.format(p.getCreateTime())).distinct().collect(Collectors.toList());
        if (times.size() > 0) {
            times.remove(0);
        }

        result.setTopicNames(topicNames);
        result.setTimes(times);

        List<LineInfo.Series> seriesList = new ArrayList<>(result.getTopicNames().size());
        for (String name : result.getTopicNames()) {
            LineInfo.Series series = new LineInfo.Series();
            series.setName(name);
            series.setType("line");
            series.setSmooth(true);
            seriesList.add(series);
        }
        result.setSeries(seriesList);

        for (LineInfo.Series series : result.getSeries()) {
            List<SysLogSize> topicSysLogSize = sysLogSizeList.stream().filter(p -> p.getTopicName().equals(series.getName())).collect(Collectors.toList());
            List<Double> data = new ArrayList<>(result.getTimes().size());
            for (String time : result.getTimes()) {
                Long preLogSize = null;
                Long curLogSize = null;
                Date preDate = null;
                Date curDate = null;
                for (int i = 0; i < topicSysLogSize.size(); i++) {
                    SysLogSize sysLogSize = topicSysLogSize.get(i);
                    if (sysLogSize.getTopicName().equals(series.getName()) && Common.format(sysLogSize.getCreateTime()).equals(time)) {
                        curLogSize = sysLogSize.getLogSize();
                        curDate = sysLogSize.getCreateTime();
                        int preIndex = i - 1;
                        if (preIndex >= 0 && preIndex < topicSysLogSize.size()) {
                            preLogSize = topicSysLogSize.get(preIndex).getLogSize();
                            preDate = topicSysLogSize.get(preIndex).getCreateTime();
                        }
                        break;
                    }
                }
                Long logSize = null;
                if (curLogSize != null) {
                    logSize = curLogSize - (preLogSize == null ? 0 : preLogSize);
                    if (logSize < 0) {
                        logSize = 0L;
                    }
                }
                Double seconds = 60.0D;
                if (curDate != null && preDate != null) {
                    seconds = (curDate.getTime() - preDate.getTime()) / 1000.0D;
                }

                if (logSize == null) {
                    data.add(null);
                } else {
                    data.add(Common.numberic(logSize / seconds));
                }
            }
            series.setData(data);
        }
        ehcacheService.set(key, result);
        return Result.ok(result);
    }

    @PostMapping("getLagChart")
    @ResponseBody
    public Result<LineInfo> getLagChart(@RequestParam(name = "groupId", required = true) String groupId,
                                        @RequestParam(name = "createTimeRange", required = true) String createTimeRange) throws ParseException {
        String key = String.format("DashboardController::getLagChart:%s:%s", groupId, createTimeRange);
        LineInfo cache = ehcacheService.get(key);
        if (cache != null) {
            return Result.ok(cache);
        }
        groupId = groupId.trim();
        createTimeRange = createTimeRange.trim();
        if (StringUtils.isEmpty(groupId) || StringUtils.isEmpty(createTimeRange)) {
            return Result.ok();
        }
        LineInfo result = new LineInfo();
        Common.TimeRange timeRange = Common.splitTime(createTimeRange);
        Date from = timeRange.getStart(), to = timeRange.getEnd();

        List<SysLag> sysLagList = sysLagService.listByGroupId(groupId, from, to);
        List<String> topicNames = sysLagList.stream().map(SysLag::getTopicName).distinct().collect(Collectors.toList());
        List<String> times = sysLagList.stream().map(p -> Common.format(p.getCreateTime())).distinct().collect(Collectors.toList());

        result.setTopicNames(topicNames);
        result.setTimes(times);

        List<LineInfo.Series> seriesList = new ArrayList<>(result.getTopicNames().size());
        for (String topicName : result.getTopicNames()) {
            LineInfo.Series series = new LineInfo.Series();
            series.setName(topicName);
            series.setType("line");
            series.setSmooth(true);
            seriesList.add(series);
        }
        result.setSeries(seriesList);

        for (LineInfo.Series series : result.getSeries()) {
            List<Double> data = new ArrayList<>(result.getTimes().size());
            for (String time : result.getTimes()) {
                List<Double> lag = sysLagList.stream().filter(p -> p.getTopicName().equals(series.getName()) && Common.format(p.getCreateTime()).equals(time)).map(p -> Double.parseDouble(p.getLag().toString())).collect(Collectors.toList());
                if (lag.size() < 1) {
                    data.add(null);
                } else {
                    data.addAll(lag);
                }
            }
            series.setData(data);
        }
        ehcacheService.set(key, result);
        return Result.ok(result);
    }


    @PostMapping("getTopicRankChart")
    @ResponseBody
    public Result<LineInfo> getTopicRankChart(@RequestParam(name = "createTimeRange", required = true) String createTimeRange) throws ParseException {
        String key = String.format("DashboardController::getTopicRankChart:%s", createTimeRange);
        LineInfo cache = ehcacheService.get(key);
        if (cache != null) {
            return Result.ok(cache);
        }

        createTimeRange = createTimeRange.trim();
        if (StringUtils.isEmpty(createTimeRange)) {
            return Result.ok();
        }
        Common.TimeRange timeRange = Common.splitTime(createTimeRange);
        Date from = timeRange.getStart(), to = timeRange.getEnd();

        LineInfo result = new LineInfo();

        List<SysLogSize> sysLogSizeList = sysLogSizeService.getTopicRank(5, from, to);

        List<String> topicNames = sysLogSizeList.stream().map(SysLogSize::getTopicName).collect(Collectors.toList());
        List<Double> logSizeList = sysLogSizeList.stream().map(p -> Double.parseDouble(p.getLogSize().toString())).collect(Collectors.toList());

        List<LineInfo.Series> seriesList = new ArrayList<>();

        LineInfo.Series series = new LineInfo.Series();
        series.setType("bar");
        series.setData(logSizeList);
        seriesList.add(series);

        result.setTopicNames(topicNames);
        result.setSeries(seriesList);
        ehcacheService.set(key, result);
        return Result.ok(result);
    }

    @PostMapping("getTopicHistoryChart")
    @ResponseBody
    @TranRead
    public Result<LineInfo> getTopicHistoryChart(@RequestParam(name = "topicName", required = true) String topicName) throws Exception {
        topicName = topicName.trim();
        if (StringUtils.isEmpty(topicName)) {
            return Result.ok();
        }

        LineInfo result = new LineInfo();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Long logsize = kafkaTopicService.getLogsize(topicName);
        Long day1 = sysLogSizeService.getHistoryLogSize(topicName, 1);
        Long day2 = sysLogSizeService.getHistoryLogSize(topicName, 2);
        Long day3 = sysLogSizeService.getHistoryLogSize(topicName, 3);
        Long day4 = sysLogSizeService.getHistoryLogSize(topicName, 4);
        Long day5 = sysLogSizeService.getHistoryLogSize(topicName, 5);
        Long day6 = sysLogSizeService.getHistoryLogSize(topicName, 6);
        Long day7 = sysLogSizeService.getHistoryLogSize(topicName, 7);

        List<String> timesList = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DATE), 0, 0, 0);
            Date date = DateUtils.addDays(calendar.getTime(), -i);
            timesList.add(sdf.format(date));
        }
        List<Double> data = new ArrayList<>();
        data.add((double) ((logsize - day1 > 0) ? logsize - day1 : 0L));
        data.add((double) ((day1 - day2 > 0) ? day1 - day2 : 0L));
        data.add((double) ((day2 - day3 > 0) ? day2 - day3 : 0L));
        data.add((double) ((day3 - day4 > 0) ? day3 - day4 : 0L));
        data.add((double) ((day4 - day5 > 0) ? day4 - day5 : 0L));
        data.add((double) ((day5 - day6 > 0) ? day5 - day6 : 0L));
        data.add((double) ((day6 - day7 > 0) ? day6 - day7 : 0L));

        List<LineInfo.Series> seriesList = new ArrayList<>();
        LineInfo.Series series = new LineInfo.Series();
        series.setType("bar");
        series.setData(data);
        seriesList.add(series);

        result.setTimes(timesList);
        result.setSeries(seriesList);

        return Result.ok(result);
    }

}