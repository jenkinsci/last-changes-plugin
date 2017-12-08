/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.adminfaces.persistence.service;

import com.github.adminfaces.persistence.model.*;
import org.apache.deltaspike.data.api.criteria.Criteria;
import org.apache.deltaspike.jpa.api.transaction.Transactional;

import javax.inject.Inject;
import javax.persistence.criteria.JoinType;
import java.io.Serializable;
import java.util.List;


/**
 * @author rmpestano
 */
public class CarService extends CrudService<Car, Integer> implements Serializable {

    @Inject
    protected CarRepository carRepository;//you can create repositories to extract complex queries from your service


    protected Criteria<Car, Car> configRestrictions(Filter<Car> filter) {

        Criteria<Car, Car> criteria = criteria();

        //create restrictions based on parameters map
        if (filter.hasParam("id")) {
            criteria.eq(Car_.id, filter.getIntParam("id"));
        }

        if (filter.hasParam("minPrice") && filter.hasParam("maxPrice")) {
            criteria.between(Car_.price, filter.getDoubleParam("minPrice"), filter.getDoubleParam("maxPrice"));
        } else if (filter.hasParam("minPrice")) {
            criteria.gtOrEq(Car_.price, filter.getDoubleParam("minPrice"));
        } else if (filter.hasParam("maxPrice")) {
            criteria.ltOrEq(Car_.price, filter.getDoubleParam("maxPrice"));
        }

        //create restrictions based on filter entity
        if (filter.getEntity() != null) {
            Car filterEntity = filter.getEntity();
            if (filterEntity.hasModel()) {
                criteria.likeIgnoreCase(Car_.model, "%" + filterEntity.getModel());
            }

            if (filterEntity.getPrice() != null) {
                criteria.eq(Car_.price, filterEntity.getPrice());
            }

            if (filterEntity.hasName()) {
                criteria.likeIgnoreCase(Car_.name, "%" + filterEntity.getName() + "%");
            }
        }
        return criteria;
    }

    public void beforeInsert(Car car) {
        validate(car);
    }

    public void beforeUpdate(Car car) {
        validate(car);
    }

    public void validate(Car car) {
        if (!car.hasModel()) {
            throw new RuntimeException("Car model cannot be empty");
        }
        if (!car.hasName()) {
            throw new RuntimeException("Car name cannot be empty");
        }

        if (car.getPrice() == null) {
            throw new RuntimeException("Car price cannot be empty");
        }

        if (count(criteria()
                .eqIgnoreCase(Car_.name, car.getName())
                .notEq(Car_.id, car.getId())) > 0) {

            throw  new RuntimeException("Car name must be unique");
        }

    }


    public List<Car> listByModel(String model) {
        return criteria()
                .likeIgnoreCase(Car_.model, model)
                .getResultList();
    }

    public List<String> getModels(String query) {
        return criteria()
                .select(String.class, attribute(Car_.model))
                .likeIgnoreCase(Car_.model, "%" + query + "%")
                .getResultList();
    }

    public Double getTotalPriceByModel(Car car) {
        if (!car.hasModel()) {
            throw new RuntimeException("Provide car model to get the total price.");
        }
        return carRepository.getTotalPriceByModel(car.getModel().toUpperCase());
    }

    @Override
    @Transactional
    public void insert(Car entity) {
        super.insert(entity);
    }

    @Override
    @Transactional
    public void remove(Car entity) {
        super.remove(entity);
    }

    @Override
    @Transactional
    public void remove(List<Car> entities) {
        super.remove(entities);
    }

    @Override
    @Transactional
    public Car update(Car entity) {
        return super.update(entity);
    }

    @Override
    @Transactional
    public Car saveOrUpdate(Car entity) {
        return super.saveOrUpdate(entity);
    }

    public List<Car> findBySalesPointAddress(String address) {
        return criteria().join(Car_.salesPoints,where(SalesPoint.class, JoinType.LEFT)
                .likeIgnoreCase(SalesPoint_.address,"%"+address+"%"))
                .getResultList();
    }

    public List<Car> findBySalesPoint(SalesPoint salesPoint) {
        criteria().join(Car_.salesPoints,where(SalesPoint.class, JoinType.LEFT));
        return criteria().join(Car_.salesPoints,where(SalesPoint.class, JoinType.LEFT)
                .in(SalesPoint_.salesPointPK,salesPoint.getSalesPointPK()))
                .getResultList();
    }
}
