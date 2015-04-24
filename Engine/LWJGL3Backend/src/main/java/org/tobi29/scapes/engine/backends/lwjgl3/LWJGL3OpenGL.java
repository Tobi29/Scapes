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

package org.tobi29.scapes.engine.backends.lwjgl3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tobi29.scapes.engine.ScapesEngine;
import org.tobi29.scapes.engine.opengl.*;
import org.tobi29.scapes.engine.opengl.texture.TextureFilter;
import org.tobi29.scapes.engine.opengl.texture.TextureWrap;
import org.tobi29.scapes.engine.utils.BufferCreatorDirect;
import org.tobi29.scapes.engine.utils.graphics.Image;
import org.tobi29.scapes.engine.utils.graphics.PNG;
import org.tobi29.scapes.engine.utils.io.filesystem.File;
import org.tobi29.scapes.engine.utils.math.FastMath;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class LWJGL3OpenGL implements OpenGL {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(LWJGL3OpenGL.class);
    protected final int[] lastTextureBind = new int[32];
    private final ScapesEngine engine;
    private final IntBuffer attachBuffer = BufferCreatorDirect.intBuffer(16);
    protected int activeTexture, activeShader;

    public LWJGL3OpenGL(ScapesEngine engine) {
        this.engine = engine;
    }
    // Basic

    @Override
    public void checkError(String message) {
        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) {
            String errorName;
            switch (error) {
                case GL11.GL_INVALID_ENUM:
                    errorName = "Enum argument out of range";
                    break;
                case GL11.GL_INVALID_VALUE:
                    errorName = "Numeric argument out of range";
                    break;
                case GL11.GL_INVALID_OPERATION:
                    errorName = "Operation illegal in current state";
                    break;
                case GL11.GL_STACK_OVERFLOW:
                    errorName = "Command would cause a stack overflow";
                    break;
                case GL11.GL_STACK_UNDERFLOW:
                    errorName = "Command would cause a stack underflow";
                    break;
                case GL11.GL_OUT_OF_MEMORY:
                    errorName = "Not enough memory left to execute command";
                    break;
                case GL30.GL_INVALID_FRAMEBUFFER_OPERATION:
                    errorName = "Framebuffer object is not complete";
                    break;
                case ARBImaging.GL_TABLE_TOO_LARGE:
                    errorName = "The specified table is too large";
                    break;
                default:
                    errorName = "Unknown error code";
                    break;
            }
            throw new GraphicsException(errorName + " in " +
                    message);
        }
    }

    @Override
    public void clear(float r, float g, float b, float a) {
        GL11.glClearColor(r, g, b, a);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    @Override
    public void clearDepth() {
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
    }

    @Override
    public void disableCulling() {
        GL11.glDisable(GL11.GL_CULL_FACE);
    }

    @Override
    public void disableDepthTest() {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    @Override
    public void disableDepthMask() {
        GL11.glDepthMask(false);
    }

    @Override
    public void disableWireframe() {
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
    }

    @Override
    public void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    @Override
    public void enableCulling() {
        GL11.glEnable(GL11.GL_CULL_FACE);
    }

    @Override
    public void enableDepthTest() {
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
    }

    @Override
    public void enableDepthMask() {
        GL11.glDepthMask(true);
    }

    @Override
    public void enableWireframe() {
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
    }

    @Override
    public void enableScissor(int x, int y, int width, int height) {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) ((double) x / 800 *
                        engine.getGraphics().getContentWidth()),
                (int) ((double) (512 - y - height) / 512 *
                        engine.getGraphics().getContentHeight()) + 1,
                (int) ((double) width / 800 *
                        engine.getGraphics().getContentWidth()),
                (int) ((double) height / 512 *
                        engine.getGraphics().getContentHeight()));
    }

    @Override
    public void setBlending(BlendingMode mode) {
        switch (mode) {
            case NONE:
                GL11.glDisable(GL11.GL_BLEND);
                break;
            case NORMAL:
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA,
                        GL11.GL_ONE_MINUS_SRC_ALPHA);
                break;
            case ADD:
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_DST_ALPHA);
                break;
            case INVERT:
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_ONE_MINUS_DST_COLOR,
                        GL11.GL_ONE_MINUS_SRC_COLOR);
                break;
        }
    }
    // FBO

    @Override
    public void viewport(int x, int y, int width, int height) {
        GL11.glViewport(x, y, width, height);
    }

    @Override
    public void drawbuffersFBO(int attachments) {
        if (attachments < 0 || attachments > 15) {
            throw new IllegalArgumentException(
                    "Attachments must be 0-15, was " + attachments);
        }
        attachBuffer.limit(attachments);
        for (int i = 0; i < attachments; i++) {
            attachBuffer.put(GL30.GL_COLOR_ATTACHMENT0 + i);
        }
        attachBuffer.rewind();
        GL20.glDrawBuffers(attachBuffer);
    }

    @Override
    public int createFBO() {
        return GL30.glGenFramebuffers();
    }

    @Override
    public void deleteFBO(int id) {
        GL30.glDeleteFramebuffers(id);
    }

    @Override
    public void bindFBO(int id) {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, id);
    }
    // Screenshot

    @Override
    public void attachColor(int texture, int i) {
        if (i < 0 || i > 31) {
            throw new IllegalArgumentException(
                    "Color Attachment must be 0-31, was " + i);
        }
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER,
                GL30.GL_COLOR_ATTACHMENT0 + i, GL11.GL_TEXTURE_2D, texture, 0);
    }

    @Override
    public void attachDepth(int texture) {
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER,
                GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, texture, 0);
    }

    @Override
    public FBOStatus checkFBO() {
        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        switch (status) {
            case GL30.GL_FRAMEBUFFER_COMPLETE:
                return FBOStatus.COMPLETE;
            case GL30.GL_FRAMEBUFFER_UNSUPPORTED:
                return FBOStatus.UNSUPPORTED;
            default:
                return FBOStatus.UNKNOWN;
        }
    }

    @Override
    public void screenShot(File file, GraphicsSystem graphics) {
        int width = graphics.getSceneWidth(), height =
                graphics.getSceneHeight();
        GL11.glReadBuffer(GL11.GL_FRONT);
        ByteBuffer buffer = BufferCreatorDirect.byteBuffer(width * height * 4);
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE, buffer);
        graphics.getEngine().getTaskExecutor().runTask(joiner -> {
            Image image = new Image(width, height, buffer);
            try {
                file.write(streamOut -> PNG.encode(image, streamOut, 9, false));
            } catch (IOException e) {
                LOGGER.error("Error saving screenshot: {}", e.toString());
            }
        }, "Saving-Screenshot");
    }
    // Shader

    @Override
    public Image screenShot(int width, int height) {
        GL11.glReadBuffer(GL11.GL_FRONT);
        ByteBuffer buffer = BufferCreatorDirect.byteBuffer(width * height * 4);
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE, buffer);
        return new Image(width, height, buffer);
    }

    @Override
    public Image screenShotFBO(GraphicsSystem graphics, FBO fbo,
            int colorAttachment) {
        graphics.getTextureManager()
                .bind(fbo.getTexturesColor()[colorAttachment], graphics);
        ByteBuffer buffer = BufferCreatorDirect
                .byteBuffer(fbo.getWidth() * fbo.getHeight() << 2);
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE, buffer);
        return new Image(fbo.getWidth(), fbo.getHeight(), buffer);
    }

    @Override
    public void activateShader(int id) {
        if (activeShader != id) {
            GL20.glUseProgram(id);
            activeShader = id;
        }
    }

    @Override
    public int createProgram() {
        return GL20.glCreateProgram();
    }

    @Override
    public int createFragmentObject() {
        return GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
    }

    @Override
    public int createVertexObject() {
        return GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
    }

    @Override
    public void deleteProgram(int id) {
        if (activeShader == id) {
            activeShader = 0;
        }
        GL20.glDeleteProgram(id);
    }

    @Override
    public void deleteShader(int id) {
        GL20.glDeleteShader(id);
    }

    @Override
    public void attach(int id, int object) {
        GL20.glAttachShader(id, object);
    }

    @Override
    public void link(int id) {
        GL20.glLinkProgram(id);
    }

    @Override
    public boolean checkLinkStatus(int id) {
        return GL20.glGetProgrami(id, GL20.GL_LINK_STATUS) == GL11.GL_TRUE;
    }

    @Override
    public void source(int id, String code) {
        GL20.glShaderSource(id, code);
    }

    @Override
    public void compile(int id) {
        GL20.glCompileShader(id);
    }

    @Override
    public void printLogShader(int id) {
        int l = GL20.glGetShaderi(id, GL20.GL_INFO_LOG_LENGTH);
        if (l > 1) {
            IntBuffer length = BufferCreatorDirect.intBuffer(1);
            length.put(l);
            length.rewind();
            ByteBuffer log = BufferCreatorDirect.byteBuffer(l);
            GL20.glGetShaderInfoLog(id, length, log);
            byte[] infoBytes = new byte[l];
            log.get(infoBytes);
            String out = new String(infoBytes);
            LOGGER.info("Shader log: {}", out);
        }
    }

    @Override
    public void printLogProgram(int id) {
        int l = GL20.glGetProgrami(id, GL20.GL_INFO_LOG_LENGTH);
        if (l > 1) {
            IntBuffer length = BufferCreatorDirect.intBuffer(1);
            length.put(l);
            length.rewind();
            ByteBuffer log = BufferCreatorDirect.byteBuffer(l);
            GL20.glGetProgramInfoLog(id, length, log);
            byte[] infoBytes = new byte[l];
            log.get(infoBytes);
            String out = new String(infoBytes);
            LOGGER.info("Shader log: {}", out);
        }
    }

    @Override
    public void bindAttributeLocation(int shader, int id, String name) {
        GL20.glBindAttribLocation(shader, id, name);
    }

    @Override
    public void bindFragmentLocation(int shader, int id, String name) {
        GL30.glBindFragDataLocation(shader, id, name);
    }

    @Override
    public int getUniformLocation(int program, String uniform) {
        return GL20.glGetUniformLocation(program, uniform);
    }

    @Override
    public void setUniform1f(int uniform, float v0) {
        GL20.glUniform1f(uniform, v0);
    }

    @Override
    public void setUniform2f(int uniform, float v0, float v1) {
        GL20.glUniform2f(uniform, v0, v1);
    }

    @Override
    public void setUniform3f(int uniform, float v0, float v1, float v2) {
        GL20.glUniform3f(uniform, v0, v1, v2);
    }

    @Override
    public void setUniform4f(int uniform, float v0, float v1, float v2,
            float v3) {
        GL20.glUniform4f(uniform, v0, v1, v2, v3);
    }

    @Override
    public void setUniform1i(int uniform, int v0) {
        GL20.glUniform1i(uniform, v0);
    }

    @Override
    public void setUniform2i(int uniform, int v0, int v1) {
        GL20.glUniform2i(uniform, v0, v1);
    }

    @Override
    public void setUniform3i(int uniform, int v0, int v1, int v2) {
        GL20.glUniform3i(uniform, v0, v1, v2);
    }

    @Override
    public void setUniform4i(int uniform, int v0, int v1, int v2, int v3) {
        GL20.glUniform4i(uniform, v0, v1, v2, v3);
    }

    @Override
    public void setUniform1(int uniform, FloatBuffer values) {
        GL20.glUniform1fv(uniform, values);
    }

    @Override
    public void setUniform2(int uniform, FloatBuffer values) {
        GL20.glUniform2fv(uniform, values);
    }

    @Override
    public void setUniform3(int uniform, FloatBuffer values) {
        GL20.glUniform3fv(uniform, values);
    }

    @Override
    public void setUniform4(int uniform, FloatBuffer values) {
        GL20.glUniform4fv(uniform, values);
    }

    @Override
    public void setUniform1(int uniform, IntBuffer values) {
        GL20.glUniform1iv(uniform, values);
    }

    @Override
    public void setUniform2(int uniform, IntBuffer values) {
        GL20.glUniform2iv(uniform, values);
    }

    @Override
    public void setUniform3(int uniform, IntBuffer values) {
        GL20.glUniform3iv(uniform, values);
    }

    @Override
    public void setUniform4(int uniform, IntBuffer values) {
        GL20.glUniform4iv(uniform, values);
    }

    @Override
    public void setUniformMatrix2(int uniform, boolean transpose,
            FloatBuffer matrices) {
        GL20.glUniformMatrix2fv(uniform, transpose, matrices);
    }

    @Override
    public void setUniformMatrix3(int uniform, boolean transpose,
            FloatBuffer matrices) {
        GL20.glUniformMatrix3fv(uniform, transpose, matrices);
    }

    @Override
    public void setUniformMatrix4(int uniform, boolean transpose,
            FloatBuffer matrices) {
        GL20.glUniformMatrix4fv(uniform, transpose, matrices);
    }

    @Override
    public void setAttribute1f(int id, float v0) {
        GL20.glVertexAttrib1f(id, v0);
    }

    @Override
    public void setAttribute2f(int id, float v0, float v1) {
        GL20.glVertexAttrib2f(id, v0, v1);
    }

    @Override
    public void setAttribute3f(int id, float v0, float v1, float v2) {
        GL20.glVertexAttrib3f(id, v0, v1, v2);
    }

    @Override
    public void setAttribute4f(int id, float v0, float v1, float v2, float v3) {
        GL20.glVertexAttrib4f(id, v0, v1, v2, v3);
    }
    // Texture

    @Override
    public void bindTexture(int id) {
        if (id != lastTextureBind[activeTexture]) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
            lastTextureBind[activeTexture] = id;
        }
    }

    @Override
    public void bufferTextureMipMap(int width, int height,
            ByteBuffer... buffers) {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL,
                buffers.length - 1);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffers[0]);
        for (int i = 1; i < buffers.length; i++) {
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, i, GL11.GL_RGBA,
                    FastMath.max(width >> i, 1), FastMath.max(height >> i, 1),
                    0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffers[i]);
        }
    }

    @Override
    public void bufferTexture(int width, int height, boolean alpha,
            ByteBuffer buffer) {
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0,
                alpha ? GL11.GL_RGBA : GL11.GL_RGB, width, height, 0,
                alpha ? GL11.GL_RGBA : GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE,
                buffer);
    }

    @Override
    public void bufferTextureFloat(int width, int height, boolean alpha,
            ByteBuffer buffer) {
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0,
                alpha ? GL30.GL_RGBA16F : GL30.GL_RGB16F, width, height, 0,
                alpha ? GL11.GL_RGBA : GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE,
                buffer);
    }

    @Override
    public void bufferTextureDepth(int width, int height, ByteBuffer buffer) {
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT24,
                width, height, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_UNSIGNED_INT,
                buffer);
    }

    @Override
    public int createTexture() {
        return GL11.glGenTextures();
    }

    @Override
    public void deleteTexture(int id) {
        if (lastTextureBind[activeTexture] == id) {
            bindTexture(0);
        }
        GL11.glDeleteTextures(id);
    }

    @Override
    public void magFilter(TextureFilter filter) {
        switch (filter) {
            case NEAREST:
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
                        GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                break;
            case LINEAR:
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
                        GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                break;
            default:
                throw new IllegalArgumentException("Illegal texture-filter!");
        }
    }

    @Override
    public void minFilter(TextureFilter filter, boolean mipmap) {
        if (mipmap) {
            switch (filter) {
                case NEAREST:
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
                            GL11.GL_TEXTURE_MIN_FILTER,
                            GL11.GL_NEAREST_MIPMAP_LINEAR);
                    break;
                case LINEAR:
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
                            GL11.GL_TEXTURE_MIN_FILTER,
                            GL11.GL_LINEAR_MIPMAP_LINEAR);
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Illegal texture-filter!");
            }
        } else {
            switch (filter) {
                case NEAREST:
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
                            GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                    break;
                case LINEAR:
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
                            GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Illegal texture-filter!");
            }
        }
    }

    @Override
    public void replaceTexture(int x, int y, int width, int height,
            ByteBuffer buffer) {
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, x, y, width, height,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
    }

    @Override
    public void replaceTextureMipMap(int x, int y, int width, int height,
            ByteBuffer... buffers) {
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, x, y, width, height,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffers[0]);
        for (int i = 1; i < buffers.length; i++) {
            int scale = (int) FastMath.pow(2, i);
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, i, x / scale, y / scale,
                    FastMath.max(width / scale, 1),
                    FastMath.max(height / scale, 1), GL11.GL_RGBA,
                    GL11.GL_UNSIGNED_BYTE, buffers[i]);
        }
    }

    @Override
    public void getImage(ByteBuffer buffer) {
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE, buffer);
    }

    @Override
    public void getDepth(FloatBuffer buffer) {
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_DEPTH_COMPONENT,
                GL11.GL_FLOAT, buffer);
    }

    @Override
    public void wrapS(TextureWrap wrap) {
        switch (wrap) {
            case REPEAT:
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S,
                        GL11.GL_REPEAT);
                break;
            case CLAMP:
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S,
                        GL12.GL_CLAMP_TO_EDGE);
                break;
            default:
                throw new IllegalArgumentException("Illegal texture-wrap!");
        }
    }

    @Override
    public void wrapT(TextureWrap wrap) {
        switch (wrap) {
            case REPEAT:
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T,
                        GL11.GL_REPEAT);
                break;
            case CLAMP:
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T,
                        GL12.GL_CLAMP_TO_EDGE);
                break;
            default:
                throw new IllegalArgumentException("Illegal texture-wrap!");
        }
    }

    @Override
    public void activeTexture(int i) {
        if (i < 0 || i > 31) {
            throw new IllegalArgumentException(
                    "Active Texture must be 0-31, was " + i);
        }
        if (activeTexture != i) {
            activeTexture = i;
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
        }
    }
    // VAO

    @Override
    public void bindVAO(int id) {
        GL30.glBindVertexArray(id);
    }

    @Override
    public int createVAO() {
        return GL30.glGenVertexArrays();
    }

    @Override
    public void deleteVAO(int id) {
        GL30.glDeleteVertexArrays(id);
    }

    @Override
    public void setAttribute(int id, int size, VertexType vertexType,
            boolean normalized, int stride, int offset) {
        GL20.glEnableVertexAttribArray(id);
        switch (vertexType) {
            case FLOAT:
                GL20.glVertexAttribPointer(id, size, GL11.GL_FLOAT, normalized,
                        stride, offset);
                break;
            case HALF_FLOAT:
                GL20.glVertexAttribPointer(id, size, GL30.GL_HALF_FLOAT,
                        normalized, stride, offset);
                break;
            case BYTE:
                GL20.glVertexAttribPointer(id, size, GL11.GL_BYTE, normalized,
                        stride, offset);
                break;
            case UNSIGNED_BYTE:
                GL20.glVertexAttribPointer(id, size, GL11.GL_UNSIGNED_BYTE,
                        normalized, stride, offset);
                break;
            case SHORT:
                GL20.glVertexAttribPointer(id, size, GL11.GL_SHORT, normalized,
                        stride, offset);
                break;
            case UNSIGNED_SHORT:
                GL20.glVertexAttribPointer(id, size, GL11.GL_UNSIGNED_SHORT,
                        normalized, stride, offset);
                break;
            default:
                throw new IllegalArgumentException("Unknown vertex type!");
        }
    }

    @Override
    public void disableAttribute(int id) {
        GL20.glDisableVertexAttribArray(id);
    }
    // VBO

    @Override
    public void bindVBOArray(int id) {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, id);
    }

    @Override
    public void bindVBOElement(int id) {
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, id);
    }

    @Override
    public void bufferVBODataArray(ByteBuffer buffer) {
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
    }

    @Override
    public void bufferVBODataElement(ByteBuffer buffer) {
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, buffer,
                GL15.GL_STATIC_DRAW);
    }

    @Override
    public int createVBO() {
        return GL15.glGenBuffers();
    }

    @Override
    public void deleteVBO(int id) {
        GL15.glDeleteBuffers(id);
    }

    @Override
    public void drawTriangles(int length, long offset) {
        GL11.glDrawElements(GL11.GL_TRIANGLES, length, GL11.GL_UNSIGNED_SHORT,
                offset);
    }

    @Override
    public void drawLines(int length, long offset) {
        GL11.glDrawElements(GL11.GL_LINES, length, GL11.GL_UNSIGNED_SHORT,
                offset);
    }
}
