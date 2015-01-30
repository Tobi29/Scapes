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

package org.tobi29.scapes.engine.utils.math.vector;

import org.tobi29.scapes.engine.utils.math.FastMath;

public class Vector2i extends Vector2 {
    public static final Vector2i ZERO = new Vector2i(0, 0);
    private final int x, y;

    public Vector2i(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public Vector2 plus(int a) {
        return new Vector2i(x + a, y + a);
    }

    @Override
    public Vector2 plus(long a) {
        return new Vector2i((int) (x + a), (int) (y + a));
    }

    @Override
    public Vector2 plus(float a) {
        return new Vector2i(FastMath.floor(x + a), FastMath.floor(y + a));
    }

    @Override
    public Vector2 plus(double a) {
        return new Vector2i(FastMath.floor(x + a), FastMath.floor(y + a));
    }

    @Override
    public Vector2 minus(int a) {
        return new Vector2i(x - a, y - a);
    }

    @Override
    public Vector2 minus(long a) {
        return new Vector2i((int) (x - a), (int) (y - a));
    }

    @Override
    public Vector2 minus(float a) {
        return new Vector2i(FastMath.floor(x - a), FastMath.floor(y - a));
    }

    @Override
    public Vector2 minus(double a) {
        return new Vector2i(FastMath.floor(x - a), FastMath.floor(y - a));
    }

    @Override
    public Vector2 multiply(int a) {
        return new Vector2i(x * a, y * a);
    }

    @Override
    public Vector2 multiply(long a) {
        return new Vector2i(FastMath.floor(x * a), FastMath.floor(y * a));
    }

    @Override
    public Vector2 multiply(float a) {
        return new Vector2i(FastMath.floor(x * a), FastMath.floor(y * a));
    }

    @Override
    public Vector2 multiply(double a) {
        return new Vector2i(FastMath.floor(x * a), FastMath.floor(y * a));
    }

    @Override
    public Vector2 div(int a) {
        return new Vector2i(x / a, y / a);
    }

    @Override
    public Vector2 div(long a) {
        return new Vector2i((int) (x / a), (int) (y / a));
    }

    @Override
    public Vector2 div(float a) {
        return new Vector2i(FastMath.floor(x / a), FastMath.floor(y / a));
    }

    @Override
    public Vector2 div(double a) {
        return new Vector2i(FastMath.floor(x / a), FastMath.floor(y / a));
    }

    @Override
    public Vector2 plus(Vector2 vector) {
        return new Vector2i(x + vector.intX(), y + vector.intY());
    }

    @Override
    public Vector2 minus(Vector2 vector) {
        return new Vector2i(x - vector.intX(), y - vector.intY());
    }

    @Override
    public Vector2 multiply(Vector2 vector) {
        return new Vector2i(x * vector.intX(), y * vector.intY());
    }

    @Override
    public Vector2 div(Vector2 vector) {
        return new Vector2i(x / vector.intX(), y / vector.intY());
    }

    @Override
    public int intX() {
        return x;
    }

    @Override
    public long longX() {
        return x;
    }

    @Override
    public float floatX() {
        return x;
    }

    @Override
    public double doubleX() {
        return x;
    }

    @Override
    public int intY() {
        return y;
    }

    @Override
    public long longY() {
        return y;
    }

    @Override
    public float floatY() {
        return y;
    }

    @Override
    public double doubleY() {
        return y;
    }

    @Override
    public boolean hasNaN() {
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = 31 * hash + x;
        hash = 31 * hash + y;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Vector2i)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        Vector2i comp = (Vector2i) obj;
        return comp.x == x && comp.y == y;
    }
}
