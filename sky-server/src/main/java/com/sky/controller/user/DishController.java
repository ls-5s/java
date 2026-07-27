package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;
    @GetMapping("/list")
    public Result<List<DishVO>> list(Long categoryId){
        log.info("根据分类id查询菜品:{}",categoryId);

        //构造redis在的key，规则：dish_分类id
        String key = "dish_" + categoryId;

        //查询redis中是否存在菜品数据
        List<DishVO> dishList = (List<DishVO>) redisTemplate.opsForValue().get(key);
        if(dishList != null && dishList.size() > 0){
            //如果存在，直接返回，无需查询数据库
            return Result.success(dishList);
        }

        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);

        //如果不存在，查询数据库，将查询到的数据放入redis中
        dishList = dishService.listWithFlavor(dish);
        redisTemplate.opsForValue().set(key,dishList);

        return Result.success(dishList);
    }
}
