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

package com.alibaba.polardbx.executor.sync;

import com.alibaba.polardbx.executor.cursor.ResultCursor;
import com.alibaba.polardbx.executor.cursor.impl.ArrayResultCursor;
import com.alibaba.polardbx.gms.topology.ServerInstIdManager;
import com.alibaba.polardbx.optimizer.core.datatype.DataTypes;
import com.alibaba.polardbx.optimizer.planmanager.PlanManager;

public class BaselineQueryAllSyncAction implements ISyncAction {

    public BaselineQueryAllSyncAction() {
    }

    @Override
    public ResultCursor sync() {
        String jsonString = PlanManager.getBaselineAsJson(PlanManager.getInstance().getBaselineMap());

        ArrayResultCursor result = new ArrayResultCursor("baselines");
        result.addColumn("inst_id", DataTypes.StringType);
        result.addColumn("baselines", DataTypes.StringType);
        result.initMeta();
        String instId = ServerInstIdManager.getInstance().getInstId();

        result.addRow(new Object[] {instId, jsonString});
        return result;
    }
}

