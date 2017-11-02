package com.github.adminfaces.persistence.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

@ApplicationScoped
public class Messages {

    private static final Logger LOG = LoggerFactory.getLogger(Messages.class.getName());

    private ResourceBundle bundle;

    @PostConstruct
    public void init() {
        try {
            bundle = ResourceBundle.getBundle("messages", FacesContext.getCurrentInstance().getViewRoot().getLocale());
        }catch (MissingResourceException e) {
            LOG.warn("Application resource bundle named 'messages' not found.");
        }
    }

    public String getMessage(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return "??" + key + "??";
        }
    }

    public String getMessage(String key, Object... params) {
        return MessageFormat.format(getMessage(key), params);
    }


    public static void addDetailMessage(String message) {
        addDetailMessage(message, null);
    }

    public static void addDetailMessage(String message, FacesMessage.Severity severity) {

        FacesMessage facesMessage = new FacesMessage("",message);
        if (severity != null && severity != FacesMessage.SEVERITY_INFO) {
            facesMessage.setSeverity(severity);
        }

        FacesContext.getCurrentInstance().addMessage(null,facesMessage);
    }
}
