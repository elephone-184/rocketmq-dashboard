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

package org.apache.rocketmq.dashboard.service;

import org.apache.rocketmq.dashboard.model.AclCidrAuditReport;

public interface AclAuditService {

    /**
     * Audit CIDR subnets and source IP rules across ACL policies on the specified cluster/broker.
     *
     * @param clusterName cluster name (optional if brokerAddr specified)
     * @param brokerAddr specific broker address (optional if clusterName specified)
     * @return structured audit report detailing wildcards, overlapping CIDRs, and syntax issues
     */
    AclCidrAuditReport auditAclCidrSubnets(String clusterName, String brokerAddr);
}
