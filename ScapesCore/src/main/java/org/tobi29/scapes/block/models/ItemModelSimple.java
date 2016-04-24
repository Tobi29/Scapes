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
package org.tobi29.scapes.block.models;

import org.tobi29.scapes.block.TerrainTexture;
import org.tobi29.scapes.block.TerrainTextureRegistry;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.Mesh;
import org.tobi29.scapes.engine.opengl.VAO;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;

public class ItemModelSimple implements ItemModel {
    private final float r, g, b, a;
    private final TerrainTextureRegistry registry;
    private final TerrainTexture texture;
    private final VAO vao, vaoInventory;

    public ItemModelSimple(TerrainTexture texture, float r, float g, float b,
            float a) {
        registry = texture.registry();
        this.texture = texture;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        vao = buildVAO(false);
        vaoInventory = buildVAO(true);
    }

    public VAO buildVAO(boolean inventory) {
        Mesh mesh = new Mesh(false);
        float texMinX = texture.x();
        float texMinY = texture.y();
        float texSize = texture.size();
        float texMaxX = texSize + texMinX;
        float texMaxY = texSize + texMinY;
        mesh.color(r, g, b, a);
        if (inventory) {
            mesh.texture(texMinX, texMinY);
            mesh.vertex(0, 0, 0);
            mesh.texture(texMaxX, texMinY);
            mesh.vertex(1, 0, 0);
            mesh.texture(texMaxX, texMaxY);
            mesh.vertex(1, 1, 0);
            mesh.texture(texMinX, texMaxY);
            mesh.vertex(0, 1, 0);
        } else {
            float pixelCount = texture.resolution();
            float pixel = 1.0f / pixelCount;
            float halfPixel = pixel / 2.0f;
            mesh.normal(0, 1, 0);
            mesh.texture(texMinX, texMinY);
            mesh.vertex(-0.5f, halfPixel, 0.5f);
            mesh.texture(texMaxX, texMinY);
            mesh.vertex(0.5f, halfPixel, 0.5f);
            mesh.texture(texMaxX, texMaxY);
            mesh.vertex(0.5f, halfPixel, -0.5f);
            mesh.texture(texMinX, texMaxY);
            mesh.vertex(-0.5f, halfPixel, -0.5f);
            for (float x = pixelCount - 1; x >= 0; x--) {
                float pos = x / pixelCount;
                float xTex = (pos + 0.5f / pixelCount) * texSize;
                float yTex = xTex + texMinY;
                xTex += texMinX;
                pos -= 0.5f;
                float posInv = pos + pixel;
                float pos2 = -pos;
                float posInv2 = pos2 - pixel;
                mesh.normal(-1, 0, 0);
                mesh.texture(xTex, texMinY);
                mesh.vertex(posInv, halfPixel, 0.5f);
                mesh.texture(xTex, texMinY);
                mesh.vertex(posInv, -halfPixel, 0.5f);
                mesh.texture(xTex, texMaxY);
                mesh.vertex(posInv, -halfPixel, -0.5f);
                mesh.texture(xTex, texMaxY);
                mesh.vertex(posInv, halfPixel, -0.5f);
                //
                mesh.normal(0, 0, -1);
                mesh.texture(texMaxX, yTex);
                mesh.vertex(0.5f, halfPixel, posInv2);
                mesh.texture(texMaxX, yTex);
                mesh.vertex(0.5f, -halfPixel, posInv2);
                mesh.texture(texMinX, yTex);
                mesh.vertex(-0.5f, -halfPixel, posInv2);
                mesh.texture(texMinX, yTex);
                mesh.vertex(-0.5f, halfPixel, posInv2);
                //
                mesh.normal(0, 0, 1);
                mesh.texture(texMaxX, yTex);
                mesh.vertex(0.5f, halfPixel, pos2);
                mesh.texture(texMinX, yTex);
                mesh.vertex(-0.5f, halfPixel, pos2);
                mesh.texture(texMinX, yTex);
                mesh.vertex(-0.5f, -halfPixel, pos2);
                mesh.texture(texMaxX, yTex);
                mesh.vertex(0.5f, -halfPixel, pos2);
                //
                mesh.normal(1, 0, 0);
                mesh.texture(xTex, texMinY);
                mesh.vertex(pos, halfPixel, 0.5f);
                mesh.texture(xTex, texMaxY);
                mesh.vertex(pos, halfPixel, -0.5f);
                mesh.texture(xTex, texMaxY);
                mesh.vertex(pos, -halfPixel, -0.5f);
                mesh.texture(xTex, texMinY);
                mesh.vertex(pos, -halfPixel, 0.5f);
            }
            mesh.normal(0, -1, 0);
            mesh.texture(texMaxX, texMinY);
            mesh.vertex(0.5f, -halfPixel, 0.5f);
            mesh.texture(texMinX, texMinY);
            mesh.vertex(-0.5f, -halfPixel, 0.5f);
            mesh.texture(texMinX, texMaxY);
            mesh.vertex(-0.5f, -halfPixel, -0.5f);
            mesh.texture(texMaxX, texMaxY);
            mesh.vertex(0.5f, -halfPixel, -0.5f);
        }
        return mesh.finish(registry.engine());
    }

    @Override
    public void render(GL gl, Shader shader) {
        texture.registry().texture().bind(gl);
        MatrixStack matrixStack = gl.matrixStack();
        Matrix matrix = matrixStack.push();
        matrix.rotate(315.0f, 0.0f, 1.0f, 0.0f);
        vao.render(gl, shader);
        matrixStack.pop();
    }

    @Override
    public void renderInventory(GL gl, Shader shader) {
        texture.registry().texture().bind(gl);
        vaoInventory.render(gl, shader);
    }
}
