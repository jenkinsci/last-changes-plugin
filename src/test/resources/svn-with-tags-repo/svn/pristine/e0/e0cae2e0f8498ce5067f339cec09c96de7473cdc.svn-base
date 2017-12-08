package com.github.adminfaces.persistence.util;

import com.github.adminfaces.persistence.model.AdminSort;
import com.github.adminfaces.persistence.model.Filter;
import com.github.adminfaces.persistence.model.PersistenceEntity;
import com.github.adminfaces.persistence.service.CrudService;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

import java.util.List;
import java.util.Map;

public class AdminDataModel<T extends PersistenceEntity> extends LazyDataModel<T> {

    private CrudService<T, ?> crudService;
    private Filter<T> filter;
    private boolean keepFiltersInSession;


    public AdminDataModel(CrudService<T, ?> crudService, Filter<T> filter) {
        this(crudService, filter, true);
    }

    public AdminDataModel(CrudService<T, ?> crudService, Filter<T> filter, boolean keepFiltersInSession) {
        this.crudService = crudService;
        this.filter = filter;
        this.keepFiltersInSession = keepFiltersInSession;
    }

    @Override
    public List<T> load(int first, int pageSize, String sortField, SortOrder sortOrder,
                        Map<String, Object> filters) {
        AdminSort order = null;
        if (sortOrder != null) {
            order = sortOrder.equals(SortOrder.ASCENDING) ? AdminSort.ASCENDING
                    : sortOrder.equals(SortOrder.DESCENDING) ? AdminSort.DESCENDING
                    : AdminSort.UNSORTED;
        }

        if (filters == null || filters.isEmpty() && keepFiltersInSession) {
            filters = filter.getParams();
        }

        filter.setFirst(first).setPageSize(pageSize)
                .setSortField(sortField).setAdminSort(order)
                .setParams(filters);
        List<T> list = crudService.paginate(filter);
        setRowCount(crudService.count(filter).intValue());
        return list;
    }

    @Override
    public int getRowCount() {
        return super.getRowCount();
    }

    @Override
    public T getRowData(String key) {
        List<T> list = (List<T>) this.getWrappedData();
        for (T t : list) {
            if (key.equals(t.getId().toString())) {
                return t;
            }
        }
        return null;
    }

    public void setFilter(Filter<T> filter) {
        this.filter = filter;
    }
}
