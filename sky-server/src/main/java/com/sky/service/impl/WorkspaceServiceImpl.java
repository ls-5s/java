package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkspaceServiceImpl implements WorkspaceService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private DishMapper dishMapper;
    @Override
    public BusinessDataVO getBusinessData() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayMin = LocalDateTime.of(today, LocalTime.MIN);
        LocalDateTime todayMax = LocalDateTime.of(today, LocalTime.MAX);

        Map map = new HashMap();
        map.put("begin",todayMin);
        map.put("end",todayMax);
        Integer newUsers = userMapper.countByMap(map);

        Integer totalOrders = orderMapper.countByMap(map);
        map.put("status", Orders.COMPLETED);
        Integer completedOrders = orderMapper.countByMap(map);
        Double orderCompletionRate = 0.0;
        if(totalOrders != 0){
            orderCompletionRate =completedOrders.doubleValue() / totalOrders.doubleValue();

        }
        List<Double> amountList = orderMapper.amountByMap(map);
        Double amounts = 0.0;
        if (amountList != null) {
            for (Double amount : amountList) {
                if (amount != null) {
                    amounts += amount;
                }
            }
        }
        Double averAmount = 0.0;
        if(completedOrders != 0){
            averAmount = amounts / completedOrders.doubleValue();
        }

        return BusinessDataVO.builder()
                .newUsers(newUsers)
                .orderCompletionRate(orderCompletionRate)
                .turnover(amounts)
                .unitPrice(averAmount)
                .validOrderCount(completedOrders)
                .build();
    }

    @Override
    public SetmealOverViewVO getverviewSetmeals() {
        Map map = new HashMap();
        map.put("status",0);
        Integer discontinued = setmealMapper.countByMap(map);
        map.put("status",1);
        Integer sold = setmealMapper.countByMap(map);

        return SetmealOverViewVO.builder()
                .discontinued(discontinued)
                .sold(sold)
                .build();
    }

    @Override
    public DishOverViewVO getDishOverView() {
        Map map = new HashMap();
        map.put("status",0);
        Integer discontinued = dishMapper.countByMap(map);
        map.put("status",1);
        Integer sold = dishMapper.countByMap(map);
        return DishOverViewVO.builder()
                .discontinued(discontinued)
                .sold(sold)
                .build();
    }

    @Override
    public OrderOverViewVO getOrderOverView() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayMin = LocalDateTime.of(today, LocalTime.MIN);
        LocalDateTime todayMax = LocalDateTime.of(today, LocalTime.MAX);

        Map map = new HashMap();
        map.put("begin",todayMin);
        map.put("end",todayMax);

        Integer allOrders = orderMapper.countByMap(map);

        map.put("status",6);
        Integer cancelledOrders = orderMapper.countByMap(map);

        map.put("status",5);
        Integer completedOrders = orderMapper.countByMap(map);

        map.put("status",3);
        Integer deliveredOrders = orderMapper.countByMap(map);

        map.put("status",2);
        Integer waitingOrders = orderMapper.countByMap(map);

        return OrderOverViewVO.builder()
                .allOrders(allOrders)
                .cancelledOrders(cancelledOrders)
                .completedOrders(completedOrders)
                .deliveredOrders(deliveredOrders)
                .waitingOrders(waitingOrders)
                .build();
    }
}
