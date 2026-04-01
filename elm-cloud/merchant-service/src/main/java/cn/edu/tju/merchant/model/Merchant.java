package cn.edu.tju.merchant.model;

import cn.edu.tju.core.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "merchant")
public class Merchant extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String contact;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }
}
