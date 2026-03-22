package cn.edu.tju.order.model.vo;

import cn.edu.tju.order.model.bo.DeliveryAddress;

public class DeliveryAddressSnapshotVO {
  private final Long id;
  private final String contactName;
  private final Integer contactSex;
  private final String contactTel;
  private final String address;
  private final Long customerId;

  public DeliveryAddressSnapshotVO(DeliveryAddress address) {
    this.id = address.getId();
    this.contactName = address.getContactName();
    this.contactSex = address.getContactSex();
    this.contactTel = address.getContactTel();
    this.address = address.getAddress();
    this.customerId = address.getCustomerId();
  }

  public Long getId() {
    return id;
  }

  public String getContactName() {
    return contactName;
  }

  public Integer getContactSex() {
    return contactSex;
  }

  public String getContactTel() {
    return contactTel;
  }

  public String getAddress() {
    return address;
  }

  public Long getCustomerId() {
    return customerId;
  }
}
