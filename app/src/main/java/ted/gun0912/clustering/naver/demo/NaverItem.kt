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

package ted.gun0912.clustering.naver.demo

import com.naver.maps.geometry.LatLng
import ted.gun0912.clustering.naver.TedNaverClusterItem

data class NaverItem(var position: LatLng) : TedNaverClusterItem() {

    override fun getLatLng(): LatLng {
        return position
    }

    /**
     * Set the title of the marker
     * @param title string to be set as title
     */
    var title: String? = null
    /**
     * Set the description of the marker
     * @param snippet string to be set as snippet
     */
    var snippet: String? = null

    constructor(lat: Double, lng: Double) : this(LatLng(lat, lng)) {
        title = null
        snippet = null
    }

    constructor(lat: Double, lng: Double, title: String?, snippet: String?) : this(
        LatLng(lat, lng)
    ) {
        this.title = title
        this.snippet = snippet
    }
}
