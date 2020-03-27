/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.help;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openiot.cloud.base.Application;
import com.openiot.cloud.base.mongo.model.Device;
import com.openiot.cloud.base.mongo.model.Group;
import com.openiot.cloud.base.mongo.model.help.AttributeEntity;
import com.openiot.cloud.base.mongo.model.help.ConfigurationEntity;
import com.openiot.cloud.base.mongo.model.validator.CreateValidator;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import javax.validation.Validator;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Collections;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class})
public class BaseUtilTest {

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  static class Foo {
    @JsonProperty("firstName")
    @NotNull(groups = {CreateValidator.class})
    @NotEmpty(groups = {CreateValidator.class})
    private String firstName;

    @JsonProperty("lastName")
    @NotNull(groups = {CreateValidator.class})
    @NotEmpty(groups = {CreateValidator.class})
    private String lastName;

    public Foo() {}

    public Foo(String firstName, String lastName) {
      this.firstName = firstName;
      this.lastName = lastName;
    }

    @Override
    public String toString() {
      return "Foo{" + "firstName='" + firstName + '\'' + ", lastName='" + lastName + '\'' + '}';
    }
  }

  private ObjectMapper mapper = new ObjectMapper();

  @Autowired
  private Validator validator;

  @Test
  public void testCheckPayload() throws Exception {
    Foo foo = new Foo(null, "Lee");
    String json = mapper.writeValueAsString(foo);

    BaseUtil.PayloadCheckResult result = BaseUtil.checkPayload(mapper.writeValueAsBytes(foo),
                                                               Foo.class,
                                                               validator,
                                                               CreateValidator.class);
    assertThat(result.isPass()).isFalse();

    foo = new Foo("", "Lee");
    result = BaseUtil.checkPayload(mapper.writeValueAsBytes(foo),
                                   Foo.class,
                                   validator,
                                   CreateValidator.class);
    assertThat(result.isPass()).isFalse();

    Foo[] fooArray = new Foo[] {new Foo("A", "Lee"), new Foo("B", "ZOk"), new Foo("", null)};
    result = BaseUtil.checkPayload(mapper.writeValueAsBytes(fooArray),
                                   Foo[].class,
                                   validator,
                                   CreateValidator.class);
    assertThat(result.isPass()).isFalse();
  }

  @Test
  public void testReplace() throws Exception {
    Device.Config configSrc = new Device.Config();
    configSrc.setDataLife(10);
    configSrc.setAttributes(Arrays.asList(new AttributeEntity("honeydew", "123"),
                                          new AttributeEntity("strawberry", "3.14")));

    Device.Config configDst = new Device.Config();
    configDst.setRefNum(1);
    configDst.setAttributes(Arrays.asList(new AttributeEntity("fig", "23"),
                                          new AttributeEntity("kiwi", "29")));

    // to replace an array
    configDst = (Device.Config) BaseUtil.replace(configSrc, configDst);
    assertThat(configDst).hasFieldOrPropertyWithValue("refNum", 1)
                         .hasFieldOrPropertyWithValue("dataLife", 10);
    assertThat(configDst.getAttributes()).extracting("av").containsOnly("123", "3.14");
    assertThat(configDst.getUserCfgs()).isNull();

    // to replace an null array
    configSrc.setUserCfgs(Arrays.asList(new ConfigurationEntity("pear", "17"),
                                        new ConfigurationEntity("pineapple", "19")));
    configDst = (Device.Config) BaseUtil.replace(configSrc, configDst);
    assertThat(configDst.getAttributes()).extracting("av").containsOnly("123", "3.14");
    assertThat(configDst.getUserCfgs()).extracting("cn").containsOnly("pear", "pineapple");

    Device src = new Device();
    src.setId("dev1");
    src.setName("dev1");
    src.setConnected(Boolean.FALSE);
    src.setGrps(Arrays.asList("grp1", "grp2", "grp3"));
    src.setConfig(configSrc);
    src.setDeviceType("mongo");

    Device dst = new Device();
    dst.setId("dev2");

    // a normal replace
    dst = (Device) BaseUtil.replace(src, dst);
    assertThat(dst).hasFieldOrPropertyWithValue("id", "dev2")
                   .hasFieldOrPropertyWithValue("deviceType", "mongo")
                   .hasFieldOrPropertyWithValue("name", "dev1")
                   .hasFieldOrPropertyWithValue("connected", Boolean.FALSE);
    assertThat(dst.getGrps()).containsOnly("grp1", "grp2", "grp3");
    assertThat(dst.getConfig()).hasFieldOrPropertyWithValue("dataLife", 10);
    assertThat(dst.getConfig().getAttributes()).extracting("av").containsOnly("123", "3.14");

    // to "delete" with an empty string
    src.setName("");
    dst = (Device) BaseUtil.replace(src, dst);
    assertThat(dst.getName()).isNull();

    // to do nothing when it is a null string
    src.setDeviceType(null);
    dst = (Device) BaseUtil.replace(src, dst);
    assertThat(dst).hasFieldOrPropertyWithValue("deviceType", "mongo");
  }

  @Test
  public void testRemoveTrailingSlash() throws Exception {
    assertThat(BaseUtil.removeTrailingSlash("/")).isEqualTo("/");
    assertThat(BaseUtil.removeTrailingSlash("///")).isEqualTo("/");
    assertThat(BaseUtil.removeTrailingSlash("abc")).isEqualTo("abc");
    assertThat(BaseUtil.removeTrailingSlash("abc///")).isEqualTo("abc");
    assertThat(BaseUtil.removeTrailingSlash("/abc")).isEqualTo("/abc");
    assertThat(BaseUtil.removeTrailingSlash("/a/b/c")).isEqualTo("/a/b/c");
    assertThat(BaseUtil.removeTrailingSlash("/abc///")).isEqualTo("/abc");
    assertThat(BaseUtil.removeTrailingSlash("///a/b/c///")).isEqualTo("/a/b/c");
  }

  @Test
  public void testParseArray() throws Exception {
    assertThat(BaseUtil.parseArray("")).isEmpty();
    assertThat(BaseUtil.parseArray("[]")).isEmpty();
    assertThat(BaseUtil.parseArray("[a]")).hasSize(1).containsOnly("a");
    assertThat(BaseUtil.parseArray("[a,b,]")).hasSize(2).containsOnly("a", "b");
    assertThat(BaseUtil.parseArray("[a,b,,]")).hasSize(2).containsOnly("a", "b");
    assertThat(BaseUtil.parseArray("[a  ,    b,     ,,,,]")).hasSize(2).containsOnly("a", "b");
    assertThat(BaseUtil.parseArray("[a,b,c]")).hasSize(3).containsOnly("a", "b", "c");
  }

  @Test
  public void testCopyPropertiesIgnoreNullWithNull() throws Exception {
    Group source = new Group();
    source.setN("apple");
    source.setMd(Arrays.asList("watermelon", "kumquat", "boysenberry"));

    Group target = new Group();
    target.setD("it is not a plum");
    target.setAs(Arrays.asList(new AttributeEntity("papaya", "1")));

    BaseUtil.copyPropertiesIgnoreCollectionNull(source, target);
    assertThat(target).hasFieldOrPropertyWithValue("n", "apple").hasFieldOrPropertyWithValue("d",
                                                                                             null);
    assertThat(target.getMd()).isEqualTo(source.getMd());
    assertThat(target.getAs()).extracting("an").containsOnly("papaya");
  }

  @Test
  public void testCopyPropertiesIgnoreNullWithEmpty() throws Exception {
    Group source = new Group();
    source.setN("apple");
    source.setMd(Arrays.asList("watermelon", "kumquat", "boysenberry"));
    source.setAs(Collections.emptyList());

    Group target = new Group();
    target.setD("it is not a plum");
    target.setAs(Arrays.asList(new AttributeEntity("papaya", "1")));

    BaseUtil.copyPropertiesIgnoreCollectionNull(source, target);
    assertThat(target).hasFieldOrPropertyWithValue("n", "apple")
                      .hasFieldOrPropertyWithValue("d", null)
                      .hasFieldOrPropertyWithValue("as", null);

    assertThat(target.getMd()).isEqualTo(source.getMd());
  }
}
