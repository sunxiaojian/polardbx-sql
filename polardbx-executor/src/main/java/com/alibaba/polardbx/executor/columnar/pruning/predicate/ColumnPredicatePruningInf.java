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

package com.alibaba.polardbx.executor.columnar.pruning.predicate;

import com.alibaba.polardbx.executor.columnar.pruning.index.BitMapRowGroupIndex;
import com.alibaba.polardbx.executor.columnar.pruning.index.BloomFilterIndex;
import com.alibaba.polardbx.executor.columnar.pruning.index.IndexPruneContext;
import com.alibaba.polardbx.executor.columnar.pruning.index.SortKeyIndex;
import com.alibaba.polardbx.executor.columnar.pruning.index.ZoneMapIndex;
import org.roaringbitmap.RoaringBitmap;

import javax.annotation.Nonnull;

/**
 * @author fangwu
 */
public interface ColumnPredicatePruningInf {

    StringBuilder display(String[] columns, IndexPruneContext ipc);

    void sortKey(@Nonnull SortKeyIndex sortKeyIndex, IndexPruneContext ipc, @Nonnull RoaringBitmap cur);

    void bitmap(@Nonnull BitMapRowGroupIndex bitMapIndex, IndexPruneContext ipc, @Nonnull RoaringBitmap cur);

    void zoneMap(@Nonnull ZoneMapIndex zoneMapIndex, IndexPruneContext ipc, @Nonnull RoaringBitmap cur);

    void bloomFilter(@Nonnull BloomFilterIndex bloomFilterIndex, IndexPruneContext ipc,
                     @Nonnull RoaringBitmap cur);
}
