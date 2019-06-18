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

package ted.gun0912.clustering.quadtree

import ted.gun0912.clustering.geometry.Bounds
import ted.gun0912.clustering.geometry.Point

/**
 * A quad tree which tracks items with a Point geometry.
 * See http://en.wikipedia.org/wiki/Quadtree for details on the data structure.
 * This class is not thread safe.
 */
internal class PointQuadTree<T : PointQuadTree.Item> private constructor(
    /**
     * The bounds of this quad.
     */
    private val bounds: Bounds,
    /**
     * The depth of this quad in the tree.
     */
    private val depth: Int
) {
    /**
     * The elements inside this quad, if any.
     */
    private var items: MutableSet<T>? = null
    /**
     * Child quads.
     */
    private var childrenQuads: MutableList<PointQuadTree<T>>? = null

    /**
     * Creates a new quad tree with specified bounds.
     *
     * @param minX
     * @param maxX
     * @param minY
     * @param maxY
     */
    constructor(minX: Double, maxX: Double, minY: Double, maxY: Double) :
            this(Bounds(minX, maxX, minY, maxY))

    constructor(bounds: Bounds) : this(bounds, 0)

    private constructor(minX: Double, maxX: Double, minY: Double, maxY: Double, depth: Int) :
            this(Bounds(minX, maxX, minY, maxY), depth)

    fun add(item: T) {
        item.point.run {
            if (bounds.contains(x, y)) {
                insert(x, y, item)
            }
        }
    }

    private fun insert(x: Double, y: Double, item: T) {
        if (childrenQuads != null) {
            if (y < bounds.midY) {
                if (x < bounds.midX) { // top left
                    childrenQuads!![0].insert(x, y, item)
                } else { // top right
                    childrenQuads!![1].insert(x, y, item)
                }
            } else {
                if (x < bounds.midX) { // bottom left
                    childrenQuads!![2].insert(x, y, item)
                } else {
                    childrenQuads!![3].insert(x, y, item)
                }
            }
            return
        }

        items = items ?: HashSet()
        items?.let {
            it.add(item)
            if (it.size > MAX_ELEMENTS && depth < MAX_DEPTH) {
                split()
            }
        }

    }

    /**
     * Split this quad.
     */
    private fun split() {

        childrenQuads = ArrayList(4)
        childrenQuads!!.add(
            PointQuadTree(
                bounds.minX,
                bounds.midX,
                bounds.minY,
                bounds.midY,
                depth + 1
            )
        )
        childrenQuads!!.add(
            PointQuadTree(
                bounds.midX,
                bounds.maxX,
                bounds.minY,
                bounds.midY,
                depth + 1
            )
        )
        childrenQuads!!.add(
            PointQuadTree(
                bounds.minX,
                bounds.midX,
                bounds.midY,
                bounds.maxY,
                depth + 1
            )
        )
        childrenQuads!!.add(
            PointQuadTree(
                bounds.midX,
                bounds.maxX,
                bounds.midY,
                bounds.maxY,
                depth + 1
            )
        )
        items?.let {
            for (item in it) {
                insert(item.point.x, item.point.y, item)
            }
        }
        items = null
    }

    /**
     * Remove the given item from the set.
     *
     * @return whether the item was removed.
     */
    fun remove(item: T): Boolean {
        val point = item.point
        return if (this.bounds.contains(point.x, point.y)) {
            remove(point.x, point.y, item)
        } else {
            false
        }
    }

    private fun remove(x: Double, y: Double, item: T): Boolean {

        return if (this.childrenQuads != null) {
            if (y < bounds.midY) {
                if (x < bounds.midX) { // top left
                    childrenQuads!![0].remove(x, y, item)
                } else { // top right
                    childrenQuads!![1].remove(x, y, item)
                }
            } else {
                if (x < bounds.midX) { // bottom left
                    childrenQuads!![2].remove(x, y, item)
                } else {
                    childrenQuads!![3].remove(x, y, item)
                }
            }
        } else {
            if (items == null) {
                false
            } else {
                items!!.remove(item)
            }
        }
    }

    /**
     * Removes all points from the quadTree
     */
    fun clear() {
        childrenQuads = null
        items?.clear()
    }

    /**
     * Search for all items within a given bounds.
     */
    fun search(searchBounds: Bounds): Collection<T> {
        val results = ArrayList<T>()
        search(searchBounds, results)
        return results
    }

    private fun search(searchBounds: Bounds, results: MutableCollection<T>) {
        if (!bounds.intersects(searchBounds)) {
            return
        }

        if (this.childrenQuads != null) {
            for (quad in childrenQuads!!) {
                quad.search(searchBounds, results)
            }
        } else if (items != null) {
            if (searchBounds.contains(bounds)) {
                results.addAll(items!!)
            } else {
                for (item in items!!) {
                    if (searchBounds.contains(item.point)) {
                        results.add(item)
                    }
                }
            }
        }
    }

    interface Item {
        val point: Point
    }

    companion object {
        /**
         * Maximum number of elements to store in a quad before splitting.
         */
        private val MAX_ELEMENTS = 50
        /**
         * Maximum depth.
         */
        private val MAX_DEPTH = 40
    }
}
