package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {
    public void savewithFavlor(DishDTO dishDTO);

    PageResult pageQuery(DishPageQueryDTO queryDTO);

    void delete(List<Long> ids);

    DishVO getbyidwithflavors(Long id);

    void updatewithFavlor(DishDTO dishDTO);

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    List<DishVO> listWithFlavor(Dish dish);

    List<Dish> list(Long categoryId);

    void startOrStop(Integer status, Long id);
}
