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

import com.alibaba.polardbx.common.exception.TddlNestableRuntimeException;
import com.alibaba.polardbx.common.utils.logger.Logger;
import com.alibaba.polardbx.common.utils.logger.LoggerFactory;
import com.alibaba.polardbx.executor.cursor.Cursor;
import com.alibaba.polardbx.executor.cursor.impl.AffectRowCursor;
import com.alibaba.polardbx.executor.spi.IRepository;
import com.alibaba.polardbx.gms.listener.impl.MetaDbConfigManager;
import com.alibaba.polardbx.gms.listener.impl.MetaDbDataIdBuilder;
import com.alibaba.polardbx.gms.metadb.MetaDbDataSource;
import com.alibaba.polardbx.gms.lbac.LBACPrivilegeCheckUtils;
import com.alibaba.polardbx.gms.lbac.LBACSecurityManager;
import com.alibaba.polardbx.gms.lbac.LBACSecurityPolicy;
import com.alibaba.polardbx.gms.lbac.accessor.LBACAccessorUtils;
import com.alibaba.polardbx.gms.lbac.accessor.LBACPolicyAccessor;
import com.alibaba.polardbx.lbac.LBACException;
import com.alibaba.polardbx.optimizer.context.ExecutionContext;
import com.alibaba.polardbx.optimizer.core.rel.dal.LogicalDal;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.sql.SqlCreateSecurityPolicy;

import java.sql.Connection;
import java.sql.SQLException;

public class LogicalCreateSecurityPolicyHandler extends HandlerCommon {

    private static final Logger logger = LoggerFactory.getLogger(LogicalCreateSecurityPolicyHandler.class);

    public LogicalCreateSecurityPolicyHandler(IRepository repo) {
        super(repo);
    }

    @Override
    public Cursor handle(RelNode logicalPlan, ExecutionContext executionContext) {
        if (!LBACPrivilegeCheckUtils.isHighPrivilege(executionContext.getPrivilegeContext().getPolarUserInfo())) {
            throw new LBACException("check privilege failed");
        }

        SqlCreateSecurityPolicy createPolicy =
            (SqlCreateSecurityPolicy) ((LogicalDal) logicalPlan).getNativeSqlNode();

        String policyName = createPolicy.getPolicyName().getSimple().toLowerCase();//全部小写
        String policyComponents = createPolicy.getPolicyComponents().getNlsString().getValue().toLowerCase();//全部小写
        LBACSecurityPolicy policy = LBACAccessorUtils.createSecurityPolicy(policyName, policyComponents);
        if (!LBACSecurityManager.getInstance().validatePolicy(policy)) {
            throw new LBACException("security policy is invalid");
        }

        try (Connection connection = MetaDbDataSource.getInstance().getConnection()) {
            LBACPolicyAccessor spAccessor = new LBACPolicyAccessor();
            spAccessor.setConnection(connection);
            int affectRow = spAccessor.insert(policy);

            MetaDbConfigManager.getInstance().notify(MetaDbDataIdBuilder.getLBACSecurityDataId(), connection);
            // wait for all cn to load metadb
            MetaDbConfigManager.getInstance().sync(MetaDbDataIdBuilder.getLBACSecurityDataId());
            return new AffectRowCursor(affectRow);
        } catch (SQLException e) {
            throw new TddlNestableRuntimeException(e);
        }
    }
}
