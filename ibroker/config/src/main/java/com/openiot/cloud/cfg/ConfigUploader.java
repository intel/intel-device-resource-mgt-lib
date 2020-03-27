/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.cfg;

import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.dao.ConfigRepository;
import com.openiot.cloud.base.mongo.model.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ConfigUploader {
  private static Logger logger = LoggerFactory.getLogger(ConfigUploader.class);

  @Value(value = "${ams.addr:127.0.0.1}")
  private String amsUserCloudAddress;

  @Value(value = "${ams.port}")
  private String amsUserCloudPort;

  @Value(
      value = "${ams.update-config.parameter:?product_name=iagent&path_name=%s&target_type=%s&target_id=%s}")
  private String amsCfgPara;

  @Value(value = "${auth.addr}")
  private String authCenterAddress;

  @Value(value = "${auth.port}")
  private String authCenterPort;

  @Autowired
  private ConfigRepository cfgRepo;
  @Autowired
  private RestTemplate restTemplate;

  private AtomicBoolean stop = new AtomicBoolean(false);

  // every 5 seconds
  @Scheduled(fixedRate = 1000 * 5)
  public synchronized void uploadToAms() {
    if (stop.get())
      return;

    List<Config> toRemoved = new ArrayList<>();
    try {
      Pageable pageable = PageRequest.of(0, ConstDef.DFLT_SIZE);
      Page<Config> configs = cfgRepo.findAll(pageable);
      if (configs.isEmpty()) {
        return;
      }

      // a standard header
      HttpHeaders headers = new HttpHeaders();
      headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.setAcceptCharset(Collections.singletonList(Charset.forName("UTF-8")));

      // /api/user/login
      Map<String, String> payloadForLogin = new HashMap<>();
      payloadForLogin.put("username", "beihai");
      payloadForLogin.put("password", "intel@123");

      HttpEntity<Map> loginRequest = new HttpEntity<>(payloadForLogin, headers);

      String authCenterUrl =
          String.format("http://%s:%s/api/user/login", authCenterAddress, authCenterPort);

      ResponseEntity<Map> responseEntity =
          restTemplate.exchange(authCenterUrl, HttpMethod.POST, loginRequest, Map.class);
      if (responseEntity.getStatusCode().is2xxSuccessful()) {
        String token = (String) responseEntity.getBody().getOrDefault("token", "");
        // set token
        headers.setBearerAuth(token);
      } else {
        logger.warn("can not login {} with {} ", authCenterUrl, payloadForLogin);
        return;
      }

      final String amsUrlPattern =
          String.format("%s%s", "http://%s:%s/ams_user_cloud/ams/v1/config/instance", amsCfgPara);

      while (configs.hasContent()) {
        for (Config cf : configs) {
          logger.debug("process a config {} ", cf);
          HttpEntity<String> uploadConfigRequest = new HttpEntity<>(cf.getConfig(), headers);

          String amsUrl = null;
          if (cf.getTargetType().equals(ConstDef.CFG_TT_DEVONGW)) {
            amsUrl = String.format(amsUrlPattern,
                                   amsUserCloudAddress,
                                   amsUserCloudPort,
                                   ConstDef.CFG_PTN_DEVCFG,
                                   ConstDef.CFG_TT_DEVONGW,
                                   URLEncoder.encode(cf.getTargetId(), "UTF-8"));
          } else if (cf.getTargetType().equals(ConstDef.CFG_TT_GRP)) {
            amsUrl = String.format(amsUrlPattern,
                                   amsUserCloudAddress,
                                   amsUserCloudPort,
                                   ConstDef.CFG_PTN_GRPCFG,
                                   ConstDef.CFG_TT_GRP,
                                   URLEncoder.encode(cf.getTargetId(), "UTF-8"));
          } else if (cf.getTargetType().equals(ConstDef.CFG_TT_PRJ)) {
            // TODO need to confirm with Xin if the product name is mushroom_client and tt is
            // ONE-PROJECT
            amsUrl = String.format(amsUrlPattern,
                                   amsUserCloudAddress,
                                   amsUserCloudPort,
                                   ConstDef.CFG_PTN_PRJCFG,
                                   ConstDef.CFG_TT_PRJ,
                                   URLEncoder.encode(cf.getTargetId(), "UTF-8"));
          } else {
            logger.warn("Skip for wrong config target type: {} ", cf);
            continue;
          }

          ResponseEntity<Config> response =
              restTemplate.exchange(amsUrl, HttpMethod.POST, uploadConfigRequest, Config.class);
          if (response.getStatusCode().is2xxSuccessful()) {
            logger.info("successfully upload {} to {}", cf, amsUrl);
            toRemoved.add(cf);
          } else {
            logger.warn("Fail to upload {} to {} with return code {}",
                        cf,
                        amsUrl,
                        response.getStatusCode());
            // continue uploading the next cfg instead of break
            continue;
          }
        }

        pageable = pageable.next();
        configs = cfgRepo.findAll(pageable);
      }
    } catch (Exception e) {
      logger.warn("meet an exception during upload configuration to AMS", e);
    } finally {
      cfgRepo.deleteAll(toRemoved);
      toRemoved.clear();
    }
  }

  public void setStop(boolean flag) {
    stop.set(flag);
  }
}
