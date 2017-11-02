package com.github.adminfaces.persistence.service;

import javax.inject.Qualifier;
import java.lang.annotation.*;

/**
 * Marker interface to allow generic CrudServive injection: 
 * <code>
     {@literal @}Inject 
     {@literal @}Service 
     CrudService&lt;Entity,PK&gt; genericService; 
 * </code>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD,ElementType.PARAMETER,ElementType.TYPE})
@Qualifier
@Documented
public @interface Service {
}
