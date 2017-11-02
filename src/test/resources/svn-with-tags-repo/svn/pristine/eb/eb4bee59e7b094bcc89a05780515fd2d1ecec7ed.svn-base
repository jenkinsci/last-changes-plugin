/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.adminfaces.persistence.model;


import javax.persistence.*;
import java.util.List;

/**
 * @author rmpestano
 */
@Entity
@Table(name = "car")
public class Car extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column(name = "model")
    private String model;

    @Column(name = "name")
    private String name;

    @Column(name = "price")
    private Double price;

    @OneToOne
    private Brand brand;

    @OneToMany
    private List<SalesPoint> salesPoints;

    @Version
    private Integer version;

    public Car() {
    }

    public Car(Integer id) {
        this.id = id;
    }

    public String getModel() {
        return model;
    }

    public Double getPrice() {
        return price;
    }

    @Override
    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Car model(String model) {
        this.model = model;
        return this;
    }

    public Car price(Double price) {
        this.price = price;
        return this;
    }

    public Car name(String name) {
        this.name = name;
        return this;
    }

    public Brand getBrand() {
        return brand;
    }

    public void setBrand(Brand brand) {
        this.brand = brand;
    }

    public List<SalesPoint> getSalesPoints() {
        return salesPoints;
    }

    public void setSalesPoints(List<SalesPoint> salesPoints) {
        this.salesPoints = salesPoints;
    }

    public boolean hasModel() {
        return model != null && !"".equals(model.trim());
    }

    public boolean hasName() {
        return name != null && !"".equals(name.trim());
    }

    public boolean hasBrand() {
        return brand != null;
    }

    public boolean hasSalesPoint() {
        return salesPoints != null && !salesPoints.isEmpty();
    }
}
