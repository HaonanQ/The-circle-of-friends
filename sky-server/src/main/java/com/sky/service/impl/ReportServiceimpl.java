package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceimpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Override
    public TurnoverReportVO getturnover(LocalDate start, LocalDate end) {
        List<LocalDate> datelist = new ArrayList<>();
        datelist.add(start);
        while (!start.isAfter(end)) {
            start = start.plusDays(1);
            datelist.add(start);
        }
        List<Double> turnoverlist = new ArrayList<>();
        for (LocalDate date : datelist) {
            LocalDateTime starttime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endtime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("status", Orders.COMPLETED);
            map.put("begin",starttime);
            map.put("end", endtime);
            Double turnover = orderMapper.countTurnover(map);
            turnover = turnover == null? 0.0 : turnover;
            turnoverlist.add(turnover);

        }

        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(datelist,","))
                .turnoverList(StringUtils.join(turnoverlist,","))
                .build();
    }
}
