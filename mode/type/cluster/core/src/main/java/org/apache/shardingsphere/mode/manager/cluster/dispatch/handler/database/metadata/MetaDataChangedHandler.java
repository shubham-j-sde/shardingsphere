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

package org.apache.shardingsphere.mode.manager.cluster.dispatch.handler.database.metadata;

import org.apache.shardingsphere.mode.event.DataChangedEvent;
import org.apache.shardingsphere.mode.event.DataChangedEvent.Type;
import org.apache.shardingsphere.mode.manager.ContextManager;
import org.apache.shardingsphere.mode.manager.cluster.dispatch.handler.database.metadata.type.SchemaChangedHandler;
import org.apache.shardingsphere.mode.manager.cluster.dispatch.handler.database.metadata.type.StorageNodeChangedHandler;
import org.apache.shardingsphere.mode.manager.cluster.dispatch.handler.database.metadata.type.StorageUnitChangedHandler;
import org.apache.shardingsphere.mode.manager.cluster.dispatch.handler.database.metadata.type.TableChangedHandler;
import org.apache.shardingsphere.mode.manager.cluster.dispatch.handler.database.metadata.type.ViewChangedHandler;
import org.apache.shardingsphere.mode.node.path.NodePathParser;
import org.apache.shardingsphere.mode.node.path.NodePathPattern;
import org.apache.shardingsphere.mode.node.path.metadata.database.TableMetadataNodePath;
import org.apache.shardingsphere.mode.node.path.metadata.database.ViewMetadataNodePath;
import org.apache.shardingsphere.mode.node.path.metadata.storage.DataSourceNodePathParser;
import org.apache.shardingsphere.mode.node.path.metadata.storage.StorageNodeNodePathParser;
import org.apache.shardingsphere.mode.node.path.metadata.storage.StorageUnitNodePathParser;

import java.util.Optional;

/**
 * Meta data changed handler.
 */
public final class MetaDataChangedHandler {
    
    private final SchemaChangedHandler schemaChangedHandler;
    
    private final TableChangedHandler tableChangedHandler;
    
    private final ViewChangedHandler viewChangedHandler;
    
    private final StorageUnitChangedHandler storageUnitChangedHandler;
    
    private final StorageNodeChangedHandler storageNodeChangedHandler;
    
    public MetaDataChangedHandler(final ContextManager contextManager) {
        schemaChangedHandler = new SchemaChangedHandler(contextManager);
        tableChangedHandler = new TableChangedHandler(contextManager);
        viewChangedHandler = new ViewChangedHandler(contextManager);
        storageUnitChangedHandler = new StorageUnitChangedHandler(contextManager);
        storageNodeChangedHandler = new StorageNodeChangedHandler(contextManager);
    }
    
    /**
     * Handle meta data changed.
     *
     * @param databaseName database name
     * @param event data changed event
     * @return handle completed or not
     */
    public boolean handle(final String databaseName, final DataChangedEvent event) {
        String eventKey = event.getKey();
        Optional<String> schemaName = NodePathParser.find(eventKey, new TableMetadataNodePath(NodePathPattern.IDENTIFIER, NodePathPattern.IDENTIFIER, null), true, false, 2);
        if (schemaName.isPresent()) {
            handleSchemaChanged(databaseName, schemaName.get(), event);
            return true;
        }
        schemaName = NodePathParser.find(eventKey, new TableMetadataNodePath(NodePathPattern.IDENTIFIER, NodePathPattern.IDENTIFIER, null), true, true, 2);
        if (schemaName.isPresent() && isTableMetaDataChanged(eventKey)) {
            handleTableChanged(databaseName, schemaName.get(), event);
            return true;
        }
        if (schemaName.isPresent() && isViewMetaDataChanged(eventKey)) {
            handleViewChanged(databaseName, schemaName.get(), event);
            return true;
        }
        if (DataSourceNodePathParser.isDataSourcePath(eventKey)) {
            handleDataSourceChanged(databaseName, event);
            return true;
        }
        return false;
    }
    
    private void handleSchemaChanged(final String databaseName, final String schemaName, final DataChangedEvent event) {
        if (Type.ADDED == event.getType() || Type.UPDATED == event.getType()) {
            schemaChangedHandler.handleCreated(databaseName, schemaName);
        } else if (Type.DELETED == event.getType()) {
            schemaChangedHandler.handleDropped(databaseName, schemaName);
        }
    }
    
    private boolean isTableMetaDataChanged(final String key) {
        return NodePathParser.isMatchedPath(key, new TableMetadataNodePath(NodePathPattern.IDENTIFIER, NodePathPattern.IDENTIFIER, NodePathPattern.IDENTIFIER), false, false)
                || NodePathParser.getVersion(new TableMetadataNodePath(NodePathPattern.IDENTIFIER, NodePathPattern.IDENTIFIER, NodePathPattern.IDENTIFIER)).isActiveVersionPath(key);
    }
    
    private void handleTableChanged(final String databaseName, final String schemaName, final DataChangedEvent event) {
        if ((Type.ADDED == event.getType() || Type.UPDATED == event.getType())
                && NodePathParser.getVersion(new TableMetadataNodePath(NodePathPattern.IDENTIFIER, NodePathPattern.IDENTIFIER, NodePathPattern.IDENTIFIER)).isActiveVersionPath(event.getKey())) {
            tableChangedHandler.handleCreatedOrAltered(databaseName, schemaName, event);
        } else if (Type.DELETED == event.getType()
                && NodePathParser.isMatchedPath(event.getKey(), new TableMetadataNodePath(NodePathPattern.IDENTIFIER, NodePathPattern.IDENTIFIER, NodePathPattern.IDENTIFIER), false, false)) {
            tableChangedHandler.handleDropped(databaseName, schemaName, event);
        }
    }
    
    private boolean isViewMetaDataChanged(final String key) {
        return NodePathParser.getVersion(new ViewMetadataNodePath(NodePathPattern.IDENTIFIER, NodePathPattern.IDENTIFIER, NodePathPattern.IDENTIFIER)).isActiveVersionPath(key)
                || NodePathParser.isMatchedPath(key, new ViewMetadataNodePath(NodePathPattern.IDENTIFIER, NodePathPattern.IDENTIFIER, NodePathPattern.IDENTIFIER), false, true);
    }
    
    private void handleViewChanged(final String databaseName, final String schemaName, final DataChangedEvent event) {
        if ((Type.ADDED == event.getType() || Type.UPDATED == event.getType())
                && NodePathParser.getVersion(new ViewMetadataNodePath(NodePathPattern.IDENTIFIER, NodePathPattern.IDENTIFIER, NodePathPattern.IDENTIFIER)).isActiveVersionPath(event.getKey())) {
            viewChangedHandler.handleCreatedOrAltered(databaseName, schemaName, event);
        } else if (Type.DELETED == event.getType()
                && NodePathParser.isMatchedPath(event.getKey(), new ViewMetadataNodePath(NodePathPattern.IDENTIFIER, NodePathPattern.IDENTIFIER, NodePathPattern.IDENTIFIER), false, true)) {
            viewChangedHandler.handleDropped(databaseName, schemaName, event);
        }
    }
    
    private void handleDataSourceChanged(final String databaseName, final DataChangedEvent event) {
        Optional<String> storageUnitName = StorageUnitNodePathParser.getVersion().findIdentifierByActiveVersionPath(event.getKey(), 2);
        boolean isActiveVersion = true;
        if (!storageUnitName.isPresent()) {
            storageUnitName = StorageUnitNodePathParser.findStorageUnitName(event.getKey());
            isActiveVersion = false;
        }
        if (storageUnitName.isPresent()) {
            handleStorageUnitChanged(databaseName, event, storageUnitName.get(), isActiveVersion);
            return;
        }
        Optional<String> storageNodeName = StorageNodeNodePathParser.getVersion().findIdentifierByActiveVersionPath(event.getKey(), 2);
        isActiveVersion = true;
        if (!storageNodeName.isPresent()) {
            storageNodeName = StorageNodeNodePathParser.findStorageNodeName(event.getKey());
            isActiveVersion = false;
        }
        if (storageNodeName.isPresent()) {
            handleStorageNodeChanged(databaseName, event, storageNodeName.get(), isActiveVersion);
        }
    }
    
    private void handleStorageUnitChanged(final String databaseName, final DataChangedEvent event, final String storageUnitName, final boolean isActiveVersion) {
        if (isActiveVersion) {
            if (Type.ADDED == event.getType()) {
                storageUnitChangedHandler.handleRegistered(databaseName, storageUnitName, event);
            } else if (Type.UPDATED == event.getType()) {
                storageUnitChangedHandler.handleAltered(databaseName, storageUnitName, event);
            }
            return;
        }
        if (Type.DELETED == event.getType()) {
            storageUnitChangedHandler.handleUnregistered(databaseName, storageUnitName);
        }
    }
    
    private void handleStorageNodeChanged(final String databaseName, final DataChangedEvent event, final String storageNodeName, final boolean isActiveVersion) {
        if (isActiveVersion) {
            if (Type.ADDED == event.getType()) {
                storageNodeChangedHandler.handleRegistered(databaseName, storageNodeName, event);
            } else if (Type.UPDATED == event.getType()) {
                storageNodeChangedHandler.handleAltered(databaseName, storageNodeName, event);
            }
            return;
        }
        if (Type.DELETED == event.getType()) {
            storageNodeChangedHandler.handleUnregistered(databaseName, storageNodeName);
        }
    }
}
