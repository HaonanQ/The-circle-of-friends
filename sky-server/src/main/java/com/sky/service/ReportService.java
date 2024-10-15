package com.sky.service;

import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import java.time.LocalDate;

public interface ReportService {

    TurnoverReportVO getturnover(LocalDate start, LocalDate end);

    UserReportVO getuser(LocalDate begin, LocalDate end);
}
