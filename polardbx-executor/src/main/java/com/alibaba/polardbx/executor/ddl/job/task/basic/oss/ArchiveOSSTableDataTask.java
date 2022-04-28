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

package com.alibaba.polardbx.executor.ddl.job.task.basic.oss;

import com.alibaba.fastjson.annotation.JSONCreator;
import com.alibaba.polardbx.common.Engine;
import com.alibaba.polardbx.common.ddl.newengine.DdlTaskState;
import com.alibaba.polardbx.common.exception.TddlRuntimeException;
import com.alibaba.polardbx.common.exception.code.ErrorCode;
import com.alibaba.polardbx.common.properties.ConnectionParams;
import com.alibaba.polardbx.common.utils.GeneralUtil;
import com.alibaba.polardbx.common.utils.Pair;
import com.alibaba.polardbx.executor.archive.writer.OSSBackFillExecutor;
import com.alibaba.polardbx.executor.archive.writer.OSSBackFillWriterTask;
import com.alibaba.polardbx.executor.ddl.job.meta.TableMetaChanger;
import com.alibaba.polardbx.executor.ddl.job.task.BaseGmsTask;
import com.alibaba.polardbx.executor.ddl.job.task.util.TaskName;
import com.alibaba.polardbx.executor.gsi.GsiBackfillManager;
import com.alibaba.polardbx.gms.engine.FileSystemUtils;
import com.alibaba.polardbx.gms.metadb.table.ColumnMetasRecord;
import com.alibaba.polardbx.gms.metadb.table.FilesRecord;
import com.alibaba.polardbx.gms.util.MetaDbUtil;
import com.alibaba.polardbx.optimizer.OptimizerContext;
import com.alibaba.polardbx.optimizer.config.table.OrcMetaUtils;
import com.alibaba.polardbx.optimizer.config.table.PolarDBXOrcSchema;
import com.alibaba.polardbx.optimizer.config.table.TableMeta;
import com.alibaba.polardbx.optimizer.context.ExecutionContext;
import com.alibaba.polardbx.optimizer.partition.PartitionInfo;
import com.alibaba.polardbx.optimizer.partition.pruning.PhysicalPartitionInfo;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import org.apache.hadoop.conf.Configuration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Getter
@TaskName(name = "ArchiveOSSTableDataTask")
public class ArchiveOSSTableDataTask extends BaseGmsTask {
    private final String loadTableSchema;
    private final String loadTableName;
    private final String physicalPartitionName;

    private final Engine targetTableEngine;

    @JSONCreator
    public ArchiveOSSTableDataTask(String schemaName, String logicalTableName,
                                   String loadTableSchema, String loadTableName,
                                   String physicalPartitionName, Engine targetTableEngine) {
        super(schemaName, logicalTableName);
        this.loadTableSchema = loadTableSchema;
        this.loadTableName = loadTableName;
        this.physicalPartitionName = physicalPartitionName;
        this.targetTableEngine = targetTableEngine;
        onExceptionTryRollback();
    }

    @Override
    protected void beforeTransaction(ExecutionContext executionContext) {
        updateSupportedCommands(true, true, null);
        updateTaskStateInNewTxn(DdlTaskState.DIRTY);
    }

    @Override
    protected void executeImpl(Connection metaDbConnection, ExecutionContext executionContext) {
        executionContext.setBackfillId(getTaskId());
        // don't continue the ddl if it was paused
        try (Connection metaDbConn = MetaDbUtil.getConnection()) {
            try {
                MetaDbUtil.beginTransaction(metaDbConn);
                List<FilesRecord> files = TableMetaChanger.lockOssFileMeta(metaDbConn, getTaskId(), schemaName, logicalTableName);
                if (files != null && files.size() > 0) {
                    throw new TddlRuntimeException(ErrorCode.ERR_CANT_CONTINUE_DDL);
                }
                MetaDbUtil.commit(metaDbConn);
            } catch (Exception e) {
                MetaDbUtil.rollback(metaDbConn, e, null, null);

                throw GeneralUtil.nestedException(e);
            } finally {
                MetaDbUtil.endTransaction(metaDbConn, null);
            }
        } catch (Exception e) {
            throw GeneralUtil.nestedException(e);
        }
        loadTable(executionContext);
    }

    @Override
    protected void rollbackImpl(Connection metaDbConnection, ExecutionContext executionContext) {
        List<FilesRecord> files = TableMetaChanger.lockOssFileMeta(metaDbConnection, getTaskId(), schemaName, logicalTableName);

        // delete remote oss files and local tmp files
        for (FilesRecord record : files) {
            FileSystemUtils.deleteIfExistsFile(record.getFileName(), this.targetTableEngine);
            File tmpFile = new File(record.getLocalPath());
            if (tmpFile.exists()) {
                if (!tmpFile.delete()) {
                    throw new TddlRuntimeException(ErrorCode.ERR_BACK_FILL_FAIL,
                        "can't delete file " + record.getLocalPath());
                }
            }
        }
        TableMetaChanger.deleteOssFileMeta(metaDbConnection, getTaskId(), schemaName, logicalTableName);

        // delete column meta and bf files
        List<ColumnMetasRecord> columnMetas = TableMetaChanger.lockOssColumnMeta(metaDbConnection, getTaskId(),
            schemaName, logicalTableName);
        for (ColumnMetasRecord record : columnMetas) {
            FileSystemUtils.deleteIfExistsFile(record.tableFileName, this.targetTableEngine);
        }
        TableMetaChanger.deleteOssColumnMeta(metaDbConnection, getTaskId(), schemaName, logicalTableName);

        // clear back-fill states
        // if checkpoint resume is supported, this code block should be removed.
        GsiBackfillManager manager = new GsiBackfillManager(schemaName);
        manager.deleteByBackfillId(getTaskId());
    }

    private void loadTable(ExecutionContext executionContext) {
        Map<Pair<String, String>, OSSBackFillWriterTask> tasks = null;
        try (Connection metaDbConn = MetaDbUtil.getConnection()) {
            try {
                String sourceLogicalSchema = this.loadTableSchema;
                String sourceLogicalTable = this.loadTableName;
                String targetLogicalSchema = this.schemaName;
                String targetLogicalTable = this.logicalTableName;

                ExecutionContext sourceDbContext = executionContext.copy();
                sourceDbContext.setSchemaName(sourceLogicalSchema);

                TableMeta sourceTableMeta =
                    executionContext.getSchemaManager(sourceLogicalSchema).getTable(sourceLogicalTable);
                if (!sourceTableMeta.isHasPrimaryKey()) {
                    throw new AssertionError("Table must have primary key");
                }

                // build orc schema
                PolarDBXOrcSchema orcSchema = OrcMetaUtils.buildPolarDBXOrcSchema(sourceTableMeta);

                // data config
                Configuration conf = OrcMetaUtils.getConfiguration(executionContext, orcSchema);

                tasks = buildOssBackFillLoaderTasks(
                    executionContext,
                    sourceLogicalSchema,
                    sourceLogicalTable,
                    targetLogicalSchema,
                    targetLogicalTable,
                    sourceTableMeta,
                    orcSchema,
                    conf);

                Map<String, Set<String>> sourcePhyTables = sourceTableMeta.getLatestTopology();
                final int parallelism =
                    executionContext.getParamManager().getInt(ConnectionParams.OSS_BACKFILL_PARALLELISM);
                final long indexStride =
                    executionContext.getParamManager().getLong(ConnectionParams.OSS_ORC_INDEX_STRIDE);

                // do back fill: select source table -> fill target orc file
                OSSBackFillExecutor backFillExecutor = new OSSBackFillExecutor(Engine.INNODB, this.targetTableEngine);
                backFillExecutor
                    .backFill2FileStore(sourceLogicalSchema, sourceLogicalTable, targetLogicalTable, sourceDbContext, sourcePhyTables,
                        (int) indexStride,
                        parallelism, tasks, physicalPartitionName);

                // flush all
                tasks.forEach((pair, task) -> task.flush(sourceDbContext));

                // wait all async task done.
                tasks.forEach((pair, task) -> task.waitAsync());

                // valid the meta files and column metas
                MetaDbUtil.beginTransaction(metaDbConn);
                TableMetaChanger.lockOssFileMeta(metaDbConn, getTaskId(), schemaName, logicalTableName);
                TableMetaChanger.validOssFileMeta(metaDbConn, getTaskId(), schemaName, logicalTableName);
                TableMetaChanger.lockOssColumnMeta(metaDbConn, getTaskId(), schemaName, logicalTableName);
                TableMetaChanger.validOssColumnMeta(metaDbConn, getTaskId(), schemaName, logicalTableName);
                MetaDbUtil.commit(metaDbConn);
            } catch (Exception e) {
                MetaDbUtil.rollback(metaDbConn, e, null, null);
                if (tasks != null) {
                    tasks.forEach((pair, task) -> task.cancelAsync());
                }
                e.printStackTrace();
                throw GeneralUtil.nestedException(e);
            } finally {
                MetaDbUtil.endTransaction(metaDbConn, null);
            }
        } catch (Exception e) {
            throw GeneralUtil.nestedException(e);
        }
    }

    @NotNull
    private Map<Pair<String, String>, OSSBackFillWriterTask> buildOssBackFillLoaderTasks(
        ExecutionContext executionContext,
        String sourceLogicalSchema,
        String sourceLogicalTable,
        String targetLogicalSchema,
        String targetLogicalTable,
        TableMeta sourceTableMeta,
        PolarDBXOrcSchema orcSchema,
        Configuration conf) {
        final long maxRowsPerFile = executionContext.getParamManager().getLong(ConnectionParams.OSS_MAX_ROWS_PER_FILE);
        final boolean removeTmpFiles =
            executionContext.getParamManager().getBoolean(ConnectionParams.OSS_REMOVE_TMP_FILES);

        Map<Pair<String, String>, OSSBackFillWriterTask> tasks = new HashMap<>();

        // handle single table.
        Pair<String, String> singleTopology = OSSTaskUtils.getSingleTopology(sourceLogicalSchema, sourceLogicalTable, sourceTableMeta);

        PartitionInfo sourceTablePartitionInfo =
            OSSTaskUtils.getSourcePartitionInfo(executionContext, sourceLogicalSchema, sourceLogicalTable);

        Map<String, List<PhysicalPartitionInfo>> targetPartitionTopology =
            OptimizerContext.getContext(targetLogicalSchema).getLatestSchemaManager().getTable(targetLogicalTable)
                .getPartitionInfo().getPhysicalPartitionTopology(ImmutableList.of());

        // traverse each physical partition (phy table)
        for (Map.Entry<String, List<PhysicalPartitionInfo>> entry : targetPartitionTopology.entrySet()) {
            for (PhysicalPartitionInfo physicalPartitionInfo : entry.getValue()) {

                String targetPhySchema = physicalPartitionInfo.getGroupKey();
                String targetPhyTable = physicalPartitionInfo.getPhyTable();

                String partName = physicalPartitionInfo.getPartName();
                Pair<String, String> sourcePhySchemaAndTable = Optional
                    .ofNullable(singleTopology)
                    .orElseGet(() -> OSSTaskUtils.getSourcePhyTable(sourceTablePartitionInfo, partName));
                String sourcePhyTable = sourcePhySchemaAndTable.getValue();

                // for each physical table, add orc write task.
                OSSBackFillWriterTask task = new OSSBackFillWriterTask(
                    // for target table
                    targetLogicalSchema,
                    targetLogicalTable,
                    targetPhySchema,
                    targetPhyTable,

                    // for source table
                    sourcePhySchemaAndTable.getKey(),
                    sourcePhyTable,
                    sourceTableMeta,
                    targetTableEngine,
                    getTaskId(),

                    // for orc file
                    conf,
                    physicalPartitionName,
                    orcSchema,
                    maxRowsPerFile,
                    removeTmpFiles
                );
                tasks.put(sourcePhySchemaAndTable, task);
            }
        }
        return tasks;
    }

    @Override
    protected void onExecutionSuccess(ExecutionContext executionContext) {
        updateSupportedCommands(true, false, null);
    }
}
