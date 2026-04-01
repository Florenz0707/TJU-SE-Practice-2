package cn.edu.tju.order.model.vo;

import java.util.List;

public class PagedOrderSnapshotVO {
  private final List<OrderSnapshotVO> orders;
  private final long total;
  private final int page;
  private final int size;

  public PagedOrderSnapshotVO(List<OrderSnapshotVO> orders, long total, int page, int size) {
    this.orders = orders;
    this.total = total;
    this.page = page;
    this.size = size;
  }

  public List<OrderSnapshotVO> getOrders() {
    return orders;
  }

  public long getTotal() {
    return total;
  }

  public int getPage() {
    return page;
  }

  public int getSize() {
    return size;
  }
}
