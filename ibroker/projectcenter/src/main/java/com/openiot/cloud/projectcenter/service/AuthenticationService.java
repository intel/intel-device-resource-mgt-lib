/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.service;

import com.openiot.cloud.base.common.model.TokenContent;
import com.openiot.cloud.base.mongo.model.help.UserRole;
import com.openiot.cloud.projectcenter.service.dto.AuthenticationDTO;
import com.openiot.cloud.projectcenter.service.dto.AuthorizationDTO;
import com.openiot.cloud.projectcenter.service.dto.ProjectDTO;
import com.openiot.cloud.projectcenter.service.dto.UserDTO;
import com.openiot.cloud.projectcenter.utils.ApiJwtTokenUtil;
import com.openiot.cloud.base.service.model.UserAndRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class AuthenticationService {
  @Autowired
  private ApiJwtTokenUtil jwtTokenUtil;
  @Autowired
  private UserService userService;
  @Autowired
  private ProjectService projectService;

  @Value("${jwt.tokenHead}")
  private String tokenHead;

  public AuthorizationDTO login(final AuthenticationDTO authenticationDTO) {
    Objects.requireNonNull(authenticationDTO);

    log.debug("AuthenticationDTO is {}", authenticationDTO);

    String username = authenticationDTO.getUsername();
    String password = authenticationDTO.getPassword();

    UserDTO userDTO = userService.findByName(username);
    if (userDTO == null) {
      log.warn("the user {} does not exist", username);
      return null;
    }

    if (!userService.mathPassword(userDTO, password)) {
      log.warn("wrong password for the user {}", username);
      return null;
    }

    // select a project for current user
    String defaultProjectId = null;
    List<ProjectDTO> projectDTOS = projectService.findByUserName(username);
    if (projectDTOS.isEmpty()) {
      defaultProjectId = null;
    } else if (projectDTOS.size() == 1) {
      // 1. if there is only one project which the user is in
      defaultProjectId = projectDTOS.get(0).getId();
    } else {
      // 2. if there are multiple projects, use the latest project
      defaultProjectId = userDTO.getRecentProject();
    }

    // but will be unset if the project has gone. a null projectId will lead to the action of select
    // a project later
    if (defaultProjectId != null && !defaultProjectId.isEmpty()) {
      ProjectDTO recentAccessProject = projectService.findByProjectId(defaultProjectId);
      if (recentAccessProject == null) {
        defaultProjectId = null;
      }
    }

    // create a token
    String token = jwtTokenUtil.generateToken(username, defaultProjectId);

    AuthorizationDTO authorizationDTO = new AuthorizationDTO();
    authorizationDTO.setToken(token);
    authorizationDTO.setPrj(defaultProjectId);
    return authorizationDTO;
  }

  public AuthorizationDTO selectProject(final AuthorizationDTO authorizationDTO) {
    Objects.requireNonNull(authorizationDTO);

    String oldToken = authorizationDTO.getToken();
    if (!jwtTokenUtil.validateToken(oldToken)) {
      log.warn("the token is invalid");
      return null;
    }

    String username = jwtTokenUtil.getUsernameFromToken(oldToken);
    UserDTO userDTO = userService.findByName(username);
    if (Objects.isNull(userDTO)) {
      log.warn("the user {} does not exist", username);
      return null;
    }

    String projectId = authorizationDTO.getPrj();
    log.debug("the user {} tries to select the project {}", username, projectId);

    if (Objects.nonNull(projectId) && !projectId.isEmpty()) {
      ProjectDTO projectDTO = projectService.findByProjectIdAndUserName(projectId, username);
      if (Objects.isNull(projectDTO)) {
        log.warn("the project {} (include a user {}) does not exist", projectId, username);
        return null;
      }

      // update the recent project of the user
      userDTO.setRecentProject(projectId);
      userService.save(userDTO);
    } else {
      // it is allowed that the user doesn't select any project
      // actually, it means the user want to remove the selected project
      // since the recent project might have been assign need to clear it
    }

    // TODO: force re-login if the password has been modified

    String newToken = jwtTokenUtil.generateToken(username, projectId);

    AuthorizationDTO newAuthorizationDTO = new AuthorizationDTO();
    newAuthorizationDTO.setToken(newToken);
    newAuthorizationDTO.setPrj(projectId);
    return newAuthorizationDTO;
  }

  public TokenContent validateToken(final AuthorizationDTO authorizationDTO) {
    Objects.requireNonNull(authorizationDTO);

    String oldToken = authorizationDTO.getToken();
    if (!jwtTokenUtil.validateToken(oldToken)) {
      log.warn("the token is JWT invalid");
      return null;
    }

    // TBD: shall we check the project information in AuthorizationDTO and token ?

    return validateToken(oldToken);
  }

  /**
   * @param token
   * @return null means validation failed
   */
  public TokenContent validateToken(String token) {
    Objects.requireNonNull(token);

    TokenContent tokenContent = new TokenContent();
    String userName = jwtTokenUtil.getUsernameFromToken(token);
    if (userName == null || userName.isEmpty()) {
      log.warn("can not find the user name in the token");
      return null;
    }

    UserDTO user = userService.findByName(userName);
    if (user == null) {
      log.warn("can not find the user with a name", userName);
      return null;
    }
    // user is checked
    tokenContent.setUser(userName);

    if (Objects.equals(UserRole.SYS_ADMIN, user.getRole())) {
      // role is checked
      tokenContent.setRole(UserRole.SYS_ADMIN);
    }

    String projectID = jwtTokenUtil.getPidFromToken(token);
    if (projectID == null) {
      log.info("it is ok if there is no project id in the token");
      return tokenContent;
    }

    ProjectDTO projectDTO = projectService.findByProjectIdAndUserName(projectID, userName);
    if (projectDTO == null) {
      log.warn("there is no project with id {} and a user {} in it", projectID, userName);
      return null;
    }

    if (Objects.equals(UserRole.SYS_ADMIN, user.getRole())) {
      // project is checked
      tokenContent.setProject(projectID);
    } else {
      UserAndRole userAndRole =
          Optional.ofNullable(projectDTO.getUser())
                  .map(userAndRoles -> userAndRoles.stream()
                                                   .filter(ur -> ur.getName().equals(userName))
                                                   .findFirst()
                                                   .orElse(null))
                  .orElse(null);
      // role is checked
      tokenContent.setRole(userAndRole.getRole());
      // project is checked
      tokenContent.setProject(projectID);
    }

    log.debug("tokenContent {}", tokenContent);
    return tokenContent;
  }

  /**
   * ignore the poject information in the authorizationDTO
   * @param oldToken
   * @return
   */
  public AuthorizationDTO refreshToken(String oldToken) {
    Objects.requireNonNull(oldToken);

    if (!jwtTokenUtil.validateToken(oldToken)) {
      log.warn("the token is JWT invalid");
      return null;
    }

    TokenContent tokenContent = validateToken(oldToken);
    if (tokenContent == null) {
      log.warn("token is user/project invalid");
      return null;
    }

    String newToken = jwtTokenUtil.generateToken(tokenContent.getUser(), tokenContent.getProject());

    AuthorizationDTO authorizationDTO = new AuthorizationDTO();
    authorizationDTO.setToken(newToken);
    authorizationDTO.setPrj(tokenContent.getProject());
    return authorizationDTO;
  }
}
