/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.service;

import com.openiot.cloud.base.common.model.TokenContent;
import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.model.help.AttributeEntity;
import com.openiot.cloud.base.mongo.model.help.ConfigurationEntity;
import com.openiot.cloud.base.mongo.model.help.UserRole;
import com.openiot.cloud.projectcenter.repository.ProjectRepository;
import com.openiot.cloud.projectcenter.repository.document.Project;
import com.openiot.cloud.projectcenter.service.dto.ProjectDTO;
import com.openiot.cloud.base.service.model.UserAndRole;
import com.openiot.cloud.sdk.event.TaskOperations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProjectService {
  @Autowired
  private ProjectRepository projectRepository;
  @Autowired
  private UserService userService;
  @Autowired
  private TaskOperations taskOperations;

  private List<String> parseArray(String asArray) {
    return Arrays.stream(asArray.substring(1, asArray.length() - 1).split(","))
                 .filter(Objects::nonNull)
                 .map(String::trim)
                 .filter(i -> !i.isEmpty())
                 .collect(Collectors.toList());
  }

  public ProjectDTO createProject(ProjectDTO newProjectDTO, TokenContent tokenContent) {
    Objects.requireNonNull(newProjectDTO);

    List<Project> projects = projectRepository.findByName(newProjectDTO.getName());
    if (!projects.isEmpty()) {
      log.warn("the project {} exits", newProjectDTO.getName());
      return null;
    }

    Project project = new Project();
    BeanUtils.copyProperties(newProjectDTO, project);
    // // do not touch the project id in case some body actually touch it
    // // always set creating time
    // project.setTime_created(Instant.now(Clock.systemUTC()).toEpochMilli());

    project = projectRepository.save(project);

    // create project.cfg on AMS server
    taskOperations.createTask("CFG_MONITOR",
                              ConstDef.EVENT_TYPE_CFG_SYNC,
                              null,
                              ConstDef.EVENT_TARGET_TYPE_PROJECT,
                              project.getId(),
                              ConstDef.DAY_SECONDS,
                              null,
                              null,
                              ConstDef.EVENT_TASK_OPTION_OVERWRITE);

    newProjectDTO.setId(project.getId());
    return newProjectDTO;
  }

  public boolean updateProject(ProjectDTO newProjectDTO, TokenContent tokenContent) {
    Objects.requireNonNull(newProjectDTO);
    Objects.requireNonNull(tokenContent);

    Project project = projectRepository.findById(newProjectDTO.getId()).orElse(null);
    if (project == null) {
      log.warn("the project {} does not exit", newProjectDTO.getName());
      return false;
    }

    log.debug("the old project is {}", project);
    if (Objects.equals(UserRole.SYS_ADMIN, tokenContent.getRole())) {
      // a sys admin can modify any project
      BaseUtil.copyPropertiesIgnoreAllNull(newProjectDTO, project);
    } else {
      if (Objects.equals(newProjectDTO.getId(), tokenContent.getProject())) {
        // if the selected project is the modified target, based on its role
        if (Objects.equals(UserRole.ADMIN, tokenContent.getRole())) {
          // a admin can modify its project
          BaseUtil.copyPropertiesIgnoreAllNull(newProjectDTO, project);
          // make sure keep the id
        } else {
          log.info("current login user {} is only project {} user and can not modify project information",
                   tokenContent.getUser(),
                   newProjectDTO.getId());
          return false;
        }
      } else {
        // if the selected project is not the modified target, ask database
        List<Project> projects =
            projectRepository.findByIdAndUserName(newProjectDTO.getId(), tokenContent.getUser());
        if (projects.isEmpty()) {
          log.warn("current login user {} is not in the project {}",
                   tokenContent.getUser(),
                   newProjectDTO.getId());
          return false;
        } else {
          UserRole roleInProject =
              projects.stream()
                      .map(Project::getUser)
                      .filter(Objects::nonNull)
                      .flatMap(Collection::stream)
                      .filter(userAndRole -> Objects.equals(tokenContent.getUser(),
                                                            userAndRole.getName()))
                      .findAny()
                      .map(UserAndRole::getRole)
                      .orElse(null);
          if (Objects.equals(UserRole.ADMIN, roleInProject)) {
            // a admin can modify its project
            BaseUtil.copyPropertiesIgnoreAllNull(newProjectDTO, project);
            // make sure keep the id
          } else {
            log.info("current login user {} is only project user and can not modify project information",
                     tokenContent.getUser(),
                     newProjectDTO.getId());
            return false;
          }
        }
      }
    }

    log.debug("the new project is {}", project);

    project = projectRepository.save(project);
    taskOperations.createTask("CFG_MONITOR",
                              ConstDef.EVENT_TYPE_CFG_SYNC,
                              null,
                              ConstDef.EVENT_TARGET_TYPE_PROJECT,
                              project.getId(),
                              ConstDef.DAY_SECONDS,
                              null,
                              null,
                              ConstDef.EVENT_TASK_OPTION_OVERWRITE);
    return true;
  }

  /**
   * gona query all
   * @param tokenContent
   * @return
   */
  public List<ProjectDTO> findAll(TokenContent tokenContent) {
    Objects.requireNonNull(tokenContent);

    List<Project> projectList = new ArrayList<>();
    if (Objects.equals(UserRole.SYS_ADMIN, tokenContent.getRole())) {
      // return all projects for SYS_ADMIN
      projectList = projectRepository.findAll();
    } else {
      // return all joined projects
      projectList = projectRepository.findByUserName(tokenContent.getUser());
    }

    return projectList.stream().map(project -> {
      ProjectDTO projectDTO = new ProjectDTO();
      BeanUtils.copyProperties(project, projectDTO);
      return projectDTO;
    }).collect(Collectors.toList());
  }

  public boolean removeProject(String projectId) {
    Objects.requireNonNull(projectId);

    Project project = projectRepository.findById(projectId).orElse(null);
    if (project == null) {
      log.warn("the project {} does not exist", projectId);
      return false;
    }

    projectRepository.deleteById(projectId);

    // TBD: delete project.cfg on AMS server
    return true;
  }

  public List<ProjectDTO> findByUserName(String userName) {
    Objects.requireNonNull(userName);

    // a system admin can see all projects
    if (userService.isSysAdmin(userName)) {
      return projectRepository.findAll().stream().map(project -> {
        ProjectDTO projectDTO = new ProjectDTO();
        BeanUtils.copyProperties(project, projectDTO);
        return projectDTO;
      }).collect(Collectors.toList());
    } else {
      return projectRepository.findByUserName(userName).stream().map(project -> {
        ProjectDTO projectDTO = new ProjectDTO();
        BeanUtils.copyProperties(project, projectDTO);
        return projectDTO;
      }).collect(Collectors.toList());
    }
  }

  public ProjectDTO findByProjectId(String projectId) {
    Objects.requireNonNull(projectId);

    return projectRepository.findById(projectId).map(project -> {
      ProjectDTO projectDTO = new ProjectDTO();
      BeanUtils.copyProperties(project, projectDTO);
      return projectDTO;

    }).orElse(null);
  }

  /**
   * find a project with a indicated project id plus that the indicated user is its user
   *
   * @param projectId
   * @param userName
   * @return
   */
  public ProjectDTO findByProjectIdAndUserName(String projectId, String userName) {
    Objects.requireNonNull(projectId);
    Objects.requireNonNull(userName);

    log.debug("find a project with id {} and a user name {}", projectId, userName);
    if (userService.isSysAdmin(userName)) {
      log.debug("{} is SYS_ADMIN and can operate any project, include {}", userName, projectId);
      return projectRepository.findById(projectId).map(project -> {
        log.debug("mapping a project document to DTO");

        ProjectDTO projectDTO = new ProjectDTO();
        BeanUtils.copyProperties(project, projectDTO);
        return projectDTO;
      }).orElseGet(() -> {
        log.debug("can not find a project with id {}", projectId);

        return null;
      });
    } else {
      List<Project> projects = projectRepository.findByIdAndUserName(projectId, userName);
      if (projects.isEmpty()) {
        return null;
      }

      ProjectDTO projectDTO = new ProjectDTO();
      BeanUtils.copyProperties(projects.get(0), projectDTO);
      return projectDTO;
    }
  }

  public void removeAll() {
    projectRepository.deleteAll();
  }

  // if it is new, insert it, if not update it
  public void save(ProjectDTO projectDTO) {
    Objects.requireNonNull(projectDTO);

    Project project = projectRepository.findById(projectDTO.getId()).orElse(null);
    if (project == null) {
      // it is a newer
      project = new Project();
    }
    BeanUtils.copyProperties(projectDTO, project);
    projectRepository.save(project);
  }

  public boolean updateOrInsertProjectAttribute(String projectId, AttributeEntity[] attributes,
                                                TokenContent tokenContent) {
    Objects.requireNonNull(projectId);
    Objects.requireNonNull(attributes);

    Project project = projectRepository.findById(projectId).orElse(null);
    if (project == null) {
      log.warn("the project {} does not exist", projectId);
      return false;
    }

    // update or insert
    List<AttributeEntity> attributeEntities =
        Optional.ofNullable(project.getAs()).orElse(new ArrayList<AttributeEntity>());
    for (AttributeEntity a : attributes) {
      int index = attributeEntities.indexOf(a);
      if (index < 0) {
        // insert
        attributeEntities.add(a);
      } else {
        // update
        attributeEntities.set(index, a);
      }
    }
    project.setAs(attributeEntities);

    project = projectRepository.save(project);
    taskOperations.createTask("CFG_MONITOR",
                              ConstDef.EVENT_TYPE_CFG_SYNC,
                              null,
                              ConstDef.EVENT_TARGET_TYPE_PROJECT,
                              project.getId(),
                              ConstDef.DAY_SECONDS,
                              null,
                              null,
                              ConstDef.EVENT_TASK_OPTION_OVERWRITE);
    return true;
  }

  public boolean removeProjectAttribute(String projectId, String attributes,
                                        TokenContent tokenContent) {
    Objects.requireNonNull(projectId);
    Objects.requireNonNull(attributes);

    Project project = projectRepository.findById(projectId).orElse(null);
    if (project == null) {
      log.warn("the project {} does not exist", projectId);
      return false;
    }

    // to remove and ignore any one doesn't exist
    Set<String> attributeSet = new HashSet<>(parseArray(attributes));
    Optional.ofNullable(project.getAs())
            .ifPresent(attributeEntities -> attributeEntities.removeIf(attributeEntity -> attributeSet.contains(attributeEntity.getAn())));

    project = projectRepository.save(project);
    taskOperations.createTask("CFG_MONITOR",
                              ConstDef.EVENT_TYPE_CFG_SYNC,
                              null,
                              ConstDef.EVENT_TARGET_TYPE_PROJECT,
                              project.getId(),
                              ConstDef.DAY_SECONDS,
                              null,
                              null,
                              ConstDef.EVENT_TASK_OPTION_OVERWRITE);
    return true;
  }

  public boolean updateOrInsertProjectConfiguration(String projectId,
                                                    ConfigurationEntity[] configurations,
                                                    TokenContent tokenContent) {
    Objects.requireNonNull(projectId);
    Objects.requireNonNull(configurations);

    Project project = projectRepository.findById(projectId).orElse(null);
    if (project == null) {
      log.warn("the project {} does not exist", projectId);
      return false;
    }

    // update or insert
    List<ConfigurationEntity> configurationEntities =
        Optional.ofNullable(project.getCs()).orElse(new ArrayList<ConfigurationEntity>());
    for (ConfigurationEntity c : configurations) {
      int index = configurationEntities.indexOf(c);
      if (index < 0) {
        // insert
        configurationEntities.add(c);
      } else {
        // update
        configurationEntities.set(index, c);
      }
    }
    project.setCs(configurationEntities);

    project = projectRepository.save(project);
    taskOperations.createTask("CFG_MONITOR",
                              ConstDef.EVENT_TYPE_CFG_SYNC,
                              null,
                              ConstDef.EVENT_TARGET_TYPE_PROJECT,
                              project.getId(),
                              ConstDef.DAY_SECONDS,
                              null,
                              null,
                              ConstDef.EVENT_TASK_OPTION_OVERWRITE);
    return true;
  }

  public boolean removeProjectConfiguration(String projectId, String configurations,
                                            TokenContent tokenContent) {
    Objects.requireNonNull(projectId);
    Objects.requireNonNull(configurations);

    Project project = projectRepository.findById(projectId).orElse(null);
    if (project == null) {
      log.warn("the project {} does not exist", projectId);
      return false;
    }

    // to remove and ignore any one doesn't exist
    Set<String> configurationSet = new HashSet<>(parseArray(configurations));
    Optional.ofNullable(project.getCs())
            .ifPresent(configurationEntities -> configurationEntities.removeIf(configurationEntity -> configurationSet.contains(configurationEntity.getCn())));

    project = projectRepository.save(project);
    taskOperations.createTask("CFG_MONITOR",
                              ConstDef.EVENT_TYPE_CFG_SYNC,
                              null,
                              ConstDef.EVENT_TARGET_TYPE_PROJECT,
                              project.getId(),
                              ConstDef.DAY_SECONDS,
                              null,
                              null,
                              ConstDef.EVENT_TASK_OPTION_OVERWRITE);
    return true;
  }

  public boolean updateOrInsertProjectMember(String projectId, ProjectDTO newProjectDTO,
                                             TokenContent tokenContent) {
    Objects.requireNonNull(projectId);
    Objects.requireNonNull(newProjectDTO);

    Project project = projectRepository.findById(projectId).orElse(null);
    if (project == null) {
      log.warn("the project {} does not exist", projectId);
      return false;
    }

    List<UserAndRole> userListOfDB =
        Optional.ofNullable(project.getUser()).orElse(new ArrayList<UserAndRole>());
    for (UserAndRole newUser : Optional.ofNullable(newProjectDTO.getUser())
                                       .orElse(Collections.emptyList())) {
      if (Objects.isNull(newUser.getName()) || newUser.getName().isEmpty()) {
        log.warn("can not work with an empty name");
        return false;
      }

      if (Objects.equals(UserRole.SYS_ADMIN, newUser.getRole())) {
        log.warn("can not assign a SYS_ADMIN {} to a project {}", newUser, projectId);
        return false;
      }

      if (Objects.isNull(newUser.getRole())) {
        // by default, it is a user
        newUser.setRole(UserRole.USER);
      }

      int index = userListOfDB.indexOf(newUser);
      if (index < 0) {
        userListOfDB.add(newUser);
      } else {
        userListOfDB.set(index, newUser);
      }
    }
    project.setUser(userListOfDB);

    project = projectRepository.save(project);
    return true;
  }

  public boolean removeProjectMember(String projectId, String removedUser,
                                     TokenContent tokenContent) {
    Objects.requireNonNull(projectId);
    Objects.requireNonNull(removedUser);

    Project project = projectRepository.findById(projectId).orElse(null);
    if (project == null) {
      log.warn("the project {} does not exist", projectId);
      return false;
    }

    Optional.ofNullable(project.getUser())
            .ifPresent(userAndRoles -> userAndRoles.removeIf(userAndRole -> Objects.equals(removedUser,
                                                                                           userAndRole.getName())));
    project = projectRepository.save(project);
    return true;
  }
}
