/*
 * Copyright 2016 Google Inc.
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

package ted.gun0912.clustering.clustering.algo

import ted.gun0912.clustering.clustering.TedClusterItem
import ted.gun0912.clustering.geometry.TedCameraPosition


/**
 *
 * This algorithm uses map position for clustering, and should be reclustered on map movement
 * @param <T>
</T> */

interface ScreenBasedAlgorithm<T : TedClusterItem> : Algorithm<T> {

    fun shouldReClusterOnMapMovement(): Boolean

    fun onCameraChange(tedCameraPosition: TedCameraPosition)
}
