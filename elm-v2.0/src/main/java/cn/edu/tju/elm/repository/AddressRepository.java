package cn.edu.tju.elm.repository;

import cn.edu.tju.elm.model.DeliveryAddress;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<DeliveryAddress, Integer> {
    // JpaRepository<E, T> -> E: 存储对象，一般在po中定义；T：主键类型
    // JpaRepository接口提供了数据库的各种操作，甚至不需要我们编写SQL语句
    // 参考链接：https://blog.csdn.net/xfx_1994/article/details/104921234
}
