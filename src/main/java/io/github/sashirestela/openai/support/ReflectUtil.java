package io.github.sashirestela.openai.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.sashirestela.openai.SimpleUncheckedException;
import io.github.sashirestela.openai.http.ResponseType;

public class ReflectUtil {
  private static ReflectUtil reflection = null;

  private ReflectUtil() {
  }

  public static ReflectUtil get() {
    if (reflection == null) {
      reflection = new ReflectUtil();
    }
    return reflection;
  }

  @SuppressWarnings("unchecked")
  public <T> T createProxy(Class<T> interfaceClass, InvocationHandler handler) {
    T proxy = (T) Proxy.newProxyInstance(
        interfaceClass.getClassLoader(),
        new Class<?>[] { interfaceClass },
        handler);
    return proxy;
  }

  public Class<? extends Annotation> getFirstAnnotTypeInList(Method method,
      List<Class<? extends Annotation>> listAnnotType) {
    Class<? extends Annotation> annotType = listAnnotType
        .stream()
        .filter(type -> type != null && method.isAnnotationPresent(type))
        .findFirst()
        .orElse(null);
    return annotType;
  }

  public Object getAnnotAttribValue(AnnotatedElement element, Class<? extends Annotation> annotType,
      String annotAttribName) {
    Object value = null;
    Annotation annotation = element.getAnnotation(annotType);
    if (annotation != null) {
      Method annotAttrib = null;
      try {
        annotAttrib = annotType.getMethod(annotAttribName);
      } catch (NoSuchMethodException | SecurityException e) {
        throw new SimpleUncheckedException("Cannot find the method {0} in the annotation {1}.", annotAttribName,
            annotType.getName(), e);
      }
      try {
        value = annotAttrib.invoke(annotation, (Object[]) null);
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        throw new SimpleUncheckedException("Cannot execute the method {0} in the annotation {1}.", annotAttribName,
            annotType.getName(), e);
      }
    }
    return value;
  }

  public MethodElement getMethodElementAnnotatedWith(Method method, Object[] arguments,
      Class<? extends Annotation> annotType) {
    List<MethodElement> methodElements = getMethodElementsAnnotatedWith(method, arguments, annotType, true);
    MethodElement methodElement = methodElements.size() > 0 ? methodElements.get(0) : null;
    return methodElement;
  }

  public List<MethodElement> getMethodElementsAnnotatedWith(Method method, Object[] arguments,
      Class<? extends Annotation> annotType) {
    List<MethodElement> methodElements = getMethodElementsAnnotatedWith(method, arguments, annotType, false);
    return methodElements;
  }

  private List<MethodElement> getMethodElementsAnnotatedWith(Method method, Object[] arguments,
      Class<? extends Annotation> annotType, boolean firstOnly) {
    List<MethodElement> methodElements = new ArrayList<>();
    boolean existsDefAnnotAttrib = this.existsAnnotAttrib(annotType, Constant.DEF_ANNOT_ATTRIB);
    int i = 0;
    Parameter[] parameters = method.getParameters();
    for (Parameter parameter : parameters) {
      if (parameter.isAnnotationPresent(annotType)) {
        Object defAnnotValue = null;
        if (existsDefAnnotAttrib) {
          defAnnotValue = getAnnotAttribValue(parameter, annotType, Constant.DEF_ANNOT_ATTRIB);
        }
        methodElements.add(new MethodElement(parameter, defAnnotValue, arguments[i]));
        if (firstOnly) {
          break;
        }
      }
      i++;
    }
    return methodElements;
  }

  public Class<?> getBaseClass(Method method) {
    String className = method.getGenericReturnType().getTypeName();
    className = CommonUtil.get().findInnerGroup(className, Constant.REGEX_GENERIC_CLASS);
    Class<?> methodReturnClass = null;
    try {
      methodReturnClass = Class.forName(className);
    } catch (ClassNotFoundException e) {
      throw new SimpleUncheckedException("Cannot find the base class {0} for the method {1}.", className,
          method.getName(), e);
    }
    return methodReturnClass;
  }

  public ResponseType getResponseType(Method method) {
    String className = method.getGenericReturnType().getTypeName();
    className = Optional.ofNullable(CommonUtil.get().findFirstMatch(className, Constant.REGEX_MIDDLE_CLASS)).orElse("");
    ResponseType responseType = ResponseType.UNKNOWN;
    for (ResponseType respType : ResponseType.values()) {
      if (respType.getClassName().equals(className)) {
        responseType = respType;
        break;
      }
    }
    return responseType;
  }

  public void executeSetMethod(Class<?> clazz, String methodName, Class<?>[] paramTypes, Object object, Object value) {
    Method method = null;
    try {
      method = clazz.getMethod(methodName, paramTypes);
    } catch (NoSuchMethodException | SecurityException e) {
      throw new SimpleUncheckedException("Cannot find the method {0} in the class {1}", methodName,
          clazz.getSimpleName(),
          e);
    }
    try {
      method.invoke(object, value);
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new SimpleUncheckedException("Cannot execute the method {0} in the class {1}", methodName,
          clazz.getSimpleName(), e);
    }
  }

  private boolean existsAnnotAttrib(Class<? extends Annotation> annotType, String annotAttribName) {
    boolean exists = true;
    try {
      annotType.getMethod(annotAttribName);
    } catch (NoSuchMethodException | SecurityException e) {
      exists = false;
    }
    return exists;
  }

  public Map<String, Object> getMapFields(Object object) {
    final String GET_PREFIX = "get";
    Map<String, Object> structure = new HashMap<>();
    Class<?> clazz = object.getClass();
    Field[] fields = getFields(clazz);
    for (Field field : fields) {
      String fieldName = field.getName();
      String methodName = GET_PREFIX + CommonUtil.get().capitalize(fieldName);
      Object fieldValue;
      try {
        Method getMethod = clazz.getMethod(methodName, new Class<?>[] {});
        fieldValue = getMethod.invoke(object);
      } catch (Exception e) {
        throw new SimpleUncheckedException("Cannot find the method {0} in the class {1}.", methodName,
            clazz.getSimpleName(), e);
      }
      if (fieldValue != null) {
        structure.put(getFieldName(field), getFieldValue(fieldValue));
      }
    }
    return structure;
  }

  private Field[] getFields(Class<?> clazz) {
    final String CLASS_OBJECT = "Object";
    Field[] fields = new Field[] {};
    Class<?> nextClazz = clazz;
    while (!nextClazz.getSimpleName().equals(CLASS_OBJECT)) {
      fields = CommonUtil.get().concatArrays(fields, nextClazz.getDeclaredFields());
      nextClazz = nextClazz.getSuperclass();
    }
    return fields;
  }

  private String getFieldName(Field field) {
    final String JSON_PROPERTY_METHOD_NAME = "value";
    String fieldName = field.getName();
    if (field.isAnnotationPresent(JsonProperty.class)) {
      fieldName = (String) getAnnotAttribValue(field, JsonProperty.class, JSON_PROPERTY_METHOD_NAME);
    }
    return fieldName;
  }

  private Object getFieldValue(Object fieldValue) {
    if (fieldValue.getClass().isEnum()) {
      String enumConstantName = ((Enum<?>) fieldValue).name();
      try {
        fieldValue = fieldValue.getClass().getField(enumConstantName).getAnnotation(JsonProperty.class).value();
      } catch (NoSuchFieldException | SecurityException e) {
        throw new SimpleUncheckedException("Cannot find the enum constant {0}.", enumConstantName, e);
      }
    }
    return fieldValue;
  }
}