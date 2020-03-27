/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.Application;
import static org.assertj.core.api.Assertions.assertThat;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class}, properties = {"mongo.db = test_openiot"})
public class ReferenceDefinitionTest {

  @Test
  public void testBasic() throws Exception {
    JSONObject json =
        new JSONObject().put("from", 12345678)
                        .put("to", 12348901)
                        .put("dsri",
                             new JSONArray().put(new JSONObject().put("di", "dev")
                                                                 .put("res", "resUrl")
                                                                 .put("pt", "propName")));
    TypeReference<HashMap<String, Object>> typeRef =
        new TypeReference<HashMap<String, Object>>() {};
    Map<String, Object> objectMap = new ObjectMapper().readValue(json.toString(), typeRef);
    System.out.println(objectMap);
    assertThat(objectMap.containsKey("dsri")).isTrue();
  }
}
