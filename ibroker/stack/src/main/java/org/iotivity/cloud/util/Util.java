/*
* Copyright (C) 2020 Intel Corporation. All rights reserved.
* SPDX-License-Identifier: Apache-2.0
*/

package org.iotivity.cloud.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.iotivity.cloud.base.device.Device;
import org.iotivity.cloud.base.protocols.IRequest;
import org.iotivity.cloud.base.protocols.MessageBuilder;
import org.iotivity.cloud.base.protocols.coap.CoapMessage;
import org.iotivity.cloud.base.protocols.enums.ContentFormat;
import org.iotivity.cloud.base.protocols.enums.ResponseStatus;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class Util {
  /**
   * utility for responding to coap endpoint
   *
   * @param srcDev
   * @param request
   * @param status
   * @param payload
   */
  public static void CoapResp(
      Device srcDev, IRequest request, ResponseStatus status, byte[] payload) {
    srcDev.sendResponse(
        MessageBuilder.createResponse(request, status, ContentFormat.APPLICATION_JSON, payload));
  }

  /**
   * get the parameter's single value from request
   *
   * @param request
   * @param p
   * @return
   */
  public static String getPara(IRequest request, String p) {
    if (request.getUriQueryMap() == null) {
      return null;
    }

    List<String> ps = request.getUriQueryMap().get(p);
    if (ps == null || ps.size() == 0) {
      return null;
    }

    return ps.get(0);
  }

  /**
   * get the single value from extra option of request
   *
   * @param request
   * @param p
   * @return
   */
  public static String getExtraOption(CoapMessage request, String key) {
    String userDetails = request.getUserString();
    if (userDetails == null) {
      return null;
    }

    HashMap<String, String> usrPrj;
    try {
      usrPrj =
          new ObjectMapper()
              .readValue(userDetails, new TypeReference<HashMap<String, String>>() {});
      return usrPrj == null ? null : usrPrj.get(key);
    } catch (JsonParseException e) {
      e.printStackTrace();
    } catch (JsonMappingException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
   * get the parameter's list value from request
   *
   * @param request
   * @param p
   * @return
   */
  public static List<String> getListPara(IRequest request, String p) {
    if (request.getUriQueryMap() == null) {
      return null;
    }

    return request.getUriQueryMap().get(p);
  }
}
