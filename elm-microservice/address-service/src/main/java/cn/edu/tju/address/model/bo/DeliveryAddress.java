package cn.edu.tju.address.model.bo;

import cn.edu.tju.core.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "delivery_address")
public class DeliveryAddress extends BaseEntity {

  private String contactName;
  private Integer contactSex;
  private String contactTel;
  private String address;

  @Column(name = "user_id", nullable = false)
  private Long customerId;

  public String getContactName() {
    return contactName;
  }

  public void setContactName(String contactName) {
    this.contactName = contactName;
  }

  public Integer getContactSex() {
    return contactSex;
  }

  public void setContactSex(Integer contactSex) {
    this.contactSex = contactSex;
  }

  public String getContactTel() {
    return contactTel;
  }

  public void setContactTel(String contactTel) {
    this.contactTel = contactTel;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public Long getCustomerId() {
    return customerId;
  }

  public void setCustomerId(Long customerId) {
    this.customerId = customerId;
  }
}