package com.github.adminfaces.persistence.model;


import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by rmpestano on 9/7/14.
 * class which holds database pagination metadata
 */
public class Filter<T extends PersistenceEntity> {
    private T entity;
    private int first;
    private int pageSize;
    private String sortField;
    private AdminSort adminSort;
    private Map<String, Object> params = new HashMap<String, Object>();


    public Filter() {
    }

    public Filter(T entity) {
        this.entity = entity;
    }

    public Filter setFirst(int first) {
        this.first = first;
        return this;
    }

    public int getFirst() {
        return first;
    }

    public Filter setPageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public int getPageSize() {
        return pageSize;
    }

    public Filter setSortField(String sortField) {
        this.sortField = sortField;
        return this;
    }

    public String getSortField() {
        return sortField;
    }

    public Filter setAdminSort(AdminSort adminSort) {
        this.adminSort = adminSort;
        return this;
    }

    public AdminSort getAdminSort() {
        return adminSort;
    }

    public Filter setParams(Map<String, Object> params) {
        this.params = params;
        return this;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public T getEntity() {
        return entity;
    }

    public Filter setEntity(T entity) {
        this.entity = entity;
        return this;
    }

    public Filter addParam(String key, Object value) {
        getParams().put(key, value);
        return this;
    }

    public boolean hasParam(String key) {
        return getParams().containsKey(key) && getParam(key) != null;
    }

    public Object getParam(String key) {
        return getParams().get(key);
    }

    public String getStringParam(String key) {
        return hasParam(key) ? getParam(key).toString() : null;
    }

    public Integer getIntParam(String key) {
        return hasParam(key) ? Integer.parseInt(getStringParam(key)) : null;
    }

    public Long getLongParam(String key) {
        return hasParam(key) ? Long.parseLong(getStringParam(key)) : null;
    }

    public Boolean getBooleanParam(String key) {
        return hasParam(key) ? Boolean.parseBoolean(getStringParam(key)) : null;
    }

    public Double getDoubleParam(String key) {
        return hasParam(key) ? Double.parseDouble(getStringParam(key)) : null;
    }
}
