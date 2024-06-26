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

package org.apache.shardingsphere.readwritesplitting.yaml.swapper;

import org.apache.shardingsphere.infra.algorithm.core.yaml.YamlAlgorithmConfiguration;
import org.apache.shardingsphere.infra.util.yaml.YamlEngine;
import org.apache.shardingsphere.infra.util.yaml.datanode.RepositoryTuple;
import org.apache.shardingsphere.mode.path.rule.RuleNodePath;
import org.apache.shardingsphere.mode.spi.RepositoryTupleSwapper;
import org.apache.shardingsphere.readwritesplitting.constant.ReadwriteSplittingOrder;
import org.apache.shardingsphere.readwritesplitting.metadata.nodepath.ReadwriteSplittingRuleNodePathProvider;
import org.apache.shardingsphere.readwritesplitting.yaml.config.YamlReadwriteSplittingRuleConfiguration;
import org.apache.shardingsphere.readwritesplitting.yaml.config.rule.YamlReadwriteSplittingDataSourceGroupRuleConfiguration;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Readwrite-splitting rule configuration repository tuple swapper.
 */
public final class ReadwriteSplittingRuleConfigurationRepositoryTupleSwapper implements RepositoryTupleSwapper<YamlReadwriteSplittingRuleConfiguration> {
    
    private final RuleNodePath ruleNodePath = new ReadwriteSplittingRuleNodePathProvider().getRuleNodePath();
    
    @Override
    public Collection<RepositoryTuple> swapToRepositoryTuples(final YamlReadwriteSplittingRuleConfiguration yamlRuleConfig) {
        Collection<RepositoryTuple> result = new LinkedList<>();
        for (Entry<String, YamlAlgorithmConfiguration> entry : yamlRuleConfig.getLoadBalancers().entrySet()) {
            result.add(new RepositoryTuple(ruleNodePath.getNamedItem(ReadwriteSplittingRuleNodePathProvider.LOAD_BALANCERS).getPath(entry.getKey()), YamlEngine.marshal(entry.getValue())));
        }
        for (Entry<String, YamlReadwriteSplittingDataSourceGroupRuleConfiguration> entry : yamlRuleConfig.getDataSourceGroups().entrySet()) {
            result.add(new RepositoryTuple(ruleNodePath.getNamedItem(ReadwriteSplittingRuleNodePathProvider.DATA_SOURCES).getPath(entry.getKey()), YamlEngine.marshal(entry.getValue())));
        }
        return result;
    }
    
    @Override
    public Optional<YamlReadwriteSplittingRuleConfiguration> swapToObject(final Collection<RepositoryTuple> repositoryTuples) {
        List<RepositoryTuple> validRepositoryTuples = repositoryTuples.stream().filter(each -> ruleNodePath.getRoot().isValidatedPath(each.getKey())).collect(Collectors.toList());
        if (validRepositoryTuples.isEmpty()) {
            return Optional.empty();
        }
        YamlReadwriteSplittingRuleConfiguration yamlRuleConfig = new YamlReadwriteSplittingRuleConfiguration();
        Map<String, YamlReadwriteSplittingDataSourceGroupRuleConfiguration> dataSourceGroups = new LinkedHashMap<>();
        Map<String, YamlAlgorithmConfiguration> loadBalancers = new LinkedHashMap<>();
        for (RepositoryTuple each : validRepositoryTuples) {
            ruleNodePath.getNamedItem(ReadwriteSplittingRuleNodePathProvider.DATA_SOURCES).getName(each.getKey())
                    .ifPresent(optional -> dataSourceGroups.put(optional, YamlEngine.unmarshal(each.getValue(), YamlReadwriteSplittingDataSourceGroupRuleConfiguration.class)));
            ruleNodePath.getNamedItem(ReadwriteSplittingRuleNodePathProvider.LOAD_BALANCERS).getName(each.getKey())
                    .ifPresent(optional -> loadBalancers.put(optional, YamlEngine.unmarshal(each.getValue(), YamlAlgorithmConfiguration.class)));
        }
        yamlRuleConfig.setDataSourceGroups(dataSourceGroups);
        yamlRuleConfig.setLoadBalancers(loadBalancers);
        return Optional.of(yamlRuleConfig);
    }
    
    @Override
    public String getRuleTypeName() {
        return "readwrite_splitting";
    }
    
    @Override
    public int getOrder() {
        return ReadwriteSplittingOrder.ORDER;
    }
    
    @Override
    public Class<YamlReadwriteSplittingRuleConfiguration> getTypeClass() {
        return YamlReadwriteSplittingRuleConfiguration.class;
    }
}
