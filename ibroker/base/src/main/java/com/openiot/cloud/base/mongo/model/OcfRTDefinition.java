/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.openiot.cloud.base.mongo.model.ResourceType.PropertyType;
import com.openiot.cloud.base.mongo.model.validator.CheckName;
import com.openiot.cloud.base.mongo.model.validator.CreateValidator;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// @formatter:off
// [
//    {
//        "definitions": {
//            "description": "description of the resource",
//            "properties": [
//                {
//                    "description": "description of the property",
//                    "ext": [
//                        {
//                            "ext_n": "default_unit",
//                            "ext_v": "cm"
//                        }
//                    ],
//                    "name": "speed",
//                    "readOnly": true,
//                    "type": "String"
//                },
//                {
//                    "description": "description of the property",
//                    "name": "direction",
//                    "type": "String"
//                },
//                {
//                    "items": {
//                        "minItems": 1,
//                        "type": "string",
//                        "uniqueItems": true
//                    },
//                    "name": "support-directions",
//                    "type": "array"
//                }
//            ],
//            "rt": "oic.r.airflow"
//        },
//        "required": [
//            "supportdirections",
//            "direction"
//        ],
//        "title": "It is a title",
//        "type": "object"
//    }
// ]
// @formatter:on
@JsonInclude(value = Include.NON_EMPTY)
@Getter
@Setter
@ToString
public class OcfRTDefinition {
  @Getter
  @Setter
  @ToString
  public static class ArrayPropDefEntry {
    @JsonProperty("type")
    String type;

    @JsonProperty("minItems")
    String minItems;

    @JsonProperty("uniqueItems")
    String uniqueItems;
  }

  @JsonInclude(value = Include.NON_EMPTY)
  @Getter
  @Setter
  @ToString
  public static class ExtPropDefEntry {
    @JsonProperty("ext_n")
    String ext_n;

    @JsonProperty("ext_v")
    String ext_v;

    @Override
    public String toString() {
      return "ExtPropDefEntry [ext_n=" + ext_n + ", ext_v=" + ext_v + "]";
    }
  }

  @JsonInclude(value = Include.NON_EMPTY)
  @Getter
  @Setter
  @ToString
  public static class PropDefEntry {
    @JsonProperty("name")
    @NotNull(groups = {CreateValidator.class})
    String name;

    @JsonProperty("type")
    String type;

    @JsonProperty("description")
    String description;

    @JsonProperty("readOnly")
    boolean readOnly;

    @JsonProperty("ext")
    List<ExtPropDefEntry> ext;

    static PropDefEntry from(PropertyType propt) {
      PropDefEntry pde = new PropDefEntry();
      Optional.ofNullable(propt.getName()).ifPresent(n -> pde.setName(n));
      Optional.ofNullable(propt.getDecription()).ifPresent(d -> pde.setDescription(d));
      Optional.ofNullable(propt.getType()).ifPresent(t -> pde.setType(t));
      Optional.ofNullable(propt.getAccess())
              .ifPresent(acc -> pde.setReadOnly(acc.compareTo("r") == 0));
      return pde;
    }
  }

  @JsonInclude(value = Include.NON_EMPTY)
  @Getter
  @Setter
  @ToString
  public static class ResDefEntry {
    @JsonProperty("rt")
    @NotNull(groups = {CreateValidator.class})
    @CheckName(value = ResDefEntry.class, message = "need an unique name",
        groups = {CreateValidator.class})
    String rt;

    @JsonProperty("description")
    String description;

    @JsonProperty("properties")
    @NotNull
    @NotEmpty
    @Valid
    List<PropDefEntry> properties;

    @JsonProperty("type")
    String type;

    static ResDefEntry from(ResourceType rt) {
      return Optional.ofNullable(rt.getName()).map(rtn -> {
        ResDefEntry rde = new ResDefEntry();
        // resource type name is necessary
        rde.setRt(rtn);

        // others are optional
        Optional.ofNullable(rt.getDescription()).ifPresent(d -> rde.setDescription(d));

        Optional.ofNullable(rt.getPropTypes())
                .filter(propts -> !propts.isEmpty())
                .map(propts -> propts.stream()
                                     .map(propt -> PropDefEntry.from(propt))
                                     .collect(Collectors.toList()))
                .ifPresent(pdes -> rde.setProperties(pdes));

        Optional.ofNullable(rt.getPropTypes())
                .filter(propts -> !propts.isEmpty())
                .map(propts -> propts.stream()
                                     .filter(propt -> !propt.getMandatory())
                                     .map(propt -> propt.getName())
                                     .collect(Collectors.toList()));
        return rde;
      }).orElse(null);
    }
  }

  @JsonIgnore
  String id;

  @JsonProperty("definitions")
  @NotNull(groups = {CreateValidator.class})
  @Valid
  ResDefEntry definitions;

  @JsonProperty("required")
  List<String> required;

  @JsonProperty("title")
  String title;

  @JsonProperty("type")
  String type;

  public static OcfRTDefinition from(ResourceType rt) {
    return Optional.ofNullable(rt.getId()).map(id -> {
      OcfRTDefinition rtDef = new OcfRTDefinition();
      rtDef.setId(id);

      Optional.ofNullable(rt.getTitle()).ifPresent(title -> rtDef.setTitle(title));
      rtDef.setType("object");

      Optional<ResDefEntry> resDef = Optional.ofNullable(ResDefEntry.from(rt));
      resDef.ifPresent(rdef -> {
        rtDef.setDefinitions(rdef);
      });

      rtDef.setRequired(Optional.ofNullable(rt.getPropTypes())
                                .map(propertyTypes -> propertyTypes.stream()
                                                                   .filter(propertyType -> propertyType.getMandatory())
                                                                   .map(propertyType -> propertyType.getName())
                                                                   .collect(Collectors.toList()))
                                .orElse(null));

      return rtDef;
    }).orElse(null);
  }
}
