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
import org.tobi29.scapes.chunk.terrain.Terrain;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.Mesh;
import org.tobi29.scapes.engine.opengl.VAO;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.engine.utils.math.FastMath;

public class BlockModelLiquid implements BlockModel {
    protected final BlockType block;
    protected final float r;
    protected final float g;
    protected final float b;
    protected final float a;
    protected final float min;
    protected final float diff;
    private final TerrainTextureRegistry registry;
    private final TerrainTexture texTop, texBottom, texSide1, texSide2,
            texSide3, texSide4;
    private final VAO vao, vaoInventory;

    public BlockModelLiquid(BlockType block, TerrainTextureRegistry registry,
            TerrainTexture texTop, TerrainTexture texBottom,
            TerrainTexture texSide1, TerrainTexture texSide2,
            TerrainTexture texSide3, TerrainTexture texSide4, float r, float g,
            float b, float a, float min, float max) {
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
        this.min = min;
        diff = max - min;
        vao = buildVAO(false);
        vaoInventory = buildVAO(true);
    }

    protected static float calcHeight(int x, int y, int z, Terrain terrain,
            BlockType block) {
        if (terrain.type(x, y, z + 1) == block ||
                terrain.type(x - 1, y, z + 1) == block ||
                terrain.type(x - 1, y - 1, z + 1) == block ||
                terrain.type(x, y - 1, z + 1) == block) {
            return 1.0f;
        }
        float height = 0;
        int heights = 0;
        if (terrain.type(x, y, z) == block) {
            height += 1 - FastMath.max(0, terrain.data(x, y, z) - 1) / 7.0f;
            heights++;
        }
        if (terrain.type(x - 1, y, z) == block) {
            height += 1 - FastMath.max(0, terrain.data(x - 1, y, z) - 1) / 7.0f;
            heights++;
        }
        if (terrain.type(x - 1, y - 1, z) == block) {
            height += 1 -
                    FastMath.max(0, terrain.data(x - 1, y - 1, z) - 1) / 7.0f;
            heights++;
        }
        if (terrain.type(x, y - 1, z) == block) {
            height += 1 - FastMath.max(0, terrain.data(x, y - 1, z) - 1) / 7.0f;
            heights++;
        }
        if (heights == 0) {
            return 0.0f;
        }
        return height / heights;
    }

    @Override
    public void addToChunkMesh(ChunkMesh mesh, TerrainClient terrain, int x,
            int y, int z, float xx, float yy, float zz, float r, float g,
            float b, float a, boolean lod) {
        r *= this.r;
        g *= this.g;
        b *= this.b;
        a *= this.a;
        int connectStage = block.connectStage(terrain, x, y, z);
        BlockType top = terrain.type(x, y, z + 1);
        float height00, height01, height11, height10;
        boolean flag = top != block;
        if (flag) {
            height00 = calcHeight(x, y, z, terrain, block) * diff + min;
            height01 = calcHeight(x, y + 1, z, terrain, block) * diff + min;
            height11 = calcHeight(x + 1, y + 1, z, terrain, block) * diff + min;
            height10 = calcHeight(x + 1, y, z, terrain, block) * diff + min;
            if (height00 >= 1.0f && height01 >= 1.0f && height11 >= 1.0f &&
                    height10 >= 1.0f) {
                flag = top.connectStage(terrain, x, y, z + 1) < connectStage;
            }
            if (flag) {
                if (texTop != null) {
                    float terrainTile = texTop.size();
                    byte anim = texTop.shaderAnimation().id();
                    mesh.addVertex(terrain, Face.UP, x, y, z + height00, xx, yy,
                            zz + height00, texTop.x(), texTop.y(), r, g, b, a,
                            lod, anim);
                    mesh.addVertex(terrain, Face.UP, x + 1, y, z + height10,
                            xx + 1, yy, zz + height10, texTop.x() + terrainTile,
                            texTop.y(), r, g, b, a, lod, anim);
                    mesh.addVertex(terrain, Face.UP, x + 1, y + 1, z + height11,
                            xx + 1, yy + 1, zz + height11,
                            texTop.x() + terrainTile, texTop.y() + terrainTile,
                            r, g, b, a, lod, anim);
                    mesh.addVertex(terrain, Face.UP, x, y + 1, z + height01, xx,
                            yy + 1, zz + height01, texTop.x(),
                            texTop.y() + terrainTile, r, g, b, a, lod, anim);
                }
            }
        } else {
            height00 = diff + min;
            height10 = height00;
            height11 = height00;
            height01 = height00;
        }
        BlockType other;
        other = terrain.type(x, y, z - 1);
        if (other != block &&
                other.connectStage(terrain, x, y, z - 1) <= connectStage) {
            if (texBottom != null) {
                float terrainTile = texBottom.size();
                byte anim = texBottom.shaderAnimation().id();
                mesh.addVertex(terrain, Face.DOWN, x, y + 1, z, xx, yy + 1, zz,
                        texBottom.x(), texBottom.y() + terrainTile, r, g, b, a,
                        lod, anim);
                mesh.addVertex(terrain, Face.DOWN, x + 1, y + 1, z, xx + 1,
                        yy + 1, zz, texBottom.x() + terrainTile,
                        texBottom.y() + terrainTile, r, g, b, a, lod, anim);
                mesh.addVertex(terrain, Face.DOWN, x + 1, y, z, xx + 1, yy, zz,
                        texBottom.x() + terrainTile, texBottom.y(), r, g, b, a,
                        lod, anim);
                mesh.addVertex(terrain, Face.DOWN, x, y, z, xx, yy, zz,
                        texBottom.x(), texBottom.y(), r, g, b, a, lod, anim);
            }
        }
        other = terrain.type(x, y - 1, z);
        if (other != block &&
                other.connectStage(terrain, x, y - 1, z) <= connectStage) {
            if (texSide1 != null) {
                float terrainTile = texSide1.size();
                byte anim = texSide1.shaderAnimation().id();
                float textureHeight00 =
                        FastMath.max(1.0f - height00, 0.0f) * terrainTile;
                float textureHeight10 =
                        FastMath.max(1.0f - height10, 0.0f) * terrainTile;
                mesh.addVertex(terrain, Face.NORTH, x + 1, y, z + height10,
                        xx + 1, yy, zz + height10, texSide1.x() + terrainTile,
                        texSide1.y() + textureHeight10, r, g, b, a, lod, anim);
                mesh.addVertex(terrain, Face.NORTH, x, y, z + height00, xx, yy,
                        zz + height00, texSide1.x(),
                        texSide1.y() + textureHeight00, r, g, b, a, lod, anim);
                mesh.addVertex(terrain, Face.NORTH, x, y, z, xx, yy, zz,
                        texSide1.x(), texSide1.y() + terrainTile, r, g, b, a,
                        lod, anim);
                mesh.addVertex(terrain, Face.NORTH, x + 1, y, z, xx + 1, yy, zz,
                        texSide1.x() + terrainTile, texSide1.y() + terrainTile,
                        r, g, b, a, lod, anim);
            }
        }
        other = terrain.type(x + 1, y, z);
        if (other != block &&
                other.connectStage(terrain, x + 1, y, z) <= connectStage) {
            if (texSide2 != null) {
                float terrainTile = texSide2.size();
                byte anim = texSide2.shaderAnimation().id();
                float textureHeight10 =
                        FastMath.max(1.0f - height10, 0.0f) * terrainTile;
                float textureHeight11 =
                        FastMath.max(1.0f - height11, 0.0f) * terrainTile;
                mesh.addVertex(terrain, Face.EAST, x + 1, y + 1, z + height11,
                        xx + 1, yy + 1, zz + height11,
                        texSide2.x() + terrainTile,
                        texSide2.y() + textureHeight11, r, g, b, a, lod, anim);
                mesh.addVertex(terrain, Face.EAST, x + 1, y, z + height10,
                        xx + 1, yy, zz + height10, texSide2.x(),
                        texSide2.y() + textureHeight10, r, g, b, a, lod, anim);
                mesh.addVertex(terrain, Face.EAST, x + 1, y, z, xx + 1, yy, zz,
                        texSide2.x(), texSide2.y() + terrainTile, r, g, b, a,
                        lod, anim);
                mesh.addVertex(terrain, Face.EAST, x + 1, y + 1, z, xx + 1,
                        yy + 1, zz, texSide2.x() + terrainTile,
                        texSide2.y() + terrainTile, r, g, b, a, lod, anim);
            }
        }
        other = terrain.type(x, y + 1, z);
        if (other != block &&
                other.connectStage(terrain, x, y + 1, z) <= connectStage) {
            if (texSide3 != null) {
                float terrainTile = texSide3.size();
                byte anim = texSide3.shaderAnimation().id();
                float textureHeight01 =
                        FastMath.max(1.0f - height01, 0.0f) * terrainTile;
                float textureHeight11 =
                        FastMath.max(1.0f - height11, 0.0f) * terrainTile;
                mesh.addVertex(terrain, Face.SOUTH, x + 1, y + 1, z, xx + 1,
                        yy + 1, zz, texSide3.x(), texSide3.y() + terrainTile, r,
                        g, b, a, lod, anim);
                mesh.addVertex(terrain, Face.SOUTH, x, y + 1, z, xx, yy + 1, zz,
                        texSide3.x() + terrainTile, texSide3.y() + terrainTile,
                        r, g, b, a, lod, anim);
                mesh.addVertex(terrain, Face.SOUTH, x, y + 1, z + height01, xx,
                        yy + 1, zz + height01, texSide3.x() + terrainTile,
                        texSide3.y() + textureHeight01, r, g, b, a, lod, anim);
                mesh.addVertex(terrain, Face.SOUTH, x + 1, y + 1, z + height11,
                        xx + 1, yy + 1, zz + height11, texSide3.x(),
                        texSide3.y() + textureHeight11, r, g, b, a, lod, anim);
            }
        }
        other = terrain.type(x - 1, y, z);
        if (other != block &&
                other.connectStage(terrain, x - 1, y, z) <= connectStage) {
            if (texSide4 != null) {
                float terrainTile = texSide4.size();
                byte anim = texSide4.shaderAnimation().id();
                float textureHeight00 =
                        FastMath.max(1.0f - height00, 0.0f) * terrainTile;
                float textureHeight01 =
                        FastMath.max(1.0f - height01, 0.0f) * terrainTile;
                mesh.addVertex(terrain, Face.WEST, x, y + 1, z, xx, yy + 1, zz,
                        texSide4.x(), texSide4.y() + terrainTile, r, g, b, a,
                        lod, anim);
                mesh.addVertex(terrain, Face.WEST, x, y, z, xx, yy, zz,
                        texSide4.x() + terrainTile, texSide4.y() + terrainTile,
                        r, g, b, a, lod, anim);
                mesh.addVertex(terrain, Face.WEST, x, y, z + height00, xx, yy,
                        zz + height00, texSide4.x() + terrainTile,
                        texSide4.y() + textureHeight00, r, g, b, a, lod, anim);
                mesh.addVertex(terrain, Face.WEST, x, y + 1, z + height01, xx,
                        yy + 1, zz + height01, texSide4.x(),
                        texSide4.y() + textureHeight01, r, g, b, a, lod, anim);
            }
        }
    }

    @Override
    public void render(GL gl, Shader shader) {
        registry.texture().bind(gl);
        vao.render(gl, shader);
    }

    @Override
    public void renderInventory(GL gl, Shader shader) {
        registry.texture().bind(gl);
        MatrixStack matrixStack = gl.matrixStack();
        Matrix matrix = matrixStack.push();
        matrix.translate(0.5f, 0.5f, 0.5f);
        matrix.rotate(57.5f, 1, 0, 0);
        matrix.rotate(45, 0, 0, 1);
        vaoInventory.render(gl, shader);
        matrixStack.pop();
    }

    protected VAO buildVAO(boolean inventory) {
        Mesh mesh = new Mesh(false);
        buildVAO(mesh, inventory);
        return mesh.finish(registry.engine());
    }

    protected void buildVAO(Mesh mesh, boolean inventory) {
        mesh.color(r, g, b, a);
        if (texTop != null) {
            float terrainTile = texTop.size();
            mesh.normal(0, 0, 1);
            mesh.texture(texTop.x(), texTop.y());
            mesh.vertex(-0.5f, -0.5f, 0.5f);
            mesh.texture(texTop.x() + terrainTile, texTop.y());
            mesh.vertex(0.5f, -0.5f, 0.5f);
            mesh.texture(texTop.x() + terrainTile, texTop.y() + terrainTile);
            mesh.vertex(0.5f, 0.5f, 0.5f);
            mesh.texture(texTop.x(), texTop.y() + terrainTile);
            mesh.vertex(-0.5f, 0.5f, 0.5f);
        }
        if (texSide2 != null) {
            float terrainTile = texSide2.size();
            mesh.normal(1, 0, 0);
            mesh.texture(texSide2.x(), texSide2.y());
            mesh.vertex(0.5f, 0.5f, 0.5f);
            mesh.texture(texSide2.x() + terrainTile, texSide2.y());
            mesh.vertex(0.5f, -0.5f, 0.5f);
            mesh.texture(texSide2.x() + terrainTile,
                    texSide2.y() + terrainTile);
            mesh.vertex(0.5f, -0.5f, -0.5f);
            mesh.texture(texSide2.x(), texSide2.y() + terrainTile);
            mesh.vertex(0.5f, 0.5f, -0.5f);
        }
        if (texSide3 != null) {
            float terrainTile = texSide3.size();
            mesh.normal(0, 1, 0);
            mesh.texture(texSide3.x() + terrainTile,
                    texSide3.y() + terrainTile);
            mesh.vertex(0.5f, 0.5f, -0.5f);
            mesh.texture(texSide3.x(), texSide3.y() + terrainTile);
            mesh.vertex(-0.5f, 0.5f, -0.5f);
            mesh.texture(texSide3.x(), texSide3.y());
            mesh.vertex(-0.5f, 0.5f, 0.5f);
            mesh.texture(texSide3.x() + terrainTile, texSide3.y());
            mesh.vertex(0.5f, 0.5f, 0.5f);
        }
        if (!inventory) {
            if (texSide4 != null) {
                float terrainTile = texSide4.size();
                mesh.normal(-1, 0, 0);
                mesh.texture(texSide4.x(), texSide4.y() + terrainTile);
                mesh.vertex(-0.5f, 0.5f, -0.5f);
                mesh.texture(texSide4.x() + terrainTile,
                        texSide4.y() + terrainTile);
                mesh.vertex(-0.5f, -0.5f, -0.5f);
                mesh.texture(texSide4.x() + terrainTile, texSide4.y());
                mesh.vertex(-0.5f, -0.5f, 0.5f);
                mesh.texture(texSide4.x(), texSide4.y());
                mesh.vertex(-0.5f, 0.5f, 0.5f);
            }
            if (texSide1 != null) {
                float terrainTile = texSide1.size();
                mesh.normal(0, -1, 0);
                mesh.texture(texSide1.x(), texSide1.y());
                mesh.vertex(0.5f, -0.5f, 0.5f);
                mesh.texture(texSide1.x() + terrainTile, texSide1.y());
                mesh.vertex(-0.5f, -0.5f, 0.5f);
                mesh.texture(texSide1.x() + terrainTile,
                        texSide1.y() + terrainTile);
                mesh.vertex(-0.5f, -0.5f, -0.5f);
                mesh.texture(texSide1.x(), texSide1.y() + terrainTile);
                mesh.vertex(0.5f, -0.5f, -0.5f);
            }
            if (texBottom != null) {
                float terrainTile = texBottom.size();
                mesh.normal(0, 0, -1);
                mesh.texture(texBottom.x(), texBottom.y() + terrainTile);
                mesh.vertex(-0.5f, 0.5f, -0.5f);
                mesh.texture(texBottom.x() + terrainTile,
                        texBottom.y() + terrainTile);
                mesh.vertex(0.5f, 0.5f, -0.5f);
                mesh.texture(texBottom.x() + terrainTile, texBottom.y());
                mesh.vertex(0.5f, -0.5f, -0.5f);
                mesh.texture(texBottom.x(), texBottom.y());
                mesh.vertex(-0.5f, -0.5f, -0.5f);
            }
        }
    }
}
