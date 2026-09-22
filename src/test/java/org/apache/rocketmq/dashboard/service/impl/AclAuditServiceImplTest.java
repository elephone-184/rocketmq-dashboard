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

import org.apache.rocketmq.dashboard.model.AclCidrAuditReport;
import org.apache.rocketmq.dashboard.service.ClusterInfoService;
import org.apache.rocketmq.remoting.protocol.body.AclInfo;
import org.apache.rocketmq.remoting.protocol.body.AclInfo.PolicyEntryInfo;
import org.apache.rocketmq.remoting.protocol.body.AclInfo.PolicyInfo;
import org.apache.rocketmq.remoting.protocol.body.ClusterInfo;
import org.apache.rocketmq.remoting.protocol.route.BrokerData;
import org.apache.rocketmq.tools.admin.MQAdminExt;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AclAuditServiceImplTest {

    @InjectMocks
    private AclAuditServiceImpl aclAuditService;

    @Mock
    private MQAdminExt mqAdminExt;

    @Mock
    private ClusterInfoService clusterInfoService;

    private static final String CLUSTER_NAME = "DefaultCluster";
    private static final String BROKER_ADDR = "127.0.0.1:10911";

    @Before
    public void setup() {
        ClusterInfo clusterInfo = new ClusterInfo();
        BrokerData brokerData = new BrokerData();
        brokerData.setBrokerName("broker-a");
        HashMap<Long, String> brokerAddrs = new HashMap<>();
        brokerAddrs.put(0L, BROKER_ADDR);
        brokerData.setBrokerAddrs(brokerAddrs);

        HashMap<String, BrokerData> brokerAddrTable = new HashMap<>();
        brokerAddrTable.put("broker-a", brokerData);
        clusterInfo.setBrokerAddrTable(brokerAddrTable);

        HashMap<String, java.util.Set<String>> clusterTable = new HashMap<>();
        clusterTable.put(CLUSTER_NAME, Collections.singleton("broker-a"));
        clusterInfo.setClusterAddrTable(clusterTable);

        when(clusterInfoService.getBrokerClusterInfo()).thenReturn(clusterInfo);
    }

    @Test
    public void testAuditAclCidrSubnetsNormal() throws Exception {
        AclInfo aclInfo = new AclInfo();
        aclInfo.setSubject("testSubject");

        PolicyInfo policy = new PolicyInfo();
        policy.setPolicyType("custom");

        PolicyEntryInfo entry = new PolicyEntryInfo();
        entry.setResource("TopicTest");
        entry.setSourceIps(Arrays.asList("192.168.1.10", "10.0.0.0/16"));
        policy.setEntries(Collections.singletonList(entry));
        aclInfo.setPolicies(Collections.singletonList(policy));

        when(mqAdminExt.listAcl(BROKER_ADDR, "", "")).thenReturn(Collections.singletonList(aclInfo));

        AclCidrAuditReport report = aclAuditService.auditAclCidrSubnets(CLUSTER_NAME, null);

        assertNotNull(report);
        assertEquals(1, report.getTotalPoliciesEvaluated());
        assertEquals(0, report.getWildcardSourceIpCount());
        assertEquals(0, report.getOverlappingRuleCount());
        assertEquals(0, report.getInvalidCidrSyntaxCount());
        assertTrue(report.getFindings().isEmpty());
    }

    @Test
    public void testAuditAclCidrSubnetsWildcardAndOverlap() throws Exception {
        AclInfo aclInfo = new AclInfo();
        aclInfo.setSubject("riskySubject");

        PolicyInfo policy = new PolicyInfo();
        PolicyEntryInfo entry = new PolicyEntryInfo();
        entry.setResource("OrderTopic");
        entry.setSourceIps(Arrays.asList("*", "192.168.1.0/24", "192.168.1.50", "invalid-ip-addr"));
        policy.setEntries(Collections.singletonList(entry));
        aclInfo.setPolicies(Collections.singletonList(policy));

        when(mqAdminExt.listAcl(BROKER_ADDR, "", "")).thenReturn(Collections.singletonList(aclInfo));

        AclCidrAuditReport report = aclAuditService.auditAclCidrSubnets(null, BROKER_ADDR);

        assertNotNull(report);
        assertEquals(1, report.getTotalPoliciesEvaluated());
        assertEquals(1, report.getWildcardSourceIpCount());
        assertEquals(1, report.getOverlappingRuleCount());
        assertEquals(1, report.getInvalidCidrSyntaxCount());
        assertEquals(3, report.getFindings().size());
    }

    @Test
    public void testAuditAclCidrSubnetsBrokerException() throws Exception {
        when(mqAdminExt.listAcl(anyString(), anyString(), anyString())).thenThrow(new RuntimeException("Connection timed out"));

        AclCidrAuditReport report = aclAuditService.auditAclCidrSubnets(CLUSTER_NAME, null);

        assertNotNull(report);
        assertEquals(1, report.getFindings().size());
        assertEquals("BROKER_QUERY_EXCEPTION", report.getFindings().get(0).getIssueType());
    }
}
