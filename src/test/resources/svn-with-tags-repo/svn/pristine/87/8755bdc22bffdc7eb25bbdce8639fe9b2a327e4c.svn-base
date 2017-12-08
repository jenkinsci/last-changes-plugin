package com.github.adminfaces.persistence.bean;

import com.github.adminfaces.persistence.service.CrudService;

import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;
import java.lang.annotation.*;

/**
 * Annotation used for providing crud services to a CrudMB:
 *  <code>
    {@literal @}ViewScoped
    {@literal @}Named
    {@literal @}BeanService(CarService.class)
    public class CarMB&lt;Entity&gt;{}
 * </code>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface BeanService {

    @Nonbinding
    Class<? extends CrudService> value();
}
