/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.service;

import com.openiot.cloud.base.common.model.TokenContent;
import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.mongo.model.help.UserRole;
import com.openiot.cloud.projectcenter.repository.ProjectRepository;
import com.openiot.cloud.projectcenter.repository.UserRepository;
import com.openiot.cloud.projectcenter.repository.document.Project;
import com.openiot.cloud.projectcenter.repository.document.User;
import com.openiot.cloud.projectcenter.service.dto.UserDTO;
import com.openiot.cloud.base.service.model.UserAndRole;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.Md5Crypt;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService {
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private ProjectRepository projectRepository;

  public UserDTO findByName(String name) {
    Objects.requireNonNull(name);
    return userRepository.findByName(name)
                         .stream()
                         .filter(user -> Objects.equals(name, user.getName()))
                         .findFirst()
                         .map(user -> {
                           UserDTO userDTO = new UserDTO();
                           BeanUtils.copyProperties(user, userDTO);
                           return userDTO;
                         })
                         .orElse(null);
  }

  public String encryptPassword(String rawPassword) {
    Objects.requireNonNull(rawPassword);
    return Md5Crypt.md5Crypt(rawPassword.getBytes(), "$1$2$3$4$5");
  }

  public boolean mathPassword(UserDTO userDTO, String rawPassword) {
    Objects.requireNonNull(userDTO);

    if (Objects.isNull(rawPassword))
      return false;

    return userRepository.findByName(userDTO.getName())
                         .stream()
                         .filter(user -> Objects.equals(userDTO.getName(), user.getName()))
                         .findFirst()
                         .map(user -> {
                           return encryptPassword(rawPassword).equals(user.getPassword());
                         })
                         .orElse(false);
  }

  public void save(UserDTO userDTO) {
    Objects.requireNonNull(userDTO);

    User updatedUser =
        userRepository.findByName(userDTO.getName())
                      .stream()
                      .filter(user -> Objects.equals(userDTO.getName(), user.getName()))
                      .findFirst()
                      .map(user -> {
                        BeanUtils.copyProperties(userDTO, user);
                        return user;
                      })
                      .orElseGet(() -> {
                        User user = new User();
                        BeanUtils.copyProperties(userDTO, user);
                        return user;
                      });
    userRepository.save(updatedUser);
  }

  public boolean isSysAdmin(String userName) {
    return userRepository.findByName(userName)
                         .stream()
                         .filter(user -> Objects.equals(UserRole.SYS_ADMIN, user.getRole()))
                         .findFirst()
                         .map(user -> Boolean.TRUE)
                         .orElse(Boolean.FALSE);
  }

  public boolean createUser(UserDTO userDTO, TokenContent tokenContent) {
    Objects.requireNonNull(userDTO);

    List<User> users = userRepository.findByName(userDTO.getName());
    if (!users.isEmpty()) {
      log.warn("the user {} exists", userDTO.getName());
      return false;
    }

    // form a document for the database
    User user = new User();
    BeanUtils.copyProperties(userDTO, user);
    // make sure id is equal to name
    user.setId(user.getName());

    if (Objects.nonNull(userDTO.getPassword()) && !userDTO.getPassword().isEmpty()) {
      user.setPassword(encryptPassword(userDTO.getPassword()));
      user.setTime_reg(Instant.now(Clock.systemUTC()).toEpochMilli());
    }

    // only a sys admin can set role
    if (!Objects.equals(UserRole.SYS_ADMIN, userDTO.getRole())) {
      user.setRole(null);
    }

    userRepository.save(user);
    return true;
  }

  public boolean updateUser(UserDTO userDTO, String newPassword, TokenContent tokenContent) {
    Objects.requireNonNull(userDTO);

    List<User> users = userRepository.findByName(userDTO.getName());
    if (users.isEmpty()) {
      log.warn("the user {} does not exist", userDTO.getName());
      return false;
    }

    // form a document for the database
    User user = users.get(0);
    BaseUtil.copyPropertiesIgnoreCollectionNull(userDTO, user);
    // do not change id

    // if gona to modify the password
    if (Objects.nonNull(newPassword) && !newPassword.isEmpty()) {
      user.setPassword(encryptPassword(newPassword));
      user.setTime_reg(Instant.now(Clock.systemUTC()).toEpochMilli());
    }

    // only a sys admin can set role
    if (!Objects.equals(UserRole.SYS_ADMIN, userDTO.getRole())) {
      user.setRole(null);
    }

    userRepository.save(user);
    return true;
  }

  public boolean removeUser(String userName, TokenContent tokenContent) {
    Objects.requireNonNull(userName);

    List<User> users = userRepository.findByName(userName);
    if (users.isEmpty()) {
      log.warn("the user {} does not exist", userName);
      return false;
    }

    userRepository.deleteByName(userName);
    return true;
  }

  public List<UserDTO> queryUser(String userName, String projectId, TokenContent tokenContent) {
    List<User> users = new ArrayList<>();
    if ((userName == null || userName.isEmpty()) && (projectId == null || projectId.isEmpty())) {
      // w/o a user name and a project id, give all
      users = userRepository.findAll();
    } else if (userName != null && !userName.isEmpty()) {
      // once indicate a user name, ignore the project id
      users = userRepository.findByName(userName);
    } else {
      Project project = projectRepository.findById(projectId).orElse(null);
      if (project != null) {
        Set<String> userNames =
            Optional.ofNullable(project.getUser())
                    .map(userAndRoles -> userAndRoles.stream()
                                                     .map(UserAndRole::getName)
                                                     .collect(Collectors.toSet()))
                    .orElse(Collections.emptySet());
        users = userRepository.findByNameIn(userNames);
      }
    }

    return users.stream().map(user -> {
      UserDTO userDTO = new UserDTO();
      BeanUtils.copyProperties(user, userDTO);
      return userDTO;
    }).collect(Collectors.toList());
  }
}
