package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
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
    @Autowired
    private UserMapper userMapper;

    private List<LocalDate> getDateList(LocalDate start, LocalDate end) {
        List<LocalDate> datelist = new ArrayList<>();
        datelist.add(start);
        while (!start.isAfter(end)) {
            start = start.plusDays(1);
            datelist.add(start);
        }
        return datelist;
    }
    @Override
    public TurnoverReportVO getturnover(LocalDate start, LocalDate end) {
        List<LocalDate> datelist = getDateList(start, end);
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

    @Override
    public UserReportVO getuser(LocalDate begin, LocalDate end) {
        List<LocalDate> datelist = getDateList(begin, end);
        List<Integer> newUserList = new ArrayList<>(); //新增用户数
        List<Integer> totalUserList = new ArrayList<>(); //总用户数
        for (LocalDate date : datelist) {
            LocalDateTime starttime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endtime = LocalDateTime.of(date, LocalTime.MAX);
            Integer newUser = getUserCount(starttime, endtime);
            newUserList.add(newUser);
            Integer totalUser = getUserCount(null,endtime);
            totalUserList.add(totalUser);
        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(datelist,","))
                .newUserList(StringUtils.join(newUserList,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .build();
    }

    private Integer getUserCount(LocalDateTime beginTime, LocalDateTime endTime) {
        Map map = new HashMap();
        map.put("begin",beginTime);
        map.put("end", endTime);
        return userMapper.getCountuser(map);
    }
}
