/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.model;

import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.help.ConstDef;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = ConstDef.C_PRODATA)
public class ProDataMongo {
  @Id
  private String id;

  @Field(ConstDef.F_PROPERTY)
  private ObjectId propertyId;

  @Field(ConstDef.F_FULLURL)
  @Indexed(background = true)
  private String fullurl;

  @Field(ConstDef.F_DEVID)
  private String devId;

  @Field(ConstDef.F_RESURI)
  private String resUrl;

  @Field(ConstDef.F_PROPNAME)
  private String propName;

  @Field(ConstDef.F_TIME)
  @Indexed(background = true)
  private
  // epoch milliseconds
  long time;

  @Field(ConstDef.F_DATA)
  private Object data;

  // TODO: remove
  @Field(ConstDef.F_DATATYPE)
  private String dataType;

  // TODO: remove
  @Field(ConstDef.F_HELPERSTR)
  private String helper;

  public ProDataMongo() {}

  public ProDataMongo(ObjectId propertyId, String devId, String resUrl, long time, Object data) {
    this.propertyId = propertyId;
    this.devId = devId;
    this.resUrl = resUrl;
    this.propName = propName;
    this.time = time;
    this.data = data;
    this.fullurl = BaseUtil.formAFullUrl(devId, resUrl);
  }

  public ProDataMongo(ObjectId propertyId, String devId, String resUrl, String propName, long time,
      Object data) {
    this.propertyId = propertyId;
    this.devId = devId;
    this.resUrl = resUrl;
    this.propName = propName;
    this.time = time;
    this.data = data;
    this.fullurl = BaseUtil.formAFullUrl(devId, resUrl, propName);
  }

  @Deprecated
  public ProDataMongo(ObjectId propertyId, String fullurl, long time, String dataType, Object data,
      String helper) {
    this.setPropertyId(propertyId);
    this.setFullurl(fullurl);
    this.setTime(time);
    this.setDataType(dataType);
    this.setData(data);
    this.setHelper(helper);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public ObjectId getPropertyId() {
    return propertyId;
  }

  public void setPropertyId(ObjectId propertyId) {
    this.propertyId = propertyId;
  }

  public long getTime() {
    return time;
  }

  public void setTime(long time) {
    this.time = time;
  }

  public Object getData() {
    return data;
  }

  public void setData(Object data) {
    this.data = data;
  }

  public String getFullurl() {
    return fullurl;
  }

  public void setFullurl(String fullurl) {
    this.fullurl = fullurl;
  }

  public String getDataType() {
    return dataType;
  }

  public void setDataType(String dataType) {
    this.dataType = dataType;
  }

  public String getHelper() {
    return helper;
  }

  public void setHelper(String helper) {
    this.helper = helper;
  }

  public String getDevId() {
    return devId;
  }

  public void setDevId(String devId) {
    this.devId = devId;
  }

  public String getResUrl() {
    return resUrl;
  }

  public void setResUrl(String resUrl) {
    this.resUrl = resUrl;
  }

  public String getPropName() {
    return propName;
  }

  public void setPropName(String propName) {
    this.propName = propName;
  }

  @Override
  public String toString() {
    return "ProDataMongo{" + "id='" + id + '\'' + ", propertyId=" + propertyId + ", fullurl='"
        + fullurl + '\'' + ", devId='" + devId + '\'' + ", resUrl='" + resUrl + '\''
        + ", propName='" + propName + '\'' + ", time=" + time + ", data=" + data + ", dataType='"
        + dataType + '\'' + ", helper='" + helper + '\'' + '}';
  }

  // TODO: change to UserDataMongo
  @Deprecated
  public static ProDataMongo from(long time, String dataType, Object data, String helper) {
    return new ProDataMongo(null, null, time, dataType, data, helper);
  }
}
