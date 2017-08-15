/*
 * Copyright 2012-2017 Tobi29
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

package org.tobi29.scapes.vanilla.basics.generator

import org.tobi29.scapes.engine.utils.math.Random
import org.tobi29.scapes.engine.utils.generation.value.SimplexNoise
import org.tobi29.scapes.engine.utils.math.*

class ClimateGenerator private constructor(private val temperatureNoise: SimplexNoise,
                                           private val humidityNoise: SimplexNoise,
                                           private val weatherNoise: SimplexNoise,
                                           private val terrainGenerator: TerrainGenerator) {
    private var day: Long = 0
    private var dayTime = 0.0
    private var sunDeclination = 0.0
    private var sunDeclinationSin = 0.0
    private var sunDeclinationCos = 0.0
    private var sunHourAngleSin = 0.0

    constructor(random: Random,
                terrainGenerator: TerrainGenerator) : this(
            SimplexNoise(random.nextLong()),
            SimplexNoise(random.nextLong()),
            SimplexNoise(random.nextLong()), terrainGenerator)

    fun add(time: Double) {
        dayTime += time
        updateTime()
    }

    fun day(): Long {
        return day
    }

    fun setDay(day: Long) {
        this.day = day
        updateTime()
    }

    fun dayTime(): Double {
        return dayTime
    }

    fun setDayTime(dayTime: Double) {
        this.dayTime = dayTime
        updateTime()
    }

    private fun updateTime() {
        while (dayTime >= 1) {
            dayTime -= 1.0
            day++
        }
        val axialTilt = (-23.44).toRad()
        sunDeclination = asin(sin(axialTilt) * sin(season() * TWO_PI))
        sunDeclinationSin = sin(sunDeclination)
        sunDeclinationCos = cos(sunDeclination)
        sunHourAngleSin = sin(dayTime * TWO_PI)
    }

    fun at(day: Long,
           dayTime: Double): ClimateGenerator {
        val climateGenerator = ClimateGenerator(temperatureNoise, humidityNoise,
                weatherNoise, terrainGenerator)
        climateGenerator.day = day
        climateGenerator.dayTime = dayTime
        climateGenerator.updateTime()
        return climateGenerator
    }

    fun season(): Double {
        return (day % 50 + dayTime) / 50.0 % 1.0
    }

    fun latitude(y: Double): Double {
        var latitude = y / SCALE * PI % TWO_PI
        if (latitude < 0.0) {
            latitude += TWO_PI
        }
        return latitude
    }

    fun sunElevation(x: Double,
                     y: Double): Double {
        return sunElevationD(latitude(y))
    }

    fun sunElevationD(latitude: Double): Double {
        return sunElevationD(sunHourAngleSin, sunDeclinationSin,
                sunDeclinationCos, latitude)
    }

    fun sunElevationD(hourAngleCos: Double,
                      declination: Double,
                      latitude: Double): Double {
        return asin(sin(latitude) * sin(declination) + cos(latitude) * cos(
                declination) * hourAngleCos)
    }

    fun sunElevationD(hourAngleCos: Double,
                      declinationSin: Double,
                      declinationCos: Double,
                      latitude: Double): Double {
        return asin(sin(latitude) * declinationSin + cos(
                latitude) * declinationCos * hourAngleCos)
    }

    fun sunIntensity(y: Double,
                     factor: Double): Double {
        return sunIntensityD(latitude(y), sunDeclination * factor)
    }

    fun sunIntensityD(latitude: Double,
                      declination: Double = sunDeclination): Double {
        val declination2 = declination * 0.5
        val swap = latitude > PI
        val latitude2: Double
        if (latitude > HALF_PI && latitude < HALF_PI * 3.0) {
            latitude2 = TWO_PI - latitude
        } else {
            latitude2 = latitude
        }
        var delta = (latitude2 - declination2) / HALF_PI % 2.0 - 1.0
        if (delta < -1.0) {
            delta += 2.0
        }
        if ((delta > 0.0) xor swap && (declination2 > 0.0) xor !swap) {
            return 0.0
        }
        return abs(delta)
    }

    fun sunAzimuth(x: Double,
                   y: Double): Double {
        return sunAzimuthD(sunElevation(x, y), latitude(y))
    }

    fun sunAzimuthD(elevation: Double,
                    latitude: Double): Double {
        return sunAzimuthD(sunHourAngleSin, sunDeclination, elevation,
                latitude)
    }

    fun sunAzimuthD(hourAngleCos: Double,
                    declination: Double,
                    elevation: Double,
                    latitude: Double): Double {
        var azimuth = sin(declination) * cos(latitude)
        azimuth -= hourAngleCos * cos(declination) * sin(latitude)
        azimuth /= cos(elevation)
        azimuth = acos(azimuth)
        if (dayTime > 0.25 && dayTime < 0.75) {
            azimuth = -azimuth
        }
        return azimuth
    }

    fun weather(x: Double,
                y: Double): Double {
        val humidity3 = humidity3(x, y)
        return weatherD(x, y, humidity3)
    }

    fun weatherD(x: Double,
                 y: Double,
                 humidity3: Double): Double {
        val xx = x / SCALE
        val yy = y / SCALE
        var weather = weatherNoise.noise(day + dayTime, xx / 0.16,
                yy / 0.16) * 0.5 + 0.5
        var rainfall = 4.0 - humidity3 * 3.4
        rainfall *= 1.0 - sinTable(dayTime * PI) * 0.5
        rainfall = max(rainfall, 0.1)
        weather = pow(weather, rainfall)
        return weather
    }

    fun temperature(x: Int,
                    y: Int,
                    z: Int): Double {
        return temperature(x.toDouble(), y.toDouble()) - max(z - 300, 0) / 10.0
    }

    fun temperatureD(temperature: Double,
                     z: Int): Double {
        return temperature - max(z - 300, 0) / 10.0
    }

    fun temperature(x: Double,
                    y: Double): Double {
        val humidity3 = humidity3(x, y)
        val temperature2 = temperature2D(x, y, humidity3)
        return temperatureD(temperature2, sunLightReduction(x, y),
                weatherD(x, y, humidity3), humidity3)
    }

    fun temperatureD(temperature2: Double,
                     sunLightReduction: Double,
                     weather: Double,
                     humidity3: Double): Double {
        var temperature = temperature2
        temperature *= clamp(2.0 - weather * 2.0, 0.0, 1.0)
        temperature -= sunLightReduction * 0.8 * (1.0 - humidity3)
        return temperature
    }

    fun temperature2(x: Double,
                     y: Double): Double {
        return temperature2D(x, y, humidity3(x, y))
    }

    fun temperature2D(x: Double,
                      y: Double,
                      humidity3: Double,
                      sunIntensity: Double = sunIntensity(y,
                              0.9)): Double {
        val xx = x / SCALE
        val yy = y / SCALE
        val noiseGlobal = temperatureNoise.noise(xx, yy,
                (day + dayTime) / 100.0)
        val noiseLocal = temperatureNoise.noiseOctave(xx / 0.2, yy / 0.2,
                (day + dayTime) / 20.0, 2, 4.0,
                0.5)
        val noiseExtreme = temperatureNoise.noise(xx / 4.0, yy / 4.0,
                (day + dayTime) / 40.0)
        var temperature = mix(sunIntensity,
                sunIntensity * sunIntensity, 0.5) * 80.0 - 10.0
        temperature += noiseGlobal * 40.0
        temperature += noiseLocal * 10.0
        temperature += max(noiseExtreme - 0.3, 0.0) * 160.0
        temperature += min(0.3 - noiseExtreme, 0.0) * 160.0
        temperature -= humidity3 * 20.0
        return temperature
    }

    fun humidity(x: Int,
                 y: Int,
                 z: Int): Double {
        return humidity(x.toDouble(), y.toDouble())
    }

    fun humidity(x: Double,
                 y: Double): Double {
        val humidity3 = humidity3(x, y)
        val temperature2 = temperature2D(x, y, humidity3)
        val weather = weatherD(x, y, humidity3)
        val humidity2 = humidity2D(temperature2, humidity3)
        return humidityD(humidity2, riverHumidity(x, y), weather)
    }

    fun humidityD(humidity2: Double,
                  riverHumidity: Double,
                  weather: Double): Double {
        return clamp(humidity2 + max(weather * 2.0 - 1.0, 0.0) +
                riverHumidity, 0.0, 1.0)
    }

    fun humidity2(x: Double,
                  y: Double): Double {
        val humidity3 = humidity3(x, y)
        val temperature2 = temperature2D(x, y, humidity3)
        return humidity2D(temperature2, humidity3)
    }

    fun humidity2D(temperature2: Double,
                   humidity3: Double): Double {
        var humidity2 = humidity3
        if (temperature2 < 0.0) {
            humidity2 += temperature2 / 30.0
        }
        return clamp(humidity2, 0.0, 1.0)
    }

    fun humidity3(x: Double,
                  y: Double): Double {
        val xx = x / SCALE
        val yy = y / SCALE
        val intensity = sunIntensity(y, 0.4)
        var rain = 0.8f - intensity
        if (rain < 0.0) {
            rain *= -8.0
        }
        return clamp(humidityNoise.noiseOctave(xx / 0.5, yy / 0.5, 2, 8.0,
                0.5) * 0.4 + rain, 0.0,
                1.0)
    }

    fun riverHumidity(x: Double,
                      y: Double): Double {
        val terrainFactor = terrainGenerator.generateTerrainFactorLayer(x, y)
        return (1.0 - terrainGenerator.generateRiverLayer(x, y,
                terrainGenerator.generateMountainFactorLayer(x, y,
                        terrainFactor), 4.0)) * 0.5
    }

    fun sunLightReduction(x: Double,
                          y: Double): Double {
        return sunLightReductionD(sunElevation(x, y))
    }

    fun sunLightReductionD(elevation: Double): Double {
        return 14.0 - clamp(elevation * 1.6 + 0.4, 0.0, 1.0) * 14.0
    }

    fun autumnLeaves(y: Double): Double {
        val yy = y / SCALE / 2.0 remP 1.0
        var factor = yy % 0.5
        if (factor > 0.25) {
            factor = 0.5 - factor
        }
        factor = min(factor * 7.0, 1.0)
        if (yy > 0.5) {
            return clamp(
                    -cosTable(season() * TWO_PI) * 18.0 - 16.0,
                    0.0, 1.0) * factor
        } else {
            return clamp(-cosTable(
                    (season() + 0.5) * TWO_PI) * 18.0 - 16.0, 0.0,
                    1.0) * factor
        }
    }

    fun grassColorR(temperature: Double,
                    humidity: Double): Double {
        if (humidity < 0.5f) {
            val v = min(humidity * 7.4f + 0.6f, 1.0)
            return (0.8f + clamp(0.5f - humidity, 0.0, 0.5) * 0.4f) * v
        } else {
            return 0.8f - clamp(humidity - 0.5f, 0.0, 0.5) * 0.2f
        }
    }

    fun grassColorG(temperature: Double,
                    humidity: Double): Double {
        if (humidity < 0.5f) {
            val v = min(humidity * 7.4f + 0.6f, 1.0)
            return (1.0 - clamp(0.5f - humidity, 0.0, 0.5) * 1.6f) * v
        } else {
            return 1.0
        }
    }

    fun grassColorB(temperature: Double,
                    humidity: Double): Double {
        val v = min(humidity * 7.4f + 0.6f, 1.0)
        return clamp(0.3f - temperature / 60.0, 0.0, 0.3) * v
    }

    companion object {
        private val SCALE = 50000.0
    }
}
