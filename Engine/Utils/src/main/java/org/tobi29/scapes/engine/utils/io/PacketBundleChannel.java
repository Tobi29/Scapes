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

import org.tobi29.scapes.engine.utils.*;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class PacketBundleChannel {
    private static final IvParameterSpec IV;
    private static final byte[] HEADER;
    private final SocketChannel channel;
    private final ConnectListener connectListener;
    private final ByteBufferOutputStream queueStreamOut;
    private final DataOutputStream dataStreamOut;
    private final ByteBufferOutputStream byteBufferStreamOut =
            new ByteBufferOutputStream(
                    length -> BufferCreator.byteBuffer(length + 1024));
    private final ByteBuffer header = BufferCreator.byteBuffer(4);
    private final Queue<ByteBuffer> queue = new ConcurrentLinkedQueue<>();
    private final List<WeakReference<ByteBuffer>> bufferCache =
            new ArrayList<>();
    private final Deflater deflater;
    private final Inflater inflater;
    private final Cipher encryptCipher, decryptCipher;
    private final AtomicInteger inRate = new AtomicInteger(), outRate =
            new AtomicInteger();
    private boolean checked, encrypt, hasInput;
    private ByteBuffer output, input = BufferCreator.byteBuffer(1024),
            inputDecrypt = BufferCreator.byteBuffer(1024);

    static {
        Random random = new Random(
                StringLongHash.hash("Totally secure initialization vector :P"));
        byte[] array = new byte[16];
        random.nextBytes(array);
        IV = new IvParameterSpec(array);
        byte[] header = "Encryption worked".getBytes(StandardCharsets.UTF_8);
        HEADER = new byte[header.length + 15 & ~15];
        System.arraycopy(header, 0, HEADER, 0, header.length);
    }

    public PacketBundleChannel(SocketChannel channel) {
        this(channel, null, null);
    }

    public PacketBundleChannel(SocketChannel channel, byte[] encryptKey,
            byte[] decryptKey) {
        this(channel, encryptKey, decryptKey, () -> {
        });
    }

    public PacketBundleChannel(SocketChannel channel, byte[] encryptKey,
            byte[] decryptKey, ConnectListener connectListener) {
        this.channel = channel;
        this.connectListener = connectListener;
        deflater = new Deflater();
        inflater = new Inflater();
        try {
            encryptCipher = Cipher.getInstance("AES/CBC/NoPadding");
            decryptCipher = Cipher.getInstance("AES/CBC/NoPadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new IllegalStateException(e);
        }
        setKey(encryptKey, decryptKey);
        queueStreamOut = new ByteBufferOutputStream(
                length -> BufferCreator.byteBuffer(length + 102400));
        dataStreamOut = new DataOutputStream(queueStreamOut);
        ByteBuffer buffer = ByteBuffer.wrap(HEADER);
        ByteBuffer bundle;
        int size;
        if (encrypt) {
            bundle = BufferCreator
                    .byteBuffer(encryptCipher.getOutputSize(HEADER.length) + 4);
            bundle.position(4);
            try {
                size = encryptCipher.update(buffer, bundle);
            } catch (ShortBufferException e) {
                throw new IllegalStateException(e);
            }
        } else {
            size = buffer.remaining();
            bundle = BufferCreator.byteBuffer(size + 4);
            bundle.position(4);
            bundle.put(buffer);
        }
        bundle.flip();
        bundle.putInt(size);
        bundle.rewind();
        queue.add(bundle);
    }

    public DataOutputStream getOutputStream() {
        return dataStreamOut;
    }

    public int bundleSize() {
        return queueStreamOut.size();
    }

    public void queueBundle() throws IOException {
        if (queueStreamOut.size() == 0) {
            return;
        }
        CompressionUtil
                .compress(queueStreamOut.getBuffer(), byteBufferStreamOut,
                        deflater);
        deflater.reset();
        int padding = 16 - (byteBufferStreamOut.size() & 15);
        if (padding > 0) {
            Random random = ThreadLocalRandom.current();
            do {
                byteBufferStreamOut.write(random.nextInt(256));
            } while (--padding > 0);
        }
        ByteBuffer buffer = byteBufferStreamOut.getBuffer();
        byteBufferStreamOut.reset();
        queueStreamOut.reset();
        int capacity;
        if (encrypt) {
            capacity = encryptCipher.getOutputSize(buffer.remaining()) + 4;
        } else {
            capacity = buffer.remaining() + 4;
        }
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
            bundle = BufferCreator.byteBuffer(capacity);
        }
        bundle.position(4);
        int size;
        if (encrypt) {
            try {
                size = encryptCipher.update(buffer, bundle);
            } catch (ShortBufferException e) {
                throw new IOException(e);
            }
        } else {
            size = buffer.remaining();
            bundle.put(buffer);
        }
        bundle.flip();
        bundle.putInt(size);
        bundle.rewind();
        queue.add(bundle);
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

    public InputStream fetch() throws IOException {
        if (!hasInput) {
            int read = channel.read(header);
            if (!header.hasRemaining()) {
                int limit = header.getInt(0);
                if (limit > input.capacity()) {
                    input = BufferCreator.byteBuffer(limit);
                } else {
                    input.limit(limit);
                    input.rewind();
                }
                hasInput = true;
                header.rewind();
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
                ByteBuffer buffer;
                if (encrypt) {
                    inputDecrypt.clear();
                    int capacity =
                            decryptCipher.getOutputSize(input.remaining());
                    if (capacity > inputDecrypt.remaining()) {
                        inputDecrypt = BufferCreator.byteBuffer(capacity);
                    }
                    try {
                        decryptCipher.update(input, inputDecrypt);
                    } catch (ShortBufferException e) {
                        throw new IOException(e);
                    }
                    inputDecrypt.flip();
                    buffer = inputDecrypt;
                } else {
                    buffer = input;
                }
                if (checked) {
                    CompressionUtil
                            .decompress(buffer, byteBufferStreamOut, inflater);
                    inflater.reset();
                    ByteBuffer bundle = byteBufferStreamOut.getBuffer();
                    byteBufferStreamOut.reset();
                    hasInput = false;
                    return new ByteBufferInputStream(bundle);
                } else {
                    DataInputStream streamIn = new DataInputStream(
                            new ByteBufferInputStream(buffer));
                    byte[] header = new byte[HEADER.length];
                    streamIn.readFully(header);
                    if (Arrays.equals(HEADER, header)) {
                        checked = true;
                        hasInput = false;
                        connectListener.connect();
                        return null;
                    } else {
                        throw new IOException("Invalid header: " +
                                new String(header, StandardCharsets.UTF_8));
                    }
                }
            } else if (read == -1) {
                throw new IOException("Connection closed");
            }
            inRate.getAndAdd(read);
        }
        return null;
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
                throw new IllegalStateException(e);
            }
            encrypt = true;
        }
    }

    public void close() throws IOException {
        channel.close();
        deflater.end();
        inflater.end();
    }

    public int getOutputRate() {
        return outRate.getAndSet(0);
    }

    public int getInputRate() {
        return inRate.getAndSet(0);
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

    @FunctionalInterface
    public interface ConnectListener {
        void connect();
    }
}
