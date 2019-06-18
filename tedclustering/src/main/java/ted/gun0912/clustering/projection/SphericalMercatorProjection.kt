/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ted.gun0912.clustering.projection

import ted.gun0912.clustering.geometry.Point
import ted.gun0912.clustering.geometry.TedLatLng


internal class SphericalMercatorProjection(private val mWorldWidth: Double) {

    fun toPoint(latLng: TedLatLng): Point {
        val x = latLng.longitude / 360 + .5
        val siny = Math.sin(Math.toRadians(latLng.latitude))
        val y = 0.5 * Math.log((1 + siny) / (1 - siny)) / -(2 * Math.PI) + .5

        return Point(x * mWorldWidth, y * mWorldWidth)
    }

    fun toLatLng(point: Point): TedLatLng {
        val x = point.x / mWorldWidth - 0.5
        val lng = x * 360

        val y = .5 - point.y / mWorldWidth
        val lat = 90 - Math.toDegrees(Math.atan(Math.exp(-y * 2.0 * Math.PI)) * 2)

        return TedLatLng(lat, lng)
    }
}
