package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetdealdishMapper {
    public List<Long> getdealdishbyids(List<Long> dishIds);
}
