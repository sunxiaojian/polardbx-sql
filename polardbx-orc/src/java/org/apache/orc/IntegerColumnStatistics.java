/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.orc;

/**
 * Statistics for all of the integer columns, such as byte, short, int, and
 * long.
 */
public interface IntegerColumnStatistics extends ColumnStatistics {
  /**
   * Get the smallest value in the column. Only defined if getNumberOfValues
   * is non-zero.
   * @return the minimum
   */
  long getMinimum();

  /**
   * Get the largest value in the column. Only defined if getNumberOfValues
   * is non-zero.
   * @return the maximum
   */
  long getMaximum();

  /**
   * Is the sum defined? If the sum overflowed the counter this will be false.
   * @return is the sum available
   */
  boolean isSumDefined();

  /**
   * Get the sum of the column. Only valid if isSumDefined returns true.
   * @return the sum of the column
   */
  long getSum();

  Long getFirst();

  Long getLatest();
}
