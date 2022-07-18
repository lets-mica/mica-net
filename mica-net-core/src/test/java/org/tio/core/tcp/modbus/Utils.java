/*
 *
 * The MIT License (MIT)
 *
 * Copyright (C) 2013 Man YUAN <epsilon@epsilony.net>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.tio.core.tcp.modbus;

import java.nio.ByteBuffer;

/**
 * @author <a href="mailto:epsilony@epsilony.net">Man YUAN</a>
 *
 */
public class Utils {
    public static int crc(ByteBuffer byteBuf, int start, int numBytes) {
        int polynomial = 0xA001;
        int crc = 0xFFFF;
        for (int i = start; i < start + numBytes; i++) {
            byte b = byteBuf.get(i);
            int low = (crc & 0xFF) ^ (b & 0xFF);
            crc &= (0xFF00);
            crc |= (low);
            for (int j = 0; j < 8; j++) {
                int t = crc & 0x01;
                crc >>= 1;
                if (t == 0) {
                    continue;
                }
                crc ^= polynomial;
            }
        }
        int first = (crc & 0xFF) << 8;
        crc = (crc & 0xFF00) >> 8 | first;
        return crc;
    }
}
