/*
 * Copyright 2012-2015 Tobi29
 *
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

package org.tobi29.scapes.engine.utils.io;

import org.tobi29.scapes.engine.utils.BufferCreator;
import org.tobi29.scapes.engine.utils.StringLongHash;
import org.tobi29.scapes.engine.utils.UnsupportedJVMException;
import org.tobi29.scapes.engine.utils.math.FastMath;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class PacketBundleChannel {
    private static final IvParameterSpec IV;
    private static final int BUNDLE_HEADER_SIZE = 4;
    private static final int BUNDLE_MAX_SIZE = 1 << 10 << 10 << 6;

    static {
        Random random = new Random(
                StringLongHash.hash("Totally secure initialization vector :P"));
        byte[] array = new byte[16];
        random.nextBytes(array);
        IV = new IvParameterSpec(array);
    }

    private final SocketChannel channel;
    private final ByteBufferStream dataStreamOut = new ByteBufferStream(
            length -> BufferCreator.bytes(length + 102400)),
            byteBufferStreamOut = new ByteBufferStream(
                    length -> BufferCreator.bytes(length + 102400));
    private final ByteBuffer header = BufferCreator.bytes(BUNDLE_HEADER_SIZE);
    private final Queue<ByteBuffer> queue = new ConcurrentLinkedQueue<>();
    private final List<WeakReference<ByteBuffer>> bufferCache =
            new ArrayList<>();
    private final CompressionUtil.Filter deflater, inflater, decipherInflater;
    private final Cipher encryptCipher, decryptCipher;
    private final AtomicInteger inRate = new AtomicInteger(), outRate =
            new AtomicInteger();
    private Optional<Selector> selector = Optional.empty();
    private boolean encrypt, hasInput;
    private ByteBuffer output, input = BufferCreator.bytes(1024);

    public PacketBundleChannel(SocketChannel channel) {
        this(channel, null, null);
    }

    public PacketBundleChannel(SocketChannel channel, byte[] encryptKey,
            byte[] decryptKey) {
        this.channel = channel;
        deflater = new CompressionUtil.ZDeflater(1);
        inflater = new CompressionUtil.ZInflater();
        decipherInflater = new DecipherZInflater();
        try {
            encryptCipher = Cipher.getInstance("AES/CBC/NoPadding");
            decryptCipher = Cipher.getInstance("AES/CBC/NoPadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new UnsupportedJVMException(e);
        }
        setKey(encryptKey, decryptKey);
    }

    public WritableByteStream getOutputStream() {
        return dataStreamOut;
    }

    public int bundleSize() {
        return dataStreamOut.buffer().position();
    }

    public void queueBundle() throws IOException {
        dataStreamOut.buffer().flip();
        if (!dataStreamOut.buffer().hasRemaining()) {
            dataStreamOut.buffer().clear();
            return;
        }
        byteBufferStreamOut.buffer().clear();
        CompressionUtil.filter(new ByteBufferStream(dataStreamOut.buffer()),
                byteBufferStreamOut, deflater);
        int capacity;
        if (encrypt) {
            int padding = 16 - (byteBufferStreamOut.buffer().position() & 15);
            Random random = ThreadLocalRandom.current();
            do {
                byteBufferStreamOut.put(random.nextInt(256));
            } while (--padding > 0);
            capacity = BUNDLE_HEADER_SIZE + encryptCipher
                    .getOutputSize(byteBufferStreamOut.buffer().position());
        } else {
            capacity = BUNDLE_HEADER_SIZE +
                    byteBufferStreamOut.buffer().position();
        }
        byteBufferStreamOut.buffer().flip();
        ByteBuffer bundle = null;
        int i = 0;
        while (i < bufferCache.size()) {
            ByteBuffer cacheBuffer = bufferCache.get(i).get();
            if (cacheBuffer == null) {
                bufferCache.remove(i);
            } else if (cacheBuffer.capacity() >= capacity) {
                bufferCache.remove(i);
                bundle = cacheBuffer;
                bundle.clear();
                break;
            } else {
                i++;
            }
        }
        if (bundle == null) {
            bundle = BufferCreator.bytes(capacity);
        }
        bundle.position(BUNDLE_HEADER_SIZE);
        int size;
        if (encrypt) {
            try {
                size = encryptCipher
                        .update(byteBufferStreamOut.buffer(), bundle);
            } catch (ShortBufferException e) {
                throw new IOException(e);
            }
        } else {
            size = byteBufferStreamOut.buffer().remaining();
            bundle.put(byteBufferStreamOut.buffer());
        }
        if (size > BUNDLE_MAX_SIZE) {
            throw new IOException(
                    "Unable to send too large bundle of size: " + size);
        }
        bundle.flip();
        bundle.putInt(size);
        bundle.rewind();
        queue.add(bundle);
        dataStreamOut.buffer().clear();
        selector.ifPresent(Selector::wakeup);
    }

    public boolean process() throws IOException {
        if (output == null) {
            output = queue.poll();
        }
        if (output != null) {
            int write = channel.write(output);
            if (!output.hasRemaining()) {
                bufferCache.add(new WeakReference<>(output));
                output = null;
            } else if (write == -1) {
                throw new IOException("Connection closed");
            }
            outRate.getAndAdd(write);
            return true;
        }
        return false;
    }

    public Optional<ReadableByteStream> fetch() throws IOException {
        if (!hasInput) {
            int read = channel.read(header);
            if (!header.hasRemaining()) {
                header.flip();
                int limit = header.getInt();
                if (limit < 0 || limit > BUNDLE_MAX_SIZE) {
                    throw new IOException("Invalid bundle length: " + limit);
                }
                if (limit > input.capacity()) {
                    input = BufferCreator.bytes(limit);
                } else {
                    input.clear().limit(limit);
                }
                hasInput = true;
                header.clear();
            } else if (read == -1) {
                throw new IOException("Connection closed");
            }
            inRate.getAndAdd(read);
        }
        if (hasInput) {
            int read = channel.read(input);
            if (!input.hasRemaining()) {
                inRate.getAndAdd(read);
                input.flip();
                byteBufferStreamOut.buffer().clear();
                if (encrypt) {
                    CompressionUtil.filter(new ByteBufferStream(input),
                            byteBufferStreamOut, decipherInflater);
                } else {
                    CompressionUtil.filter(new ByteBufferStream(input),
                            byteBufferStreamOut, inflater);
                }
                ByteBuffer bundle = byteBufferStreamOut.buffer();
                bundle.flip();
                hasInput = false;
                return Optional.of(new ByteBufferStream(bundle));
            } else if (read == -1) {
                throw new IOException("Connection closed");
            }
            inRate.getAndAdd(read);
        }
        return Optional.empty();
    }

    public void setKey(byte[] encryptKey, byte[] decryptKey) {
        if (encryptKey == null || decryptKey == null) {
            encrypt = false;
        } else {
            SecretKeySpec encryptKeySpec = new SecretKeySpec(encryptKey, "AES");
            SecretKeySpec decryptKeySpec = new SecretKeySpec(decryptKey, "AES");
            try {
                encryptCipher.init(Cipher.ENCRYPT_MODE, encryptKeySpec, IV);
                decryptCipher.init(Cipher.DECRYPT_MODE, decryptKeySpec, IV);
            } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
                throw new UnsupportedJVMException(e);
            }
            encrypt = true;
        }
    }

    public void register(Selector selector, int opt) throws IOException {
        channel.register(selector, opt);
        this.selector = Optional.of(selector);
    }

    public void close() throws IOException {
        channel.close();
        deflater.close();
        inflater.close();
    }

    public int getOutputRate() {
        return outRate.getAndSet(0);
    }

    public int getInputRate() {
        return inRate.getAndSet(0);
    }

    public Optional<InetSocketAddress> getRemoteAddress() {
        try {
            SocketAddress address = channel.getRemoteAddress();
            if (address instanceof InetSocketAddress) {
                return Optional.of((InetSocketAddress) address);
            }
        } catch (IOException e) {
        }
        return Optional.empty();
    }

    @SuppressWarnings("ObjectToString")
    @Override
    public String toString() {
        try {
            return channel.getRemoteAddress().toString();
        } catch (IOException e) {
        }
        return super.toString();
    }

    @SuppressWarnings("AmbiguousFieldAccess")
    public class DecipherZInflater extends CompressionUtil.ZInflater {
        public DecipherZInflater() {
            this(8192);
        }

        public DecipherZInflater(int buffer) {
            super(buffer);
        }

        @Override
        public void input(ReadableByteStream buffer) throws IOException {
            int len = FastMath.min(buffer.remaining(), this.buffer);
            if (input.length < len) {
                input = new byte[len];
                output = new byte[len];
            }
            buffer.get(output, 0, len);
            try {
                decryptCipher.update(output, 0, len, input, 0);
            } catch (ShortBufferException e) {
                throw new IOException(e);
            }
            inflater.setInput(input, 0, len);
        }
    }
}
