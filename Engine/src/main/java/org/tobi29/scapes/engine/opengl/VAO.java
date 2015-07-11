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

package org.tobi29.scapes.engine.opengl;

import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.BufferCreatorDirect;
import org.tobi29.scapes.engine.utils.math.FastMath;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class VAO {
    private static final List<VAO> VAO_LIST = new ArrayList<>();
    private static int disposeOffset;
    private final RenderType renderType;
    private final int length, stride;
    private final ByteBuffer buffer;
    private final ByteBuffer index;
    private final List<VAOAttributeData> attributes = new ArrayList<>();
    private int vertexID, indexID, arrayID;
    private boolean stored, used, markAsDisposed;

    public VAO(List<VAOAttribute> attributes, int vertices, int[] index,
            RenderType renderType) {
        this(attributes, vertices, index, index.length, renderType);
    }

    public VAO(List<VAOAttribute> attributes, int vertices, int[] index,
            int length, RenderType renderType) {
        if (renderType == RenderType.TRIANGLES && index.length % 3 != 0) {
            throw new IllegalArgumentException("Length not multiply of 3");
        } else if (renderType == RenderType.LINES && index.length % 2 != 0) {
            throw new IllegalArgumentException("Length not multiply of 2");
        }
        this.renderType = renderType;
        this.length = length;
        int stride = 0;
        for (VAOAttribute attribute : attributes) {
            if (attribute.length != vertices * attribute.size) {
                throw new IllegalArgumentException(
                        "Inconsistent attribute data length");
            }
            this.attributes.add(new VAOAttributeData(attribute, stride));
            attribute.offset = stride;
            int size = attribute.size * attribute.vertexType.getBytes();
            stride += (size | 0x03) + 1;
        }
        this.stride = stride;
        buffer = BufferCreatorDirect.byteBuffer(vertices * stride)
                .order(ByteOrder.nativeOrder());
        attributes.forEach(attribute -> addToBuffer(attribute, vertices));
        this.index = BufferCreatorDirect.byteBuffer(index.length << 1)
                .order(ByteOrder.nativeOrder());
        for (int i : index) {
            this.index.putShort((short) i);
        }
    }

    @OpenGLFunction
    public static void disposeUnused(GL gl) {
        OpenGL openGL = gl.getOpenGL();
        for (int i = disposeOffset; i < VAO_LIST.size(); i += 16) {
            VAO vao = VAO_LIST.get(i);
            assert vao.stored;
            if (vao.markAsDisposed || !vao.used) {
                openGL.deleteVBO(vao.vertexID);
                openGL.deleteVBO(vao.indexID);
                openGL.deleteVAO(vao.arrayID);
                vao.stored = false;
                VAO_LIST.remove(vao);
            }
            vao.used = false;
        }
        disposeOffset++;
        disposeOffset &= 15;
    }

    @OpenGLFunction
    public static void disposeAll(GL gl) {
        VAO_LIST.forEach(VAO::markAsDisposed);
        while (!VAO_LIST.isEmpty()) {
            disposeUnused(gl);
        }
    }

    public static int getVAOCount() {
        return VAO_LIST.size();
    }

    private void storeAttribute(GL gl, VAOAttributeData attribute) {
        gl.getOpenGL().setAttribute(attribute.id, attribute.size,
                attribute.vertexType, attribute.normalized, stride,
                attribute.offset);
    }

    private void addToBuffer(VAOAttribute attribute, int vertices) {
        if (attribute.floatArray == null) {
            switch (attribute.vertexType) {
                case BYTE:
                case UNSIGNED_BYTE:
                    for (int i = 0; i < vertices; i++) {
                        int is = i * attribute.size;
                        buffer.position(attribute.offset + i * stride);
                        for (int j = 0; j < attribute.size; j++) {
                            int ij = is + j;
                            buffer.put(attribute.byteArray[ij]);
                        }
                    }
                    break;
                case SHORT:
                case UNSIGNED_SHORT:
                    for (int i = 0; i < vertices; i++) {
                        int is = i * attribute.size;
                        buffer.position(attribute.offset + i * stride);
                        for (int j = 0; j < attribute.size; j++) {
                            int ij = is + j << 1;
                            buffer.putShort(
                                    (short) (attribute.byteArray[ij + 1] << 8 |
                                            attribute.byteArray[ij]));
                        }
                    }
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Invalid array in vao attribute!");
            }
        } else {
            switch (attribute.vertexType) {
                case FLOAT:
                    for (int i = 0; i < vertices; i++) {
                        int is = i * attribute.size;
                        buffer.position(attribute.offset + i * stride);
                        for (int j = 0; j < attribute.size; j++) {
                            int ij = is + j;
                            buffer.putFloat(attribute.floatArray[ij]);
                        }
                    }
                    break;
                case HALF_FLOAT:
                    for (int i = 0; i < vertices; i++) {
                        int is = i * attribute.size;
                        buffer.position(attribute.offset + i * stride);
                        for (int j = 0; j < attribute.size; j++) {
                            int ij = is + j;
                            buffer.putShort(FastMath.convertFloatToHalf(
                                    attribute.floatArray[ij]));
                        }
                    }
                    break;
                case BYTE:
                    if (attribute.normalized) {
                        for (int i = 0; i < vertices; i++) {
                            int is = i * attribute.size;
                            buffer.position(attribute.offset + i * stride);
                            for (int j = 0; j < attribute.size; j++) {
                                int ij = is + j;
                                buffer.put((byte) FastMath
                                        .round(attribute.floatArray[ij] *
                                                127.0f));
                            }
                        }
                    } else {
                        for (int i = 0; i < vertices; i++) {
                            int is = i * attribute.size;
                            buffer.position(attribute.offset + i * stride);
                            for (int j = 0; j < attribute.size; j++) {
                                int ij = is + j;
                                buffer.put((byte) FastMath
                                        .round(attribute.floatArray[ij]));
                            }
                        }
                    }
                    break;
                case UNSIGNED_BYTE:
                    if (attribute.normalized) {
                        for (int i = 0; i < vertices; i++) {
                            int is = i * attribute.size;
                            buffer.position(attribute.offset + i * stride);
                            for (int j = 0; j < attribute.size; j++) {
                                int ij = is + j;
                                buffer.put((byte) FastMath
                                        .round(attribute.floatArray[ij] *
                                                255.0f));
                            }
                        }
                    } else {
                        for (int i = 0; i < vertices; i++) {
                            int is = i * attribute.size;
                            buffer.position(attribute.offset + i * stride);
                            for (int j = 0; j < attribute.size; j++) {
                                int ij = is + j;
                                buffer.put((byte) FastMath
                                        .round(attribute.floatArray[ij]));
                            }
                        }
                    }
                    break;
                case SHORT:
                    if (attribute.normalized) {
                        for (int i = 0; i < vertices; i++) {
                            int is = i * attribute.size;
                            buffer.position(attribute.offset + i * stride);
                            for (int j = 0; j < attribute.size; j++) {
                                int ij = is + j;
                                buffer.putShort((short) FastMath
                                        .round(attribute.floatArray[ij] *
                                                32768.0f));
                            }
                        }
                    } else {
                        for (int i = 0; i < vertices; i++) {
                            int is = i * attribute.size;
                            buffer.position(attribute.offset + i * stride);
                            for (int j = 0; j < attribute.size; j++) {
                                int ij = is + j;
                                buffer.putShort((short) FastMath
                                        .round(attribute.floatArray[ij]));
                            }
                        }
                    }
                    break;
                case UNSIGNED_SHORT:
                    if (attribute.normalized) {
                        for (int i = 0; i < vertices; i++) {
                            int is = i * attribute.size;
                            buffer.position(attribute.offset + i * stride);
                            for (int j = 0; j < attribute.size; j++) {
                                int ij = is + j;
                                buffer.putShort((short) FastMath
                                        .round(attribute.floatArray[ij] *
                                                65535.0f));
                            }
                        }
                    } else {
                        for (int i = 0; i < vertices; i++) {
                            int is = i * attribute.size;
                            buffer.position(attribute.offset + i * stride);
                            for (int j = 0; j < attribute.size; j++) {
                                int ij = is + j;
                                buffer.putShort((short) FastMath
                                        .round(attribute.floatArray[ij]));
                            }
                        }
                    }
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Invalid array in vao attribute!");
            }
        }
    }

    public void markAsDisposed() {
        markAsDisposed = true;
    }

    @OpenGLFunction
    public void render(GL gl, Shader shader) {
        ensureStored(gl);
        Matrix matrix = gl.getMatrixStack().current();
        OpenGL openGL = gl.getOpenGL();
        openGL.bindVAO(arrayID);
        openGL.activateShader(shader.getProgramID());
        Queue<Shader.Uniform> uniforms = shader.getUniforms();
        while (!uniforms.isEmpty()) {
            uniforms.poll().set(openGL);
        }
        int uniformLocation = shader.getUniformLocation(0);
        if (uniformLocation != -1) {
            openGL.setUniformMatrix4(uniformLocation, false,
                    matrix.getModelViewMatrix().getBuffer());
        }
        uniformLocation = shader.getUniformLocation(1);
        if (uniformLocation != -1) {
            openGL.setUniformMatrix4(uniformLocation, false,
                    gl.getModelViewProjectionMatrix().getBuffer());
        }
        uniformLocation = shader.getUniformLocation(2);
        if (uniformLocation != -1) {
            openGL.setUniformMatrix3(uniformLocation, false,
                    matrix.getNormalMatrix().getBuffer());
        }
        switch (renderType) {
            case TRIANGLES:
                openGL.drawTriangles(length, 0);
                break;
            case LINES:
                openGL.drawLines(length, 0);
                break;
        }
    }

    @OpenGLFunction
    public void ensureStored(GL gl) {
        if (!stored) {
            store(gl);
        }
        used = true;
    }

    private void store(GL gl) {
        buffer.rewind();
        index.rewind();
        OpenGL openGL = gl.getOpenGL();
        arrayID = openGL.createVAO();
        openGL.bindVAO(arrayID);
        vertexID = openGL.createVBO();
        indexID = openGL.createVBO();
        openGL.bindVBOArray(vertexID);
        openGL.bufferVBODataArray(buffer);
        openGL.bindVBOElement(indexID);
        openGL.bufferVBODataElement(index);
        attributes.stream().forEach(attribute -> storeAttribute(gl, attribute));
        VAO_LIST.add(this);
        stored = true;
    }

    public static class VAOAttribute {
        private final VertexType vertexType;
        private final int id, length, size;
        private final boolean normalized;
        private final float[] floatArray;
        private final byte[] byteArray;
        private int offset;

        public VAOAttribute(int id, int size, byte[] array,
                VertexType vertexType) {
            this(id, size, array, array.length, vertexType);
        }

        public VAOAttribute(int id, int size, byte[] array, int length,
                VertexType vertexType) {
            this.id = id;
            this.length = length / vertexType.getBytes();
            this.size = size;
            this.vertexType = vertexType;
            normalized = false;
            byteArray = array;
            floatArray = null;
        }

        public VAOAttribute(int id, int size, float[] array, boolean normalized,
                VertexType vertexType) {
            this(id, size, array, array.length, normalized, vertexType);
        }

        public VAOAttribute(int id, int size, float[] array, int length,
                boolean normalized, VertexType vertexType) {
            this.id = id;
            this.length = length;
            this.size = size;
            this.normalized = normalized;
            this.vertexType = vertexType;
            floatArray = array;
            byteArray = null;
        }
    }

    private static class VAOAttributeData {
        private final VertexType vertexType;
        private final int id, size, offset;
        private final boolean normalized;

        private VAOAttributeData(VAOAttribute attribute, int offset) {
            this.offset = offset;
            vertexType = attribute.vertexType;
            id = attribute.id;
            size = attribute.size;
            normalized = attribute.normalized;
        }
    }
}
