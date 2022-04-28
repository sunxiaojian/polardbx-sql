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

package com.alibaba.polardbx.executor.handler;

import com.alibaba.polardbx.common.utils.logger.Logger;
import com.alibaba.polardbx.common.utils.logger.LoggerFactory;
import com.alibaba.polardbx.executor.ddl.job.factory.localpartition.RepartitionLocalPartitionJobFactory;
import com.alibaba.polardbx.executor.ddl.newengine.job.DdlJob;
import com.alibaba.polardbx.executor.handler.ddl.LogicalCommonDdlHandler;
import com.alibaba.polardbx.executor.spi.IRepository;
import com.alibaba.polardbx.executor.utils.PolarPrivilegeUtils;
import com.alibaba.polardbx.optimizer.context.ExecutionContext;
import com.alibaba.polardbx.optimizer.core.rel.ddl.BaseDdlOperation;
import com.alibaba.polardbx.optimizer.partition.LocalPartitionDefinitionInfo;
import com.taobao.tddl.common.privilege.PrivilegePoint;
import org.apache.calcite.sql.SqlAlterTable;
import org.apache.calcite.sql.SqlAlterTableRepartitionLocalPartition;

public class LogicalAlterTableRepartitionLocalPartitionHandler extends LogicalCommonDdlHandler {

    private static final Logger logger = LoggerFactory.getLogger(LogicalAlterTableRepartitionLocalPartitionHandler.class);

    public LogicalAlterTableRepartitionLocalPartitionHandler(IRepository repo) {
        super(repo);
    }

    @Override
    public DdlJob buildDdlJob(BaseDdlOperation logicalDdlPlan, ExecutionContext executionContext) {
        SqlAlterTable sqlAlterTable = (SqlAlterTable) logicalDdlPlan.getNativeSqlNode();
        SqlAlterTableRepartitionLocalPartition sqlAllocateLocalPartition = (SqlAlterTableRepartitionLocalPartition) sqlAlterTable;
        final String schemaName = logicalDdlPlan.getSchemaName();
        final String primaryTableName = logicalDdlPlan.getTableName();

        PolarPrivilegeUtils.checkPrivilege(schemaName, primaryTableName, PrivilegePoint.ALTER, executionContext);
        PolarPrivilegeUtils.checkPrivilege(schemaName, primaryTableName, PrivilegePoint.DROP, executionContext);

        LocalPartitionDefinitionInfo definitionInfo = LocalPartitionDefinitionInfo.create(
            schemaName,
            primaryTableName,
            sqlAllocateLocalPartition.getLocalPartition()
        );

        //3. 执行
        return new RepartitionLocalPartitionJobFactory(
                schemaName,
                primaryTableName,
                definitionInfo,
                logicalDdlPlan.relDdl,
                executionContext
            ).create();
    }

}