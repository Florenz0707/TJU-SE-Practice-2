package com.neusoft.elmboot.mapper;

import com.neusoft.elmboot.po.Orders;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface OrdersMapper {

  @Insert(
      "insert into orders(userId,businessId,orderDate,orderTotal,daId,orderState)"
          + " values(#{userId},#{businessId},#{orderDate},#{orderTotal},#{daId},0)")
  @Options(useGeneratedKeys = true, keyProperty = "orderId", keyColumn = "orderId")
  public int saveOrders(Orders orders);

  public Orders getOrdersById(Integer orderId);

  public List<Orders> listOrdersByUserId(String userId);
}
