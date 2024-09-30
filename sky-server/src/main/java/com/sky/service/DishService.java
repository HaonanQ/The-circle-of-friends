package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;

import java.util.List;

public interface DishService {
    public void savewithFavlor(DishDTO dishDTO);

    PageResult pageQuery(DishPageQueryDTO queryDTO);

    void delete(List<Long> ids);
}
