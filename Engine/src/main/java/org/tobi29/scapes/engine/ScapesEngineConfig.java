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

package org.tobi29.scapes.engine;

import org.tobi29.scapes.engine.utils.io.tag.TagStructure;

public class ScapesEngineConfig {
    private final TagStructure tagStructure;
    private int fps;
    private float musicVolume, soundVolume, resolutionMultiplier;
    private boolean vSync, fullscreen;

    ScapesEngineConfig(TagStructure tagStructure) {
        this.tagStructure = tagStructure;
        vSync = tagStructure.getBoolean("VSync");
        fps = tagStructure.getInteger("Framerate");
        resolutionMultiplier = tagStructure.getFloat("ResolutionMultiplier");
        musicVolume = tagStructure.getFloat("MusicVolume");
        soundVolume = tagStructure.getFloat("SoundVolume");
        fullscreen = tagStructure.getBoolean("Fullscreen");
    }

    public boolean getVSync() {
        return vSync;
    }

    public void setVSync(boolean vSync) {
        this.vSync = vSync;
        tagStructure.setBoolean("VSync", true);
    }

    public int getFPS() {
        return fps;
    }

    public void setFPS(int fps) {
        this.fps = fps;
        tagStructure.setInteger("Framerate", fps);
    }

    public float getResolutionMultiplier() {
        return resolutionMultiplier;
    }

    public void setResolutionMultiplier(float resolutionMultiplier) {
        this.resolutionMultiplier = resolutionMultiplier;
        tagStructure.setFloat("ResolutionMultiplier", resolutionMultiplier);
    }

    public float getMusicVolume() {
        return musicVolume;
    }

    public void setMusicVolume(float musicVolume) {
        this.musicVolume = musicVolume;
        tagStructure.setFloat("MusicVolume", musicVolume);
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public void setSoundVolume(float soundVolume) {
        this.soundVolume = soundVolume;
        tagStructure.setFloat("SoundVolume", soundVolume);
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
        tagStructure.setBoolean("Fullscreen", fullscreen);
    }
}
