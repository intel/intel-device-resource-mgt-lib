/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.help;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

public class BaseUtil {
  private static final Logger logger = LoggerFactory.getLogger(BaseUtil.class);

  public static String composeUri(String... paths) {
    return "/" + String.join("/", paths);
  }

  public static String composeQuery(String[]... queries) {
    return Arrays.asList(queries)
                 .stream()
                 .map(q -> q[0] + "=" + q[1])
                 .collect(Collectors.joining("&"));
  }

  public static Map<String, String> validate(Validator validator, Object obj, Class... clazz) {
    return parseViolationMessage(validator.validate(obj, clazz));
  }

  public static Map<String, String>
      parseViolationMessage(Set<ConstraintViolation<Object>> varResult) {
    return varResult.stream().collect(Collectors.toMap(i -> i.getPropertyPath().toString(),
                                                       i -> i.getMessage()));
  }

  @Deprecated
  public static Date getNow() {
    return Date.from(Instant.now(Clock.systemUTC()));
  }

  public static long getNowAsEpochMillis() {
    return Instant.now(Clock.systemUTC()).toEpochMilli();
  }

  public static long getStartOfTodayAsEpochMillis() {
    return LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
  }

  public static long getLatestNSecondsAsEpochMillis(int n) {
    return LocalDateTime.now(ZoneOffset.UTC)
                        .minusSeconds(n)
                        .toInstant(ZoneOffset.UTC)
                        .toEpochMilli();
  }

  public static List<String> getPathSegments(String fullurl) {
    return Arrays.asList(fullurl.split("/"))
                 .stream()
                 .filter(seg -> seg != null && !seg.isEmpty())
                 .collect(Collectors.toList());
  }

  public static String getStackTrace(Throwable exception) {
    exception.printStackTrace();
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw, true);
    exception.printStackTrace(pw);
    return sw.toString();
  }

  /**
   * parse a stirng like "[a,b,c]" into a List<String> includes "a", "b", "c"
   *
   * @param asArray
   * @return
   */
  public static List<String> parseArray(String asArray) {
    return asArray.isEmpty() ? Collections.emptyList()
        : Arrays.asList(asArray.substring(1, asArray.length() - 1).split(","))
                .stream()
                .map(i -> i.trim())
                .filter(i -> !i.isEmpty())
                .collect(Collectors.toList());
  }

  public static boolean isNonequivalentQuery(String queryKey, String queryValue) {
    return queryValue == null || queryValue.isEmpty() || queryValue.startsWith("lt")
        || queryValue.startsWith("gt") || queryValue.startsWith("gte")
        || queryValue.startsWith("lte") || queryValue.startsWith("ne");
  }

  public static String formAFullUrl(String devId, String resUri) {
    String fullUrl = String.join("/", devId, resUri);
    List<String> segments = Arrays.asList(fullUrl.split("/"))
                                  .stream()
                                  .filter(seg -> seg != null && !seg.isEmpty())
                                  .collect(Collectors.toList());
    return "/" + String.join("/", segments);
  }

  public static String formAFullUrl(String devId, String resUri, String propName) {
    String fullUrl = String.join("/", devId, resUri, propName);
    List<String> segments = Arrays.asList(fullUrl.split("/"))
                                  .stream()
                                  .filter(seg -> seg != null && !seg.isEmpty())
                                  .collect(Collectors.toList());
    return "/" + String.join("/", segments);
  }

  public static String getDiFromRi(String ri) {
    return Optional.ofNullable(ri).map(res -> {
      int index = ri.indexOf("/");
      if (index < 1) {
        return null;
      }
      return ri.substring(0, index);
    }).orElse(null);
  }

  public static String getResUriFromRi(String ri) {
    return Optional.ofNullable(ri).map(res -> {
      int index = ri.indexOf("/");
      if (index < 1) {
        return null;
      }
      return ri.substring(index);
    }).orElse(null);
  }

  public static class PayloadCheckResult {
    // pass payload check or not
    private boolean pass;
    // if it is failed, provide error information
    // if it is pass, errInfo is "pass"
    private String errInfo;
    // payload object
    private Object obj;

    public boolean isPass() {
      return pass;
    }

    public void setPass(boolean pass) {
      this.pass = pass;
    }

    public String getErrInfo() {
      return errInfo;
    }

    public void setErrInfo(String errInfo) {
      this.errInfo = errInfo;
    }

    public Object getObj() {
      return obj;
    }

    public void setObj(Object obj) {
      this.obj = obj;
    }

    public static PayloadCheckResult failed(String errInfo) {
      PayloadCheckResult result = new PayloadCheckResult();
      result.pass = false;
      result.errInfo = errInfo;
      return result;
    }

    public static PayloadCheckResult passed(Object obj) {
      PayloadCheckResult result = new PayloadCheckResult();
      result.pass = true;
      result.obj = obj;
      result.errInfo = "pass";
      return result;
    }
  }

  public static PayloadCheckResult checkPayload(byte[] payload, Class objClass, Validator validator,
                                                Class validateClass) {
    Optional<byte[]> payloadOpt = Optional.ofNullable(payload);
    if (!payloadOpt.isPresent()) {
      return PayloadCheckResult.failed("{\"payload\": \"can not be " + "null\"}");
    }

    Optional<Object> objOpt = payloadOpt.map(p -> {
      try {
        ObjectMapper objMapper = new ObjectMapper();
        // ignore all unexpected fields
        objMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objMapper.readValue(p, objClass);
      } catch (IOException e) {
        logger.warn(getStackTrace(e));
        return PayloadCheckResult.failed("{\"payload\" : \"can not deserialize in JSON\"}");
      }
    });
    if (!objOpt.isPresent()) {
      return PayloadCheckResult.failed("{\"payload\" : \"deserialization get nothing\"}");
    }

    Object objInPayload = objOpt.get();
    System.out.println("the object in the payload " + objInPayload);

    if (objClass.isArray()) {
      Object[] objectArray = (Object[]) objInPayload;
      for (Object obj : objectArray) {
        Map<String, String> validateResult = validate(validator, obj, validateClass);
        System.out.println("validateResult " + validateResult);
        if (!validateResult.isEmpty()) {
          return PayloadCheckResult.failed(validateResult.toString());
        }
      }
    } else if (objClass.isAssignableFrom(Collection.class)) {
      Collection<Object> objectCollection = (Collection<Object>) objInPayload;
      for (Object obj : objectCollection) {
        Map<String, String> validateResult = validate(validator, obj, validateClass);
        if (!validateResult.isEmpty()) {
          return PayloadCheckResult.failed(validateResult.toString());
        }
      }
    } else {
      Map<String, String> validateResult = validate(validator, objInPayload, validateClass);
      if (!validateResult.isEmpty()) {
        return PayloadCheckResult.failed(validateResult.toString());
      }
    }

    return PayloadCheckResult.passed(objOpt.get());
  }

  // TODO: should be a part of enum
  public static String detectDataType(Object data) {
    if (data instanceof Integer) {
      return ConstDef.DT_INT;
    } else if (data instanceof Float || data instanceof Double) {
      return ConstDef.DT_FLOAT;
    } else if (data instanceof Byte || data instanceof Character) {
      return ConstDef.DT_BYTE;
    } else if (data instanceof String) {
      return ConstDef.DT_STRING;
    } else if (data instanceof Boolean) {
      return ConstDef.DT_BOOLEAN;
    } else {
      return ConstDef.DT_OPAQUE;
    }
  }

  /**
   * replace or clear dst based on src. if src is null, do nothing. if src is empty, clear dst. if
   * src is not empty, replace dst content with src content
   *
   * @param dst, nullable
   * @param src, nullable
   * @return
   */
  public static <T> Collection<T> replaceOrClear(final Collection<T> src, Collection<T> dst) {
    if (src == null) {
      return dst;
    }

    if (src.isEmpty()) {
      return null;
    } else {
      return src;
    }
  }

  /**
   * update or insert item into list if item is null, do nothing if a item is contained in list,
   * update list. if item is not contained in list, insert into list.
   *
   * @param src, nullable
   * @param item, nullable
   * @param <T>
   * @return
   */
  public static <T> Collection<T> insertOrUpdate(Collection<T> src, final T item) {
    if (item == null) {
      return src;
    }

    src = src == null ? new LinkedList() : src;
    if (src.contains(item)) {
      src.remove(item);
    }
    src.add(item);

    return src;
  }

  /**
   * to replace dst fields with src fields. if src is null, do nothing for all fields. replace dst
   * with src only if they are not null.
   *
   * @param src, nullable
   * @param dst, not null
   * @return
   */
  public static Object replace(final Object src, final Object dst) {
    if (src == null) {
      return dst;
    }

    if (dst == null) {
      return src;
    }

    if (!src.getClass().equals(dst.getClass())) {
      return dst;
    }

    if (src.getClass().isInterface()) {
      return dst;
    }

    Class<?> type = dst.getClass();
    if (type.isEnum() || type.isPrimitive() || type.isArray() || type == Boolean.class
        || type == Byte.class || type == Character.class || type == Short.class
        || type == Integer.class || type == Long.class || type == Double.class
        || type == Float.class) {
      return src;
    }

    // an empty string means remove
    if (type == String.class) {
      if (((String) src).isEmpty()) {
        return null;
      } else {
        return src;
      }
    }

    if (Collection.class.isAssignableFrom(type)) {
      return replaceOrClear((Collection) src, (Collection) dst);
    }

    Object replaced = null;
    try {
      replaced = type.getConstructor().newInstance();
    } catch (NoSuchMethodException | IllegalAccessException | InstantiationException
        | InvocationTargetException e) {
      logger.warn("new instance failed " + BaseUtil.getStackTrace(e));
      return null;
    }

    Field[] fields = type.getDeclaredFields();
    for (Field field : fields) {
      try {
        field.setAccessible(true);

        logger.debug(String.format("--> process the field name=%s, type=%s, value=%s -?-> %s\n",
                                   field.getName(),
                                   field.getType(),
                                   field.get(src),
                                   field.get(dst)));

        // if a field is untouchable, using the value of the dst
        if (field.isAnnotationPresent(Untouchable.class)) {
          field.set(replaced, field.get(dst));
          continue;
        }

        field.set(replaced, replace(field.get(src), field.get(dst)));
      } catch (IllegalAccessException e) {
        logger.warn("set(...) failed " + BaseUtil.getStackTrace(e));
        return null;
      }
    }

    return replaced;
  }

  /**
   * update dst fields with src fields. if src is null, do nothing for all fields. update or insert
   * dst ones with src ones only if they are not null.
   *
   * @param src, nullable
   * @param dst, not null
   * @return
   */
  public static Object update(final Object src, final Object dst) {
    if (src == null) {
      return dst;
    }

    if (dst == null) {
      return src;
    }

    if (!src.getClass().equals(dst.getClass())) {
      return dst;
    }

    if (src.getClass().isInterface()) {
      return dst;
    }

    Class<?> type = dst.getClass();
    if (type.isEnum() || type.isPrimitive() || type.isArray() || type == String.class
        || type == Boolean.class || type == Byte.class || type == Character.class
        || type == Short.class || type == Integer.class || type == Long.class
        || type == Double.class || type == Float.class) {
      return src;
    }

    if (Collection.class.isAssignableFrom(type)) {
      return insertOrUpdate((Collection) src, (Collection) dst);
    }

    Object updated = null;
    try {
      updated = type.getConstructor().newInstance();
    } catch (NoSuchMethodException | IllegalAccessException | InstantiationException
        | InvocationTargetException e) {
      logger.warn("new instance failed " + BaseUtil.getStackTrace(e));
      return null;
    }

    Field[] fields = type.getDeclaredFields();
    for (Field field : fields) {
      try {
        field.setAccessible(true);
        type = field.getType();

        logger.debug(String.format("--> process the field name=%s, type=%s, value=%s\n",
                                   field.getName(),
                                   type,
                                   field.get(src)));

        if (field.get(src) == null) {
          field.set(updated, field.get(dst));
          continue;
        }

        if (field.isAnnotationPresent(Untouchable.class)) {
          field.set(updated, field.get(dst));
          continue;
        }

        field.set(updated, update(field.get(src), field.get(dst)));

      } catch (IllegalAccessException e) {
        logger.warn("set(...) failed " + BaseUtil.getStackTrace(e));
        return null;
      }
    }

    return updated;
  }

  public static String removeTrailingSlash(String url) {
    return (url.startsWith("/") ? "/" : "") + Arrays.asList(url.split("/"))
                                                    .stream()
                                                    .filter(s -> s.length() > 0)
                                                    .collect(Collectors.joining("/"));
  }


  /**
   * it will copy every matched (with same name) property from source to target and ignore all collection properties with null values
   * since in RestAPIs we always use below rules on a collection property, like a list, a set or a map
   * - null to represent do not touch
   * - empty to represent clear all
   * - [...]/XX to represent replace the original value with the new value
   *
   * it should be considered during updating
   * @param source
   * @param target
   */
  public static void copyPropertiesIgnoreCollectionNull(Object source, Object target) {
    BeanWrapperImpl wrapper = new BeanWrapperImpl(source);
    PropertyDescriptor[] pds = wrapper.getPropertyDescriptors();

    // do not touch all null collection properties
    Set<String> nullPropertyNames = new HashSet<String>();
    for (PropertyDescriptor pd : pds) {
      if (Collection.class.isAssignableFrom(wrapper.getPropertyType(pd.getName()))
          && Objects.isNull(wrapper.getPropertyValue(pd.getName()))) {
        nullPropertyNames.add(pd.getName());
      }
    }

    BeanUtils.copyProperties(source,
                             target,
                             nullPropertyNames.toArray(new String[nullPropertyNames.size()]));

    // remove empty collection properties from target
    wrapper = new BeanWrapperImpl(target);
    pds = wrapper.getPropertyDescriptors();

    for (PropertyDescriptor pd : pds) {
      if (Collection.class.isAssignableFrom(wrapper.getPropertyType(pd.getName()))
          && Objects.nonNull(wrapper.getPropertyValue(pd.getName()))
          && ((Collection) wrapper.getPropertyValue(pd.getName())).isEmpty()) {
        wrapper.setPropertyValue(pd.getName(), null);
      }
    }
  }

  public static void copyPropertiesIgnoreAllNull(Object source, Object target) {
    BeanWrapperImpl wrapper = new BeanWrapperImpl(source);
    PropertyDescriptor[] pds = wrapper.getPropertyDescriptors();

    // do not touch all null collection properties
    Set<String> nullPropertyNames = new HashSet<String>();
    for (PropertyDescriptor pd : pds) {
      if (Objects.isNull(wrapper.getPropertyValue(pd.getName()))) {
        nullPropertyNames.add(pd.getName());
      }
    }

    BeanUtils.copyProperties(source,
                             target,
                             nullPropertyNames.toArray(new String[nullPropertyNames.size()]));

    // remove empty collection properties from the target
    // remove empty string properties form the target
    wrapper = new BeanWrapperImpl(target);
    pds = wrapper.getPropertyDescriptors();

    for (PropertyDescriptor pd : pds) {
      if (Collection.class.isAssignableFrom(wrapper.getPropertyType(pd.getName()))
          && Objects.nonNull(wrapper.getPropertyValue(pd.getName()))
          && ((Collection) wrapper.getPropertyValue(pd.getName())).isEmpty()) {
        wrapper.setPropertyValue(pd.getName(), null);
      }

      if (String.class.isAssignableFrom(wrapper.getPropertyType(pd.getName()))
          && Objects.nonNull(wrapper.getPropertyValue(pd.getName()))
          && ((String) wrapper.getPropertyValue(pd.getName())).isEmpty()) {
        wrapper.setPropertyValue(pd.getName(), null);
      }
    }
  }
}
