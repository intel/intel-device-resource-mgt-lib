/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.projectcenter.repository;

import com.openiot.cloud.base.mongo.model.help.ConfigurationEntity;
import com.openiot.cloud.base.mongo.model.help.UserRole;
import com.openiot.cloud.projectcenter.repository.document.Project;
import com.openiot.cloud.base.service.model.UserAndRole;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
public class ProjectRepositoryTest {
  @Autowired
  private ProjectRepository projectRepository;

  private List<Project> projectList = new ArrayList<>();

  @Before
  public void setup() throws Exception {
    projectRepository.deleteAll();

    //////////////////////////////////////////////////////////////////////
    Project project1 = new Project();
    project1.setId("strawberry");
    project1.setName("strawberry");
    projectList.add(project1);

    //////////////////////////////////////////////////////////////////////
    UserAndRole userAndRole2 = new UserAndRole();
    userAndRole2.setName("banana");
    userAndRole2.setRole(UserRole.USER);

    ConfigurationEntity configurationEntity2 = new ConfigurationEntity("blackberry", "17");

    Project project2 = new Project();
    project2.setName("grapefruit");
    List<UserAndRole> userAndRoleSet = new ArrayList<>();
    userAndRoleSet.add(userAndRole2);
    project2.setUser(userAndRoleSet);
    List<ConfigurationEntity> configurationEntitySet = new ArrayList<>();
    configurationEntitySet.add(configurationEntity2);
    project2.setCs(configurationEntitySet);
    projectList.add(project2);

    //////////////////////////////////////////////////////////////////////
    projectRepository.saveAll(projectList);
  }

  @Test
  public void testBasic() throws Exception {
    assertThat(projectRepository.findById(projectList.get(0).getId())
                                .orElse(null)).isNotNull()
                                              .hasFieldOrPropertyWithValue("name",
                                                                           projectList.get(0)
                                                                                      .getName());

    assertThat(projectRepository.findByName(projectList.get(0).getName())
                                .get(0)).hasFieldOrPropertyWithValue("name",
                                                                     projectList.get(0).getName())
                                        .hasFieldOrPropertyWithValue("user", null);

    assertThat(projectRepository.findByUserName(projectList.get(1)
                                                           .getUser()
                                                           .get(0)
                                                           .getName())).hasSize(1)
                                                                       .element(0)
                                                                       .hasFieldOrPropertyWithValue("name",
                                                                                                    projectList.get(1)
                                                                                                               .getName());
  }
}
