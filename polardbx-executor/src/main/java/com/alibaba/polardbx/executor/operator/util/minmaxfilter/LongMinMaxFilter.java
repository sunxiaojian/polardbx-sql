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

package com.alibaba.polardbx.executor.operator.util.minmaxfilter;

import com.alibaba.polardbx.common.utils.bloomfilter.MinMaxFilterInfo;
import com.alibaba.polardbx.executor.chunk.Block;

/**
 * @author chenzilin
 * @date 2021/12/8 14:40
 */
public class LongMinMaxFilter extends MinMaxFilter {
    private Long min;
    private Long max;

    public LongMinMaxFilter() {

    }

    public LongMinMaxFilter(Long min, Long max) {
        this.min = min;
        this.max = max;
    }

    public Long getMin() {
        return min;
    }

    public void setMin(Long min) {
        this.min = min;
    }

    public Long getMax() {
        return max;
    }

    public void setMax(Long max) {
        this.max = max;
    }

    @Override
    public void put(Block block, int pos) {
        if (!block.isNull(pos)) {
            long num = block.getLong(pos);
            if (min == null || num < min) {
                min = num;
            }
            if (max == null || num > max) {
                max = num;
            }
        }
    }

    @Override
    public MinMaxFilterInfo toMinMaxFilterInfo() {
        return new MinMaxFilterInfo(
                MinMaxFilterInfo.TYPE.LONG,
                min == null ? null : min.longValue(),
                max == null ? null : max.longValue(), null, null, null, null, null, null);
    }

    @Override
    public Number getMinNumber() {
        return min;
    }

    @Override
    public Number getMaxNumber() {
        return max;
    }

    @Override
    public String getMinString() {
        return null;
    }

    @Override
    public String getMaxString() {
        return null;
    }
}
