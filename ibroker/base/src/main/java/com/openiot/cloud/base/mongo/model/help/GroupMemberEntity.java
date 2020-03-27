/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.model.help;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.openiot.cloud.base.help.ConstDef;
import com.openiot.cloud.base.mongo.model.Group.MemberResRef;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.List;

@Data
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
public class GroupMemberEntity {
  @Field(ConstDef.F_MD)
  @JsonProperty(ConstDef.F_MD)
  List<String> md;

  @Field(ConstDef.F_MR)
  @JsonProperty(ConstDef.F_MR)
  List<MemberResRef> mr;
}
