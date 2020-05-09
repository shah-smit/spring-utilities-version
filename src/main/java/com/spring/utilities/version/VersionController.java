package com.spring.utilities.version;

import com.spring.utilities.version.annotation.VersionGetHandler;
import com.spring.utilities.version.annotation.VersionHandler;
import com.spring.utilities.version.annotation.VersionPostHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Component
public class VersionController {

    @Autowired
    private ApplicationContext applicationContext;


    public Object findVersionHandler(String endpoint, String version, String httpMethod, Object... args) {

        List<Class> classes = getClassBasedOnVersion(version);

        for(Class currentClass: classes){

            Method method = getMethodFromClassAndHttpMethodAndEndpoint(currentClass, httpMethod, endpoint);

            if(method != null){
                var constructors = currentClass.getConstructors();
                Object result = null;
                try {
                    result = method.invoke(constructors[0].newInstance(), args);
                } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                    return new RuntimeException("Un Processable Entity");
                }

                return result;
            }
        }

        return new RuntimeException("Not Found");
    }

    private List<Class> getClassBasedOnVersion(String version) {
        List<Class> classes = new ArrayList<>();
        String[] allBeanNames = applicationContext.getBeanNamesForAnnotation(VersionHandler.class);
        for (String beanName : allBeanNames) {
            Class annotatedClass = applicationContext.getType(beanName);
            if (annotatedClass != null) {
                VersionHandler versionHandler = (VersionHandler) annotatedClass.getAnnotation(VersionHandler.class);
                if (versionHandler != null && versionHandler.version().equals(version)) {
                    classes.add(annotatedClass);
                }
            }
        }
        return classes;
    }

    private Method getMethodFromClassAndHttpMethodAndEndpoint(Class currentClass, String httpMethod, String endpoint){

        Method[] methods = currentClass.getMethods();
        for (Method method : methods) {

            if (method.isAnnotationPresent(VersionGetHandler.class)) {
                if(httpMethod.matches("GET")) {
                    VersionGetHandler versionGetHandler = method.getAnnotation(VersionGetHandler.class);

                    if (versionGetHandler.endpoint().equals(endpoint)) {
                        return method;
                    }
                }
            }
            if (method.isAnnotationPresent(VersionPostHandler.class)) {
                if(httpMethod.matches("POST")) {
                    VersionPostHandler versionPostHandler = method.getAnnotation(VersionPostHandler.class);

                    if (versionPostHandler != null && versionPostHandler.endpoint().equals(endpoint)) {
                        return method;
                    }
                }
            }
        }

        return null;
    }
}