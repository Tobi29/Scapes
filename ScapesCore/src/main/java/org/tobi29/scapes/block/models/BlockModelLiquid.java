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
import org.tobi29.scapes.block.ShaderAnimation;
import org.tobi29.scapes.block.TerrainTexture;
import org.tobi29.scapes.block.TerrainTextureRegistry;
import org.tobi29.scapes.chunk.data.ChunkMesh;
import org.tobi29.scapes.chunk.terrain.Terrain;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.engine.graphics.GL;
import org.tobi29.scapes.engine.graphics.Matrix;
import org.tobi29.scapes.engine.graphics.MatrixStack;
import org.tobi29.scapes.engine.graphics.Shader;
import org.tobi29.scapes.engine.graphics.Mesh;
import org.tobi29.scapes.engine.graphics.Model;
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
    private final Model model, modelInventory;

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
        model = buildVAO(false);
        modelInventory = buildVAO(true);
    }

    protected static float calcHeight(int x, int y, int z, Terrain terrain,
            BlockType block) {
        if (terrain.type(x, y, z + 1) == block ||
                terrain.type(x - 1, y, z + 1) == block ||
                terrain.type(x - 1, y - 1, z + 1) == block ||
                terrain.type(x, y - 1, z + 1) == block) {
            return 3.0f;
        }
        boolean other1 = terrain.type(x, y, z) == block;
        boolean other2 = terrain.type(x - 1, y, z) == block;
        boolean other3 = terrain.type(x - 1, y - 1, z) == block;
        boolean other4 = terrain.type(x, y - 1, z) == block;
        if (!other1 || !other2 || !other3 || !other4) {
            boolean bottom1 = terrain.type(x, y, z - 1) == block;
            boolean bottom2 = terrain.type(x - 1, y, z - 1) == block;
            boolean bottom3 = terrain.type(x - 1, y - 1, z - 1) == block;
            boolean bottom4 = terrain.type(x, y - 1, z - 1) == block;
            if (bottom1 && !other1 ||
                    bottom2 && !other2 ||
                    bottom3 && !other3 ||
                    bottom4 && !other4) {
                return 2.0f;
            }
        }
        float height = 0;
        int heights = 0;
        if (other1) {
            height += 1 - FastMath.max(0, terrain.data(x, y, z) - 1) / 7.0f;
            heights++;
        }
        if (other2) {
            height += 1 - FastMath.max(0, terrain.data(x - 1, y, z) - 1) / 7.0f;
            heights++;
        }
        if (other3) {
            height += 1 -
                    FastMath.max(0, terrain.data(x - 1, y - 1, z) - 1) / 7.0f;
            heights++;
        }
        if (other4) {
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
        boolean static00, static01, static11, static10;
        boolean flag = top != block;
        byte noAnim = ShaderAnimation.NONE.id();
        if (flag) {
            height00 = calcHeight(x, y, z, terrain, block) * diff + min;
            height01 = calcHeight(x, y + 1, z, terrain, block) * diff + min;
            height11 = calcHeight(x + 1, y + 1, z, terrain, block) * diff + min;
            height10 = calcHeight(x + 1, y, z, terrain, block) * diff + min;
            static00 = height00 > 1.5f;
            static01 = height01 > 1.5f;
            static11 = height11 > 1.5f;
            static10 = height10 > 1.5f;
            if (static00) {
                height00 -= 2.0f;
            }
            if (static01) {
                height01 -= 2.0f;
            }
            if (static11) {
                height11 -= 2.0f;
            }
            if (static10) {
                height10 -= 2.0f;
            }
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
                            lod, static00 ? noAnim : anim);
                    mesh.addVertex(terrain, Face.UP, x + 1, y, z + height10,
                            xx + 1, yy, zz + height10, texTop.x() + terrainTile,
                            texTop.y(), r, g, b, a, lod,
                            static10 ? noAnim : anim);
                    mesh.addVertex(terrain, Face.UP, x + 1, y + 1, z + height11,
                            xx + 1, yy + 1, zz + height11,
                            texTop.x() + terrainTile, texTop.y() + terrainTile,
                            r, g, b, a, lod, static11 ? noAnim : anim);
                    mesh.addVertex(terrain, Face.UP, x, y + 1, z + height01, xx,
                            yy + 1, zz + height01, texTop.x(),
                            texTop.y() + terrainTile, r, g, b, a, lod,
                            static01 ? noAnim : anim);
                }
            }
        } else {
            height00 = diff + min;
            height10 = height00;
            height11 = height00;
            height01 = height00;
            static00 = true;
            static10 = true;
            static11 = true;
            static01 = true;
        }
        BlockType other;
        other = terrain.type(x, y, z - 1);
        if (other != block &&
                other.connectStage(terrain, x, y, z - 1) <= connectStage) {
            if (texBottom != null) {
                float terrainTile = texBottom.size();
                mesh.addVertex(terrain, Face.DOWN, x, y + 1, z, xx, yy + 1, zz,
                        texBottom.x(), texBottom.y() + terrainTile, r, g, b, a,
                        lod, noAnim);
                mesh.addVertex(terrain, Face.DOWN, x + 1, y + 1, z, xx + 1,
                        yy + 1, zz, texBottom.x() + terrainTile,
                        texBottom.y() + terrainTile, r, g, b, a, lod, noAnim);
                mesh.addVertex(terrain, Face.DOWN, x + 1, y, z, xx + 1, yy, zz,
                        texBottom.x() + terrainTile, texBottom.y(), r, g, b, a,
                        lod, noAnim);
                mesh.addVertex(terrain, Face.DOWN, x, y, z, xx, yy, zz,
                        texBottom.x(), texBottom.y(), r, g, b, a, lod, noAnim);
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
                        texSide1.y() + textureHeight10, r, g, b, a, lod,
                        static10 ? noAnim : anim);
                mesh.addVertex(terrain, Face.NORTH, x, y, z + height00, xx, yy,
                        zz + height00, texSide1.x(),
                        texSide1.y() + textureHeight00, r, g, b, a, lod,
                        static00 ? noAnim : anim);
                mesh.addVertex(terrain, Face.NORTH, x, y, z, xx, yy, zz,
                        texSide1.x(), texSide1.y() + terrainTile, r, g, b, a,
                        lod, noAnim);
                mesh.addVertex(terrain, Face.NORTH, x + 1, y, z, xx + 1, yy, zz,
                        texSide1.x() + terrainTile, texSide1.y() + terrainTile,
                        r, g, b, a, lod, noAnim);
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
                        texSide2.y() + textureHeight11, r, g, b, a, lod,
                        static11 ? noAnim : anim);
                mesh.addVertex(terrain, Face.EAST, x + 1, y, z + height10,
                        xx + 1, yy, zz + height10, texSide2.x(),
                        texSide2.y() + textureHeight10, r, g, b, a, lod,
                        static10 ? noAnim : anim);
                mesh.addVertex(terrain, Face.EAST, x + 1, y, z, xx + 1, yy, zz,
                        texSide2.x(), texSide2.y() + terrainTile, r, g, b, a,
                        lod, noAnim);
                mesh.addVertex(terrain, Face.EAST, x + 1, y + 1, z, xx + 1,
                        yy + 1, zz, texSide2.x() + terrainTile,
                        texSide2.y() + terrainTile, r, g, b, a, lod, noAnim);
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
                        g, b, a, lod, noAnim);
                mesh.addVertex(terrain, Face.SOUTH, x, y + 1, z, xx, yy + 1, zz,
                        texSide3.x() + terrainTile, texSide3.y() + terrainTile,
                        r, g, b, a, lod, noAnim);
                mesh.addVertex(terrain, Face.SOUTH, x, y + 1, z + height01, xx,
                        yy + 1, zz + height01, texSide3.x() + terrainTile,
                        texSide3.y() + textureHeight01, r, g, b, a, lod,
                        static01 ? noAnim : anim);
                mesh.addVertex(terrain, Face.SOUTH, x + 1, y + 1, z + height11,
                        xx + 1, yy + 1, zz + height11, texSide3.x(),
                        texSide3.y() + textureHeight11, r, g, b, a, lod,
                        static11 ? noAnim : anim);
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
                        lod, noAnim);
                mesh.addVertex(terrain, Face.WEST, x, y, z, xx, yy, zz,
                        texSide4.x() + terrainTile, texSide4.y() + terrainTile,
                        r, g, b, a, lod, noAnim);
                mesh.addVertex(terrain, Face.WEST, x, y, z + height00, xx, yy,
                        zz + height00, texSide4.x() + terrainTile,
                        texSide4.y() + textureHeight00, r, g, b, a, lod,
                        static00 ? noAnim : anim);
                mesh.addVertex(terrain, Face.WEST, x, y + 1, z + height01, xx,
                        yy + 1, zz + height01, texSide4.x(),
                        texSide4.y() + textureHeight01, r, g, b, a, lod,
                        static01 ? noAnim : anim);
            }
        }
    }

    @Override
    public void render(GL gl, Shader shader) {
        registry.texture().bind(gl);
        model.render(gl, shader);
    }

    @Override
    public void renderInventory(GL gl, Shader shader) {
        registry.texture().bind(gl);
        MatrixStack matrixStack = gl.matrixStack();
        Matrix matrix = matrixStack.push();
        matrix.translate(0.5f, 0.5f, 0.5f);
        matrix.rotate(57.5f, 1, 0, 0);
        matrix.rotate(45, 0, 0, 1);
        modelInventory.render(gl, shader);
        matrixStack.pop();
    }

    protected Model buildVAO(boolean inventory) {
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
