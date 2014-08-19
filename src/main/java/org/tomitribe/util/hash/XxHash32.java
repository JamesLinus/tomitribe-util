/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tomitribe.util.hash;

import static java.lang.Long.rotateLeft;
import static org.tomitribe.util.hash.JvmUtils.unsafe;
import static org.tomitribe.util.hash.Preconditions.checkPositionIndexes;

/**
 * Lifted from Airlift Slice
 * @author Martin Traverso
 */
public class XxHash32 {
    private final static long PRIME32_1 = 0x9E3779B185EBCA87L;
    private final static long PRIME32_2 = 0xC2B2AE3D27D4EB4FL;
    private final static long PRIME32_3 = 0x165667B19E3779F9L;
    private final static long PRIME32_4 = 0x85EBCA77C2b2AE63L;
    private final static long PRIME32_5 = 0x27D4EB2F165667C5L;

    private final static long DEFAULT_SEED = 0;

    public static long hash(long value) {
        long hash = DEFAULT_SEED + PRIME32_5 + SizeOf.SIZE_OF_LONG;
        hash = updateTail(hash, value);
        hash = finalShuffle(hash);

        return hash;
    }

    public static long hash(String data) {
        return hash(Slices.utf8Slice(data));
    }

    public static long hash(Slice data) {
        return hash(data, 0, data.length());
    }

    public static long hash(long seed, Slice data) {
        return hash(seed, data, 0, data.length());
    }

    public static long hash(Slice data, int offset, int length) {
        return hash(DEFAULT_SEED, data, offset, length);
    }

    public static long hash(long seed, Slice data, int offset, int length) {
        checkPositionIndexes(0, offset + length, data.length());

        Object base = data.getBase();
        long index = data.getAddress() + offset;
        long end = index + length;

        long hash;

        if (length >= 32) {
            long v1 = seed + PRIME32_1 + PRIME32_2;
            long v2 = seed + PRIME32_2;
            long v3 = seed + 0;
            long v4 = seed - PRIME32_1;

            long limit = end - 32;
            do {
                v1 = mix(v1, unsafe.getLong(base, index));
                index += 8;

                v2 = mix(v2, unsafe.getLong(base, index));
                index += 8;

                v3 = mix(v3, unsafe.getLong(base, index));
                index += 8;

                v4 = mix(v4, unsafe.getLong(base, index));
                index += 8;
            }
            while (index <= limit);

            hash = rotateLeft(v1, 1) + rotateLeft(v2, 7) + rotateLeft(v3, 12) + rotateLeft(v4, 18);

            hash = update(hash, v1);
            hash = update(hash, v2);
            hash = update(hash, v3);
            hash = update(hash, v4);
        } else {
            hash = seed + PRIME32_5;
        }

        hash += length;

        while (index <= end - 8) {
            hash = updateTail(hash, unsafe.getLong(base, index));
            index += 8;
        }

        if (index <= end - 4) {
            hash = updateTail(hash, unsafe.getInt(base, index));
            index += 4;
        }

        while (index < end) {
            hash = updateTail(hash, unsafe.getByte(base, index));
            index++;
        }

        hash = finalShuffle(hash);

        return hash;
    }

    private static long mix(long current, long value) {
        return rotateLeft(current + value * PRIME32_2, 31) * PRIME32_1;
    }

    private static long update(long hash, long value) {
        long temp = hash ^ mix(0, value);
        return temp * PRIME32_1 + PRIME32_4;
    }

    private static long updateTail(long hash, long value) {
        long temp = hash ^ mix(0, value);
        return rotateLeft(temp, 27) * PRIME32_1 + PRIME32_4;
    }

    private static long updateTail(long hash, int value) {
        long unsigned = value & 0xFFFFFFFFL;
        long temp = hash ^ (unsigned * PRIME32_1);
        return rotateLeft(temp, 23) * PRIME32_2 + PRIME32_3;
    }

    private static long updateTail(long hash, byte value) {
        int unsigned = value & 0xFF;
        long temp = hash ^ (unsigned * PRIME32_5);
        return rotateLeft(temp, 11) * PRIME32_1;
    }

    private static long finalShuffle(long hash) {
        hash ^= hash >>> 33;
        hash *= PRIME32_2;
        hash ^= hash >>> 29;
        hash *= PRIME32_3;
        hash ^= hash >>> 32;
        return hash;
    }
}