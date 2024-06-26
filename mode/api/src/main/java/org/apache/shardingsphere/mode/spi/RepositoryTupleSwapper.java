/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.mode.spi;

import org.apache.shardingsphere.infra.spi.annotation.SingletonSPI;
import org.apache.shardingsphere.infra.spi.type.ordered.OrderedSPI;
import org.apache.shardingsphere.infra.util.yaml.datanode.RepositoryTuple;
import org.apache.shardingsphere.infra.yaml.config.pojo.rule.YamlRuleConfiguration;

import java.util.Collection;
import java.util.Optional;

/**
 * Repository tuple swapper.
 *
 * @param <T> type of swapped YAML rule configuration
 */
@SingletonSPI
public interface RepositoryTupleSwapper<T extends YamlRuleConfiguration> extends OrderedSPI<T> {
    
    /**
     * Swap to repository tuples.
     *
     * @param yamlRuleConfig YAML rule configuration to be swapped
     * @return repository tuples
     */
    Collection<RepositoryTuple> swapToRepositoryTuples(T yamlRuleConfig);
    
    /**
     * Swap from repository tuple to YAML rule configurations.
     *
     * @param repositoryTuples repository tuples
     * @return swapped YAML rule configurations
     */
    Optional<T> swapToObject(Collection<RepositoryTuple> repositoryTuples);
    
    /**
     * Get rule type name.
     *
     * @return rule type name
     */
    String getRuleTypeName();
}
