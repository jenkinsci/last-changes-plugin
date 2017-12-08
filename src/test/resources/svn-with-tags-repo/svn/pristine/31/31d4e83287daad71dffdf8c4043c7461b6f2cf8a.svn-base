package com.github.adminfaces.persistence.infra;


import com.github.database.rider.core.util.EntityManagerProvider;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;


@ApplicationScoped
public class EntityManagerProducer {


    @Produces
    public EntityManager produce() {
        return  EntityManagerProvider.instance("persistenceDB").em();
    }
}
