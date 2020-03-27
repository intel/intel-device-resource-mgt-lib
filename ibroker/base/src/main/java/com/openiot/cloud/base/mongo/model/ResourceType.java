/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.model;

import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.help.Untouchable;
import com.openiot.cloud.base.mongo.model.OcfRTDefinition.PropDefEntry;
import com.openiot.cloud.base.mongo.model.OcfRTDefinition.ResDefEntry;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Document(collection = ConstDef.C_RESTYPE)
public class ResourceType {

  @Untouchable
  @Id
  String id;

  @Untouchable
  @Field(ConstDef.F_NAME)
  String name;

  @Field(ConstDef.F_TITLE)
  String title;

  @Field(ConstDef.F_DSCRB)
  String description;

  @Field(ConstDef.F_PRODEFS)
  List<PropertyType> propTypes;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<PropertyType> getPropTypes() {
    return propTypes;
  }

  public void setPropTypes(List<PropertyType> proDefs) {
    this.propTypes = proDefs;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public static class PropertyType {

    @Untouchable
    @Field(ConstDef.F_SHTNAME)
    String shortName;

    @Untouchable
    @Field(ConstDef.F_NAME)
    String name;

    @Field(ConstDef.F_MAND)
    Boolean mandatory;

    @Field(ConstDef.F_ACCESS)
    String access;

    @Field(ConstDef.F_TYPE)
    String type;

    @Field(ConstDef.F_UNIT)
    String unit;

    @Field(ConstDef.F_DSCRB)
    String decription;

    public PropertyType() {
      this.mandatory = false;
    }

    public String getShortName() {
      return shortName;
    }

    public void setShortName(String shortName) {
      this.shortName = shortName;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Boolean getMandatory() {
      return mandatory;
    }

    public void setMandatory(Boolean mandatory) {
      this.mandatory = mandatory;
    }

    public String getAccess() {
      return access;
    }

    public void setAccess(String access) {
      this.access = access;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getUnit() {
      return unit;
    }

    public void setUnit(String unit) {
      this.unit = unit;
    }

    public String getDecription() {
      return decription;
    }

    public void setDecription(String decription) {
      this.decription = decription;
    }

    @Override
    public String toString() {
      return "PropertyType [shortName=" + shortName + ", name=" + name + ", mandatory=" + mandatory
          + ", access=" + access + ", type=" + type + ", unit=" + unit + ", decription="
          + decription + "]";
    }

    public static PropertyType from(PropDefEntry rtPropDef) {
      PropertyType pt = new PropertyType();
      // name has to be non-null
      pt.setName(rtPropDef.getName());
      pt.setShortName(rtPropDef.getName());

      // type has to be non-null
      pt.setType(rtPropDef.getType());

      // readOnly could be null, but has a default value
      // so, non-null
      pt.setAccess(rtPropDef.isReadOnly() ? "r" : "rw");

      // nullable
      Optional.ofNullable(rtPropDef.getDescription()).ifPresent(desc -> pt.setDecription(desc));
      return pt;
    }
  }

  @Override
  public String toString() {
    return "ResourceType{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", title='" + title
        + '\'' + ", description='" + description + '\'' + ", propTypes=" + propTypes + '}';
  }

  public static ResourceType from(OcfRTDefinition rtDef) {
    ResourceType rt = new ResourceType();

    // definitions has to be non-null
    ResDefEntry rtResDef = rtDef.getDefinitions();

    // rt has to be non-null
    rt.setName(rtResDef.getRt());

    // use rt if there is no title
    rt.setTitle(Optional.ofNullable(rtDef.getTitle()).orElse(rtResDef.getRt()));

    // nullable
    Optional.ofNullable(rtResDef.getDescription()).ifPresent(desc -> rt.setDescription(desc));

    Optional.ofNullable(rtResDef.getProperties())
            .filter(propsDef -> !propsDef.isEmpty())
            .map(propsDef -> propsDef.stream()
                                     .map(propDef -> PropertyType.from(propDef))
                                     .collect(Collectors.toList()))
            .ifPresent(propTypes -> rt.setPropTypes(propTypes));

    Optional.ofNullable(rtDef.getRequired())
            .filter(requiredProps -> !requiredProps.isEmpty())
            .ifPresent(requiredProps -> {
              Optional.ofNullable(rt.getPropTypes())
                      .ifPresent(propTypes -> propTypes.stream()
                                                       .filter(propType -> requiredProps.contains(propType.getName()))
                                                       .forEach(propType -> propType.setMandatory(true)));
            });

    return rt;
  }

  // replace dst fields with src fields
  public static ResourceType repleace(ResourceType src, ResourceType dst) {
    if (src == null) {
      return dst;
    }

    ResourceType result = new ResourceType();
    // use dst _id firstly, since id is not changeable
    result.setId(dst.getId());
    // use dst name firstly, since name is not changeable
    result.setName(dst.getName());

    // otherwise, use src fields firstly
    result.setTitle(Optional.ofNullable(src.getTitle()).orElse(dst.getTitle()));
    result.setDescription(Optional.ofNullable(src.getDescription()).orElse(dst.getDescription()));
    result.setPropTypes(Optional.ofNullable(src.getPropTypes()).orElse(dst.getPropTypes()));
    return result;
  }
}
