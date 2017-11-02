package com.github.adminfaces.persistence.model;

import javax.persistence.*;

@Entity
@Table(name = "sales_point")
public class SalesPoint implements PersistenceEntity {

    @EmbeddedId
    private SalesPointPK salesPointPK;

    private String name;

    private String address;

    public SalesPoint() {
    }

    public SalesPoint(SalesPointPK salesPointPK) {
        this.salesPointPK = salesPointPK;
    }

    public SalesPointPK getSalesPointPK() {
        return salesPointPK;
    }

    public void setSalesPointPK(SalesPointPK salesPointPK) {
        this.salesPointPK = salesPointPK;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public SalesPointPK getId() {
        return salesPointPK;
    }
}
