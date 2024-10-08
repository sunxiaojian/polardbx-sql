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

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.polardbx.executor.mpp.server;

import com.alibaba.polardbx.common.exception.code.ErrorCode;
import com.alibaba.polardbx.common.exception.code.ErrorType;
import com.alibaba.polardbx.executor.mpp.execution.QueryInfo;
import com.alibaba.polardbx.executor.mpp.execution.QueryState;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.net.URI;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Objects.requireNonNull;

/**
 * Lightweight version of QueryInfo. Parts of the web UI depend on the fields
 * being named consistently across these classes.
 */
@Immutable
public class BasicQueryInfo {
    private final String queryId;
    private final QueryState state;
    private final boolean scheduled;
    private final URI self;
    private final String query;
    private final BasicQueryStats queryStats;
    private final ErrorType errorType;
    private final ErrorCode errorCode;

    public BasicQueryInfo(
        String queryId,
        QueryState state,
        boolean scheduled,
        URI self,
        String query,
        BasicQueryStats queryStats,
        ErrorType errorType,
        ErrorCode errorCode) {
        this.queryId = requireNonNull(queryId, "queryId is null");
        this.state = requireNonNull(state, "state is null");
        this.errorType = errorType;
        this.errorCode = errorCode;
        this.scheduled = scheduled;
        this.self = requireNonNull(self, "self is null");
        this.query = requireNonNull(query, "query is null");
        this.queryStats = requireNonNull(queryStats, "queryStats is null");
    }

    public BasicQueryInfo(QueryInfo queryInfo) {
        this(queryInfo.getQueryId(),
            queryInfo.getState(),
            queryInfo.isScheduled(),
            queryInfo.getSelf(),
            queryInfo.getQuery(),
            new BasicQueryStats(queryInfo.getQueryStats()),
            queryInfo.getErrorType(),
            queryInfo.getErrorCode());
    }

    @JsonProperty
    public String getQueryId() {
        return queryId;
    }

    @JsonProperty
    public QueryState getState() {
        return state;
    }

    @JsonProperty
    public boolean isScheduled() {
        return scheduled;
    }

    @JsonProperty
    public URI getSelf() {
        return self;
    }

    @JsonProperty
    public String getQuery() {
        return query;
    }

    @JsonProperty
    public BasicQueryStats getQueryStats() {
        return queryStats;
    }

    @Nullable
    @JsonProperty
    public ErrorType getErrorType() {
        return errorType;
    }

    @Nullable
    @JsonProperty
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
            .add("queryId", queryId)
            .add("state", state)
            .toString();
    }
}
