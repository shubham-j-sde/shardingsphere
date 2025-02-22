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

package org.apache.shardingsphere.mode.node.path;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.shardingsphere.mode.node.path.version.VersionNodePathParser;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Node path parser.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NodePathParser {
    
    /**
     * Find node segment.
     *
     * @param path to be searched path
     * @param nodePathCriteria node path criteria
     * @param trimEmptyNode null variable should trim parent node if true
     * @param containsChildPath whether contains child path
     * @param searchSegmentIndex search segment index, start from 1
     * @return found node segment
     */
    public static Optional<String> find(final String path, final NodePath nodePathCriteria, final boolean trimEmptyNode, final boolean containsChildPath, final int searchSegmentIndex) {
        Matcher matcher = createPattern(nodePathCriteria, trimEmptyNode, containsChildPath).matcher(path);
        return matcher.find() ? Optional.of(matcher.group(searchSegmentIndex)) : Optional.empty();
    }
    
    /**
     * Whether to matched path.
     *
     * @param path to be searched path
     * @param nodePathCriteria node path criteria
     * @param trimEmptyNode null variable should trim parent node if true
     * @param containsChildPath whether contains child path
     * @return is matched path or not
     */
    public static boolean isMatchedPath(final String path, final NodePath nodePathCriteria, final boolean trimEmptyNode, final boolean containsChildPath) {
        return createPattern(nodePathCriteria, trimEmptyNode, containsChildPath).matcher(path).find();
    }
    
    private static Pattern createPattern(final NodePath nodePathCriteria, final boolean trimEmptyNode, final boolean containsChildPath) {
        String endPattern = containsChildPath ? "?" : "$";
        return Pattern.compile(NodePathGenerator.toPath(nodePathCriteria, trimEmptyNode) + endPattern, Pattern.CASE_INSENSITIVE);
    }
    
    /**
     * Get version node path parser.
     *
     * @param nodePathCriteria node path criteria
     * @return version node path parser
     */
    public static VersionNodePathParser getVersion(final NodePath nodePathCriteria) {
        return new VersionNodePathParser(NodePathGenerator.toPath(nodePathCriteria, false));
    }
}
