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

package org.apache.shardingsphere.sqlfederation.yaml.swapper;

import org.apache.shardingsphere.mode.path.GlobalNodePath;
import org.apache.shardingsphere.infra.util.yaml.YamlEngine;
import org.apache.shardingsphere.infra.util.yaml.datanode.RepositoryTuple;
import org.apache.shardingsphere.mode.spi.RepositoryTupleSwapper;
import org.apache.shardingsphere.sqlfederation.constant.SQLFederationOrder;
import org.apache.shardingsphere.sqlfederation.yaml.config.YamlSQLFederationRuleConfiguration;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * SQL federation rule configuration repository tuple swapper.
 */
public final class SQLFederationRuleConfigurationRepositoryTupleSwapper implements RepositoryTupleSwapper<YamlSQLFederationRuleConfiguration> {
    
    @Override
    public Collection<RepositoryTuple> swapToRepositoryTuples(final YamlSQLFederationRuleConfiguration yamlRuleConfig) {
        return Collections.singleton(new RepositoryTuple(getRuleTypeName(), YamlEngine.marshal(yamlRuleConfig)));
    }
    
    @Override
    public Optional<YamlSQLFederationRuleConfiguration> swapToObject(final Collection<RepositoryTuple> repositoryTuples) {
        for (RepositoryTuple each : repositoryTuples) {
            if (GlobalNodePath.getVersion(getRuleTypeName(), each.getKey()).isPresent()) {
                return Optional.of(YamlEngine.unmarshal(each.getValue(), YamlSQLFederationRuleConfiguration.class));
            }
        }
        return Optional.empty();
    }
    
    @Override
    public String getRuleTypeName() {
        return "sql_federation";
    }
    
    @Override
    public int getOrder() {
        return SQLFederationOrder.ORDER;
    }
    
    @Override
    public Class<YamlSQLFederationRuleConfiguration> getTypeClass() {
        return YamlSQLFederationRuleConfiguration.class;
    }
}
