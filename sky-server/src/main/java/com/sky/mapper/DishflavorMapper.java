package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishflavorMapper {
    //批量插入菜品配料信息
    void insertBatch(List<DishFlavor> flavors);
//    @Delete("DELETE FROM dish_flavor WHERE dish_id = #{id}")
    void deletebydishId(List<Long> dishIds);

    @Select("SELECT * FROM dish_flavor WHERE dish_id = #{dishid}")
    List<DishFlavor> getbydishId(Long dishid);
    @Delete("DELETE FROM dish_flavor WHERE dish_id = #{id}")
    void deletebydishId2(Long id);
}
