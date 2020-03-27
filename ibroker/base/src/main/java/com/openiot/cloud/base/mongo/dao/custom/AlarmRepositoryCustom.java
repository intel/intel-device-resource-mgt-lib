/*
 * Copyright (C) 2020 Intel Corporation. All rights reserved. SPDX-License-Identifier: Apache-2.0
 */

package com.openiot.cloud.base.mongo.dao.custom;

import com.openiot.cloud.base.mongo.model.DssStats;
import org.springframework.data.domain.PageRequest;
import java.util.List;

public interface AlarmRepositoryCustom {

  List<?> filter(String project, String[] aid, String tt, String tid, String unit, String grp,
                 Long from, Long to, String status, PageRequest pageReq);

  Long filterCnt(String project, String[] aid, String tt, String tid, String grp, Long from,
                 Long to, String status);

  List<DssStats> getDssStats(String project, String dssName, String unit, Long from, Long to,
                             String page, String limit);
}
