package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.DishflavorMapper;
import com.sky.mapper.SetdealdishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DishServiceimpl implements DishService {
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishflavorMapper dishflavorMapper;
    @Autowired
    private SetdealdishMapper setdealdishMapper;
    @Override
    @Transactional // 开启事务 保证数据的一致性
    public void savewithFavlor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
         log.info("保存菜品信息：{}", dishDTO);
         dishMapper.insert(dish);
         Long dishid = dish.getId();

         //插入口味表数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && flavors.size() > 0) {
            flavors.forEach(dishflavor -> {
                dishflavor.setDishId(dishid);
            });
            dishflavorMapper.insertBatch(flavors);
        }
    }

    @Override
    public PageResult pageQuery(DishPageQueryDTO queryDTO) {
        PageHelper.startPage(queryDTO.getPage(), queryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(queryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    @Transactional
    public void delete(List<Long> ids) {
        for(Long id : ids){
            Dish dish = dishMapper.getbyid(id);
            if(dish.getStatus() == StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        List<Long> dealdishids = setdealdishMapper.getdealdishbyids(ids);
        if (dealdishids.size() > 0 && dealdishids!= null){
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
//        for(Long id : ids){
//            dishMapper.deletebyid(id);
//            dishflavorMapper.deletebydishId(id);
//        }
        dishMapper.deletebyid(ids);
        dishflavorMapper.deletebydishId(ids);
    }

    @Override
    public DishVO getbyidwithflavors(Long id) {
        Dish dish = dishMapper.getbyid(id);
        List<DishFlavor> flavors = dishflavorMapper.getbydishId(id);
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(flavors);
        return dishVO;
    }

    @Override
    public void updatewithFavlor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.update(dish);

        //更新口味表数据
        dishflavorMapper.deletebydishId2(dishDTO.getId());
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && flavors.size() > 0) {
            flavors.forEach(dishflavor -> {
                dishflavor.setDishId(dishDTO.getId());
            });
            dishflavorMapper.insertBatch(flavors);
        }
    }
    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishflavorMapper.getbydishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }

    @Override
    public List<Dish> list(Long categoryId) {
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        return dishMapper.list(dish);
    }
}
