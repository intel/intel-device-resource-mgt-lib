package com.openiot.cloud.base.service.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openiot.cloud.base.help.ConstDef;
import lombok.Data;

/**
 should be as same as the com.openiot.cloud.service.dto.GatewayDTO.
 Use for resourcedirectory/src/main/java/com/openiot/cloud/rd/service/ResourceHierarchyService.java
 */
@Data
public class GatewayDTO {
  @JsonProperty(ConstDef.F_PROV_IAGENTID)
  private String iAgentId;
  @JsonProperty(ConstDef.F_PROV_SERIALNUM)
  private String hwSn;
  private String provKey;
  private long provTime;
  private boolean reset;
  private String newHwSn;
  private String domain;
  private String projectId;
  private boolean manual;
}
