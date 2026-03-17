package com.neusoft.elmboot.service.impl;

import com.neusoft.elmboot.mapper.FoodMapper;
import com.neusoft.elmboot.po.Food;
import com.neusoft.elmboot.service.FoodService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FoodServiceImpl implements FoodService {

  @Autowired private FoodMapper foodMapper;

  @Override
  public List<Food> listFoodByBusinessId(Integer businessId) {
    return foodMapper.listFoodByBusinessId(businessId);
  }
}
