package com.neusoft.elmboot.mapper;

import com.neusoft.elmboot.po.OrderDetailet;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderDetailetMapper {

  public int saveOrderDetailetBatch(List<OrderDetailet> list);

  public List<OrderDetailet> listOrderDetailetByOrderId(Integer orderOd);
}
