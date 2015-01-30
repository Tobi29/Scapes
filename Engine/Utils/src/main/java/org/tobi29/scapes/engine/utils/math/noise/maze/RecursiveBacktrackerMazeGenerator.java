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

package org.tobi29.scapes.engine.utils.math.noise.maze;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Random;
import java.util.Stack;

public class RecursiveBacktrackerMazeGenerator implements MazeGenerator {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(RecursiveBacktrackerMazeGenerator.class);
    private final int width, height, startX, startY;
    private boolean[][] north;
    private boolean[][] west;

    public RecursiveBacktrackerMazeGenerator(int width, int height,
            Random random) {
        this(width, height, random.nextInt(width), random.nextInt(height));
    }

    public RecursiveBacktrackerMazeGenerator(int width, int height, int startX,
            int startY) {
        this.width = width;
        this.height = height;
        this.startX = startX;
        this.startY = startY;
    }

    @Override
    public void generate(Random random) {
        long time = System.currentTimeMillis();
        north = new boolean[width][height];
        west = new boolean[width][height];
        boolean[][] visited = new boolean[width][height];
        for (boolean[] array : north) {
            Arrays.fill(array, true);
        }
        for (boolean[] array : west) {
            Arrays.fill(array, true);
        }
        int maxX = width - 1;
        int maxY = height - 1;
        Cell current = new Cell(startX, startY);
        Stack<Cell> path = new Stack<>();
        path.add(current);
        Direction[] directions = new Direction[4];
        while (current != null) {
            visited[current.x][current.y] = true;
            int validDirections = 0;
            if (current.x < maxX) {
                if (!visited[current.x + 1][current.y]) {
                    directions[validDirections++] = Direction.EAST;
                }
            }
            if (current.y < maxY) {
                if (!visited[current.x][current.y + 1]) {
                    directions[validDirections++] = Direction.SOUTH;
                }
            }
            if (current.x > 0) {
                if (!visited[current.x - 1][current.y]) {
                    directions[validDirections++] = Direction.WEST;
                }
            }
            if (current.y > 0) {
                if (!visited[current.x][current.y - 1]) {
                    directions[validDirections++] = Direction.NORTH;
                }
            }
            if (validDirections > 0) {
                Direction direction =
                        directions[random.nextInt(validDirections)];
                if (direction == Direction.WEST) {
                    west[current.x][current.y] = false;
                } else if (direction == Direction.NORTH) {
                    north[current.x][current.y] = false;
                }
                current = new Cell(current.x + direction.x,
                        current.y + direction.y);
                path.add(current);
                if (direction == Direction.EAST) {
                    west[current.x][current.y] = false;
                } else if (direction == Direction.SOUTH) {
                    north[current.x][current.y] = false;
                }
            } else {
                if (!path.isEmpty()) {
                    current = path.pop();
                } else {
                    current = null;
                }
            }
        }
        LOGGER.info("Generated recursive-backtracker-maze in {} ms.",
                System.currentTimeMillis() - time);
    }

    @Override
    public boolean[][] createMap(int roomSizeX, int roomSizeY) {
        int cellSizeX = roomSizeX + 1;
        int cellSizeY = roomSizeY + 1;
        boolean[][] blocks =
                new boolean[width * cellSizeX + 1][height * cellSizeY + 1];
        for (int y = 0; y < height; y++) {
            int yy = y * cellSizeY;
            for (int x = 0; x < width; x++) {
                int xx = x * cellSizeX;
                if (north[x][y]) {
                    for (int wall = 0; wall <= cellSizeX; wall++) {
                        blocks[xx + wall][yy] = true;
                    }
                }
                if (west[x][y]) {
                    for (int wall = 0; wall <= cellSizeX; wall++) {
                        blocks[xx][yy + wall] = true;
                    }
                }
            }
        }
        int i = blocks.length - 1;
        for (int y = 0; y < blocks[i].length; y++) {
            blocks[i][y] = true;
        }
        i = blocks[0].length - 1;
        for (int x = 0; x < blocks.length; x++) {
            blocks[x][i] = true;
        }
        return blocks;
    }

    private enum Direction {
        EAST(1, 0),
        SOUTH(0, 1),
        WEST(-1, 0),
        NORTH(0, -1);
        public final int x, y;

        Direction(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private static class Cell {
        private final int x, y;

        public Cell(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
