package cn.edu.tju.address.model;

import cn.edu.tju.core.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "delivery_address")
public class DeliveryAddress extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long customerId;

    @Transient
    private CustomerView customer;

    private String contactName;
    private Integer contactSex;
    private String contactTel;

    @Column(nullable = false)
    private String address;

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    // Frontend contract expects nested `customer?: User`.
    @Transient
    public CustomerView getCustomer() {
        if (customer != null) return customer;
        if (this.customerId == null) return null;
        return new CustomerView(this.customerId);
    }

    @Transient
    public void setCustomer(CustomerView customer) {
        this.customer = customer;
        if (customer != null) {
            this.customerId = customer.getId();
        }
    }

    public static class CustomerView {
        private Long id;

        public CustomerView() {}

        public CustomerView(Long id) {
            this.id = id;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    // Keep userId getters/setters for compatibility with AddressInternalServiceImpl
    public Long getUserId() {
        return customerId;
    }

    public void setUserId(Long userId) {
        this.customerId = userId;
    }

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
}
