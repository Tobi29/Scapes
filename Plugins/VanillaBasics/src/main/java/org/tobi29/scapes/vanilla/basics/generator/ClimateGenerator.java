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
package org.tobi29.scapes.vanilla.basics.generator;

import org.tobi29.scapes.engine.utils.math.FastMath;
import org.tobi29.scapes.engine.utils.math.noise.value.SimplexNoise;
import org.tobi29.scapes.engine.utils.math.noise.value.ValueNoise;

import java.util.Random;

public class ClimateGenerator {
    private static final double SCALE = 50000.0;
    private final TerrainGenerator terrainGenerator;
    private final ValueNoise temperatureNoise, humidityNoise, weatherNoise;
    private long day;
    private double dayTime;
    private double sunDeclination, sunHourAngleCos;

    public ClimateGenerator(Random random, TerrainGenerator terrainGenerator) {
        this(new SimplexNoise(random.nextLong()),
                new SimplexNoise(random.nextLong()),
                new SimplexNoise(random.nextLong()), terrainGenerator);
    }

    private ClimateGenerator(ValueNoise temperatureNoise,
            ValueNoise humidityNoise, ValueNoise weatherNoise,
            TerrainGenerator terrainGenerator) {
        this.temperatureNoise = temperatureNoise;
        this.humidityNoise = humidityNoise;
        this.weatherNoise = weatherNoise;
        this.terrainGenerator = terrainGenerator;
    }

    public void add(double time) {
        dayTime += time;
        updateTime();
    }

    public long day() {
        return day;
    }

    public void setDay(long day) {
        this.day = day;
        updateTime();
    }

    public double dayTime() {
        return dayTime;
    }

    public void setDayTime(double dayTime) {
        this.dayTime = dayTime;
        updateTime();
    }

    private void updateTime() {
        while (dayTime >= 1) {
            dayTime -= 1;
            day++;
        }
        double axialTilt = -23.44 * FastMath.DEG_2_RAD;
        sunDeclination = FastMath.asin(FastMath.sin(axialTilt) *
                FastMath.sin(season() * FastMath.TWO_PI));
        sunHourAngleCos = FastMath.sin(dayTime * FastMath.TWO_PI);
    }

    public ClimateGenerator at(long day, double dayTime) {
        ClimateGenerator climateGenerator =
                new ClimateGenerator(temperatureNoise, humidityNoise,
                        weatherNoise, terrainGenerator);
        climateGenerator.day = day;
        climateGenerator.dayTime = dayTime;
        climateGenerator.updateTime();
        return climateGenerator;
    }

    public double season() {
        return (day % 50 + dayTime) / 50.0 % 1.0;
    }

    public double latitude(double y) {
        double latitude = y / SCALE * FastMath.PI % FastMath.TWO_PI;
        if (latitude < 0.0) {
            latitude += FastMath.TWO_PI;
        }
        return latitude;
    }

    public double sunElevation(double x, double y) {
        return sunElevationD(latitude(y));
    }

    public double sunElevationD(double latitude) {
        return sunElevationD(sunHourAngleCos, sunDeclination, latitude);
    }

    public double sunElevationD(double hourAngleCos, double declination,
            double latitude) {
        return FastMath.asinTable(
                FastMath.sinTable(latitude) * FastMath.sinTable(declination) +
                        FastMath.cosTable(latitude) *
                                FastMath.cosTable(declination) *
                                hourAngleCos);
    }

    public double sunIntensity(double y, double factor) {
        return sunIntensityD(latitude(y), sunDeclination * factor);
    }

    public double sunIntensityD(double latitude) {
        return sunIntensityD(latitude, sunDeclination);
    }

    public double sunIntensityD(double latitude, double declination) {
        declination *= 0.5;
        boolean swap = latitude > FastMath.PI;
        if (latitude > FastMath.HALF_PI && latitude < FastMath.HALF_PI * 3.0) {
            latitude = FastMath.TWO_PI - latitude;
        }
        double delta = (latitude - declination) / FastMath.HALF_PI % 2.0 - 1.0;
        if (delta < -1.0) {
            delta += 2.0;
        }
        if (delta > 0.0 ^ swap && declination > 0.0 ^ !swap) {
            return 0.0;
        }
        return FastMath.abs(delta);
    }

    public double sunAzimuth(double x, double y) {
        return sunAzimuthD(sunElevation(x, y), latitude(y));
    }

    public double sunAzimuthD(double elevation, double latitude) {
        return sunAzimuthD(sunHourAngleCos, sunDeclination, elevation,
                latitude);
    }

    public double sunAzimuthD(double hourAngleCos, double declination,
            double elevation, double latitude) {
        double azimuth =
                FastMath.sinTable(declination) * FastMath.cosTable(latitude);
        azimuth -= hourAngleCos * FastMath.cosTable(declination) *
                FastMath.sinTable(latitude);
        azimuth /= FastMath.cosTable(elevation);
        azimuth = FastMath.acos(azimuth);
        if (dayTime > 0.25 && dayTime < 0.75) {
            azimuth = -azimuth;
        }
        return azimuth;
    }

    public double weather(double x, double y) {
        double humidity3 = humidity3(x, y);
        return weatherD(x, y, humidity3);
    }

    public double weatherD(double x, double y, double humidity3) {
        x /= SCALE;
        y /= SCALE;
        double weather =
                weatherNoise.noise(day + dayTime, x / 0.16, y / 0.16) * 0.5 +
                        0.5;
        double rainfall = 4.0 - humidity3 * 3.4;
        rainfall *= 1.0 - FastMath.sinTable(dayTime * FastMath.PI) * 0.5;
        rainfall = FastMath.max(rainfall, 0.1);
        weather = FastMath.pow(weather, rainfall);
        return weather;
    }

    public double temperature(int x, int y, int z) {
        return temperature(x, y) - FastMath.max(z - 300, 0) / 10.0;
    }

    public double temperatureD(double temperature, int z) {
        return temperature - FastMath.max(z - 300, 0) / 10.0;
    }

    public double temperature(double x, double y) {
        double humidity3 = humidity3(x, y);
        double temperature2 = temperature2D(x, y, humidity3);
        return temperatureD(temperature2, sunLightReduction(x, y),
                weatherD(x, y, humidity3), humidity3);
    }

    public double temperatureD(double temperature2, double sunLightReduction,
            double weather, double humidity3) {
        temperature2 *= FastMath.clamp(2.0 - weather * 2.0, 0.0, 1.0);
        temperature2 -= sunLightReduction * 0.8 * (1.0 - humidity3);
        return temperature2;
    }

    public double temperature2(double x, double y) {
        return temperature2D(x, y, humidity3(x, y));
    }

    public double temperature2D(double x, double y, double humidity3) {
        return temperature2D(x, y, humidity3, sunIntensity(y, 0.9));
    }

    public double temperature2D(double x, double y, double humidity3,
            double sunIntensity) {
        x /= SCALE;
        y /= SCALE;
        double temperature =
                FastMath.mix(sunIntensity, sunIntensity * sunIntensity, 0.5) *
                        80.0 - 10.0;
        double noiseGlobal =
                temperatureNoise.noise(x, y, (day + dayTime) / 100.0);
        double noiseLocal = temperatureNoise
                .noiseOctave(x / 0.2, y / 0.2, (day + dayTime) / 20.0, 2, 4.0,
                        0.5);
        double noiseExtreme = temperatureNoise
                .noise(x / 4.0, y / 4.0, (day + dayTime) / 40.0);
        temperature += noiseGlobal * 40.0;
        temperature += noiseLocal * 10.0;
        temperature += FastMath.max(noiseExtreme - 0.3, 0.0) * 160.0;
        temperature += FastMath.min(0.3 - noiseExtreme, 0.0) * 160.0;
        temperature -= humidity3 * 20.0;
        return temperature;
    }

    public double humidity(int x, int y, int z) {
        return humidity(x, y);
    }

    public double humidity(double x, double y) {
        double humidity3 = humidity3(x, y);
        double temperature2 = temperature2D(x, y, humidity3);
        double weather = weatherD(x, y, humidity3);
        double humidity2 = humidity2D(temperature2, humidity3);
        return humidityD(humidity2, riverHumidity(x, y), weather);
    }

    public double humidityD(double humidity2, double riverHumidity,
            double weather) {
        return FastMath
                .clamp(humidity2 + FastMath.max(weather * 2.0 - 1.0, 0.0) +
                        riverHumidity, 0.0, 1.0);
    }

    public double humidity2(double x, double y) {
        double humidity3 = humidity3(x, y);
        double temperature2 = temperature2D(x, y, humidity3);
        return humidity2D(temperature2, humidity3);
    }

    public double humidity2D(double temperature2, double humidity3) {
        if (temperature2 < 0.0) {
            humidity3 += temperature2 / 30.0;
        }
        return FastMath.clamp(humidity3, 0.0, 1.0);
    }

    public double humidity3(double x, double y) {
        double intensity = sunIntensity(y, 0.4);
        x /= SCALE;
        y /= SCALE;
        double rain = 0.8f - intensity;
        if (rain < 0.0) {
            rain *= -8.0;
        }
        return FastMath.clamp(humidityNoise
                        .noiseOctave(x / 0.5, y / 0.5, 2, 8.0, 0.5) * 0.4 + rain, 0.0,
                1.0);
    }

    public double riverHumidity(double x, double y) {
        double terrainFactor =
                terrainGenerator.generateTerrainFactorLayer(x, y);
        return (1.0 - terrainGenerator.generateRiverLayer(x, y, terrainGenerator
                .generateMountainFactorLayer(x, y, terrainFactor), 4.0)) * 0.5;
    }

    public double sunLightReduction(double x, double y) {
        return sunLightReductionD(sunElevation(x, y));
    }

    public double sunLightReductionD(double elevation) {
        return 15.0 - FastMath.clamp(elevation * 2.0 + 0.6, 0.0, 1.0) * 15.0;
    }

    public double autumnLeaves(double y) {
        double yy = y / SCALE / 4.0 % 1.0;
        if (yy < 0.0) {
            yy++;
        }
        double factor = yy % 0.5;
        if (factor > 0.25) {
            factor = 0.5 - factor;
        }
        factor = FastMath.min(factor * 5.0, 1.0);
        if (yy > 0.5) {
            return FastMath
                    .clamp(-FastMath.cosTable(season() * FastMath.TWO_PI) * 20 -
                            18, 0, 1) * factor;
        } else {
            return FastMath.clamp(-FastMath
                            .cosTable((season() + 0.5) * FastMath.TWO_PI) * 20 - 18, 0,
                    1) * factor;
        }
    }

    public double grassColorR(double temperature, double humidity) {
        if (humidity < 0.5f) {
            double v = FastMath.min(humidity * 7.4f + 0.6f, 1.0);
            return (0.8f + FastMath.clamp(0.5f - humidity, 0.0, 0.5f) * 0.4f) *
                    v;
        } else {
            return 0.8f - FastMath.clamp(humidity - 0.5f, 0.0, 0.5f) * 0.2f;
        }
    }

    public double grassColorG(double temperature, double humidity) {
        if (humidity < 0.5f) {
            double v = FastMath.min(humidity * 7.4f + 0.6f, 1.0);
            return (1.0 - FastMath.clamp(0.5f - humidity, 0.0, 0.5f) * 1.6f) *
                    v;
        } else {
            return 1.0;
        }
    }

    public double grassColorB(double temperature, double humidity) {
        double v = FastMath.min(humidity * 7.4f + 0.6f, 1.0);
        return FastMath.clamp(0.3f - temperature / 60.0, 0.0, 0.3f) * v;
    }
}
