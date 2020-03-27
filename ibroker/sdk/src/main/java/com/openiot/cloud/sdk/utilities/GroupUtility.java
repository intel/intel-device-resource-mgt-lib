/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.sdk.utilities;

import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.dao.GroupRepository;
import com.openiot.cloud.base.mongo.dao.GroupTypeRepository;
import com.openiot.cloud.base.mongo.model.Group;
import com.openiot.cloud.base.mongo.model.Group.MemberResRef;
import com.openiot.cloud.base.mongo.model.help.AttributeEntity;
import com.openiot.cloud.base.mongo.model.help.ConfigurationEntity;
import com.openiot.cloud.base.mongo.model.help.DataSourceEntity;
import com.openiot.cloud.sdk.service.ApplicationContextProvider;
import com.openiot.cloud.sdk.event.TaskOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The utility to access groups in openiot cloud.<br>
 * <br>
 *
 * @author saigon
 * @version 1.0
 * @since Oct 2017
 */
@Component
public class GroupUtility {
  private static final Logger logger = LoggerFactory.getLogger(GroupUtility.class.getName());
  @Autowired
  GroupTypeRepository grptRepo;

  @Autowired
  GroupRepository grpRepo;

  @Autowired
  TaskOperations taskOps;

  public static GroupUtility getInstance() {
    return ApplicationContextProvider.getBean(GroupUtility.class);
  }

  /**
   * Method to create a new Group with specified group name and group type name.
   *
   * @param name This parameter is the specified name of group.
   * @param gtName This parameter is the specified name of group type.
   * @return the Group with assigned internal group id.
   * @see com.openiot.cloud.base.mongo.model.Group
   */
  public Group addGroup(String project, String name, String displayName, String gtName) {
    return addOrUpdateGroup(new Group().setN(name)
                                       .setDpn(displayName)
                                       .setGt(gtName)
                                       .setPrj(project));
  }

  /**
   * Method to create a new GroupType with specified Group object.
   *
   * @param group Group object that without group id.
   * @return the Group with assigned internal group id.
   * @see com.openiot.cloud.base.mongo.model.Group
   */
  public Group addOrUpdateGroup(Group group) {
    return Optional.ofNullable(grpRepo.createOrUpdateOne(group)).map(g -> {
      taskOps.createTask("CFG_MONITOR",
                         ConstDef.EVENT_TYPE_CFG_SYNC,
                         null,
                         ConstDef.EVENT_TARGET_TYPE_GROUP,
                         group.getN(),
                         ConstDef.DAY_SECONDS,
                         null,
                         null);
      return g;
    }).orElse(null);
  }

  /**
   * Method to get Group with specified group name.
   *
   * @param name This parameter is the specified name of group.
   * @return the group searched with specified name.
   * @see com.openiot.cloud.base.mongo.model.Group
   */
  public Group getGroupByName(String name) {
    return grpRepo.findOneByName(name);
  }

  /**
   * Method to get all Groups with specified conditions.<br>
   * [note]:<br>
   * 1. all parameters are optional, which means that any parameter can be set null. If it is null,
   * then this <br>
   * condition take no effect.<br>
   * 2. returned data is the result of all conditions are matched.<br>
   * <br>
   *
   * @param gtName The name of Group Type that this group belong to.
   * @param with_attrs The name list of the attributes that the Group shall contain.
   * @param with_dss The name list of the data source that the Group shall contain.
   * @param with_cfgs The name list of the configure properties that the Group shall contain.
   * @return List of all groups that match all specified conditions.
   * @see com.openiot.cloud.base.mongo.model.Group
   */
  public List<Group> getGroupWithValue(String projectId, String gName, String gtName, String dpName,
                                       Map<String, String> with_attrs, Map<String, String> with_dss,
                                       Map<String, String> with_cfgs) {
    return grpRepo.filter(Optional.ofNullable(projectId),
                          Optional.ofNullable(gName).map(gn -> Arrays.asList(new String[] {gn})),
                          Optional.ofNullable(gtName).map(gtn -> Arrays.asList(new String[] {gtn})),
                          Optional.ofNullable(dpName).map(dpn -> Arrays.asList(new String[] {dpn})),
                          Optional.empty(),
                          Optional.empty(),
                          Optional.empty(),
                          Optional.empty(),
                          Optional.empty(),
                          Optional.empty(),
                          Optional.empty(),
                          Optional.ofNullable(with_attrs),
                          Optional.ofNullable(with_cfgs),
                          Optional.of(Group.allFields()),
                          null);
  }

  public List<Group> getGroupWith(String gName, String gtName, String dpName,
                                  List<String> attrNames, List<String> cfgNames,
                                  List<String> dssNames) {
    return grpRepo.filter(Optional.empty(),
                          Optional.ofNullable(gName).map(gn -> Arrays.asList(new String[] {gn})),
                          Optional.ofNullable(gtName).map(gtn -> Arrays.asList(new String[] {gtn})),
                          Optional.ofNullable(dpName).map(dpn -> Arrays.asList(new String[] {dpn})),
                          Optional.empty(),
                          Optional.empty(),
                          Optional.empty(),
                          Optional.empty(),
                          Optional.ofNullable(attrNames),
                          Optional.ofNullable(cfgNames),
                          Optional.ofNullable(dssNames),
                          Optional.empty(),
                          Optional.empty(),
                          Optional.of(Group.allFields()),
                          null);
  }

  /**
   * Method to update the Group with specified list of attributes.
   *
   * @param attrs list of name and value pair, for existing attribute name, value will be update to
   *     new value, for new attribute name, new attribute with value will be added into this group
   * @return 0: success 1:failed.
   * @see com.openiot.cloud.base.mongo.model.Group
   */
  public boolean updateGroupAttr(String gName, Map<String, String> attrs) {
    return Optional.ofNullable(grpRepo.findOneByName(gName)).map(g -> {
      return Optional.ofNullable(attrs)
                     .filter(as -> !as.isEmpty())
                     .map(as -> as.entrySet()
                                  .stream()
                                  .map(entry -> new AttributeEntity(entry.getKey(),
                                                                    entry.getValue()))
                                  .collect(Collectors.toList()))
                     .map(as -> {
                       as.forEach(a -> g.insertOrUpdateAs(a));
                       grpRepo.save(g);
                       taskOps.createTask("CFG_MONITOR",
                                          ConstDef.EVENT_TYPE_CFG_SYNC,
                                          null,
                                          ConstDef.EVENT_TARGET_TYPE_GROUP,
                                          g.getN(),
                                          ConstDef.DAY_SECONDS,
                                          null,
                                          null);
                       return true;
                     })
                     .orElse(false);
    }).orElse(false);
  }

  /**
   * Method to update the Group with specified list of configure properties.
   *
   * @param gName name of the group to be updated
   * @param cfgs properties list of name and value pair, for existing configure property name, value
   *     will be update to new value, for new configure property name, new configure property with
   *     value will be added into this group
   * @return 0: success 1:failed.
   * @see com.openiot.cloud.base.mongo.model.Group
   */
  // TODO need to check
  public boolean updateGroupCfg(String gName, Map<String, String> cfgs) {
    return Optional.ofNullable(grpRepo.findOneByName(gName)).map(g -> {
      return Optional.ofNullable(cfgs)
                     .filter(cs -> !cs.isEmpty())
                     .map(cs -> cs.entrySet()
                                  .stream()
                                  .map(entry -> new ConfigurationEntity(entry.getKey(),
                                                                        entry.getValue()))
                                  .collect(Collectors.toList()))
                     .map(cs -> {
                       cs.forEach(c -> g.insertOrUpdateCs(c));
                       grpRepo.save(g);
                       taskOps.createTask("CFG_MONITOR",
                                          ConstDef.EVENT_TYPE_CFG_SYNC,
                                          null,
                                          ConstDef.EVENT_TARGET_TYPE_GROUP,
                                          g.getN(),
                                          ConstDef.DAY_SECONDS,
                                          null,
                                          null);
                       return true;
                     })
                     .orElse(false);
    }).orElse(false);
  }

  /**
   * Method to update the Group with specified list of data source.
   *
   * @param gName name of the group to be updated
   * @param dtsrcs source list of name and value pair, for existing data source name, value will be
   *     update to new value, for new data source name, new data source with value will be added
   *     into this group
   * @return 0: success 1:failed.
   * @see com.openiot.cloud.base.mongo.model.Group
   */
  public boolean updateGroupDss(String gName, Map<String, String> dtsrcs) {
    return Optional.ofNullable(grpRepo.findOneByName(gName)).map(g -> {
      return Optional.ofNullable(dtsrcs)
                     .filter(dss -> !dss.isEmpty())
                     .map(dss -> dss.entrySet().stream().map(entry -> {
                       DataSourceEntity ds = new DataSourceEntity(entry.getKey(), entry.getValue());
                       return ds;
                     }).collect(Collectors.toList()))
                     .map(dss -> {
                       dss.forEach(ds -> g.insertOrUpdateDss(ds));
                       grpRepo.save(g);
                       taskOps.createTask("CFG_MONITOR",
                                          ConstDef.EVENT_TYPE_CFG_SYNC,
                                          null,
                                          ConstDef.EVENT_TARGET_TYPE_GROUP,
                                          g.getN(),
                                          ConstDef.DAY_SECONDS,
                                          null,
                                          null);
                       return true;
                     })
                     .orElse(false);
    }).orElse(false);
  }

  /**
   * Method to add device or resource entities with specified id into the Group.
   *
   * @param groupName name of the group to be updated
   * @param device id list to be added into the group, this parameter is optional
   * @param resource id list to be added into the group, this parameter is optional
   * @return 0: success 1:failed.
   * @see com.openiot.cloud.base.mongo.model.Group
   */
  // public boolean updateGroupEntities(String gName,
  // Map<String, String> entities) {
  // return Optional.ofNullable(grpRepo.findOneByName(gName)).map(g -> {
  // return Optional.ofNullable(entities)
  // .filter(ms -> !ms.isEmpty())
  // .map(ms -> ms.entrySet().stream()
  // .map(entry -> new EntityRef(
  // entry.getKey(),
  // entry.getValue()))
  // .collect(Collectors.toList()))
  // .map(ms -> {
  // ms.forEach(m -> g.addMsItem(m));
  // grpRepo.save(g);
  // ttrigger.pullAsGroupCfgSync(g.getN());
  // return true;
  // }).orElse(false);
  // }).orElse(false);
  // }

  /**
   * Method to add device members with specified id into the Group.
   *
   * @param gName name of the group to be updated
   * @param resMap id list to be added into the group, this parameter is optional
   * @return 0: success 1:failed.
   * @see com.openiot.cloud.base.mongo.model.Group
   */
  public boolean updateGroupDevMember(String gName, Map<String, String> resMap) {
    return Optional.ofNullable(grpRepo.findOneByName(gName)).map(g -> {
      return Optional.ofNullable(resMap)
                     .filter(resources -> !resources.isEmpty())
                     .map(resources -> resources.entrySet()
                                                .stream()
                                                .map(entry -> new MemberResRef(entry.getKey(),
                                                                               entry.getValue()))
                                                .collect(Collectors.toList()))
                     .map(mr -> {
                       g.insertOrUpdateMrs(mr);
                       grpRepo.save(g);
                       taskOps.createTask("CFG_MONITOR",
                                          ConstDef.EVENT_TYPE_CFG_SYNC,
                                          null,
                                          ConstDef.EVENT_TARGET_TYPE_GROUP,
                                          g.getN(),
                                          ConstDef.DAY_SECONDS,
                                          null,
                                          null);
                       return true;
                     })
                     .orElse(false);
    }).orElse(false);
  }

  /**
   * Method to add resource members with specified device id and resourc uri into the Group.
   *
   * @param gName of the group to be updated
   * @param devIds list to be added into the group, this parameter is optional
   * @return 0: success 1:failed.
   * @see com.openiot.cloud.base.mongo.model.Group
   */
  public boolean updateGroupResMember(String gName, List<String> devIds) {
    return Optional.ofNullable(grpRepo.findOneByName(gName)).map(g -> {
      return Optional.ofNullable(devIds).map(devs -> {
        g.insertOrUpdateMds(devs);
        grpRepo.save(g);
        taskOps.createTask("CFG_MONITOR",
                           ConstDef.EVENT_TYPE_CFG_SYNC,
                           null,
                           ConstDef.EVENT_TARGET_TYPE_GROUP,
                           g.getN(),
                           ConstDef.DAY_SECONDS,
                           null,
                           null);
        return true;
      }).orElse(false);
    }).orElse(false);
  }

  /**
   * Method to replace all attributes of the Group with the new ones.
   *
   * @param gName name of the group to be updated
   * @param attrs list of name and value pair to be replaced with
   * @return 0: success 1:failed.
   * @see com.openiot.cloud.base.mongo.model.Group
   */
  public boolean replaceGroupAttr(String gName, Map<String, String> attrs) {
    return Optional.ofNullable(grpRepo.findOneByName(gName)).map(g -> {
      return Optional.ofNullable(attrs)
                     .filter(as -> !as.isEmpty())
                     .map(as -> as.entrySet()
                                  .stream()
                                  .map(entry -> new AttributeEntity(entry.getKey(),
                                                                    entry.getValue()))
                                  .collect(Collectors.toList()))
                     .map(as -> {
                       g.setAs(as);
                       grpRepo.save(g);
                       taskOps.createTask("CFG_MONITOR",
                                          ConstDef.EVENT_TYPE_CFG_SYNC,
                                          null,
                                          ConstDef.EVENT_TARGET_TYPE_GROUP,
                                          g.getN(),
                                          ConstDef.DAY_SECONDS,
                                          null,
                                          null);
                       return true;
                     })
                     .orElse(false);
    }).orElse(false);
  }

  /**
   * Method to replace all configure properties of the Group with the new ones.
   *
   * @param groupName name of the group to be updated
   * @param configure properties list of name and value pair to be replaced with
   * @return 0: success 1:failed.
   * @see com.openiot.cloud.base.mongo.model.Group
   */
  public boolean replaceGroupCfg(String gName, Map<String, String> cfgs) {
    return Optional.ofNullable(grpRepo.findOneByName(gName)).map(g -> {
      return Optional.ofNullable(cfgs)
                     .filter(cs -> !cs.isEmpty())
                     .map(cs -> cs.entrySet()
                                  .stream()
                                  .map(entry -> new ConfigurationEntity(entry.getKey(),
                                                                        entry.getValue()))
                                  .collect(Collectors.toList()))
                     .map(cs -> {
                       g.setCs(cs);
                       grpRepo.save(g);
                       taskOps.createTask("CFG_MONITOR",
                                          ConstDef.EVENT_TYPE_CFG_SYNC,
                                          null,
                                          ConstDef.EVENT_TARGET_TYPE_GROUP,
                                          g.getN(),
                                          ConstDef.DAY_SECONDS,
                                          null,
                                          null);
                       return true;
                     })
                     .orElse(false);
    }).orElse(false);
  }

  /**
   * Method to replace all data sources of the Group with the new ones.
   *
   * @param groupName name of the group to be updated
   * @param data source list of name and value pair to be replaced with
   * @return 0: success 1:failed.
   * @see com.openiot.cloud.base.mongo.model.Group
   */
  public boolean replaceGroupDss(String gName, Map<String, String> dtsrcs) {
    return Optional.ofNullable(grpRepo.findOneByName(gName)).map(g -> {
      return Optional.ofNullable(dtsrcs)
                     .filter(dss -> !dss.isEmpty())
                     .map(dss -> dss.entrySet().stream().map(entry -> {
                       DataSourceEntity ds = new DataSourceEntity(entry.getKey(), entry.getValue());
                       return ds;
                     }).collect(Collectors.toList()))
                     .map(dss -> {
                       g.setDss(dss);
                       grpRepo.save(g);
                       taskOps.createTask("CFG_MONITOR",
                                          ConstDef.EVENT_TYPE_CFG_SYNC,
                                          null,
                                          ConstDef.EVENT_TARGET_TYPE_GROUP,
                                          g.getN(),
                                          ConstDef.DAY_SECONDS,
                                          null,
                                          null);
                       return true;
                     })
                     .orElse(false);
    }).orElse(false);
  }

  /**
   * Method to delete all attributes with specified name in the Group.
   *
   * @param groupName name of the group to be updated
   * @param attribute list of name to be deleted
   * @return 0: success 1:failed.
   * @see com.openiot.cloud.base.mongo.model.Group
   */
  public boolean deleteGroupAttr(String gName, List<String> attrs) {
    return Optional.ofNullable(grpRepo.findOneByName(gName)).map(g -> {
      return Optional.ofNullable(attrs)
                     .filter(as -> !as.isEmpty())
                     .map(as -> as.stream()
                                  .map(attrName -> new AttributeEntity(attrName, null))
                                  .collect(Collectors.toList()))
                     .map(as -> {
                       g.getAs().removeAll(as);
                       grpRepo.save(g);
                       taskOps.createTask("CFG_MONITOR",
                                          ConstDef.EVENT_TYPE_CFG_SYNC,
                                          null,
                                          ConstDef.EVENT_TARGET_TYPE_GROUP,
                                          g.getN(),
                                          ConstDef.DAY_SECONDS,
                                          null,
                                          null);
                       return true;
                     })
                     .orElse(false);
    }).orElse(false);
  }

  /**
   * Method to delete all configure properties with specified name in the Group.
   *
   * @param groupName name of the group to be updated
   * @param configure property list of name to be deleted
   * @return 0: success 1:failed.
   * @see com.openiot.cloud.base.mongo.model.Group
   */
  public boolean deleteGroupCfg(String gName, List<String> cfgs) {
    return Optional.ofNullable(grpRepo.findOneByName(gName)).map(g -> {
      return Optional.ofNullable(cfgs)
                     .filter(cs -> !cs.isEmpty())
                     .map(cs -> cs.stream()
                                  .map(cfgName -> new ConfigurationEntity(cfgName, null))
                                  .collect(Collectors.toList()))
                     .map(cs -> {
                       g.getCs().removeAll(cs);
                       grpRepo.save(g);
                       taskOps.createTask("CFG_MONITOR",
                                          ConstDef.EVENT_TYPE_CFG_SYNC,
                                          null,
                                          ConstDef.EVENT_TARGET_TYPE_GROUP,
                                          g.getN(),
                                          ConstDef.DAY_SECONDS,
                                          null,
                                          null);
                       return true;
                     })
                     .orElse(false);
    }).orElse(false);
  }

  /**
   * Method to delete all data sources with specified name in the Group.
   *
   * @param groupName name of the group to be updated
   * @param data source list of name to be deleted
   * @return 0: success 1:failed.
   * @see com.openiot.cloud.base.mongo.model.Group
   */
  public boolean deleteGroupDss(String gName, Map<String, String> dtsrcs) {
    return Optional.ofNullable(grpRepo.findOneByName(gName)).map(g -> {
      return Optional.ofNullable(dtsrcs)
                     .filter(dss -> !dss.isEmpty())
                     .map(dss -> dss.entrySet().stream().map(entry -> {
                       DataSourceEntity ds = new DataSourceEntity(entry.getKey(), entry.getValue());
                       return ds;
                     }).collect(Collectors.toList()))
                     .map(dss -> {
                       g.getDss().removeAll(dss);
                       grpRepo.save(g);
                       taskOps.createTask("CFG_MONITOR",
                                          ConstDef.EVENT_TYPE_CFG_SYNC,
                                          null,
                                          ConstDef.EVENT_TARGET_TYPE_GROUP,
                                          g.getN(),
                                          ConstDef.DAY_SECONDS,
                                          null,
                                          null);
                       return true;
                     })
                     .orElse(false);
    }).orElse(false);
  }

  /**
   * Method to remove device or resource entities with specified id into the Group.
   *
   * @param groupName name of the group to be updated
   * @param device id list to be removed from the group, this parameter is optional
   * @param resource id list to be removed from the group, this parameter is optional
   * @return 0: success 1:failed.
   * @see com.openiot.cloud.base.mongo.model.Group
   */
  // public boolean deleteGroupEntities(String gName,
  // Map<String, String> entities) {
  // return Optional.ofNullable(grpRepo.findOneByName(gName)).map(g -> {
  // return Optional.ofNullable(entities)
  // .filter(ms -> !ms.isEmpty())
  // .map(ms -> ms.entrySet().stream()
  // .map(entry -> new EntityRef(
  // entry.getKey(),
  // entry.getValue()))
  // .collect(Collectors.toList()))
  // .map(ms -> {
  // g.getMs().removeAll(ms);
  // grpRepo.save(g);
  // ttrigger.pullAsGroupCfgSync(g.getN());
  // return true;
  // }).orElse(false);
  // }).orElse(false);
  // }

  /**
   * Method to remove devices with specified id from the Group.
   *
   * @param groupName name of the group to be updated
   * @param device id list to be removed from the group, this parameter is optional
   * @param resource id list to be removed from the group, this parameter is optional
   * @return 0: success 1:failed.
   * @see com.openiot.cloud.base.mongo.model.Group
   */
  public boolean deleteGroupDevMember(String gName, List<String> devList) {
    return Optional.ofNullable(grpRepo.findOneByName(gName)).map(g -> {
      return Optional.ofNullable(devList).map(devs -> {
        // g.getMs().removeAll(ms);
        g.removeItemsFromMd(devs);
        grpRepo.save(g);
        taskOps.createTask("CFG_MONITOR",
                           ConstDef.EVENT_TYPE_CFG_SYNC,
                           null,
                           ConstDef.EVENT_TARGET_TYPE_GROUP,
                           g.getN(),
                           ConstDef.DAY_SECONDS,
                           null,
                           null);
        return true;
      }).orElse(false);
    }).orElse(false);
  }

  /**
   * Method to remove resources entities with specified device Id and resource uri from the Group.
   *
   * @param groupName name of the group to be updated
   * @param device id list to be removed from the group, this parameter is optional
   * @param resource id list to be removed from the group, this parameter is optional
   * @return 0: success 1:failed.
   * @see com.openiot.cloud.base.mongo.model.Group
   */
  public boolean deleteGroupResMember(String gName, Map<String, String> resMap) {
    return Optional.ofNullable(grpRepo.findOneByName(gName)).map(g -> {
      return Optional.ofNullable(resMap)
                     .filter(resources -> !resources.isEmpty())
                     .map(resources -> resources.entrySet()
                                                .stream()
                                                .map(entry -> new MemberResRef(entry.getKey(),
                                                                               entry.getValue()))
                                                .collect(Collectors.toList()))
                     .map(mr -> {
                       g.removeItemsFromMr(mr);
                       grpRepo.save(g);
                       taskOps.createTask("CFG_MONITOR",
                                          ConstDef.EVENT_TYPE_CFG_SYNC,
                                          null,
                                          ConstDef.EVENT_TARGET_TYPE_GROUP,
                                          g.getN(),
                                          ConstDef.DAY_SECONDS,
                                          null,
                                          null);
                       return true;
                     })
                     .orElse(false);
    }).orElse(false);
  }

  /**
   * Method to delete the Group with specified name.
   *
   * @param name name of the group to be deleted
   * @return 0: success 1:failed.
   * @see com.openiot.cloud.base.mongo.model.Group
   */
  public boolean deleteGroupByName(String name) {
    return Optional.ofNullable(name).map(n -> grpRepo.findOneByName(n)).map(g -> {
      grpRepo.delete(g);
      return true;
    }).orElse(false);
  }

  public boolean updateGroupCfg(Group group, Map<String, String> cfgs) {
    return updateGroupCfg(group.getN(), cfgs);
  }

  public DataSourceEntity findOneDsByGroupNameAndDsName(String gName, String dsName) {
    return Optional.ofNullable(grpRepo.findDssByGroupNameAndDsName(gName, dsName).get(0))
                   .orElse(null);
  }

  public void changeGroupType(Group group, String newGT) {
    Optional.ofNullable(group)
            .map(g -> addOrUpdateGroup(g.setGt(newGT)))
            .map(g -> g.getN())
            .ifPresent(n -> taskOps.createTask("CFG_MONITOR",
                                               ConstDef.EVENT_TYPE_CFG_SYNC,
                                               null,
                                               ConstDef.EVENT_TARGET_TYPE_GROUP,
                                               group.getN(),
                                               ConstDef.DAY_SECONDS,
                                               null,
                                               null));
  }

  public List<Group> getGroupWithMemberDev(String devId) {
    return grpRepo.findAllGroupByDevId(devId);
  }

  public List<Group> getGroupWithMemberRes(String di, String uri) {
    return grpRepo.findAllGroupByRes(di, uri);
  }

  public void deleteProjectInfoInGroup(String groupName) {
    Group group = grpRepo.findOneByName(groupName);
    if (group != null) {
      group.setPrj(null);
      grpRepo.save(group);
    }
  }
}
