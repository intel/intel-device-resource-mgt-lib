/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.testbase;

import com.openiot.cloud.base.help.BaseUtil;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.dao.ResProRepository;
import com.openiot.cloud.base.mongo.dao.ResourceRepository;
import com.openiot.cloud.base.mongo.model.ResProperty;
import com.openiot.cloud.base.mongo.model.Resource;
import org.iotivity.cloud.base.connector.ConnectorPool;
import org.iotivity.cloud.base.protocols.IResponse;
import org.iotivity.cloud.base.protocols.enums.ContentFormat;
import org.iotivity.cloud.base.protocols.enums.RequestMethod;
import org.iotivity.cloud.base.protocols.enums.ResponseStatus;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import java.net.InetSocketAddress;
import java.util.Date;

/**
 * Need to start services(rd, dp) first to run GenTestDb, and these services should use test_openiot
 * database
 *
 * <p>Warning: be careful, GenTestDb will erase all data from database!!! Make sure you change
 * services connected to test database!!!
 *
 * @author <a href="mailto:hualiang.deng@intel.com">Hualiang Deng</a>
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@SpringBootApplication(scanBasePackages = {"com.openiot.cloud"})
@TestPropertySource({"classpath:application-test.properties"})
public class GenImportDb {

  @Autowired
  ResourceRepository resRepo;
  @Autowired
  ResProRepository proRepo;

  @Autowired
  TestUtil tu;

  @Before
  public void setUp() throws Exception {
    ConnectorPool.addConnection(ConstDef.U_RD, new InetSocketAddress("127.0.0.1", 5684), false);
    ConnectorPool.addConnection(ConstDef.U_DP, new InetSocketAddress("127.0.0.1", 5685), false);

    tu.dropCollection(ConstDef.C_GRPTYPE, ConstDef.C_DEVTYPE, ConstDef.C_RESTYPE);
    tu.importTestDb(ConstDef.C_GRPTYPE, ConstDef.C_DEVTYPE, ConstDef.C_RESTYPE);
  }

  @After
  public void tearDown() throws Exception {
    ConnectorPool.disconnectAll();
  }

  @Test
  public void generate() throws Exception {
    // > generate devices, resources, resProperties, devSessions
    IResponse iRsp = tu.sendIRequest(ConstDef.U_RD,
                                     RequestMethod.POST,
                                     BaseUtil.composeUri(ConstDef.U_RD),
                                     "",
                                     "/GenImportDb/updateRd-request.json");
    assertEquals(ResponseStatus.CHANGED, iRsp.getStatus());

    // > generate groups
    iRsp = tu.sendIRequest(ConstDef.U_RD,
                           RequestMethod.PUT,
                           BaseUtil.composeUri(ConstDef.U_RD, ConstDef.U_GRP),
                           "",
                           "/GenImportDb/create-group1-payload.json");
    assertEquals(ResponseStatus.CREATED, iRsp.getStatus());
    iRsp = tu.sendIRequest(ConstDef.U_RD,
                           RequestMethod.PUT,
                           BaseUtil.composeUri(ConstDef.U_RD, ConstDef.U_GRP),
                           "",
                           "/GenImportDb/create-group2-payload.json");
    assertEquals(ResponseStatus.CREATED, iRsp.getStatus());

    // > add user configuration
    iRsp =
        tu.sendIRequest(ConstDef.U_RD,
                        RequestMethod.POST,
                        BaseUtil.composeUri(ConstDef.U_RD, ConstDef.U_DEV),
                        BaseUtil.composeQuery(new String[] {ConstDef.Q_ID, TestConstDef.I_DEV1}),
                        "{\"cs\" : { \"uc\" : { \"shared\": true, \"floor\": \"1st\" } } }".getBytes());
    assertEquals(ResponseStatus.CHANGED, iRsp.getStatus());

    Resource r3 = resRepo.findOneByDevIdAndUrl(TestConstDef.I_DEV2, TestConstDef.U_RES3);
    iRsp =
        tu.sendIRequest(ConstDef.U_RD,
                        RequestMethod.POST,
                        BaseUtil.composeUri(ConstDef.U_RD, ConstDef.U_RES),
                        BaseUtil.composeQuery(new String[] {ConstDef.Q_ID, r3.getId()}),
                        "{\"cs\" : { \"uc\" : { \"obs\": true, \"life\": 300, \"tag\": \"obsRes\" } } }".getBytes());
    assertEquals(ResponseStatus.CHANGED, iRsp.getStatus());

    ResProperty p5 =
        proRepo.filter(TestConstDef.I_DEV2, null, TestConstDef.N_RT2PRO2, true, null).get(0);
    iRsp =
        tu.sendIRequest(ConstDef.U_RD,
                        RequestMethod.POST,
                        BaseUtil.composeUri(ConstDef.U_RD, ConstDef.U_RESPRO),
                        BaseUtil.composeQuery(new String[] {ConstDef.Q_ID, p5.getId()}),
                        "{ \"uc\" : { \"o\": true, \"omin\": 60, \"ci\": -12, \"tl\": -25, \"th\": 100 } }".getBytes());
    assertEquals(ResponseStatus.CHANGED, iRsp.getStatus());

    // > generate more device sessions
    for (int i = 0; i < 3; i++) {
      Thread.sleep(5000);
      iRsp = tu.sendIRequest(ConstDef.U_RD,
                             RequestMethod.DELETE,
                             BaseUtil.composeUri(ConstDef.U_RD, ConstDef.U_DEVSESS),
                             BaseUtil.composeQuery(new String[] {ConstDef.Q_DEVID,
                                 TestConstDef.I_DEV1}),
                             ("{\"e\" : " + new Date().getTime() + "}").getBytes());
      assertEquals(ResponseStatus.CHANGED, iRsp.getStatus());
      iRsp = tu.sendIRequest(ConstDef.U_RD,
                             RequestMethod.DELETE,
                             BaseUtil.composeUri(ConstDef.U_RD, ConstDef.U_DEVSESS),
                             BaseUtil.composeQuery(new String[] {ConstDef.Q_DEVID,
                                 TestConstDef.I_DEV2}),
                             ("{\"e\" : " + new Date().getTime() + "}").getBytes());
      assertEquals(ResponseStatus.CHANGED, iRsp.getStatus());

      Thread.sleep(5000);
      iRsp = tu.sendIRequest(ConstDef.U_RD,
                             RequestMethod.POST,
                             BaseUtil.composeUri(ConstDef.U_RD),
                             "",
                             "/GenImportDb/updateRd-request.json");
      assertEquals(ResponseStatus.CHANGED, iRsp.getStatus());
    }

    // > generate data
    for (int i = 0; i < 3; i++) {
      tu.sendIRequest(ConstDef.U_DP,
                      RequestMethod.PUT,
                      BaseUtil.composeUri(ConstDef.U_DP,
                                          ConstDef.C_DEV /* place holder for devType */,
                                          TestConstDef.I_DEV2,
                                          TestConstDef.U_RES3,
                                          TestConstDef.N_RT1PRO1),
                      null,
                      ContentFormat.APPLICATION_TEXTPLAIN,
                      "25".getBytes()); // - 25 will be encoded to
      // "MjU=" by
      // Base64

      assertEquals(true, false); // receives error response;
      tu.sendIRequest(ConstDef.U_DP,
                      RequestMethod.PUT,
                      BaseUtil.composeUri(ConstDef.U_DP,
                                          ConstDef.C_DEV /* place holder for devType */,
                                          TestConstDef.I_DEV2,
                                          TestConstDef.U_RES3,
                                          TestConstDef.N_RT2PRO1),
                      null,
                      ContentFormat.APPLICATION_TEXTPLAIN,
                      "80".getBytes());

      assertEquals(true, false); // receives error response;
      tu.sendIRequest(ConstDef.U_DP,
                      RequestMethod.PUT,
                      BaseUtil.composeUri(ConstDef.U_DP,
                                          ConstDef.C_DEV /* place holder for devType */,
                                          TestConstDef.I_DEV2,
                                          TestConstDef.U_RES3,
                                          TestConstDef.N_RT2PRO2),
                      null,
                      ContentFormat.APPLICATION_TEXTPLAIN,
                      "normal".getBytes());

      assertEquals(true, false); // receives error response;
    }
  }
}
