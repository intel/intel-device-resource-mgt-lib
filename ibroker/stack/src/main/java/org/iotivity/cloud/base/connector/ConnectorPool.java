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

import org.iotivity.cloud.base.device.IRequestChannel;
import org.iotivity.cloud.util.Log;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectorPool {

  static ConcurrentHashMap<String, IRequestChannel> mConnection = new ConcurrentHashMap<>();

  static CoapConnector mConnector = new CoapConnector();

  public ConnectorPool() {}

  public static void addConnection(String name, InetSocketAddress inetAddr, boolean tlsMode)
      throws InterruptedException {
    Log.d(String.format("try to setup a new connection named %s to %s", name, inetAddr));
    mConnector.connect(name, inetAddr, tlsMode, true);
    Log.d(String.format("aft setup a new connection named %s to %s", name, inetAddr));
  }

  public static IRequestChannel getConnection(String name) {
    return mConnection.get(name);
  }

  public static ArrayList<IRequestChannel> getConnectionList() {
    return new ArrayList<IRequestChannel>(mConnection.values());
  }

  public static void addConnection(String name, IRequestChannel requestChannel) {
    Log.d(String.format("add a connection named %s into the pool", name));
    mConnection.put(name, requestChannel);
  }

  public static ArrayList<String> getConnectionNameList() {
    return new ArrayList<String>(mConnection.keySet());
  }

  public static boolean containsConnection(String name) {
    return mConnection.containsKey(name);
  }

  public static void disconnectAll() {
    try {
      mConnector.disconnect();
    } catch (Exception e) {
      Log.w("meet an exception during disconnectAll", e);
    } finally {
      mConnection.clear();
    }
  }

  public static void shutdown() {
    mConnector.shutdown();
  }

  public static IRequestChannel getConnectionWithMinMatch(String name) {
    if (name == null) return null;

    String matchedPath = null;
    IRequestChannel matchedConnection = null;
    for (String key : mConnection.keySet()) {
      if (name.startsWith(key)) {
        if (matchedPath == null || matchedPath.length() < key.length()) {
          matchedPath = key;
          matchedConnection = mConnection.get(key);
        }
      }
    }
    return matchedConnection;
  }
}
