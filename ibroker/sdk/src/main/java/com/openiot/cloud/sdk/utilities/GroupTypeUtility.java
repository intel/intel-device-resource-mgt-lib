/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.sdk.utilities;

import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.dao.GroupRepository;
import com.openiot.cloud.base.mongo.dao.GroupTypeRepository;
import com.openiot.cloud.base.mongo.model.GroupType;
import com.openiot.cloud.base.mongo.model.help.AttributeEntity;
import com.openiot.cloud.base.mongo.model.help.ConfigurationEntity;
import com.openiot.cloud.base.mongo.model.help.DataSourceEntity;
import com.openiot.cloud.sdk.service.ApplicationContextProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component(value = "groupTypeUtility")
public class GroupTypeUtility {
  @Autowired
  GroupTypeRepository grptRepo;

  @Autowired
  GroupRepository grpRepo;

  public static GroupTypeUtility getInstance() {
    return ApplicationContextProvider.getBean(GroupTypeUtility.class);
  }

  /**
   * Method to create a new GroupType with specified group type name.
   *
   * @param name This parameter is the specified name of group type.
   * @return the GroupType with assigned internal group type id.
   * @see com.openiot.cloud.base.mongo.model.GroupType
   */
  public GroupType addGroupType(String project, String name, String displayName) {
    return addOrUpdateGroupType(new GroupType().setN(name).setDpn(displayName).setPrj(project));
  }

  /**
   * Method to create a new GroupType with specified GroupType object.
   *
   * @param gt GroupType object that without group type id.
   * @return the GroupType with assigned internal group type id.
   * @see com.openiot.cloud.base.mongo.model.GroupType
   */
  public GroupType addOrUpdateGroupType(GroupType gt) {
    return grptRepo.createOrUpdateOne(gt);
  }

  /**
   * Method to get GroupType with specified group type name.
   *
   * @param name This parameter is the specified name of group type.
   * @return the group type searched with specified name.
   * @see com.openiot.cloud.base.mongo.model.GroupType
   */
  public GroupType getGroupTypeByName(String name) {
    return grptRepo.findOneByN(name);
  }

  /**
   * Method to get all GroupTypes with specified conditions.<br>
   * [note]:<br>
   * 1. all parameters are optional, which means that any parameter can be set null. If it is null,
   * then this <br>
   * condition take no effect.<br>
   * 2. returned data is the result of all conditions are matched.<br>
   * <br>
   *
   * @param with_attrs The name list of the attributes that the GroupType shall contain.
   * @param with_dss The name list of the data source that the GroupType shall contain.
   * @param with_cfgs The name list of the configure properties that the GroupType shall contain.
   * @param attrsValues The value condition list of attributes that the GroupType shall contain,
   *     example: attr.temp=">40"
   * @param cfgsValues The value condition list of configure properties that the GroupType shall
   *     contain, example: cfg.humidity="<70"
   * @return List of all group types that match all specified conditions.
   * @see com.openiot.cloud.base.mongo.model.GroupType
   */
  public List<GroupType> getGroupTypeWith(String project, List<String> attrNames,
                                          List<String> cfgNames, List<String> dssNames,
                                          Map<String, String> attrsMap,
                                          Map<String, String> cfgsMap) {
    return grptRepo.filter(Optional.ofNullable(project),
                           Optional.empty(),
                           Optional.empty(),
                           attrNames,
                           cfgNames,
                           dssNames,
                           attrsMap,
                           cfgsMap,
                           GroupType.allFields(),
                           new PageRequest(0, ConstDef.MAX_SIZE));
  }

  // TODO: UPDATE
  /**
   * Method to update the GroupType with specified list of attributes.
   *
   * @param gtName name of the group type to be updated
   * @param attrs list of name and value pair, for existing attribute name, value will be update to
   *     new value, for new attribute name, new attribute with value will be added into this group
   *     type
   * @return 0: success 1:failed.
   * @see com.openiot.cloud.base.mongo.model.GroupType
   */
  public boolean updateGroupTypeAttr(String gtName, Map<String, String> attrs) {
    return Optional.ofNullable(grptRepo.findOneByN(gtName)).map(gt -> {
      return Optional.ofNullable(attrs)
                     .filter(as -> !as.isEmpty())
                     .map(as -> as.entrySet()
                                  .stream()
                                  .map(entry -> new AttributeEntity(entry.getKey(),
                                                                    entry.getValue()))
                                  .collect(Collectors.toList()))
                     .map(as -> {
                       as.forEach(a -> gt.insertOrUpdateAs(a));
                       grptRepo.save(gt);
                       return true;
                     })
                     .orElse(false);
    }).orElse(false);
  }

  /**
   * Method to update the GroupType with specified list of configure properties.
   *
   * @param gtName name of the group type to be updated
   * @param configure properties list of name and value pair, for existing configure property name,
   *     value will be update to new value, for new configure property name, new configure property
   *     with value will be added into this group type
   * @return 0: success 1:failed.
   * @see com.openiot.cloud.base.mongo.model.GroupType
   */
  public boolean updateGroupTypeCfg(String gtName, Map<String, String> cfgs) {
    return Optional.ofNullable(grptRepo.findOneByN(gtName)).map(gt -> {
      return Optional.ofNullable(cfgs)
                     .filter(cs -> !cs.isEmpty())
                     .map(cs -> cs.entrySet()
                                  .stream()
                                  .map(entry -> new ConfigurationEntity(entry.getKey(),
                                                                        entry.getValue()))
                                  .collect(Collectors.toList()))
                     .map(cs -> {
                       cs.forEach(c -> gt.insertOrUpdateCs(c));
                       grptRepo.save(gt);
                       return true;
                     })
                     .orElse(false);
    }).orElse(false);
  }

  /**
   * Method to update the GroupType with specified list of data source.
   *
   * @param gtName name of the group type to be updated
   * @param data source list of name and value pair, for existing data source name, value will be
   *     update to new value, for new data source name, new data source with value will be added
   *     into this group type
   * @return 0: success 1:failed.
   * @see com.openiot.cloud.base.mongo.model.GroupType
   */
  public boolean updateGroupTypeDss(String gtName, Map<String, String> dtsrcs) {
    return Optional.ofNullable(grptRepo.findOneByN(gtName)).map(gt -> {
      return Optional.ofNullable(dtsrcs)
                     .filter(dss -> !dss.isEmpty())
                     .map(dss -> dss.entrySet().stream().map(entry -> {
                       DataSourceEntity ds = new DataSourceEntity(entry.getKey(), entry.getValue());
                       return ds;
                     }).collect(Collectors.toList()))
                     .map(dss -> {
                       dss.forEach(ds -> gt.insertOrUpdateDss(ds));
                       grptRepo.save(gt);
                       return true;
                     })
                     .orElse(false);
    }).orElse(false);
  }

  /**
   * Method to replace all attributes of the GroupType with the new ones.
   *
   * @param gtName name of the group type to be updated
   * @param attributes list of name and value pair to be replaced with
   * @return 0: success 1:failed.
   * @see com.openiot.cloud.base.mongo.model.GroupType
   */
  public boolean replaceGroupTypeAttr(String gtName, Map<String, String> attrs) {
    return Optional.ofNullable(grptRepo.findOneByN(gtName)).map(gt -> {
      return Optional.ofNullable(attrs)
                     .filter(as -> !as.isEmpty())
                     .map(as -> as.entrySet()
                                  .stream()
                                  .map(entry -> new AttributeEntity(entry.getKey(),
                                                                    entry.getValue()))
                                  .collect(Collectors.toList()))
                     .map(as -> {
                       gt.setAs(as);
                       grptRepo.save(gt);
                       return true;
                     })
                     .orElse(false);
    }).orElse(false);
  }

  /**
   * Method to replace all configure properties of the GroupType with the new ones.
   *
   * @param gtName name of the group type to be updated
   * @param configure properties list of name and value pair to be replaced with
   * @return 0: success 1:failed.
   * @see com.openiot.cloud.base.mongo.model.GroupType
   */
  public boolean replaceGroupTypeCfg(String gtName, Map<String, String> cfgs) {
    return Optional.ofNullable(grptRepo.findOneByN(gtName)).map(gt -> {
      return Optional.ofNullable(cfgs)
                     .filter(cs -> !cs.isEmpty())
                     .map(cs -> cs.entrySet()
                                  .stream()
                                  .map(entry -> new ConfigurationEntity(entry.getKey(),
                                                                        entry.getValue()))
                                  .collect(Collectors.toList()))
                     .map(cs -> {
                       gt.setCs(cs);
                       grptRepo.save(gt);
                       return true;
                     })
                     .orElse(false);
    }).orElse(false);
  }

  /**
   * Method to replace all data sources of the GroupType with the new ones.
   *
   * @param gtName name of the group type to be updated
   * @param data source list of name and value pair to be replaced with
   * @return 0: success 1:failed.
   * @see com.openiot.cloud.base.mongo.model.GroupType
   */
  public boolean replaceGroupTypeDss(String gtName, Map<String, String> dtsrcs) {
    return Optional.ofNullable(grptRepo.findOneByN(gtName)).map(gt -> {
      return Optional.ofNullable(dtsrcs)
                     .filter(dss -> !dss.isEmpty())
                     .map(dss -> dss.entrySet().stream().map(entry -> {
                       DataSourceEntity ds = new DataSourceEntity(entry.getKey(), entry.getValue());
                       return ds;
                     }).collect(Collectors.toList()))
                     .map(dss -> {
                       gt.setDss(dss);
                       grptRepo.save(gt);
                       return true;
                     })
                     .orElse(false);
    }).orElse(false);
  }

  /**
   * Method to delete all attributes with specified name in the GroupType.
   *
   * @param gtName name of the group type to be updated
   * @param attribute list of name to be deleted
   * @return 0: success 1:failed.
   * @see com.openiot.cloud.base.mongo.model.GroupType
   */
  public boolean deleteGroupTypeAttr(String gtName, List<String> attrs) {
    return Optional.ofNullable(grptRepo.findOneByN(gtName)).map(gt -> {
      return Optional.ofNullable(attrs)
                     .filter(as -> !as.isEmpty())
                     .map(as -> as.stream()
                                  .map(attrName -> new AttributeEntity(attrName, null))
                                  .collect(Collectors.toList()))
                     .map(as -> {
                       gt.getAs().removeAll(as);
                       grptRepo.save(gt);
                       return true;
                     })
                     .orElse(false);
    }).orElse(false);
  }

  /**
   * Method to delete all configure properties with specified name in the GroupType.
   *
   * @param gtName name of the group type to be updated
   * @param configure property list of name to be deleted
   * @return 0: success 1:failed.
   * @see com.openiot.cloud.base.mongo.model.GroupType
   */
  public boolean deleteGroupTypeCfg(String gtName, List<String> cfgs) {
    return Optional.ofNullable(grptRepo.findOneByN(gtName)).map(gt -> {
      return Optional.ofNullable(cfgs)
                     .filter(cs -> !cs.isEmpty())
                     .map(cs -> cs.stream()
                                  .map(cfgName -> new ConfigurationEntity(cfgName, null))
                                  .collect(Collectors.toList()))
                     .map(cs -> {
                       gt.getCs().removeAll(cs);
                       grptRepo.save(gt);
                       return true;
                     })
                     .orElse(false);
    }).orElse(false);
  }

  /**
   * Method to delete all data sources with specified name in the GroupType.
   *
   * @param gtName name of the group type to be updated
   * @param data source list of name to be deleted
   * @return 0: success 1:failed.
   * @see com.openiot.cloud.base.mongo.model.GroupType
   */
  public boolean deleteGroupTypeDss(String gtName, Map<String, String> dtsrcs) {
    return Optional.ofNullable(grptRepo.findOneByN(gtName)).map(gt -> {
      return Optional.ofNullable(dtsrcs)
                     .filter(dss -> !dss.isEmpty())
                     .map(dss -> dss.entrySet().stream().map(entry -> {
                       DataSourceEntity ds = new DataSourceEntity(entry.getKey(), entry.getValue());
                       return ds;
                     }).collect(Collectors.toList()))
                     .map(dss -> {
                       gt.getDss().removeAll(dss);
                       grptRepo.save(gt);
                       return true;
                     })
                     .orElse(false);
    }).orElse(false);
  }

  /**
   * Method to delete the GroupType with specified name.
   *
   * @param name name of the GroupType to be deleted
   * @return 0: success 1:failed.
   * @see com.openiot.cloud.base.mongo.model.GroupType
   */
  public boolean deleteGroupTypeByName(String name) {
    return Optional.ofNullable(name).map(n -> grptRepo.findOneByN(n)).map(g -> {
      grptRepo.delete(g);
      return true;
    }).orElse(false);
  }
}
