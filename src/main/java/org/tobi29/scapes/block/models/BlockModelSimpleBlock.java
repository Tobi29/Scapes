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

import org.tobi29.scapes.block.BlockType;
import org.tobi29.scapes.block.TerrainTexture;
import org.tobi29.scapes.block.TerrainTextureRegistry;
import org.tobi29.scapes.chunk.data.ChunkMesh;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.engine.opengl.GraphicsSystem;
import org.tobi29.scapes.engine.opengl.Mesh;
import org.tobi29.scapes.engine.opengl.VAO;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.math.Face;

public class BlockModelSimpleBlock implements BlockModel {
    protected final BlockType block;
    protected final float r;
    protected final float g;
    protected final float b;
    protected final float a;
    protected final VAO vao;
    protected final VAO vaoInventory;
    private final TerrainTextureRegistry registry;
    private final TerrainTexture texTop, texBottom, texSide1, texSide2,
            texSide3, texSide4;

    public BlockModelSimpleBlock(BlockType block,
            TerrainTextureRegistry registry, TerrainTexture texTop,
            TerrainTexture texBottom, TerrainTexture texSide1,
            TerrainTexture texSide2, TerrainTexture texSide3,
            TerrainTexture texSide4, float r, float g, float b, float a) {
        this.block = block;
        this.registry = registry;
        this.texTop = texTop;
        this.texBottom = texBottom;
        this.texSide1 = texSide1;
        this.texSide2 = texSide2;
        this.texSide3 = texSide3;
        this.texSide4 = texSide4;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        vao = buildVAO(false);
        vaoInventory = buildVAO(true);
    }

    @Override
    public void addToChunkMesh(ChunkMesh mesh, TerrainClient terrain, int x,
            int y, int z, float xx, float yy, float zz, float r, float g,
            float b, float a, boolean lod) {
        addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz, texTop, texBottom,
                texSide1, texSide2, texSide3, texSide4, r, g, b, a, lod);
    }

    @Override
    public void render(GraphicsSystem graphics, Shader shader) {
        registry.getTexture().bind(graphics);
        vao.render(graphics, shader);
    }

    @Override
    public void renderInventory(GraphicsSystem graphics, Shader shader) {
        registry.getTexture().bind(graphics);
        MatrixStack matrixStack = graphics.getMatrixStack();
        Matrix matrix = matrixStack.push();
        matrix.translate(0.5f, 0.5f, 0.5f);
        matrix.rotate(57.5f, 1, 0, 0);
        matrix.rotate(45, 0, 0, 1);
        vaoInventory.render(graphics, shader);
        matrixStack.pop();
    }

    protected void addToChunkMesh(ChunkMesh mesh, TerrainClient terrain, int x,
            int y, int z, float xx, float yy, float zz, TerrainTexture texTop,
            TerrainTexture texBottom, TerrainTexture texSide1,
            TerrainTexture texSide2, TerrainTexture texSide3,
            TerrainTexture texSide4, float r, float g, float b, float a,
            boolean lod) {
        r *= this.r;
        g *= this.g;
        b *= this.b;
        a *= this.a;
        int connectStage = block.connectStage(terrain, x, y, z);
        int x0 = x - 1;
        int y0 = y - 1;
        int z0 = z - 1;
        int x1 = x + 1;
        int y1 = y + 1;
        int z1 = z + 1;
        if (texTop != null &&
                terrain.getBlockType(x, y, z1).connectStage(terrain, x, y, z1) <
                        connectStage) {
            float xx1 = xx + 1.0f;
            float yy1 = yy + 1.0f;
            float zz1 = zz + 1.0f;
            float terrainTile = texTop.getSize();
            byte anim = texTop.getShaderAnimation().getID();
            mesh.addVertex(terrain, Face.UP, x, y, z1, xx, yy, zz1,
                    texTop.getX(), texTop.getY(), r, g, b, a, lod, anim);
            mesh.addVertex(terrain, Face.UP, x1, y, z1, xx1, yy, zz1,
                    texTop.getX() + terrainTile, texTop.getY(), r, g, b, a, lod,
                    anim);
            mesh.addVertex(terrain, Face.UP, x1, y1, z1, xx1, yy1, zz1,
                    texTop.getX() + terrainTile, texTop.getY() + terrainTile, r,
                    g, b, a, lod, anim);
            mesh.addVertex(terrain, Face.UP, x, y1, z1, xx, yy1, zz1,
                    texTop.getX(), texTop.getY() + terrainTile, r, g, b, a, lod,
                    anim);
        }
        if (texBottom != null &&
                terrain.getBlockType(x, y, z0).connectStage(terrain, x, y, z0) <
                        connectStage) {
            float xx1 = xx + 1.0f;
            float yy1 = yy + 1.0f;
            float terrainTile = texBottom.getSize();
            byte anim = texBottom.getShaderAnimation().getID();
            mesh.addVertex(terrain, Face.DOWN, x, y1, z, xx, yy1, zz,
                    texBottom.getX(), texBottom.getY() + terrainTile, r, g, b,
                    a, lod, anim);
            mesh.addVertex(terrain, Face.DOWN, x1, y1, z, xx1, yy1, zz,
                    texBottom.getX() + terrainTile,
                    texBottom.getY() + terrainTile, r, g, b, a, lod, anim);
            mesh.addVertex(terrain, Face.DOWN, x1, y, z, xx1, yy, zz,
                    texBottom.getX() + terrainTile, texBottom.getY(), r, g, b,
                    a, lod, anim);
            mesh.addVertex(terrain, Face.DOWN, x, y, z, xx, yy, zz,
                    texBottom.getX(), texBottom.getY(), r, g, b, a, lod, anim);
        }
        if (texSide1 != null &&
                terrain.getBlockType(x, y0, z).connectStage(terrain, x, y0, z) <
                        connectStage) {
            float xx1 = xx + 1.0f;
            float zz1 = zz + 1.0f;
            float terrainTile = texSide1.getSize();
            byte anim = texSide1.getShaderAnimation().getID();
            mesh.addVertex(terrain, Face.NORTH, x1, y, z1, xx1, yy, zz1,
                    texSide1.getX() + terrainTile, texSide1.getY(), r, g, b, a,
                    lod, anim);
            mesh.addVertex(terrain, Face.NORTH, x, y, z1, xx, yy, zz1,
                    texSide1.getX(), texSide1.getY(), r, g, b, a, lod, anim);
            mesh.addVertex(terrain, Face.NORTH, x, y, z, xx, yy, zz,
                    texSide1.getX(), texSide1.getY() + terrainTile, r, g, b, a,
                    lod, anim);
            mesh.addVertex(terrain, Face.NORTH, x1, y, z, xx1, yy, zz,
                    texSide1.getX() + terrainTile,
                    texSide1.getY() + terrainTile, r, g, b, a, lod, anim);
        }
        if (texSide2 != null &&
                terrain.getBlockType(x1, y, z).connectStage(terrain, x1, y, z) <
                        connectStage) {
            float xx1 = xx + 1.0f;
            float yy1 = yy + 1.0f;
            float zz1 = zz + 1.0f;
            float terrainTile = texSide2.getSize();
            byte anim = texSide2.getShaderAnimation().getID();
            mesh.addVertex(terrain, Face.EAST, x1, y1, z1, xx1, yy1, zz1,
                    texSide2.getX() + terrainTile, texSide2.getY(), r, g, b, a,
                    lod, anim);
            mesh.addVertex(terrain, Face.EAST, x1, y, z1, xx1, yy, zz1,
                    texSide2.getX(), texSide2.getY(), r, g, b, a, lod, anim);
            mesh.addVertex(terrain, Face.EAST, x1, y, z, xx1, yy, zz,
                    texSide2.getX(), texSide2.getY() + terrainTile, r, g, b, a,
                    lod, anim);
            mesh.addVertex(terrain, Face.EAST, x1, y1, z, xx1, yy1, zz,
                    texSide2.getX() + terrainTile,
                    texSide2.getY() + terrainTile, r, g, b, a, lod, anim);
        }
        if (texSide3 != null &&
                terrain.getBlockType(x, y1, z).connectStage(terrain, x, y1, z) <
                        connectStage) {
            float xx1 = xx + 1.0f;
            float yy1 = yy + 1.0f;
            float zz1 = zz + 1.0f;
            float terrainTile = texSide3.getSize();
            byte anim = texSide3.getShaderAnimation().getID();
            mesh.addVertex(terrain, Face.SOUTH, x1, y1, z, xx1, yy1, zz,
                    texSide3.getX(), texSide3.getY() + terrainTile, r, g, b, a,
                    lod, anim);
            mesh.addVertex(terrain, Face.SOUTH, x, y1, z, xx, yy1, zz,
                    texSide3.getX() + terrainTile,
                    texSide3.getY() + terrainTile, r, g, b, a, lod, anim);
            mesh.addVertex(terrain, Face.SOUTH, x, y1, z1, xx, yy1, zz1,
                    texSide3.getX() + terrainTile, texSide3.getY(), r, g, b, a,
                    lod, anim);
            mesh.addVertex(terrain, Face.SOUTH, x1, y1, z1, xx1, yy1, zz1,
                    texSide3.getX(), texSide3.getY(), r, g, b, a, lod, anim);
        }
        if (texSide4 != null &&
                terrain.getBlockType(x0, y, z).connectStage(terrain, x0, y, z) <
                        connectStage) {
            float yy1 = yy + 1.0f;
            float zz1 = zz + 1.0f;
            float terrainTile = texSide4.getSize();
            byte anim = texSide4.getShaderAnimation().getID();
            mesh.addVertex(terrain, Face.WEST, x, y1, z, xx, yy1, zz,
                    texSide4.getX(), texSide4.getY() + terrainTile, r, g, b, a,
                    lod, anim);
            mesh.addVertex(terrain, Face.WEST, x, y, z, xx, yy, zz,
                    texSide4.getX() + terrainTile,
                    texSide4.getY() + terrainTile, r, g, b, a, lod, anim);
            mesh.addVertex(terrain, Face.WEST, x, y, z1, xx, yy, zz1,
                    texSide4.getX() + terrainTile, texSide4.getY(), r, g, b, a,
                    lod, anim);
            mesh.addVertex(terrain, Face.WEST, x, y1, z1, xx, yy1, zz1,
                    texSide4.getX(), texSide4.getY(), r, g, b, a, lod, anim);
        }
    }

    protected VAO buildVAO(boolean inventory) {
        Mesh mesh = new Mesh(false);
        buildVAO(mesh, inventory, texTop, texBottom, texSide1, texSide2,
                texSide3, texSide4);
        return mesh.finish();
    }

    protected void buildVAO(Mesh mesh, boolean inventory, TerrainTexture texTop,
            TerrainTexture texBottom, TerrainTexture texSide1,
            TerrainTexture texSide2, TerrainTexture texSide3,
            TerrainTexture texSide4) {
        mesh.color(r, g, b, a);
        if (texTop != null) {
            float terrainTile = texTop.getSize();
            mesh.normal(0, 0, 1);
            mesh.texture(texTop.getX(), texTop.getY());
            mesh.vertex(-0.5f, -0.5f, 0.5f);
            mesh.texture(texTop.getX() + terrainTile, texTop.getY());
            mesh.vertex(0.5f, -0.5f, 0.5f);
            mesh.texture(texTop.getX() + terrainTile,
                    texTop.getY() + terrainTile);
            mesh.vertex(0.5f, 0.5f, 0.5f);
            mesh.texture(texTop.getX(), texTop.getY() + terrainTile);
            mesh.vertex(-0.5f, 0.5f, 0.5f);
        }
        if (texSide2 != null) {
            if (inventory) {
                mesh.color(r * 0.7f, g * 0.7f, b * 0.7f, a * 1.0f);
            }
            float terrainTile = texSide2.getSize();
            mesh.normal(1, 0, 0);
            mesh.texture(texSide2.getX(), texSide2.getY());
            mesh.vertex(0.5f, 0.5f, 0.5f);
            mesh.texture(texSide2.getX() + terrainTile, texSide2.getY());
            mesh.vertex(0.5f, -0.5f, 0.5f);
            mesh.texture(texSide2.getX() + terrainTile,
                    texSide2.getY() + terrainTile);
            mesh.vertex(0.5f, -0.5f, -0.5f);
            mesh.texture(texSide2.getX(), texSide2.getY() + terrainTile);
            mesh.vertex(0.5f, 0.5f, -0.5f);
        }
        if (texSide3 != null) {
            if (inventory) {
                mesh.color(r * 0.8f, g * 0.8f, b * 0.8f, a);
            }
            float terrainTile = texSide3.getSize();
            mesh.normal(0, 1, 0);
            mesh.texture(texSide3.getX() + terrainTile,
                    texSide3.getY() + terrainTile);
            mesh.vertex(0.5f, 0.5f, -0.5f);
            mesh.texture(texSide3.getX(), texSide3.getY() + terrainTile);
            mesh.vertex(-0.5f, 0.5f, -0.5f);
            mesh.texture(texSide3.getX(), texSide3.getY());
            mesh.vertex(-0.5f, 0.5f, 0.5f);
            mesh.texture(texSide3.getX() + terrainTile, texSide3.getY());
            mesh.vertex(0.5f, 0.5f, 0.5f);
        }
        if (!inventory) {
            if (texSide4 != null) {
                float terrainTile = texSide4.getSize();
                mesh.normal(-1, 0, 0);
                mesh.texture(texSide4.getX(), texSide4.getY() + terrainTile);
                mesh.vertex(-0.5f, 0.5f, -0.5f);
                mesh.texture(texSide4.getX() + terrainTile,
                        texSide4.getY() + terrainTile);
                mesh.vertex(-0.5f, -0.5f, -0.5f);
                mesh.texture(texSide4.getX() + terrainTile, texSide4.getY());
                mesh.vertex(-0.5f, -0.5f, 0.5f);
                mesh.texture(texSide4.getX(), texSide4.getY());
                mesh.vertex(-0.5f, 0.5f, 0.5f);
            }
            if (texSide1 != null) {
                float terrainTile = texSide1.getSize();
                mesh.normal(0, -1, 0);
                mesh.texture(texSide1.getX(), texSide1.getY());
                mesh.vertex(0.5f, -0.5f, 0.5f);
                mesh.texture(texSide1.getX() + terrainTile, texSide1.getY());
                mesh.vertex(-0.5f, -0.5f, 0.5f);
                mesh.texture(texSide1.getX() + terrainTile,
                        texSide1.getY() + terrainTile);
                mesh.vertex(-0.5f, -0.5f, -0.5f);
                mesh.texture(texSide1.getX(), texSide1.getY() + terrainTile);
                mesh.vertex(0.5f, -0.5f, -0.5f);
            }
            if (texBottom != null) {
                float terrainTile = texBottom.getSize();
                mesh.normal(0, 0, -1);
                mesh.texture(texBottom.getX(), texBottom.getY() + terrainTile);
                mesh.vertex(-0.5f, 0.5f, -0.5f);
                mesh.texture(texBottom.getX() + terrainTile,
                        texBottom.getY() + terrainTile);
                mesh.vertex(0.5f, 0.5f, -0.5f);
                mesh.texture(texBottom.getX() + terrainTile, texBottom.getY());
                mesh.vertex(0.5f, -0.5f, -0.5f);
                mesh.texture(texBottom.getX(), texBottom.getY());
                mesh.vertex(-0.5f, -0.5f, -0.5f);
            }
        }
    }
}
