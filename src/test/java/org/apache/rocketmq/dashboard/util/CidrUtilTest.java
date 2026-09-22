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

package org.apache.rocketmq.dashboard.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CidrUtilTest {

    @Test
    public void testIsWildcard() {
        assertTrue(CidrUtil.isWildcard("*"));
        assertTrue(CidrUtil.isWildcard("0.0.0.0/0"));
        assertTrue(CidrUtil.isWildcard("::/0"));
        assertFalse(CidrUtil.isWildcard("192.168.1.1"));
        assertFalse(CidrUtil.isWildcard("10.0.0.0/8"));
        assertFalse(CidrUtil.isWildcard(null));
    }

    @Test
    public void testIsValidIpv4OrCidr() {
        assertTrue(CidrUtil.isValidIpv4OrCidr("192.168.1.1"));
        assertTrue(CidrUtil.isValidIpv4OrCidr("10.0.0.0/8"));
        assertTrue(CidrUtil.isValidIpv4OrCidr("172.16.0.0/16"));
        assertTrue(CidrUtil.isValidIpv4OrCidr("192.168.0.0/24"));
        assertTrue(CidrUtil.isValidIpv4OrCidr("*"));
        assertFalse(CidrUtil.isValidIpv4OrCidr("999.1.1.1"));
        assertFalse(CidrUtil.isValidIpv4OrCidr("192.168.1.1/33"));
        assertFalse(CidrUtil.isValidIpv4OrCidr("abc"));
        assertFalse(CidrUtil.isValidIpv4OrCidr(null));
        assertFalse(CidrUtil.isValidIpv4OrCidr(""));
    }

    @Test
    public void testIpv4ToLong() {
        assertEquals(0x7F000001L, CidrUtil.ipv4ToLong("127.0.0.1"));
        assertEquals(0L, CidrUtil.ipv4ToLong("0.0.0.0"));
        assertEquals(0xFFFFFFFFL, CidrUtil.ipv4ToLong("255.255.255.255"));
    }

    @Test
    public void testParseRange() {
        CidrUtil.IpRange range = CidrUtil.parseRange("192.168.1.0/24");
        assertEquals(CidrUtil.ipv4ToLong("192.168.1.0"), range.getStart());
        assertEquals(CidrUtil.ipv4ToLong("192.168.1.255"), range.getEnd());

        CidrUtil.IpRange single = CidrUtil.parseRange("10.1.2.3");
        assertEquals(CidrUtil.ipv4ToLong("10.1.2.3"), single.getStart());
        assertEquals(CidrUtil.ipv4ToLong("10.1.2.3"), single.getEnd());

        CidrUtil.IpRange wildcard = CidrUtil.parseRange("*");
        assertEquals(0L, wildcard.getStart());
        assertEquals(0xFFFFFFFFL, wildcard.getEnd());
    }

    @Test
    public void testIsOverlap() {
        assertTrue(CidrUtil.isOverlap("192.168.1.0/24", "192.168.1.100"));
        assertTrue(CidrUtil.isOverlap("192.168.1.0/24", "192.168.0.0/16"));
        assertFalse(CidrUtil.isOverlap("192.168.1.0/24", "192.168.2.0/24"));
        assertTrue(CidrUtil.isOverlap("*", "10.0.0.1"));
    }

    @Test
    public void testContains() {
        assertTrue(CidrUtil.contains("192.168.0.0/16", "192.168.1.0/24"));
        assertTrue(CidrUtil.contains("192.168.0.0/16", "192.168.1.50"));
        assertFalse(CidrUtil.contains("192.168.1.0/24", "192.168.0.0/16"));
        assertFalse(CidrUtil.contains("10.0.0.0/8", "192.168.1.1"));
    }
}
