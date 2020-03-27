/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.sdk.utilities;

import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.mongo.dao.ResProRepository;
import com.openiot.cloud.base.mongo.model.ResProperty;
import com.openiot.cloud.base.mongo.model.help.ConfigurationEntity;
import com.openiot.cloud.sdk.service.ApplicationContextProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.HashMap;

/**
 * The utility to access resource properties in openiot cloud.<br>
 * <br>
 *
 * @author saigon
 * @version 1.0
 * @since Oct 2017
 */
@Component
public class ResourcePropertyUtility {
  @Autowired
  ResProRepository rpRepo;

  public static ResourcePropertyUtility getInstance() {
    return ApplicationContextProvider.getBean(ResourcePropertyUtility.class);
  }

  public String updateUserConfig(String fu, HashMap<String, String> uc) {
    if (uc == null || uc.size() == 0)
      return "empty user config";

    String result = "";
    ResProperty rp = rpRepo.findOneByFullUrl(fu);
    if (rp != null) {
      for (String key : uc.keySet()) {
        ConfigurationEntity findCs = null;
        if (rp.getUserCfgs() != null) {
          findCs = rp.getUserCfgs()
                     .stream()
                     .filter(cfg -> key.equals(cfg.getCn()))
                     .findAny()
                     .orElse(null);
        }
        if (findCs == null) {
          rp.addUserCfgsItem(new ConfigurationEntity(key, uc.get(key)));
          result = "add new ConfigurationEntity [cn=" + key + ", cv=" + uc.get(key) + "]";
        } else {
          findCs.setCv(uc.get(key));
          result = "update existing " + findCs;
        }
      }
      rpRepo.save(rp);
    } else {
      result = "no ressource property found for " + fu;
    }

    return result;
  }

  public String updateUserConfig(String di, String res, String pn, HashMap<String, String> uc) {
    if (uc == null || uc.size() == 0)
      return "empty user config";

    String result = "";
    String fu = BaseUtil.formAFullUrl(di, res, pn);
    ResProperty rp = rpRepo.findOneByFullUrl(fu);
    if (rp != null) {
      for (String key : uc.keySet()) {
        ConfigurationEntity findCs = null;
        if (rp.getUserCfgs() != null) {
          findCs = rp.getUserCfgs()
                     .stream()
                     .filter(cfg -> key.equals(cfg.getCn()))
                     .findAny()
                     .orElse(null);
        }
        if (findCs == null) {
          rp.addUserCfgsItem(new ConfigurationEntity(key, uc.get(key)));
          result = "add new ConfigurationEntity [cn=" + key + ", cv=" + uc.get(key) + "]";
        } else {
          findCs.setCv(uc.get(key));
          result = "update existing " + findCs;
        }
      }
      rpRepo.save(rp);
    } else {
      result = "no ressource property found for " + fu;
    }

    return result;
  }
}
