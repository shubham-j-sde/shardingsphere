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

package org.apache.shardingsphere.metadata.persist.service.config.database.rule;

import com.google.common.base.Strings;
import org.apache.shardingsphere.infra.config.rule.RuleConfiguration;
import org.apache.shardingsphere.infra.metadata.version.MetaDataVersion;
import org.apache.shardingsphere.infra.spi.type.ordered.OrderedSPILoader;
import org.apache.shardingsphere.infra.util.yaml.datanode.RepositoryTuple;
import org.apache.shardingsphere.infra.yaml.config.pojo.rule.YamlRuleConfiguration;
import org.apache.shardingsphere.infra.yaml.config.swapper.rule.YamlRuleConfigurationSwapperEngine;
import org.apache.shardingsphere.metadata.persist.node.metadata.DatabaseRuleMetaDataNode;
import org.apache.shardingsphere.metadata.persist.service.config.RepositoryTuplePersistService;
import org.apache.shardingsphere.metadata.persist.service.config.RepositoryTupleSwapperEngine;
import org.apache.shardingsphere.metadata.persist.service.config.database.DatabaseBasedPersistService;
import org.apache.shardingsphere.mode.spi.PersistRepository;
import org.apache.shardingsphere.mode.spi.RepositoryTupleSwapper;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

/**
 * Database rule persist service.
 */
public final class DatabaseRulePersistService implements DatabaseBasedPersistService<Collection<RuleConfiguration>> {
    
    private static final String DEFAULT_VERSION = "0";
    
    private final PersistRepository repository;
    
    private final RepositoryTuplePersistService repositoryTuplePersistService;
    
    public DatabaseRulePersistService(final PersistRepository repository) {
        this.repository = repository;
        repositoryTuplePersistService = new RepositoryTuplePersistService(repository);
    }
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void persist(final String databaseName, final Collection<RuleConfiguration> configs) {
        Collection<YamlRuleConfiguration> yamlRuleConfigs = new YamlRuleConfigurationSwapperEngine().swapToYamlRuleConfigurations(configs);
        for (Entry<YamlRuleConfiguration, RepositoryTupleSwapper> entry : OrderedSPILoader.getServices(RepositoryTupleSwapper.class, yamlRuleConfigs).entrySet()) {
            Collection<RepositoryTuple> repositoryTuples = entry.getValue().swapToRepositoryTuples(entry.getKey());
            if (!repositoryTuples.isEmpty()) {
                persistDataNodes(databaseName, entry.getValue().getRuleTypeName(), repositoryTuples);
            }
        }
    }
    
    @Override
    public Collection<RuleConfiguration> load(final String databaseName) {
        return new RepositoryTupleSwapperEngine().swapToRuleConfigurations(repositoryTuplePersistService.loadRepositoryTuples(DatabaseRuleMetaDataNode.getRulesNode(databaseName)));
    }
    
    @Override
    public void delete(final String databaseName, final String name) {
        repository.delete(DatabaseRuleMetaDataNode.getDatabaseRuleNode(databaseName, name));
    }
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Collection<MetaDataVersion> persistConfigurations(final String databaseName, final Collection<RuleConfiguration> configs) {
        Collection<MetaDataVersion> result = new LinkedList<>();
        Collection<YamlRuleConfiguration> yamlRuleConfigs = new YamlRuleConfigurationSwapperEngine().swapToYamlRuleConfigurations(configs);
        for (Entry<YamlRuleConfiguration, RepositoryTupleSwapper> entry : OrderedSPILoader.getServices(RepositoryTupleSwapper.class, yamlRuleConfigs).entrySet()) {
            Collection<RepositoryTuple> repositoryTuples = entry.getValue().swapToRepositoryTuples(entry.getKey());
            if (!repositoryTuples.isEmpty()) {
                result.addAll(persistDataNodes(databaseName, entry.getValue().getRuleTypeName(), repositoryTuples));
            }
        }
        return result;
    }
    
    private Collection<MetaDataVersion> persistDataNodes(final String databaseName, final String ruleName, final Collection<RepositoryTuple> repositoryTuples) {
        Collection<MetaDataVersion> result = new LinkedList<>();
        for (RepositoryTuple each : repositoryTuples) {
            List<String> versions = repository.getChildrenKeys(DatabaseRuleMetaDataNode.getDatabaseRuleVersionsNode(databaseName, ruleName, each.getKey()));
            String nextVersion = versions.isEmpty() ? DEFAULT_VERSION : String.valueOf(Integer.parseInt(versions.get(0)) + 1);
            repository.persist(DatabaseRuleMetaDataNode.getDatabaseRuleVersionNode(databaseName, ruleName, each.getKey(), nextVersion), each.getValue());
            if (Strings.isNullOrEmpty(getActiveVersion(databaseName, ruleName, each.getKey()))) {
                repository.persist(DatabaseRuleMetaDataNode.getDatabaseRuleActiveVersionNode(databaseName, ruleName, each.getKey()), DEFAULT_VERSION);
            }
            result.add(new MetaDataVersion(DatabaseRuleMetaDataNode.getDatabaseRuleNode(databaseName, ruleName, each.getKey()), getActiveVersion(databaseName, ruleName, each.getKey()), nextVersion));
        }
        return result;
    }
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Collection<MetaDataVersion> deleteConfigurations(final String databaseName, final Collection<RuleConfiguration> configs) {
        Collection<MetaDataVersion> result = new LinkedList<>();
        Collection<YamlRuleConfiguration> yamlRuleConfigs = new YamlRuleConfigurationSwapperEngine().swapToYamlRuleConfigurations(configs);
        for (Entry<YamlRuleConfiguration, RepositoryTupleSwapper> entry : OrderedSPILoader.getServices(RepositoryTupleSwapper.class, yamlRuleConfigs).entrySet()) {
            Collection<RepositoryTuple> repositoryTuples = entry.getValue().swapToRepositoryTuples(entry.getKey());
            if (repositoryTuples.isEmpty()) {
                continue;
            }
            List<RepositoryTuple> newRepositoryTuples = new LinkedList<>(repositoryTuples);
            Collections.reverse(newRepositoryTuples);
            result.addAll(deleteDataNodes(databaseName, entry.getValue().getRuleTypeName(), newRepositoryTuples));
        }
        return result;
    }
    
    private Collection<MetaDataVersion> deleteDataNodes(final String databaseName, final String ruleName, final Collection<RepositoryTuple> repositoryTuples) {
        Collection<MetaDataVersion> result = new LinkedList<>();
        for (RepositoryTuple each : repositoryTuples) {
            String delKey = DatabaseRuleMetaDataNode.getDatabaseRuleNode(databaseName, ruleName, each.getKey());
            repository.delete(delKey);
            result.add(new MetaDataVersion(delKey));
        }
        return result;
    }
    
    private String getActiveVersion(final String databaseName, final String ruleName, final String key) {
        return repository.getDirectly(DatabaseRuleMetaDataNode.getDatabaseRuleActiveVersionNode(databaseName, ruleName, key));
    }
}
