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

package ted.gun0912.clustering.clustering.algo

import androidx.collection.LruCache
import ted.gun0912.clustering.clustering.Cluster
import ted.gun0912.clustering.clustering.TedClusterItem
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Optimistically fetch clusters for adjacent zoom levels, caching them as necessary.
 */
class PreCachingAlgorithmDecorator<T : TedClusterItem>(private val mAlgorithm: Algorithm<T>) :
    Algorithm<T> {

    // TODO: evaluate maxSize parameter for LruCache.
    private val mCache = LruCache<Int, Set<Cluster<T>>>(5)
    private val mCacheLock = ReentrantReadWriteLock()

    override val items: Collection<T>
        get() = mAlgorithm.items

    override var maxDistanceBetweenClusteredItems: Int
        get() = mAlgorithm.maxDistanceBetweenClusteredItems
        set(maxDistance) {
            mAlgorithm.maxDistanceBetweenClusteredItems = maxDistance
            clearCache()
        }

    override fun addItem(item: T) {
        mAlgorithm.addItem(item)
        clearCache()
    }

    override fun addItems(items: Collection<T>) {
        mAlgorithm.addItems(items)
        clearCache()
    }

    override fun clearItems() {
        mAlgorithm.clearItems()
        clearCache()
    }

    override fun removeItem(item: T) {
        mAlgorithm.removeItem(item)
        clearCache()
    }

    private fun clearCache() {
        mCache.evictAll()
    }

    override fun getClusters(zoom: Double): Set<Cluster<T>> {
        val discreteZoom = zoom.toInt()
        val results = getClustersInternal(discreteZoom)
        // TODO: Check if requests are already in-flight.
        if (mCache.get(discreteZoom + 1) == null) {
            Thread(PrecacheRunnable(discreteZoom + 1)).start()
        }
        if (mCache.get(discreteZoom - 1) == null) {
            Thread(PrecacheRunnable(discreteZoom - 1)).start()
        }
        return results
    }

    private fun getClustersInternal(discreteZoom: Int): Set<Cluster<T>> {
        var results: Set<Cluster<T>>?
        mCacheLock.readLock().lock()
        results = mCache.get(discreteZoom)
        mCacheLock.readLock().unlock()

        if (results == null) {
            mCacheLock.writeLock().lock()
            results = mCache.get(discreteZoom)
            if (results == null) {
                results = mAlgorithm.getClusters(discreteZoom.toDouble())
                mCache.put(discreteZoom, results)
            }
            mCacheLock.writeLock().unlock()
        }
        return results
    }

    private inner class PrecacheRunnable(private val mZoom: Int) : Runnable {

        override fun run() {
            try {
                // Wait between 500 - 1000 ms.
                Thread.sleep((Math.random() * 500 + 500).toLong())
            } catch (e: InterruptedException) {
                // ignore. keep going.
            }

            getClustersInternal(mZoom)
        }
    }
}
