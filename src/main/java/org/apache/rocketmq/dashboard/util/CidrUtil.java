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

import java.util.regex.Pattern;

public class CidrUtil {

    private static final Pattern IPV4_PATTERN =
            Pattern.compile("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");

    public static boolean isWildcard(String ipPattern) {
        if (ipPattern == null) {
            return false;
        }
        String trimmed = ipPattern.trim();
        return "*".equals(trimmed) || "0.0.0.0/0".equals(trimmed) || "::/0".equals(trimmed);
    }

    public static boolean isValidIpv4OrCidr(String ipOrCidr) {
        if (ipOrCidr == null || ipOrCidr.trim().isEmpty()) {
            return false;
        }
        String trimmed = ipOrCidr.trim();
        if ("*".equals(trimmed)) {
            return true;
        }
        String[] parts = trimmed.split("/");
        if (parts.length == 1) {
            return IPV4_PATTERN.matcher(parts[0]).matches();
        } else if (parts.length == 2) {
            if (!IPV4_PATTERN.matcher(parts[0]).matches()) {
                return false;
            }
            try {
                int mask = Integer.parseInt(parts[1]);
                return mask >= 0 && mask <= 32;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    public static long ipv4ToLong(String ip) {
        String[] octets = ip.split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result = (result << 8) | (Integer.parseInt(octets[i]) & 0xFF);
        }
        return result;
    }

    public static IpRange parseRange(String ipOrCidr) {
        if (ipOrCidr == null) {
            throw new IllegalArgumentException("IP/CIDR cannot be null");
        }
        String trimmed = ipOrCidr.trim();
        if ("*".equals(trimmed) || "0.0.0.0/0".equals(trimmed)) {
            return new IpRange(0L, 0xFFFFFFFFL, trimmed);
        }
        String[] parts = trimmed.split("/");
        if (parts.length == 1) {
            if (!IPV4_PATTERN.matcher(parts[0]).matches()) {
                throw new IllegalArgumentException("Invalid IPv4 address: " + parts[0]);
            }
            long ipNum = ipv4ToLong(parts[0]);
            return new IpRange(ipNum, ipNum, trimmed);
        } else if (parts.length == 2) {
            if (!IPV4_PATTERN.matcher(parts[0]).matches()) {
                throw new IllegalArgumentException("Invalid IPv4 base in CIDR: " + parts[0]);
            }
            int mask = Integer.parseInt(parts[1]);
            if (mask < 0 || mask > 32) {
                throw new IllegalArgumentException("Invalid CIDR prefix length: " + mask);
            }
            long ipNum = ipv4ToLong(parts[0]);
            long netmask = mask == 0 ? 0L : (0xFFFFFFFFL << (32 - mask)) & 0xFFFFFFFFL;
            long start = ipNum & netmask;
            long end = start | (~netmask & 0xFFFFFFFFL);
            return new IpRange(start, end, trimmed);
        }
        throw new IllegalArgumentException("Malformed IP or CIDR specification: " + ipOrCidr);
    }

    public static boolean isOverlap(String cidr1, String cidr2) {
        try {
            IpRange range1 = parseRange(cidr1);
            IpRange range2 = parseRange(cidr2);
            return range1.getStart() <= range2.getEnd() && range2.getStart() <= range1.getEnd();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean contains(String parentCidr, String childCidr) {
        try {
            IpRange parent = parseRange(parentCidr);
            IpRange child = parseRange(childCidr);
            return parent.getStart() <= child.getStart() && parent.getEnd() >= child.getEnd();
        } catch (Exception e) {
            return false;
        }
    }

    public static class IpRange {
        private final long start;
        private final long end;
        private final String originalText;

        public IpRange(long start, long end, String originalText) {
            this.start = start;
            this.end = end;
            this.originalText = originalText;
        }

        public long getStart() {
            return start;
        }

        public long getEnd() {
            return end;
        }

        public String getOriginalText() {
            return originalText;
        }
    }
}
