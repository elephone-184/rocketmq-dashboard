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

package org.apache.rocketmq.dashboard.model;

import java.util.ArrayList;
import java.util.List;

public class AclCidrAuditReport {

    private String clusterName;
    private String brokerAddress;
    private int totalPoliciesEvaluated;
    private int wildcardSourceIpCount;
    private int overlappingRuleCount;
    private int invalidCidrSyntaxCount;
    private List<AuditFinding> findings = new ArrayList<>();

    public AclCidrAuditReport() {
    }

    public AclCidrAuditReport(String clusterName, String brokerAddress) {
        this.clusterName = clusterName;
        this.brokerAddress = brokerAddress;
    }

    public static class AuditFinding {
        private String subject;
        private String resource;
        private String level;
        private String issueType;
        private String description;
        private List<String> relatedSourceIps = new ArrayList<>();

        public AuditFinding() {
        }

        public AuditFinding(String subject, String resource, String level, String issueType,
                            String description, List<String> relatedSourceIps) {
            this.subject = subject;
            this.resource = resource;
            this.level = level;
            this.issueType = issueType;
            this.description = description;
            if (relatedSourceIps != null) {
                this.relatedSourceIps = new ArrayList<>(relatedSourceIps);
            }
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public String getIssueType() {
            return issueType;
        }

        public void setIssueType(String issueType) {
            this.issueType = issueType;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<String> getRelatedSourceIps() {
            return relatedSourceIps;
        }

        public void setRelatedSourceIps(List<String> relatedSourceIps) {
            this.relatedSourceIps = relatedSourceIps;
        }
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getBrokerAddress() {
        return brokerAddress;
    }

    public void setBrokerAddress(String brokerAddress) {
        this.brokerAddress = brokerAddress;
    }

    public int getTotalPoliciesEvaluated() {
        return totalPoliciesEvaluated;
    }

    public void setTotalPoliciesEvaluated(int totalPoliciesEvaluated) {
        this.totalPoliciesEvaluated = totalPoliciesEvaluated;
    }

    public int getWildcardSourceIpCount() {
        return wildcardSourceIpCount;
    }

    public void setWildcardSourceIpCount(int wildcardSourceIpCount) {
        this.wildcardSourceIpCount = wildcardSourceIpCount;
    }

    public int getOverlappingRuleCount() {
        return overlappingRuleCount;
    }

    public void setOverlappingRuleCount(int overlappingRuleCount) {
        this.overlappingRuleCount = overlappingRuleCount;
    }

    public int getInvalidCidrSyntaxCount() {
        return invalidCidrSyntaxCount;
    }

    public void setInvalidCidrSyntaxCount(int invalidCidrSyntaxCount) {
        this.invalidCidrSyntaxCount = invalidCidrSyntaxCount;
    }

    public List<AuditFinding> getFindings() {
        return findings;
    }

    public void setFindings(List<AuditFinding> findings) {
        this.findings = findings;
    }
}
