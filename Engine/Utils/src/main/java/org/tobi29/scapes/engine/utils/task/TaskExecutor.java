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

package org.tobi29.scapes.engine.utils.task;

import org.tobi29.scapes.engine.utils.Crashable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TaskExecutor {
    private final List<TaskWorker> tasks = new ArrayList<>();
    private final ThreadPoolExecutor threadPool;
    private final Crashable crashHandler;
    private final String name;
    private final boolean root;

    public TaskExecutor(TaskExecutor parent, String name) {
        this(parent, name, new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L,
                TimeUnit.SECONDS, new SynchronousQueue<>()));
    }

    public TaskExecutor(TaskExecutor parent, String name,
            ThreadPoolExecutor threadPool) {
        this(parent.crashHandler, parent.name + name, threadPool, false);
    }

    public TaskExecutor(Crashable crashHandler, String name) {
        this(crashHandler, name,
                new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L,
                        TimeUnit.SECONDS, new SynchronousQueue<>()), true);
    }

    private TaskExecutor(Crashable crashHandler, String name,
            ThreadPoolExecutor threadPool, boolean root) {
        this.crashHandler = crashHandler;
        this.name = name + '-';
        this.threadPool = threadPool;
        this.root = root;
    }

    public void tick() {
        long time = System.currentTimeMillis();
        synchronized (tasks) {
            int i = 0;
            while (i < tasks.size()) {
                TaskWorker task = tasks.get(i);
                if (time >= task.delay) {
                    try {
                        if (task.async) {
                            runTask(joiner -> {
                                long delay = task.task.run();
                                if (delay < 0) {
                                    task.stopped = true;
                                } else {
                                    task.delay = time + delay;
                                }
                            }, task.name);
                        } else {
                            long delay = task.task.run();
                            if (delay < 0) {
                                task.stopped = true;
                            } else {
                                task.delay = time + delay;
                            }
                        }
                    } catch (Throwable e) {
                        task.stopped = true;
                        crashHandler.crash(e);
                    }
                }
                if (task.stopped) {
                    tasks.remove(i);
                } else {
                    i++;
                }
            }
        }
    }

    public Joiner runTask(ASyncTask task, String name) {
        ThreadWrapper thread = new ThreadWrapper(task, this.name + name);
        threadPool.execute(thread);
        return thread.joinable.getJoiner();
    }

    public void addTask(Task task, String name, long delay, boolean async) {
        delay += System.currentTimeMillis();
        synchronized (tasks) {
            tasks.add(new TaskWorker(task, name, delay, async));
        }
    }

    public void shutdown() {
        if (root) {
            threadPool.shutdown();
            try {
                threadPool.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
            }
        }
    }

    @FunctionalInterface
    public interface Task {
        long run();
    }

    @FunctionalInterface
    public interface ASyncTask {
        void run(Joiner joiner) throws Exception;
    }

    private static class TaskWorker {
        private final Task task;
        private final String name;
        private final boolean async;
        private long delay;
        private boolean stopped;

        private TaskWorker(Task task, String name, long delay, boolean async) {
            this.task = task;
            this.name = name;
            this.delay = delay;
            this.async = async;
        }
    }

    private class ThreadWrapper implements Runnable {
        private final ASyncTask task;
        private final String name;
        private final Joiner.Joinable joinable;

        private ThreadWrapper(ASyncTask task, String name) {
            this.task = task;
            this.name = name;
            joinable = new Joiner.Joinable();
        }

        @SuppressWarnings("OverlyBroadCatchBlock")
        @Override
        public void run() {
            try {
                Thread thread = Thread.currentThread();
                thread.setName(name);
                task.run(joinable.getJoiner());
            } catch (Throwable e) { // Yes this catches ThreadDeath, so don't use it
                crashHandler.crash(e);
            } finally {
                joinable.join();
            }
        }
    }
}
