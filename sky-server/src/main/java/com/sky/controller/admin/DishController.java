package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController("adminDishController")
@RequestMapping("/admin/dish")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    /*
    * 分页查询
    * */
    @GetMapping("/page")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){
        log.info("分页查询{}",dishPageQueryDTO);
        PageResult pageResult = dishService.page(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /*
    * 添加菜品
    * */
    @PostMapping
    public Result sava(@RequestBody DishDTO dishDTO){
        log.info("添加菜品：{}",dishDTO);
        dishService.savaWithFlavor(dishDTO);

        //清除缓存数据
        String key = "dish_" + dishDTO.getCategoryId();
        //redisTemplate.delete(key);
        cleanCache(key);

        return Result.success();
    }

    /*
    * 删除菜品
    * */
    @DeleteMapping
    public Result delete(@RequestParam List<Long> ids){
        log.info("删除菜品：{}",ids);
        dishService.delete(ids);

        //将所有的菜品缓存数据清除掉，所有以dish_头的key
        /*Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);*/
        cleanCache("dish_*");

        return Result.success();
    }

    /*
    * 根据id查询菜品 回显
    * */
    @GetMapping("/{id}")
    public Result<DishVO> getById(@PathVariable Long id){
        log.info("根据id查询菜品：{}",id);
        DishVO dishVO =  dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    /*
    * 修改菜品
    * */
    @PutMapping
    public Result update(@RequestBody DishDTO dishDTO){
        log.info("修改菜品：{}",dishDTO);
        dishService.update(dishDTO);

        //将所有的菜品缓存数据清除掉，所有以dish_头的key
        /*Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);*/
        cleanCache("dish_*");

        return Result.success();
    }

    /*
    * 起售、停售
    * */
    @PostMapping("/status/{status}")
    public Result startOrForbidden(@PathVariable Integer status,@RequestParam Long id){
        log.info("起售、停售菜品：{},菜品id：{}",status,id);
        dishService.startOrForbidden(status,id);

        //将所有的菜品缓存数据清除掉，所有以dish_头的key
        Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);

        return Result.success();
    }

    /*
    * 根据分类id查询菜品
    * */
    @GetMapping("/list")
    public Result<List<Dish>> listByCategoryId(@RequestParam Long categoryId){
        log.info("根据分类id：{}查询菜品",categoryId);
        List<Dish> dishList = dishService.listByCategoryId(categoryId);
        return Result.success(dishList);
    }

    /*
    * 清理缓存数据
    * */
    private void cleanCache(String pattern){
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }
}
