/*
 * Copyright 2015 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.linecorp.armeria.server.thrift;

import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import io.netty.buffer.ByteBuf;

final class TByteBufTransport extends TTransport {

    private ByteBuf buf;

    void reset(ByteBuf buf) {
        this.buf = buf;
    }

    void clear() {
        buf = null;
    }

    @Override
    public void close() { }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void open() { }

    @Override
    public int read(byte[] buf, int off, int len) {
        int bytesRemaining = this.buf.readableBytes();
        int amtToRead = len > bytesRemaining ? bytesRemaining : len;
        if (amtToRead > 0) {
            this.buf.readBytes(buf, off, amtToRead);
        }
        return amtToRead;
    }

    @Override
    public int readAll(byte[] buf, int off, int len) throws TTransportException {
        int bytesRemaining = this.buf.readableBytes();
        if (len > bytesRemaining) {
            throw new TTransportException("unexpected end of frame");
        }

        this.buf.readBytes(buf, off, len);
        return len;
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        this.buf.writeBytes(buf, off, len);
    }

    @Override
    public byte[] getBuffer() {
        ByteBuf buf = this.buf;
        if (buf == null || !buf.hasArray())  {
            return null;
        } else {
            return buf.array();
        }
    }

    @Override
    public int getBufferPosition() {
        ByteBuf buf = this.buf;
        if (buf == null || !buf.hasArray())  {
            return 0;
        } else {
            return buf.arrayOffset() + buf.readerIndex();
        }
    }

    @Override
    public int getBytesRemainingInBuffer() {
        ByteBuf buf = this.buf;
        if (buf.hasArray()) {
            return buf.readableBytes();
        } else {
            return -1;
        }
    }

    @Override
    public void consumeBuffer(int len) {
        buf.skipBytes(len);
    }
}
