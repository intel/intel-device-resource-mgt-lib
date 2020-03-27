/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.help;

import com.openiot.cloud.base.Application;
import com.openiot.cloud.base.mongo.model.help.UserRole;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class})
public class UserRoleTest {
  @Test
  public void testBasic() throws Exception {
    UserRole ur1 = UserRole.SYS_ADMIN;
    String urlString = ur1.toString().toUpperCase();
    assertThat(UserRole.valueOf("SYS_ADMIN")).isEqualTo(ur1);
    // assertThat(Enum.valueOf(UserRole.class, urlString)).isEqualTo(ur1);
  }
}
