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
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;

public class PacketBundleChannel {
    private static final IvParameterSpec IV;
    private static final ChecksumUtil.ChecksumAlgorithm HASH_ALGORITHM =
            ChecksumUtil.ChecksumAlgorithm.SHA256;
    private static final int HASH_SIZE, BUNDLE_HEADER_SIZE = 4;
    private final SocketChannel channel;
    private final ByteBufferOutputStream queueStreamOut;
    private final DataOutputStream dataStreamOut;
    private final ByteBufferOutputStream byteBufferStreamOut =
            new ByteBufferOutputStream(
                    length -> BufferCreator.byteBuffer(length + 1024));
    private final ByteBuffer header = BufferCreator.byteBuffer(4);
    private final Queue<ByteBuffer> queue = new ConcurrentLinkedQueue<>();
    private final List<WeakReference<ByteBuffer>> bufferCache =
            new ArrayList<>();
    private final byte[] hashEncrypted;
    private final Deflater deflater;
    private final Inflater inflater;
    private final Cipher encryptCipher, decryptCipher;
    private final MessageDigest digest;
    private final AtomicInteger inRate = new AtomicInteger(), outRate =
            new AtomicInteger();
    private Optional<Selector> selector = Optional.empty();
    private boolean encrypt, hasInput;
    private ByteBuffer output, input = BufferCreator.byteBuffer(1024),
            inputDecrypt = BufferCreator.byteBuffer(1024);

    static {
        Random random = new Random(
                StringLongHash.hash("Totally secure initialization vector :P"));
        byte[] array = new byte[16];
        random.nextBytes(array);
        IV = new IvParameterSpec(array);
        try {
            HASH_SIZE = MessageDigest.getInstance(HASH_ALGORITHM.getName())
                    .getDigestLength();
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedJVMException(e);
        }
    }

    public PacketBundleChannel(SocketChannel channel) {
        this(channel, null, null);
    }

    public PacketBundleChannel(SocketChannel channel, byte[] encryptKey,
            byte[] decryptKey) {
        this.channel = channel;
        deflater = new Deflater();
        inflater = new Inflater();
        try {
            encryptCipher = Cipher.getInstance("AES/CBC/NoPadding");
            decryptCipher = Cipher.getInstance("AES/CBC/NoPadding");
            digest = MessageDigest.getInstance(HASH_ALGORITHM.getName());
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new UnsupportedJVMException(e);
        }
        setKey(encryptKey, decryptKey);
        queueStreamOut = new ByteBufferOutputStream(
                length -> BufferCreator.byteBuffer(length + 102400));
        dataStreamOut = new DataOutputStream(queueStreamOut);
        hashEncrypted = new byte[HASH_SIZE];
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
        try (DeflaterOutputStream deflaterStreamOut = new DeflaterOutputStream(
                byteBufferStreamOut, deflater)) {
            ProcessStream.process(
                    new ByteBufferInputStream(queueStreamOut.getBuffer()),
                    (byte[] buffer, int offset, int length) -> {
                        deflaterStreamOut.write(buffer, offset, length);
                        digest.update(buffer, offset, length);
                    });
            deflaterStreamOut.finish();
        }
        byte[] hash = digest.digest();
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
        int bundleSize = HASH_SIZE + buffer.remaining();
        int capacity;
        if (encrypt) {
            capacity = BUNDLE_HEADER_SIZE +
                    encryptCipher.getOutputSize(bundleSize);
        } else {
            capacity = BUNDLE_HEADER_SIZE + bundleSize;
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
        bundle.position(BUNDLE_HEADER_SIZE);
        int size;
        if (encrypt) {
            try {
                size = encryptCipher.update(hash, 0, HASH_SIZE, hashEncrypted);
                bundle.put(hashEncrypted);
                size += encryptCipher.update(buffer, bundle);
            } catch (ShortBufferException e) {
                throw new IOException(e);
            }
        } else {
            size = bundleSize;
            bundle.put(hash);
            bundle.put(buffer);
        }
        bundle.flip();
        bundle.putInt(size);
        bundle.rewind();
        queue.add(bundle);
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

    public Optional<DataInputStream> fetch() throws IOException {
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
                buffer.position(HASH_SIZE);
                CompressionUtil
                        .decompress(buffer, byteBufferStreamOut, inflater);
                inflater.reset();
                ByteBuffer bundle = byteBufferStreamOut.getBuffer();
                byteBufferStreamOut.reset();
                ProcessStream.process(new ByteBufferInputStream(bundle),
                        digest::update);
                bundle.flip();
                byte[] hash = digest.digest();
                buffer.position(0);
                for (byte i : hash) {
                    if (i != buffer.get()) {
                        throw new IOException("Integrity check failed");
                    }
                }
                hasInput = false;
                return Optional.of(new DataInputStream(
                        new ByteBufferInputStream(bundle)));
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
        deflater.end();
        inflater.end();
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
}
