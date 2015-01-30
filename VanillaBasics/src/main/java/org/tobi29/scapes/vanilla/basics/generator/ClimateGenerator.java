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
    private static final double SCALE = 100000;
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

    public long getDay() {
        return day;
    }

    public void setDay(long day) {
        this.day = day;
        updateTime();
    }

    public double getDayTime() {
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
                FastMath.sin(getSeason() * FastMath.TWO_PI));
        sunHourAngleCos = FastMath.sinTable(dayTime * FastMath.TWO_PI);
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

    public double getSeason() {
        return (day % 50 + dayTime) / 50.0 % 1.0;
    }

    public double getLatitude(double y) {
        double latitude = y / SCALE * FastMath.PI % FastMath.TWO_PI;
        if (latitude < 0.0) {
            latitude += FastMath.TWO_PI;
        }
        return latitude;
    }

    public double getSunElevation(double x, double y) {
        return getSunElevationD(getLatitude(y));
    }

    public double getSunElevationD(double latitude) {
        return getSunElevationD(sunHourAngleCos, sunDeclination, latitude);
    }

    public double getSunElevationD(double hourAngleCos, double declination,
            double latitude) {
        return FastMath.asin(FastMath.sinTable(latitude) *
                FastMath.sinTable(declination) + FastMath.cosTable(latitude) *
                FastMath.cosTable(declination) *
                hourAngleCos);
    }

    public double getSunIntensity(double y, double factor) {
        return getSunIntensityD(getLatitude(y), sunDeclination * factor);
    }

    public double getSunIntensityD(double latitude) {
        return getSunIntensityD(latitude, sunDeclination);
    }

    public double getSunIntensityD(double latitude, double declination) {
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

    public double getSunAzimuth(double x, double y) {
        return getSunAzimuthD(getSunElevation(x, y), getLatitude(y));
    }

    public double getSunAzimuthD(double elevation, double latitude) {
        return getSunAzimuthD(sunHourAngleCos, sunDeclination, elevation,
                latitude);
    }

    public double getSunAzimuthD(double hourAngleCos, double declination,
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

    public double getWeather(double x, double y) {
        return getWeatherD(x, y, getHumidity3(x, y));
    }

    public double getWeatherD(double x, double y, double humidity3) {
        double weather = weatherNoise
                .noise((day + dayTime) / 2.0, x / 8000.0, y / 8000.0) * 0.5 +
                0.5;
        double a = humidity3 + weather * (1 - humidity3);
        a *= a;
        return weather * a * a;
    }

    public double getTemperature(int x, int y, int z) {
        return getTemperature(x, y) - FastMath.max(z - 300, 0) / 10.0;
    }

    public double getTemperature(double x, double y) {
        double humidity3 = getHumidity3(x, y);
        double temperature2 = getTemperature2D(x, y, humidity3);
        return getTemperatureD(temperature2, getSunLightReduction(x, y),
                getWeatherD(x, y, humidity3), humidity3);
    }

    public double getTemperatureD(double temperature2, double sunLightReduction,
            double weather, double humidity3) {
        temperature2 *= FastMath.clamp(2.0 - weather * 2.0, 0.0, 1.0);
        temperature2 -= sunLightReduction * 0.8 * (1.0 - humidity3);
        return temperature2;
    }

    public double getTemperature2(double x, double y) {
        return getTemperature2D(x, y, getHumidity3(x, y));
    }

    public double getTemperature2D(double x, double y, double humidity3) {
        return getTemperature2D(x, y, humidity3, getSunIntensity(y, 0.9));
    }

    public double getTemperature2D(double x, double y, double humidity3,
            double sunIntensity) {
        double temperature = FastMath.sqr(sunIntensity) * 70.0;
        double noiseGlobal = temperatureNoise
                .noise(x / 100000.0, y / 100000.0, (day + dayTime) / 100.0);
        double noiseLocal = temperatureNoise
                .noiseOctave(x / 20000.0, y / 20000.0, (day + dayTime) / 20.0,
                        2, 4.0, 0.5);
        double noiseExtreme = temperatureNoise
                .noise(x / 400000.0, y / 400000.0, (day + dayTime) / 40.0);
        temperature += noiseGlobal * 40.0;
        temperature += noiseLocal * 10.0;
        temperature += FastMath.max(noiseExtreme - 0.3, 0.0) * 160.0;
        temperature += FastMath.min(0.3 - noiseExtreme, 0.0) * 160.0;
        temperature -= humidity3 * 20.0;
        return temperature;
    }

    public double getHumidity(int x, int y, int z) {
        return getHumidity(x, y);
    }

    public double getHumidity(double x, double y) {
        double humidity3 = getHumidity3(x, y);
        double weather = getWeatherD(x, y, humidity3);
        double temperature2 = getTemperature2D(x, y, humidity3);
        double humidity2 = getHumidity2D(temperature2, humidity3);
        return getHumidityD(humidity2, getRiverHumidity(x, y), weather);
    }

    public double getHumidityD(double humidity2, double riverHumidity,
            double weather) {
        return FastMath
                .clamp(humidity2 + FastMath.max(weather * 2.0 - 1.0, 0.0) +
                        riverHumidity, 0.0, 1.0);
    }

    public double getHumidity2(double x, double y) {
        double humidity3 = getHumidity3(x, y);
        double temperature2 = getTemperature2D(x, y, humidity3);
        return getHumidity2D(temperature2, humidity3);
    }

    public double getHumidity2D(double temperature2, double humidity3) {
        if (temperature2 < 0.0) {
            humidity3 += temperature2 / 30.0;
        }
        return FastMath.clamp(humidity3, 0.0, 1.0);
    }

    public double getHumidity3(double x, double y) {
        double intensity = getSunIntensity(y, 0.4);
        double rain = 0.8f - intensity;
        if (rain < 0.0) {
            rain *= -8.0;
        }
        return FastMath.clamp(humidityNoise
                .noiseOctave(x / 50000.0, y / 50000.0, 2, 8.0, 0.5) * 0.4f +
                rain, 0.0, 1.0);
    }

    public double getRiverHumidity(double x, double y) {
        return 1.0 - terrainGenerator.generateRiverLayer(x, y,
                terrainGenerator.generateMountainFactorLayer(x, y), 4.0);
    }

    public double getSunLightReduction(double x, double y) {
        return getSunLightReductionD(getSunElevation(x, y));
    }

    public double getSunLightReductionD(double elevation) {
        return 15.0 - FastMath.clamp(elevation * 2.0 + 0.6, 0.0, 1.0) * 15.0;
    }

    public double getAutumnLeaves(double y) {
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
                    .clamp(-FastMath.cosTable(getSeason() * FastMath.TWO_PI) *
                            20 - 18, 0, 1) * factor;
        } else {
            return FastMath.clamp(-FastMath.cosTable(
                            (getSeason() + 0.5) * FastMath.TWO_PI) * 20 - 18, 0,
                    1) * factor;
        }
    }

    public double getGrassColorR(double temperature, double humidity) {
        if (humidity < 0.5f) {
            double v = FastMath.min(humidity * 7.4f + 0.6f, 1.0);
            return (0.8f + FastMath.clamp(0.5f - humidity, 0.0, 0.5f) * 0.4f) *
                    v;
        } else {
            return 0.8f - FastMath.clamp(humidity - 0.5f, 0.0, 0.5f) * 0.2f;
        }
    }

    public double getGrassColorG(double temperature, double humidity) {
        if (humidity < 0.5f) {
            double v = FastMath.min(humidity * 7.4f + 0.6f, 1.0);
            return (1.0 - FastMath.clamp(0.5f - humidity, 0.0, 0.5f) * 1.6f) *
                    v;
        } else {
            return 1.0;
        }
    }

    public double getGrassColorB(double temperature, double humidity) {
        double v = FastMath.min(humidity * 7.4f + 0.6f, 1.0);
        return FastMath.clamp(0.3f - temperature / 60.0, 0.0, 0.3f) * v;
    }
}
