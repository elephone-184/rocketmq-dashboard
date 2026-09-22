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

package org.apache.rocketmq.dashboard.service.impl;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.dashboard.model.AclCidrAuditReport;
import org.apache.rocketmq.dashboard.model.AclCidrAuditReport.AuditFinding;
import org.apache.rocketmq.dashboard.service.AbstractCommonService;
import org.apache.rocketmq.dashboard.service.AclAuditService;
import org.apache.rocketmq.dashboard.util.CidrUtil;
import org.apache.rocketmq.remoting.protocol.body.AclInfo;
import org.apache.rocketmq.tools.admin.MQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class AclAuditServiceImpl extends AbstractCommonService implements AclAuditService {

    private static final Logger log = LoggerFactory.getLogger(AclAuditServiceImpl.class);

    @Autowired
    private MQAdminExt mqAdminExt;

    @Override
    public AclCidrAuditReport auditAclCidrSubnets(String clusterName, String brokerAddr) {
        AclCidrAuditReport report = new AclCidrAuditReport(clusterName, brokerAddr);
        List<String> targetBrokers;
        try {
            targetBrokers = getBrokerAddressList(clusterName, brokerAddr);
        } catch (Exception e) {
            log.error("Failed to retrieve broker list for cluster: {}, broker: {}", clusterName, brokerAddr, e);
            report.getFindings().add(new AuditFinding(
                    "SYSTEM", "BROKER_TOPOLOGY", "ERROR", "BROKER_RESOLUTION_FAILURE",
                    "Unable to resolve broker list: " + e.getMessage(), Collections.emptyList()));
            return report;
        }

        if (CollectionUtils.isEmpty(targetBrokers)) {
            report.getFindings().add(new AuditFinding(
                    "SYSTEM", "BROKER_TOPOLOGY", "WARN", "NO_TARGET_BROKER",
                    "No reachable brokers found for given query parameters", Collections.emptyList()));
            return report;
        }

        int totalPolicies = 0;
        int wildcardCount = 0;
        int overlappingCount = 0;
        int invalidSyntaxCount = 0;
        List<AuditFinding> findings = new ArrayList<>();

        for (String addr : targetBrokers) {
            List<AclInfo> aclInfoList;
            try {
                aclInfoList = mqAdminExt.listAcl(addr, "", "");
            } catch (Exception e) {
                log.warn("Failed to query ACL policies from broker: {}", addr, e);
                findings.add(new AuditFinding(
                        "BROKER", addr, "WARN", "BROKER_QUERY_EXCEPTION",
                        "Failed to query ACL from broker: " + e.getMessage(), Collections.singletonList(addr)));
                continue;
            }

            if (CollectionUtils.isEmpty(aclInfoList)) {
                continue;
            }

            for (AclInfo aclInfo : aclInfoList) {
                String subject = aclInfo.getSubject();
                if (CollectionUtils.isEmpty(aclInfo.getPolicies())) {
                    continue;
                }

                for (AclInfo.PolicyInfo policy : aclInfo.getPolicies()) {
                    if (CollectionUtils.isEmpty(policy.getEntries())) {
                        continue;
                    }

                    for (AclInfo.PolicyEntryInfo entry : policy.getEntries()) {
                        totalPolicies++;
                        String resource = entry.getResource();
                        List<String> sourceIps = entry.getSourceIps();

                        if (CollectionUtils.isEmpty(sourceIps)) {
                            findings.add(new AuditFinding(
                                    subject, resource, "WARN", "EMPTY_SOURCE_IPS",
                                    "Policy entry has no source IP restrictions specified", Collections.emptyList()));
                            continue;
                        }

                        List<String> validIps = new ArrayList<>();
                        for (String ip : sourceIps) {
                            if (StringUtils.isBlank(ip)) {
                                continue;
                            }
                            if (CidrUtil.isWildcard(ip)) {
                                wildcardCount++;
                                findings.add(new AuditFinding(
                                        subject, resource, "CRITICAL", "WILDCARD_SOURCE_IP",
                                        "Policy entry allows unrestricted global access via wildcard: " + ip,
                                        Collections.singletonList(ip)));
                            } else if (!CidrUtil.isValidIpv4OrCidr(ip)) {
                                invalidSyntaxCount++;
                                findings.add(new AuditFinding(
                                        subject, resource, "HIGH", "INVALID_CIDR_SYNTAX",
                                        "Malformed IPv4 address or CIDR notation: " + ip,
                                        Collections.singletonList(ip)));
                            } else {
                                validIps.add(ip.trim());
                            }
                        }

                        // Check mutual overlap within the entry
                        for (int i = 0; i < validIps.size(); i++) {
                            for (int j = i + 1; j < validIps.size(); j++) {
                                String ip1 = validIps.get(i);
                                String ip2 = validIps.get(j);
                                if (CidrUtil.isOverlap(ip1, ip2)) {
                                    overlappingCount++;
                                    findings.add(new AuditFinding(
                                            subject, resource, "MEDIUM", "OVERLAPPING_CIDR_RULES",
                                            String.format("Redundant or overlapping CIDR rules detected: %s vs %s", ip1, ip2),
                                            Arrays.asList(ip1, ip2)));
                                }
                            }
                        }
                    }
                }
            }
        }

        report.setTotalPoliciesEvaluated(totalPolicies);
        report.setWildcardSourceIpCount(wildcardCount);
        report.setOverlappingRuleCount(overlappingCount);
        report.setInvalidCidrSyntaxCount(invalidSyntaxCount);
        report.setFindings(findings);
        return report;
    }
}
