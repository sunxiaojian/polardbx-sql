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

package com.alibaba.polardbx.optimizer.state;

import com.alibaba.polardbx.common.datatype.Decimal;
import com.alibaba.polardbx.optimizer.datastruct.BooleanSegmentArrayList;
import com.alibaba.polardbx.optimizer.datastruct.ObjectWithClassSegmentArrayList;
import org.openjdk.jol.info.ClassLayout;

/**
 * Nullable Decimal Group State
 *
 * @author Eric Fu
 */
public class NullableDecimalGroupState implements GroupState {

    private static final long INSTANCE_SIZE = ClassLayout.parseClass(NullableDecimalGroupState.class).instanceSize();

    /**
     * The null value bitmap.
     */
    private final BooleanSegmentArrayList valueIsNull;

    /**
     * Disaggregated stored decimal objects.
     */
    private final ObjectWithClassSegmentArrayList<Decimal> decimals;

    public NullableDecimalGroupState(int capacity) {
        this.valueIsNull = new BooleanSegmentArrayList(capacity);
        this.decimals = new ObjectWithClassSegmentArrayList(capacity, Decimal.class);
    }

    public NullableDecimalGroupState(
        BooleanSegmentArrayList valueIsNull,
        ObjectWithClassSegmentArrayList<Decimal> decimals) {
        this.valueIsNull = valueIsNull;
        this.decimals = decimals;
    }

    public void set(int groupId, Decimal value) {
        if (value == null) {
            valueIsNull.set(groupId, true);
            return;
        }
        valueIsNull.set(groupId, false);
        decimals.set(groupId, value);
    }

    public void appendNull() {
        decimals.add(null);
        valueIsNull.add(true);
    }

    public void append(Decimal value) {
        appendNull();
        set(valueIsNull.size() - 1, value);
    }

    public void setNull(int groupId) {
        valueIsNull.set(groupId, true);
    }

    public boolean isNull(int groupId) {
        return valueIsNull.get(groupId);
    }

    public Decimal get(int groupId) {
        return decimals.get(groupId);
    }

    @Override
    public long estimateSize() {
        long size = INSTANCE_SIZE + decimals.estimateSize() + valueIsNull.estimateSize();
        return size;
    }
}
