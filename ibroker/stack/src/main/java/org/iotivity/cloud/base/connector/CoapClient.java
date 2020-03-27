/*
* Copyright (C) 2020 Intel Corporation. All rights reserved.
* SPDX-License-Identifier: Apache-2.0
*/

/*
 * //******************************************************************
 * //
 * // Copyright 2016 Samsung Electronics All Rights Reserved.
 * //
 * //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * //
 * // Licensed under the Apache License, Version 2.0 (the "License");
 * // you may not use this file except in compliance with the License.
 * // You may obtain a copy of the License at
 * //
 * //      http://www.apache.org/licenses/LICENSE-2.0
 * //
 * // Unless required by applicable law or agreed to in writing, software
 * // distributed under the License is distributed on an "AS IS" BASIS,
 * // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * // See the License for the specific language governing permissions and
 * // limitations under the License.
 * //
 * //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package org.iotivity.cloud.base.connector;

import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.help.ConcurrentRequestMap;
import io.netty.channel.Channel;
import org.iotivity.cloud.base.OICConstants;
import org.iotivity.cloud.base.device.IRequestChannel;
import org.iotivity.cloud.base.device.IResponseEventHandler;
import org.iotivity.cloud.base.exception.ClientException;
import org.iotivity.cloud.base.protocols.IRequest;
import org.iotivity.cloud.base.protocols.IResponse;
import org.iotivity.cloud.base.protocols.MessageBuilder;
import org.iotivity.cloud.base.protocols.coap.CoapRequest;
import org.iotivity.cloud.base.protocols.coap.CoapResponse;
import org.iotivity.cloud.base.protocols.enums.ContentFormat;
import org.iotivity.cloud.base.protocols.enums.Observe;
import org.iotivity.cloud.base.protocols.enums.ResponseStatus;
import org.iotivity.cloud.util.Bytes;
import org.iotivity.cloud.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Random;

public class CoapClient implements IRequestChannel, IResponseEventHandler {
  Logger logger = LoggerFactory.getLogger(CoapClient.class);

  private class RequestInfo {
    private byte[] originToken = null;
    private IRequest originRequest = null;
    private IResponseEventHandler responseHandler = null;
    private Observe observe = Observe.NOTHING;

    public RequestInfo(
        byte[] originToken,
        IRequest originRequest,
        IResponseEventHandler responseHandler,
        Observe observe) {
      this.originToken = originToken;
      this.originRequest = originRequest;
      this.responseHandler = responseHandler;
      this.observe = observe;
    }
  }

  // [WATERMARK] every gateway will send messages from their devices every 5 second
  // [WATERMARK] each gateway would have 10 devices or more specifically 1000 endpoints
  // [WATERMARK] it means each coap client(ibroker) should support 2000 message / second at least
  // [WATERMARK] consider every message is able to stay 20s(timeout), its capacity of requests map
  // is 40,000
  private ConcurrentRequestMap<Long, RequestInfo> mTokenExchanger =
      new ConcurrentRequestMap<>(
          // which means 1048576 requests / 30 seconds -> 34952/second
          40000,
          7,
          20,
          (k, v) -> {
            if (v != null && v.responseHandler != null) {
              IResponse response =
                  MessageBuilder.createResponse(v.originRequest, ResponseStatus.GATEWAY_TIMEOUT);
              ((CoapResponse) response).setToken(v.originToken);
              try {
                v.responseHandler.onResponseReceived(response);
                logger.warn("set Coap Resp to: GATEWAY_TIMEOUT (504)");
              } catch (ClientException e) {
                Log.e(BaseUtil.getStackTrace(e));
              }
            } else {
              logger.warn("response handler is null for req: " + k);
            }
          });

  private Long mToken = 0L;
  private Channel mChannel = null;

  private HashMap<Long, Long> mSubscription = new HashMap<>();
  private Random r = new Random(379);

  public CoapClient(Channel channel) {
    mChannel = channel;
  }

  @Override
  public void sendRequest(IRequest request, IResponseEventHandler responseEvent) {
    try {
      // Exchange request token to internal token and
      // add token with responseHandler to map
      if (mChannel == null) {
        Log.w("channel is null");
        if (responseEvent != null) {
          responseEvent.onResponseReceived(
              MessageBuilder.createResponse(
                  request,
                  ResponseStatus.SERVICE_UNAVAILABLE,
                  ContentFormat.APPLICATION_TEXTPLAIN,
                  "the channel has been disconnected".getBytes()));
        }
        return;
      }

      if (!mChannel.isOpen()
          || !mChannel.isActive()
          || !mChannel.isRegistered()
          || !mChannel.isWritable()) {
        Log.w(
            String.format(
                "channel %s is not in a normal state open(%s),active(%s),writable(%s),registered(%s)",
                mChannel.id().asLongText(),
                mChannel.isOpen(),
                mChannel.isActive(),
                mChannel.isWritable(),
                mChannel.isRegistered()));
        if (responseEvent != null) {
          responseEvent.onResponseReceived(
              MessageBuilder.createResponse(
                  request,
                  ResponseStatus.SERVICE_UNAVAILABLE,
                  ContentFormat.APPLICATION_TEXTPLAIN,
                  "the channel is in a abnormal status".getBytes()));
        }
        return;
      }

      byte[] token = null;
      long newToken;
      synchronized (mToken) {
        newToken = mToken;
      }

      CoapRequest coapRequest = (CoapRequest) request;

      token = coapRequest.getToken();

      Observe observe = request.getObserve();

      switch (request.getObserve()) {
        case UNSUBSCRIBE:
          newToken = removeObserve(Bytes.bytesToLong(token));
          break;

        case SUBSCRIBE:
          addObserve(Bytes.bytesToLong(token), newToken);
        default:
          // We create temp token
          // TODO: temporal handling
          if (request.getUriPath().equals(OICConstants.RESOURCE_PRESENCE_FULL_URI)) {
            addObserve(Bytes.bytesToLong(token), newToken);
            observe = Observe.SUBSCRIBE;
          }

          synchronized (mToken) {
            newToken = mToken++;
          }
          break;
      }

      coapRequest.setToken(Bytes.longTo8Bytes(newToken));
      if (responseEvent != null) {
        mTokenExchanger.put(newToken, new RequestInfo(token, request, responseEvent, observe));
      }

      long finalNewToken = newToken;
      mChannel
          .writeAndFlush(request)
          .addListener(
              future -> {
                if (!future.isSuccess()) {
                  mTokenExchanger.remove(finalNewToken);
                  Log.w("writeAndFlush is failed");
                  if (responseEvent != null) {
                    responseEvent.onResponseReceived(
                        MessageBuilder.createResponse(
                            request,
                            ResponseStatus.SERVICE_UNAVAILABLE,
                            ContentFormat.APPLICATION_TEXTPLAIN,
                            "send failed".getBytes()));
                  }
                }
              });
    } catch (Exception e) {
      Log.f(mChannel, e);
    }
  }

  @Override
  public void onResponseReceived(IResponse response) throws ClientException {
    // Response received from this device.
    // Exchange internal token to request token
    // And call actual requester device
    // Response is always CoapResponse
    CoapResponse coapResponse = (CoapResponse) response;

    RequestInfo reqInfo = mTokenExchanger.remove(Bytes.bytesToLong(coapResponse.getToken()));

    if (reqInfo == null) {
      Log.w("Unable to find " + Bytes.bytesToLong(coapResponse.getToken()));
      return;
    }

    ((CoapRequest) reqInfo.originRequest).setToken(reqInfo.originToken);

    // Subscription response should stored
    if (reqInfo.observe == Observe.UNSUBSCRIBE) {
      mTokenExchanger.remove(Bytes.bytesToLong(coapResponse.getToken()));
      if (mSubscription.containsKey(Bytes.bytesToLong(reqInfo.originToken))) {
        mSubscription.remove(Bytes.bytesToLong(reqInfo.originToken));
      }
    }

    try {
      if (reqInfo.responseHandler != null) {
        coapResponse.setToken(reqInfo.originToken);
        reqInfo.responseHandler.onResponseReceived(coapResponse);
      }
    } catch (Exception e) {
      Log.e(BaseUtil.getStackTrace(e));
    }
  }

  public void addObserve(long token, long newtoken) {
    mSubscription.put(token, newtoken);
  }

  public Long removeObserve(long token) {
    Long getToken = mSubscription.remove(token);
    return getToken;
  }

  public Long isObserveRequest(Long token) {
    Long getToken = null;
    getToken = mSubscription.get(token);

    return getToken;
  }

  public Channel getChannel() {
    return mChannel;
  }
}
