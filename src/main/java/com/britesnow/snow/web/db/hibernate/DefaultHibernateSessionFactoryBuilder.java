package com.britesnow.snow.web.db.hibernate;

import java.util.Map;

import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

import com.britesnow.snow.SnowException;
import com.britesnow.snow.web.Initializable;
import com.britesnow.snow.web.binding.ApplicationProperties;
import com.britesnow.snow.web.binding.EntityClasses;
import com.britesnow.snow.web.db.hibernate.binding.HibernateInterceptorBinding;
import com.google.inject.Inject;

@Singleton
public class DefaultHibernateSessionFactoryBuilder implements HibernateSessionFactoryBuilder, Initializable {

    public enum Error{
        ERROR_INITIALIZING_NAMING_STRATEGY_CLASS;
    }
    private SessionFactory sessionFactory;
    
    @Inject
    private @ApplicationProperties Map properties;

    @Inject(optional=true)
    private @HibernateInterceptorBinding Interceptor hibernateInterceptor;
    
    @Inject
    private @EntityClasses Class[] entityClasses;
    
    @Override
    public void init() {
        
        Configuration cfg = new Configuration();
        for (Class cls : entityClasses) {
            cfg.addAnnotatedClass(cls);
        }

        //set the hibernate properties
        for (Object key : properties.keySet()) {
            String keyStr = key.toString();
            if (keyStr.startsWith("hibernate.")) {
                String value = properties.get(key).toString();
                cfg.setProperty(keyStr, value);
            }
        }

        //get the eventual namingStrategy
        String namingStrategyClassStr = (String) properties.get("snow.hibernate.namingStrategyClass");
        if (namingStrategyClassStr != null) {
            Class namingStrategyClass;
            try {
                namingStrategyClass = Class.forName(namingStrategyClassStr);
                NamingStrategy namingStrategy = (NamingStrategy) namingStrategyClass.newInstance();
                cfg.setNamingStrategy(namingStrategy);
            } catch (Exception e) {
                throw new SnowException(Error.ERROR_INITIALIZING_NAMING_STRATEGY_CLASS,e, "namingStrategyClass",
                                        namingStrategyClassStr);
            }

        }

        if (hibernateInterceptor != null) {
            cfg.setInterceptor(hibernateInterceptor);
        }
        
        sessionFactory = configureSessionFactory(cfg);
        
    }
    
    private static SessionFactory configureSessionFactory(Configuration configuration) throws HibernateException {
        //Configuration configuration = new Configuration();
        //configuration.configure();
        ServiceRegistry serviceRegistry = new ServiceRegistryBuilder().applySettings(configuration.getProperties()).buildServiceRegistry();        
        SessionFactory sessionFactory = configuration.buildSessionFactory(serviceRegistry);
        return sessionFactory;
    }
    
    @Override
    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }


}
