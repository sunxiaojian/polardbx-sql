/*
 * Copyright [2013-2021], Alibaba Group Holding Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.polardbx.optimizer.core.rel.ddl.data;

import com.alibaba.polardbx.common.utils.Pair;

import java.util.ArrayList;
import java.util.List;

public class AlterTableGroupRenamePartitionPreparedData extends DdlPreparedData {

    public AlterTableGroupRenamePartitionPreparedData() {
    }

    private String tableGroupName;

    private List<Pair<String, String>> changePartitionsPair;
    private String sourceSql;

    public String getTableGroupName() {
        return tableGroupName;
    }

    public void setTableGroupName(String tableGroupName) {
        this.tableGroupName = tableGroupName;
    }

    public List<Pair<String, String>> getChangePartitionsPair() {
        return changePartitionsPair;
    }

    public void setChangePartitionsPair(
        List<Pair<String, String>> changePartitionsPair) {
        this.changePartitionsPair = changePartitionsPair;
    }

    public String getSourceSql() {
        return sourceSql;
    }

    public void setSourceSql(String sourceSql) {
        this.sourceSql = sourceSql;
    }

    public List<String> getRelatedPartitions() {
        List<String> relatedParts = new ArrayList<>();
        for (Pair<String, String> changePair : changePartitionsPair) {
            relatedParts.add(changePair.getKey());
            relatedParts.add(changePair.getValue());
        }

        return relatedParts;
    }
}
