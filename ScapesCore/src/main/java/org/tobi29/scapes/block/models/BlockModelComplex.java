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

import org.tobi29.scapes.block.ShaderAnimation;
import org.tobi29.scapes.block.TerrainTexture;
import org.tobi29.scapes.block.TerrainTextureRegistry;
import org.tobi29.scapes.chunk.data.ChunkMesh;
import org.tobi29.scapes.chunk.terrain.TerrainClient;
import org.tobi29.scapes.engine.opengl.GL;
import org.tobi29.scapes.engine.opengl.Mesh;
import org.tobi29.scapes.engine.opengl.VAO;
import org.tobi29.scapes.engine.opengl.matrix.Matrix;
import org.tobi29.scapes.engine.opengl.matrix.MatrixStack;
import org.tobi29.scapes.engine.opengl.shader.Shader;
import org.tobi29.scapes.engine.utils.Streams;
import org.tobi29.scapes.engine.utils.math.Face;
import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.vector.Vector3;
import org.tobi29.scapes.engine.utils.math.vector.Vector3f;

import java.util.List;

public class BlockModelComplex implements BlockModel {
    private final TerrainTextureRegistry registry;
    private final List<Shape> shapes;
    private final VAO vao, vaoInventory;

    public BlockModelComplex(TerrainTextureRegistry registry,
            List<Shape> shapes, float scale) {
        this.registry = registry;
        this.shapes = shapes;
        Streams.of(shapes).forEach(shape -> {
            shape.scale(scale);
            shape.center();
        });
        vao = buildVAO(false);
        vaoInventory = buildVAO(true);
    }

    @Override
    public void addToChunkMesh(ChunkMesh mesh, TerrainClient terrain, int x,
            int y, int z, float xx, float yy, float zz, float r, float g,
            float b, float a, boolean lod) {
        Streams.of(shapes).forEach(shape -> shape
                .addToChunkMesh(mesh, terrain, x, y, z, xx, yy, zz, r, g, b, a,
                        lod));
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
        return mesh.finish();
    }

    protected void buildVAO(Mesh mesh, boolean inventory) {
        Streams.of(shapes).forEach(shape -> shape.addToMesh(mesh, inventory));
    }

    public abstract static class Shape {
        protected float minX, minY, minZ, maxX, maxY, maxZ, r, g, b, a;
        protected Vector3 lll, hll, hhl, lhl, llh, hlh, hhh, lhh, tlll, thll,
                thhl, tlhl, tllh, thlh, thhh, tlhh;

        private static Vector3 rotateX(Vector3 a, float cos, float sin) {
            float x = a.floatX();
            float y = a.floatY();
            float z = a.floatZ();
            float yy = y;
            y = yy * cos - z * sin;
            z = yy * sin + z * cos;
            return new Vector3f(x, y, z);
        }

        private static Vector3 rotateY(Vector3 a, float cos, float sin) {
            float x = a.floatX();
            float y = a.floatY();
            float z = a.floatZ();
            float xx = x;
            x = xx * cos - z * sin;
            z = xx * sin + z * cos;
            return new Vector3f(x, y, z);
        }

        private static Vector3 rotateZ(Vector3 a, float cos, float sin) {
            float x = a.floatX();
            float y = a.floatY();
            float z = a.floatZ();
            float xx = x;
            x = xx * cos - y * sin;
            y = xx * sin + y * cos;
            return new Vector3f(x, y, z);
        }

        public void scale(float scale) {
            lll = lll.multiply(scale);
            hll = hll.multiply(scale);
            hhl = hhl.multiply(scale);
            lhl = lhl.multiply(scale);
            llh = llh.multiply(scale);
            hlh = hlh.multiply(scale);
            hhh = hhh.multiply(scale);
            lhh = lhh.multiply(scale);
            minX *= scale;
            minY *= scale;
            minZ *= scale;
            maxX *= scale;
            maxY *= scale;
            maxZ *= scale;
        }

        public void center() {
            tlll = lll.plus(0.5f);
            thll = hll.plus(0.5f);
            thhl = hhl.plus(0.5f);
            tlhl = lhl.plus(0.5f);
            tllh = llh.plus(0.5f);
            thlh = hlh.plus(0.5f);
            thhh = hhh.plus(0.5f);
            tlhh = lhh.plus(0.5f);
            minX += 0.5f;
            minY += 0.5f;
            minZ += 0.5f;
            maxX += 0.5f;
            maxY += 0.5f;
            maxZ += 0.5f;
            float z = minZ;
            minZ = 1.0f - maxZ;
            maxZ = 1.0f - z;
        }

        public void translate(float x, float y, float z) {
            lll = lll.plus(new Vector3f(x, y, z));
            hll = hll.plus(new Vector3f(x, y, z));
            hhl = hhl.plus(new Vector3f(x, y, z));
            lhl = lhl.plus(new Vector3f(x, y, z));
            llh = llh.plus(new Vector3f(x, y, z));
            hlh = hlh.plus(new Vector3f(x, y, z));
            hhh = hhh.plus(new Vector3f(x, y, z));
            lhh = lhh.plus(new Vector3f(x, y, z));
        }

        public void rotateX(float dir) {
            dir *= FastMath.DEG_2_RAD;
            float cos = (float) FastMath.cosTable(dir);
            float sin = (float) FastMath.sinTable(dir);
            lll = rotateX(lll, cos, sin);
            hll = rotateX(hll, cos, sin);
            hhl = rotateX(hhl, cos, sin);
            lhl = rotateX(lhl, cos, sin);
            llh = rotateX(llh, cos, sin);
            hlh = rotateX(hlh, cos, sin);
            hhh = rotateX(hhh, cos, sin);
            lhh = rotateX(lhh, cos, sin);
        }

        public void rotateY(float dir) {
            dir *= FastMath.DEG_2_RAD;
            float cos = (float) FastMath.cosTable(dir);
            float sin = (float) FastMath.sinTable(dir);
            lll = rotateY(lll, cos, sin);
            hll = rotateY(hll, cos, sin);
            hhl = rotateY(hhl, cos, sin);
            lhl = rotateY(lhl, cos, sin);
            llh = rotateY(llh, cos, sin);
            hlh = rotateY(hlh, cos, sin);
            hhh = rotateY(hhh, cos, sin);
            lhh = rotateY(lhh, cos, sin);
        }

        public void rotateZ(float dir) {
            dir *= FastMath.DEG_2_RAD;
            float cos = (float) FastMath.cosTable(dir);
            float sin = (float) FastMath.sinTable(dir);
            lll = rotateZ(lll, cos, sin);
            hll = rotateZ(hll, cos, sin);
            hhl = rotateZ(hhl, cos, sin);
            lhl = rotateZ(lhl, cos, sin);
            llh = rotateZ(llh, cos, sin);
            hlh = rotateZ(hlh, cos, sin);
            hhh = rotateZ(hhh, cos, sin);
            lhh = rotateZ(lhh, cos, sin);
        }

        public abstract void addToChunkMesh(ChunkMesh mesh,
                TerrainClient terrain, double x, double y, double z, float xx,
                float yy, float zz, float r, float g, float b, float a,
                boolean lod);

        public abstract void addToMesh(Mesh mesh, boolean inventory);
    }

    public static class ShapeBox extends Shape {
        private final TerrainTexture texTop, texBottom, texSide1, texSide2,
                texSide3, texSide4;

        public ShapeBox(TerrainTexture texTop, TerrainTexture texBottom,
                TerrainTexture texSide1, TerrainTexture texSide2,
                TerrainTexture texSide3, TerrainTexture texSide4, float minX,
                float minY, float minZ, float maxX, float maxY, float maxZ,
                float r, float g, float b, float a) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            lll = new Vector3f(minX, minY, minZ);
            hll = new Vector3f(maxX, minY, minZ);
            hhl = new Vector3f(maxX, maxY, minZ);
            lhl = new Vector3f(minX, maxY, minZ);
            llh = new Vector3f(minX, minY, maxZ);
            hlh = new Vector3f(maxX, minY, maxZ);
            hhh = new Vector3f(maxX, maxY, maxZ);
            lhh = new Vector3f(minX, maxY, maxZ);
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.texTop = texTop;
            this.texBottom = texBottom;
            this.texSide1 = texSide1;
            this.texSide2 = texSide2;
            this.texSide3 = texSide3;
            this.texSide4 = texSide4;
        }

        @Override
        public void addToChunkMesh(ChunkMesh mesh, TerrainClient terrain,
                double x, double y, double z, float xx, float yy, float zz,
                float r, float g, float b, float a, boolean lod) {
            r *= this.r;
            g *= this.g;
            b *= this.b;
            a *= this.a;
            if (texTop != null) {
                float terrainTile = texTop.size();
                byte anim = texTop.shaderAnimation().id();
                float texMinX = terrainTile * minX;
                float texMaxX = terrainTile * maxX;
                float texMinY = terrainTile * minY;
                float texMaxY = terrainTile * maxY;
                mesh.addVertex(terrain, Face.UP, x + tllh.floatX(),
                        y + tllh.floatY(), z + tllh.floatZ(),
                        xx + tllh.floatX(), yy + tllh.floatY(),
                        zz + tllh.floatZ(), texTop.x() + texMinX,
                        texTop.y() + texMinY, r, g, b, a, lod, anim);
                mesh.addVertex(terrain, Face.UP, x + thlh.floatX(),
                        y + thlh.floatY(), z + thlh.floatZ(),
                        xx + thlh.floatX(), yy + thlh.floatY(),
                        zz + thlh.floatZ(), texTop.x() + texMaxX,
                        texTop.y() + texMinY, r, g, b, a, lod, anim);
                mesh.addVertex(terrain, Face.UP, x + thhh.floatX(),
                        y + thhh.floatY(), z + thhh.floatZ(),
                        xx + thhh.floatX(), yy + thhh.floatY(),
                        zz + thhh.floatZ(), texTop.x() + texMaxX,
                        texTop.y() + texMaxY, r, g, b, a, lod, anim);
                mesh.addVertex(terrain, Face.UP, x + tlhh.floatX(),
                        y + tlhh.floatY(), z + tlhh.floatZ(),
                        xx + tlhh.floatX(), yy + tlhh.floatY(),
                        zz + tlhh.floatZ(), texTop.x() + texMinX,
                        texTop.y() + texMaxY, r, g, b, a, lod, anim);
            }
            if (texBottom != null) {
                float terrainTile = texBottom.size();
                byte anim = texBottom.shaderAnimation().id();
                float texMinX = terrainTile * minX;
                float texMaxX = terrainTile * maxX;
                float texMinY = terrainTile * minY;
                float texMaxY = terrainTile * maxY;
                mesh.addVertex(terrain, Face.DOWN, x + tlhl.floatX(),
                        y + tlhl.floatY(), z + tlhl.floatZ(),
                        xx + tlhl.floatX(), yy + tlhl.floatY(),
                        zz + tlhl.floatZ(), texBottom.x() + texMinX,
                        texBottom.y() + texMaxY, r, g, b, a, lod, anim);
                mesh.addVertex(terrain, Face.DOWN, x + thhl.floatX(),
                        y + thhl.floatY(), z + thhl.floatZ(),
                        xx + thhl.floatX(), yy + thhl.floatY(),
                        zz + thhl.floatZ(), texBottom.x() + texMaxX,
                        texBottom.y() + texMaxY, r, g, b, a, lod, anim);
                mesh.addVertex(terrain, Face.DOWN, x + thll.floatX(),
                        y + thll.floatY(), z + thll.floatZ(),
                        xx + thll.floatX(), yy + thll.floatY(),
                        zz + thll.floatZ(), texBottom.x() + texMaxX,
                        texBottom.y() + texMinY, r, g, b, a, lod, anim);
                mesh.addVertex(terrain, Face.DOWN, x + tlll.floatX(),
                        y + tlll.floatY(), z + tlll.floatZ(),
                        xx + tlll.floatX(), yy + tlll.floatY(),
                        zz + tlll.floatZ(), texBottom.x() + texMinX,
                        texBottom.y() + texMinY, r, g, b, a, lod, anim);
            }
            if (texSide1 != null) {
                float terrainTile = texSide1.size();
                byte anim = texSide1.shaderAnimation().id();
                float texMinX = terrainTile * minX;
                float texMaxX = terrainTile * maxX;
                float texMinY = terrainTile * minZ;
                float texMaxY = terrainTile * maxZ;
                mesh.addVertex(terrain, Face.NORTH, x + thlh.floatX(),
                        y + thlh.floatY(), z + thlh.floatZ(),
                        xx + thlh.floatX(), yy + thlh.floatY(),
                        zz + thlh.floatZ(), texSide1.x() + texMaxX,
                        texSide1.y() + texMinY, r, g, b, a, lod, anim);
                mesh.addVertex(terrain, Face.NORTH, x + tllh.floatX(),
                        y + tllh.floatY(), z + tllh.floatZ(),
                        xx + tllh.floatX(), yy + tllh.floatY(),
                        zz + tllh.floatZ(), texSide1.x() + texMinX,
                        texSide1.y() + texMinY, r, g, b, a, lod, anim);
                mesh.addVertex(terrain, Face.NORTH, x + tlll.floatX(),
                        y + tlll.floatY(), z + tlll.floatZ(),
                        xx + tlll.floatX(), yy + tlll.floatY(),
                        zz + tlll.floatZ(), texSide1.x() + texMinX,
                        texSide1.y() + texMaxY, r, g, b, a, lod, anim);
                mesh.addVertex(terrain, Face.NORTH, x + thll.floatX(),
                        y + thll.floatY(), z + thll.floatZ(),
                        xx + thll.floatX(), yy + thll.floatY(),
                        zz + thll.floatZ(), texSide1.x() + texMaxX,
                        texSide1.y() + texMaxY, r, g, b, a, lod, anim);
            }
            if (texSide2 != null) {
                float terrainTile = texSide2.size();
                byte anim = texSide2.shaderAnimation().id();
                float texMinX = terrainTile * minY;
                float texMaxX = terrainTile * maxY;
                float texMinY = terrainTile * minZ;
                float texMaxY = terrainTile * maxZ;
                mesh.addVertex(terrain, Face.EAST, x + thhh.floatX(),
                        y + thhh.floatY(), z + thhh.floatZ(),
                        xx + thhh.floatX(), yy + thhh.floatY(),
                        zz + thhh.floatZ(), texSide2.x() + texMaxX,
                        texSide2.y() + texMinY, r, g, b, a, lod, anim);
                mesh.addVertex(terrain, Face.EAST, x + thlh.floatX(),
                        y + thlh.floatY(), z + thlh.floatZ(),
                        xx + thlh.floatX(), yy + thlh.floatY(),
                        zz + thlh.floatZ(), texSide2.x() + texMinX,
                        texSide2.y() + texMinY, r, g, b, a, lod, anim);
                mesh.addVertex(terrain, Face.EAST, x + thll.floatX(),
                        y + thll.floatY(), z + thll.floatZ(),
                        xx + thll.floatX(), yy + thll.floatY(),
                        zz + thll.floatZ(), texSide2.x() + texMinX,
                        texSide2.y() + texMaxY, r, g, b, a, lod, anim);
                mesh.addVertex(terrain, Face.EAST, x + thhl.floatX(),
                        y + thhl.floatY(), z + thhl.floatZ(),
                        xx + thhl.floatX(), yy + thhl.floatY(),
                        zz + thhl.floatZ(), texSide2.x() + texMaxX,
                        texSide2.y() + texMaxY, r, g, b, a, lod, anim);
            }
            if (texSide3 != null) {
                float terrainTile = texSide3.size();
                byte anim = texSide3.shaderAnimation().id();
                float texMinX = terrainTile * minX;
                float texMaxX = terrainTile * maxX;
                float texMinZ = terrainTile * minZ;
                float texMaxZ = terrainTile * maxZ;
                mesh.addVertex(terrain, Face.SOUTH, x + thhl.floatX(),
                        y + thhl.floatY(), z + thhl.floatZ(),
                        xx + thhl.floatX(), yy + thhl.floatY(),
                        zz + thhl.floatZ(), texSide3.x() + texMinX,
                        texSide3.y() + texMaxZ, r, g, b, a, lod, anim);
                mesh.addVertex(terrain, Face.SOUTH, x + tlhl.floatX(),
                        y + tlhl.floatY(), z + tlhl.floatZ(),
                        xx + tlhl.floatX(), yy + tlhl.floatY(),
                        zz + tlhl.floatZ(), texSide3.x() + texMaxX,
                        texSide3.y() + texMaxZ, r, g, b, a, lod, anim);
                mesh.addVertex(terrain, Face.SOUTH, x + tlhh.floatX(),
                        y + tlhh.floatY(), z + tlhh.floatZ(),
                        xx + tlhh.floatX(), yy + tlhh.floatY(),
                        zz + tlhh.floatZ(), texSide3.x() + texMaxX,
                        texSide3.y() + texMinZ, r, g, b, a, lod, anim);
                mesh.addVertex(terrain, Face.SOUTH, x + thhh.floatX(),
                        y + thhh.floatY(), z + thhh.floatZ(),
                        xx + thhh.floatX(), yy + thhh.floatY(),
                        zz + thhh.floatZ(), texSide3.x() + texMinX,
                        texSide3.y() + texMinZ, r, g, b, a, lod, anim);
            }
            if (texSide4 != null) {
                float terrainTile = texSide4.size();
                byte anim = texSide4.shaderAnimation().id();
                float texMinX = terrainTile * minY;
                float texMaxX = terrainTile * maxY;
                float texMinZ = terrainTile * minZ;
                float texMaxZ = terrainTile * maxZ;
                mesh.addVertex(terrain, Face.WEST, x + tlhl.floatX(),
                        y + tlhl.floatY(), z + tlhl.floatZ(),
                        xx + tlhl.floatX(), yy + tlhl.floatY(),
                        zz + tlhl.floatZ(), texSide4.x() + texMinX,
                        texSide4.y() + texMaxZ, r, g, b, a, lod, anim);
                mesh.addVertex(terrain, Face.WEST, x + tlll.floatX(),
                        y + tlll.floatY(), z + tlll.floatZ(),
                        xx + tlll.floatX(), yy + tlll.floatY(),
                        zz + tlll.floatZ(), texSide4.x() + texMaxX,
                        texSide4.y() + texMaxZ, r, g, b, a, lod, anim);
                mesh.addVertex(terrain, Face.WEST, x + tllh.floatX(),
                        y + tllh.floatY(), z + tllh.floatZ(),
                        xx + tllh.floatX(), yy + tllh.floatY(),
                        zz + tllh.floatZ(), texSide4.x() + texMaxX,
                        texSide4.y() + texMinZ, r, g, b, a, lod, anim);
                mesh.addVertex(terrain, Face.WEST, x + tlhh.floatX(),
                        y + tlhh.floatY(), z + tlhh.floatZ(),
                        xx + tlhh.floatX(), yy + tlhh.floatY(),
                        zz + tlhh.floatZ(), texSide4.x() + texMinX,
                        texSide4.y() + texMinZ, r, g, b, a, lod, anim);
            }
        }

        @Override
        public void addToMesh(Mesh mesh, boolean inventory) {
            mesh.color(r, g, b, a);
            if (texTop != null) {
                float terrainTile = texTop.size();
                float texMinX = terrainTile * minX;
                float texMaxX = terrainTile * maxX;
                float texMinY = terrainTile * minY;
                float texMaxY = terrainTile * maxY;
                mesh.normal(0, 0, 1);
                mesh.texture(texTop.x() + texMinX, texTop.y() + texMinY);
                mesh.vertex(llh.floatX(), llh.floatY(), llh.floatZ());
                mesh.texture(texTop.x() + texMaxX, texTop.y() + texMinY);
                mesh.vertex(hlh.floatX(), hlh.floatY(), hlh.floatZ());
                mesh.texture(texTop.x() + texMaxX, texTop.y() + texMaxY);
                mesh.vertex(hhh.floatX(), hhh.floatY(), hhh.floatZ());
                mesh.texture(texTop.x() + texMinX, texTop.y() + texMaxY);
                mesh.vertex(lhh.floatX(), lhh.floatY(), lhh.floatZ());
            }
            if (texSide2 != null) {
                if (inventory) {
                    mesh.color(r * 0.7f, g * 0.7f, b * 0.7f, a * 1.0f);
                }
                float terrainTile = texSide2.size();
                float texMinX = terrainTile * minY;
                float texMaxX = terrainTile * maxY;
                float texMinY = terrainTile * minZ;
                float texMaxY = terrainTile * maxZ;
                mesh.normal(1, 0, 0);
                mesh.texture(texSide2.x() + texMinX, texSide2.y() + texMinY);
                mesh.vertex(hhh.floatX(), hhh.floatY(), hhh.floatZ());
                mesh.texture(texSide2.x() + texMaxX, texSide2.y() + texMinY);
                mesh.vertex(hlh.floatX(), hlh.floatY(), hlh.floatZ());
                mesh.texture(texSide2.x() + texMaxX, texSide2.y() + texMaxY);
                mesh.vertex(hll.floatX(), hll.floatY(), hll.floatZ());
                mesh.texture(texSide2.x() + texMinX, texSide2.y() + texMaxY);
                mesh.vertex(hhl.floatX(), hhl.floatY(), hhl.floatZ());
            }
            if (texSide3 != null) {
                if (inventory) {
                    mesh.color(r * 0.8f, g * 0.8f, b * 0.8f, a);
                }
                float terrainTile = texSide3.size();
                float texMinX = terrainTile * minX;
                float texMaxX = terrainTile * maxX;
                float texMinY = terrainTile * minZ;
                float texMaxY = terrainTile * maxZ;
                mesh.normal(0, 1, 0);
                mesh.texture(texSide3.x() + texMaxX, texSide3.y() + texMaxY);
                mesh.vertex(hhl.floatX(), hhl.floatY(), hhl.floatZ());
                mesh.texture(texSide3.x() + texMinX, texSide3.y() + texMaxY);
                mesh.vertex(lhl.floatX(), lhl.floatY(), lhl.floatZ());
                mesh.texture(texSide3.x() + texMinX, texSide3.y() + texMinY);
                mesh.vertex(lhh.floatX(), lhh.floatY(), lhh.floatZ());
                mesh.texture(texSide3.x() + texMaxX, texSide3.y() + texMinY);
                mesh.vertex(hhh.floatX(), hhh.floatY(), hhh.floatZ());
            }
            if (!inventory) {
                if (texSide4 != null) {
                    float terrainTile = texSide4.size();
                    float texMinX = terrainTile * minY;
                    float texMaxX = terrainTile * maxY;
                    float texMinY = terrainTile * minZ;
                    float texMaxY = terrainTile * maxZ;
                    mesh.normal(-1, 0, 0);
                    mesh.texture(texSide4.x() + texMinX,
                            texSide4.y() + texMaxY);
                    mesh.vertex(lhl.floatX(), lhl.floatY(), lhl.floatZ());
                    mesh.texture(texSide4.x() + texMaxX,
                            texSide4.y() + texMaxY);
                    mesh.vertex(lll.floatX(), lll.floatY(), lll.floatZ());
                    mesh.texture(texSide4.x() + texMaxX,
                            texSide4.y() + texMinY);
                    mesh.vertex(llh.floatX(), llh.floatY(), llh.floatZ());
                    mesh.texture(texSide4.x() + texMinX,
                            texSide4.y() + texMinY);
                    mesh.vertex(lhh.floatX(), lhh.floatY(), lhh.floatZ());
                }
                if (texSide1 != null) {
                    float terrainTile = texSide1.size();
                    float texMinX = terrainTile * minX;
                    float texMaxX = terrainTile * maxX;
                    float texMinY = terrainTile * minZ;
                    float texMaxY = terrainTile * maxZ;
                    mesh.normal(0, -1, 0);
                    mesh.texture(texSide1.x() + texMinX,
                            texSide1.y() + texMinY);
                    mesh.vertex(hlh.floatX(), hlh.floatY(), hlh.floatZ());
                    mesh.texture(texSide1.x() + texMaxX,
                            texSide1.y() + texMinY);
                    mesh.vertex(llh.floatX(), llh.floatY(), llh.floatZ());
                    mesh.texture(texSide1.x() + texMaxX,
                            texSide1.y() + texMaxY);
                    mesh.vertex(lll.floatX(), lll.floatY(), lll.floatZ());
                    mesh.texture(texSide1.x() + texMinX,
                            texSide1.y() + texMaxY);
                    mesh.vertex(hll.floatX(), hll.floatY(), hll.floatZ());
                }
                if (texBottom != null) {
                    float terrainTile = texBottom.size();
                    float texMinX = terrainTile * minX;
                    float texMaxX = terrainTile * maxX;
                    float texMinY = terrainTile * minY;
                    float texMaxY = terrainTile * maxY;
                    mesh.normal(0, 0, -1);
                    mesh.texture(texBottom.x() + texMinX,
                            texBottom.y() + texMaxY);
                    mesh.vertex(lhl.floatX(), lhl.floatY(), lhl.floatZ());
                    mesh.texture(texBottom.x() + texMaxX,
                            texBottom.y() + texMaxY);
                    mesh.vertex(hhl.floatX(), hhl.floatY(), hhl.floatZ());
                    mesh.texture(texBottom.x() + texMaxX,
                            texBottom.y() + texMinY);
                    mesh.vertex(hll.floatX(), hll.floatY(), hll.floatZ());
                    mesh.texture(texBottom.x() + texMinX,
                            texBottom.y() + texMinY);
                    mesh.vertex(lll.floatX(), lll.floatY(), lll.floatZ());
                }
            }
        }
    }

    public static class ShapeBillboard extends Shape {
        private final TerrainTexture texture;
        private final float nx, ny, nz;

        public ShapeBillboard(TerrainTexture texture, float minX, float minY,
                float minZ, float maxX, float maxY, float maxZ, float r,
                float g, float b, float a) {
            this(texture, minX, minY, minZ, maxX, maxY, maxZ, Float.NaN,
                    Float.NaN, Float.NaN, r, g, b, a);
        }

        public ShapeBillboard(TerrainTexture texture, float minX, float minY,
                float minZ, float maxX, float maxY, float maxZ, float nx,
                float ny, float nz, float r, float g, float b, float a) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            float middleX = (minX + maxX) / 2.0f;
            float middleY = (minY + maxY) / 2.0f;
            lll = new Vector3f(middleX, minY, minZ);
            lhl = new Vector3f(middleX, maxY, minZ);
            lhh = new Vector3f(middleX, maxY, maxZ);
            llh = new Vector3f(middleX, minY, maxZ);
            hll = new Vector3f(minX, middleY, minZ);
            hhl = new Vector3f(maxX, middleY, minZ);
            hhh = new Vector3f(maxX, middleY, maxZ);
            hlh = new Vector3f(minX, middleY, maxZ);
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.texture = texture;
        }

        @Override
        public void addToChunkMesh(ChunkMesh mesh, TerrainClient terrain,
                double x, double y, double z, float xx, float yy, float zz,
                float r, float g, float b, float a, boolean lod) {
            r *= this.r;
            g *= this.g;
            b *= this.b;
            a *= this.a;
            if (texture != null) {
                float terrainTile = texture.size();
                ShaderAnimation animation = texture.shaderAnimation();
                byte animBottom = animation.id();
                byte animTop;
                if (animation == ShaderAnimation.TALL_GRASS) {
                    animTop = ShaderAnimation.NONE.id();
                } else {
                    animTop = animBottom;
                }
                float texMinX = terrainTile * minX;
                float texMaxX = terrainTile * maxX;
                float texMinY = terrainTile * minY;
                float texMaxY = terrainTile * maxY;
                mesh.addVertex(terrain, Face.NONE, x + tlll.floatX(),
                        y + tlll.floatY(), z + tlll.floatZ(),
                        xx + tlll.floatX(), yy + tlll.floatY(),
                        zz + tlll.floatZ(), nx, ny, nz, texture.x() + texMinX,
                        texture.y() + texMaxY, r, g, b, a, lod, animTop);
                mesh.addVertex(terrain, Face.NONE, x + tlhl.floatX(),
                        y + tlhl.floatY(), z + tlhl.floatZ(),
                        xx + tlhl.floatX(), yy + tlhl.floatY(),
                        zz + tlhl.floatZ(), nx, ny, nz, texture.x() + texMaxX,
                        texture.y() + texMaxY, r, g, b, a, lod, animTop);
                mesh.addVertex(terrain, Face.NONE, x + tlhh.floatX(),
                        y + tlhh.floatY(), z + tlhh.floatZ(),
                        xx + tlhh.floatX(), yy + tlhh.floatY(),
                        zz + tlhh.floatZ(), nx, ny, nz, texture.x() + texMaxX,
                        texture.y() + texMinY, r, g, b, a, lod, animBottom);
                mesh.addVertex(terrain, Face.NONE, x + tllh.floatX(),
                        y + tllh.floatY(), z + tllh.floatZ(),
                        xx + tllh.floatX(), yy + tllh.floatY(),
                        zz + tllh.floatZ(), nx, ny, nz, texture.x() + texMinX,
                        texture.y() + texMinY, r, g, b, a, lod, animBottom);
                mesh.addVertex(terrain, Face.NONE, x + tllh.floatX(),
                        y + tllh.floatY(), z + tllh.floatZ(),
                        xx + tllh.floatX(), yy + tllh.floatY(),
                        zz + tllh.floatZ(), nx, ny, nz, texture.x() + texMaxX,
                        texture.y() + texMinY, r, g, b, a, lod, animBottom);
                mesh.addVertex(terrain, Face.NONE, x + tlhh.floatX(),
                        y + tlhh.floatY(), z + tlhh.floatZ(),
                        xx + tlhh.floatX(), yy + tlhh.floatY(),
                        zz + tlhh.floatZ(), nx, ny, nz, texture.x() + texMinX,
                        texture.y() + texMinY, r, g, b, a, lod, animBottom);
                mesh.addVertex(terrain, Face.NONE, x + tlhl.floatX(),
                        y + tlhl.floatY(), z + tlhl.floatZ(),
                        xx + tlhl.floatX(), yy + tlhl.floatY(),
                        zz + tlhl.floatZ(), nx, ny, nz, texture.x() + texMinX,
                        texture.y() + texMaxY, r, g, b, a, lod, animTop);
                mesh.addVertex(terrain, Face.NONE, x + tlll.floatX(),
                        y + tlll.floatY(), z + tlll.floatZ(),
                        xx + tlll.floatX(), yy + tlll.floatY(),
                        zz + tlll.floatZ(), nx, ny, nz, texture.x() + texMaxX,
                        texture.y() + texMaxY, r, g, b, a, lod, animTop);
                mesh.addVertex(terrain, Face.NONE, x + thll.floatX(),
                        y + thll.floatY(), z + thll.floatZ(),
                        xx + thll.floatX(), yy + thll.floatY(),
                        zz + thll.floatZ(), nx, ny, nz, texture.x() + texMinX,
                        texture.y() + texMaxY, r, g, b, a, lod, animTop);
                mesh.addVertex(terrain, Face.NONE, x + thhl.floatX(),
                        y + thhl.floatY(), z + thhl.floatZ(),
                        xx + thhl.floatX(), yy + thhl.floatY(),
                        zz + thhl.floatZ(), nx, ny, nz, texture.x() + texMaxX,
                        texture.y() + texMaxY, r, g, b, a, lod, animTop);
                mesh.addVertex(terrain, Face.NONE, x + thhh.floatX(),
                        y + thhh.floatY(), z + thhh.floatZ(),
                        xx + thhh.floatX(), yy + thhh.floatY(),
                        zz + thhh.floatZ(), nx, ny, nz, texture.x() + texMaxX,
                        texture.y() + texMinY, r, g, b, a, lod, animBottom);
                mesh.addVertex(terrain, Face.NONE, x + thlh.floatX(),
                        y + thlh.floatY(), z + thlh.floatZ(),
                        xx + thlh.floatX(), yy + thlh.floatY(),
                        zz + thlh.floatZ(), nx, ny, nz, texture.x() + texMinX,
                        texture.y() + texMinY, r, g, b, a, lod, animBottom);
                mesh.addVertex(terrain, Face.NONE, x + thlh.floatX(),
                        y + thlh.floatY(), z + thlh.floatZ(),
                        xx + thlh.floatX(), yy + thlh.floatY(),
                        zz + thlh.floatZ(), nx, ny, nz, texture.x() + texMaxX,
                        texture.y() + texMinY, r, g, b, a, lod, animBottom);
                mesh.addVertex(terrain, Face.NONE, x + thhh.floatX(),
                        y + thhh.floatY(), z + thhh.floatZ(),
                        xx + thhh.floatX(), yy + thhh.floatY(),
                        zz + thhh.floatZ(), nx, ny, nz, texture.x() + texMinX,
                        texture.y() + texMinY, r, g, b, a, lod, animBottom);
                mesh.addVertex(terrain, Face.NONE, x + thhl.floatX(),
                        y + thhl.floatY(), z + thhl.floatZ(),
                        xx + thhl.floatX(), yy + thhl.floatY(),
                        zz + thhl.floatZ(), nx, ny, nz, texture.x() + texMinX,
                        texture.y() + texMaxY, r, g, b, a, lod, animTop);
                mesh.addVertex(terrain, Face.NONE, x + thll.floatX(),
                        y + thll.floatY(), z + thll.floatZ(),
                        xx + thll.floatX(), yy + thll.floatY(),
                        zz + thll.floatZ(), nx, ny, nz, texture.x() + texMaxX,
                        texture.y() + texMaxY, r, g, b, a, lod, animTop);
            }
        }

        @Override
        public void addToMesh(Mesh mesh, boolean inventory) {
            if (texture != null) {
                float terrainTile = texture.size();
                float texMinX = terrainTile * minX;
                float texMaxX = terrainTile * maxX;
                float texMinY = terrainTile * minY;
                float texMaxY = terrainTile * maxY;
                mesh.color(r, g, b, a);
                mesh.normal(0, 0, 1); // TODO: Implement proper normals
                mesh.texture(texture.x() + texMinX, texture.y() + texMaxY);
                mesh.vertex(lll.floatX(), lll.floatY(), lll.floatZ());
                mesh.texture(texture.x() + texMaxX, texture.y() + texMaxY);
                mesh.vertex(lhl.floatX(), lhl.floatY(), lhl.floatZ());
                mesh.texture(texture.x() + texMaxX, texture.y() + texMinY);
                mesh.vertex(lhh.floatX(), lhh.floatY(), lhh.floatZ());
                mesh.texture(texture.x() + texMinX, texture.y() + texMinY);
                mesh.vertex(llh.floatX(), llh.floatY(), llh.floatZ());
                mesh.texture(texture.x() + texMinX, texture.y() + texMinY);
                mesh.vertex(llh.floatX(), llh.floatY(), llh.floatZ());
                mesh.texture(texture.x() + texMaxX, texture.y() + texMinY);
                mesh.vertex(lhh.floatX(), lhh.floatY(), lhh.floatZ());
                mesh.texture(texture.x() + texMaxX, texture.y() + texMaxY);
                mesh.vertex(lhl.floatX(), lhl.floatY(), lhl.floatZ());
                mesh.texture(texture.x() + texMinX, texture.y() + texMaxY);
                mesh.vertex(lll.floatX(), lll.floatY(), lll.floatZ());
                mesh.texture(texture.x() + texMinX, texture.y() + texMaxY);
                mesh.vertex(hll.floatX(), hll.floatY(), hll.floatZ());
                mesh.texture(texture.x() + texMaxX, texture.y() + texMaxY);
                mesh.vertex(hhl.floatX(), hhl.floatY(), hhl.floatZ());
                mesh.texture(texture.x() + texMaxX, texture.y() + texMinY);
                mesh.vertex(hhh.floatX(), hhh.floatY(), hhh.floatZ());
                mesh.texture(texture.x() + texMinX, texture.y() + texMinY);
                mesh.vertex(hlh.floatX(), hlh.floatY(), hlh.floatZ());
                mesh.texture(texture.x() + texMinX, texture.y() + texMinY);
                mesh.vertex(hlh.floatX(), hlh.floatY(), hlh.floatZ());
                mesh.texture(texture.x() + texMaxX, texture.y() + texMinY);
                mesh.vertex(hhh.floatX(), hhh.floatY(), hhh.floatZ());
                mesh.texture(texture.x() + texMaxX, texture.y() + texMaxY);
                mesh.vertex(hhl.floatX(), hhl.floatY(), hhl.floatZ());
                mesh.texture(texture.x() + texMinX, texture.y() + texMaxY);
                mesh.vertex(hll.floatX(), hll.floatY(), hll.floatZ());
            }
        }
    }
}
