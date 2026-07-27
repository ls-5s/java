package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {
    PageResult page(DishPageQueryDTO dishPageQueryDTO);

    void savaWithFlavor(DishDTO dishDTO);

    void delete(List<Long> ids);

    DishVO getByIdWithFlavor(Long id);

    void update(DishDTO dishDTO);

    void startOrForbidden(Integer status, Long id);

    List<Dish> listByCategoryId(Long categoryId);

    List<DishVO> listWithFlavor(Dish dish);
}
