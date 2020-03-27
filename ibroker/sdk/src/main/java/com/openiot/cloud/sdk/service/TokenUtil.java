/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.sdk.service;

import com.openiot.cloud.base.common.model.TokenContent;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.model.help.UserRole;
import java.util.Optional;

public class TokenUtil {
  public static TokenContent formTokenContent(IConnectRequest request) {
    TokenContent tokenContent = new TokenContent();

    // by default, we believe all IConnectRequests are from internal services which are able to
    // access everything
    tokenContent.setUser(Optional.ofNullable(request.getTokenInfo(ConstDef.MSG_KEY_USR))
                                 .orElse("beihai"));
    Optional.ofNullable(request.getTokenInfo(ConstDef.MSG_KEY_PRJ))
            .ifPresent(project -> tokenContent.setProject(project));
    tokenContent.setRole(Optional.ofNullable(request.getTokenInfo(ConstDef.MSG_KEY_ROLE))
                                 .map(roleString -> UserRole.valueOf(roleString.replaceFirst("ROLE_",
                                                                                             "")))
                                 .orElse(UserRole.SYS_ADMIN));

    return tokenContent;
  }
}
