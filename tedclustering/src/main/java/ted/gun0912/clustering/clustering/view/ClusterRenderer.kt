package ted.gun0912.clustering.clustering.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.os.*
import android.util.SparseArray
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.annotation.ColorInt
import ted.gun0912.clustering.*
import ted.gun0912.clustering.clustering.Cluster
import ted.gun0912.clustering.clustering.TedClusterItem
import ted.gun0912.clustering.clustering.ClusterManager
import ted.gun0912.clustering.geometry.Point
import ted.gun0912.clustering.geometry.TedLatLng
import ted.gun0912.clustering.geometry.TedLatLngBounds
import ted.gun0912.clustering.projection.SphericalMercatorProjection
import ted.gun0912.clustering.ui.IconGenerator
import ted.gun0912.clustering.ui.SquareTextView
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


internal class ClusterRenderer<Clustering, T : TedClusterItem, RealMarker, Marker : TedMarker<ImageDescriptor>, Map, ImageDescriptor>(
    private val builder: BaseBuilder<Clustering, T, RealMarker, Marker, Map, ImageDescriptor>,
    private val mClusterManager: ClusterManager<Clustering, T, RealMarker, Marker, Map, ImageDescriptor>
) {

    private val context: Context = builder.context
    private val tedMap: TedMap<RealMarker, Marker, ImageDescriptor> = builder.map

    private val mIconGenerator: IconGenerator
    private val mDensity: Float
    private var clusterAnimation: Boolean = builder.clusterAnimation
    private var mColoredCircleBackground: ShapeDrawable? = null

    /**
     * Markers that are currently on the tedMap.
     */
    private var mMarkers: MutableSet<MarkerWithPosition<Marker, ImageDescriptor>> =
        Collections.newSetFromMap(
            ConcurrentHashMap()
        )

    /**
     * Icons for each bucket.
     */
    private val mIcons = SparseArray<ImageDescriptor>()

    /**
     * Markers for single ClusterItems.
     */
    private val mMarkerCache = MarkerCache<T, Marker, ImageDescriptor>()

    /**
     * The currently displayed set of clusters.
     */
    private var mClusters: Set<Cluster<T>>? = null

    /**
     * Lookup between markers and the associated cluster.
     */
    private val mMarkerToCluster = HashMap<Marker, Cluster<T>>()
    private val mClusterToMarker = HashMap<Cluster<T>, Marker>()

    /**
     * The target zoom level for the current set of clusters.
     */
    private var mZoom: Double = 0.0

    private val mViewModifier = ViewModifier()

    init {
        mDensity = context.resources.displayMetrics.density
        mIconGenerator = IconGenerator(context)
        mIconGenerator.setContentView(makeSquareTextView(context))
        mIconGenerator.setTextAppearance(R.style.amu_ClusterIcon_TextAppearance)
        mIconGenerator.setBackground(makeClusterBackground())

        mClusterManager.markerMarkerCollection
            .setOnMarkerClickListener { marker ->
                if (builder.clickToCenter) {
                    tedMap.moveToCenter(marker.position)
                }
                builder.markerClickListener?.invoke(mMarkerCache[marker])
            }

        mClusterManager.clusterMarkerMarkerCollection
            .setOnMarkerClickListener { marker ->
                if (builder.clickToCenter) {
                    tedMap.moveToCenter(marker.position)
                }
                builder.clusterClickListener?.invoke(mMarkerToCluster[marker]!!)
            }
    }


    private fun makeClusterBackground(): LayerDrawable {
        mColoredCircleBackground = ShapeDrawable(OvalShape())
        val outline = ShapeDrawable(OvalShape())
        outline.paint.color = -0x7f000001 // Transparent white.
        val background = LayerDrawable(arrayOf<Drawable>(outline, mColoredCircleBackground!!))
        val strokeWidth = (mDensity * 3).toInt()
        background.setLayerInset(1, strokeWidth, strokeWidth, strokeWidth, strokeWidth)
        return background
    }

    private fun makeSquareTextView(context: Context): SquareTextView {
        val squareTextView = SquareTextView(context)
        val layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        squareTextView.layoutParams = layoutParams
        squareTextView.id = R.id.amu_text
        val twelveDpi = (12 * mDensity).toInt()
        squareTextView.setPadding(twelveDpi, twelveDpi, twelveDpi, twelveDpi)
        return squareTextView
    }

    @ColorInt
    private fun getDefaultClusterBackground(clusterSize: Int): Int {
        val hueRange = 220f
        val sizeRange = 300f
        val size = Math.min(clusterSize.toFloat(), sizeRange)
        val hue = (sizeRange - size) * (sizeRange - size) / (sizeRange * sizeRange) * hueRange
        return Color.HSVToColor(floatArrayOf(hue, 1f, .6f))
    }

    private fun getDefaultClusterText(bucket: Int): String {
        return if (bucket < builder.clusterBuckets[0]) {
            bucket.toString()
        } else "$bucket+"
    }

    /**
     * Gets the "bucket" for a particular cluster. By default, uses the number of points within the
     * cluster, bucketed to some set points.
     */
    private fun getBucket(cluster: Cluster<T>): Int {
        val buckets = builder.clusterBuckets
        val size = cluster.size
        if (size <= buckets[0]) {
            return size
        }
        for (i in 0 until buckets.size - 1) {
            if (size < buckets[i + 1]) {
                return buckets[i]
            }
        }
        return buckets[buckets.size - 1]
    }

    /**
     * ViewModifier ensures only one re-rendering of the view occurs at a time, and schedules
     * re-rendering, which is performed by the RenderTask.
     */
    @SuppressLint("HandlerLeak")
    private inner class ViewModifier : Handler() {
        private var mViewModificationInProgress = false
        private var mNextClusters: RenderTask? = null

        override fun handleMessage(msg: Message) {
            if (msg.what == TASK_FINISHED) {
                mViewModificationInProgress = false
                if (mNextClusters != null) {
                    // Run the task that was queued up.
                    sendEmptyMessage(RUN_TASK)
                }
                return
            }
            removeMessages(RUN_TASK)

            if (mViewModificationInProgress) {
                // Busy - wait for the callback.
                return
            }

            if (mNextClusters == null) {
                // Nothing to do.
                return
            }
            val renderTask: RenderTask
            synchronized(this) {
                renderTask = mNextClusters as RenderTask
                mNextClusters = null
                mViewModificationInProgress = true
            }

            renderTask.setCallback(Runnable { sendEmptyMessage(TASK_FINISHED) })
            renderTask.setMapZoom(tedMap.getCameraPosition().zoom)
            Thread(renderTask).start()
        }

        fun queue(clusters: Set<Cluster<T>>) {
            synchronized(this) {
                // Overwrite any pending cluster tasks - we don't care about intermediate states.
                mNextClusters = RenderTask(clusters)
            }
            sendEmptyMessage(RUN_TASK)
        }

        private val RUN_TASK = 0
        private val TASK_FINISHED = 1

    }

    /**
     * Determine whether the cluster should be rendered as individual markers or a cluster.
     */
    private fun shouldRenderAsCluster(cluster: Cluster<T>): Boolean {
        return cluster.size > builder.minClusterSize
    }

    /**
     * Transforms the current view (represented by ClusterRenderer.mClusters and ClusterRenderer.mZoom) to a
     * new zoom level and set of clusters.
     *
     *
     * This must be run off the UI thread. Work is coordinated in the RenderTask, then queued up to
     * be executed by a MarkerModifier.
     *
     *
     * There are three stages for the render:
     *
     *
     * 1. Markers are added to the tedMap
     *
     *
     * 2. Markers are animated to their final position
     *
     *
     * 3. Any old markers are removed from the tedMap
     *
     *
     * When zooming in, markers are animated out from the nearest existing cluster. When zooming
     * out, existing clusters are animated to the nearest new cluster.
     */
    private inner class RenderTask(internal val clusters: Set<Cluster<T>>) :
        Runnable {
        private var mCallback: Runnable? = null
        private var mSphericalMercatorProjection: SphericalMercatorProjection? = null
        private var mMapZoom: Double = 0.0

        /**
         * A callback to be run when all work has been completed.
         *
         * @param callback
         */
        fun setCallback(callback: Runnable) {
            mCallback = callback
        }

        fun setMapZoom(zoom: Double) {
            this.mMapZoom = zoom
            this.mSphericalMercatorProjection =
                SphericalMercatorProjection(256 * Math.pow(2.0, Math.min(zoom, mZoom)))
        }

        @SuppressLint("NewApi")
        override fun run() {
            if (clusters == this@ClusterRenderer.mClusters) {
                mCallback!!.run()
                return
            }

            val markerModifier = MarkerModifier()

            val zoom = mMapZoom
            val zoomingIn = zoom > mZoom
            val zoomDelta = zoom - mZoom

            val markersToRemove = mMarkers
            // Prevent crashes: https://issuetracker.google.com/issues/35827242
            val visibleBounds: TedLatLngBounds = try {
                tedMap.getVisibleLatLngBounds()
            } catch (e: Exception) {
                e.printStackTrace()
                TedLatLngBounds()
            }

            // TODO: Add some padding, so that markers can animate in from off-screen.

            // Find all of the existing clusters that are on-screen. These are candidates for
            // markers to animate from.
            var existingClustersOnScreen: MutableList<Point>? = null
            if (this@ClusterRenderer.mClusters != null && SHOULD_ANIMATE && clusterAnimation) {
                existingClustersOnScreen = ArrayList()
                for (c in this@ClusterRenderer.mClusters!!) {
                    if (shouldRenderAsCluster(c) && visibleBounds.contains(c.position)) {
                        val point = mSphericalMercatorProjection!!.toPoint(c.position)
                        existingClustersOnScreen.add(point)
                    }
                }
            }

            // Create the new markers and animate them to their new positions.
            val newMarkers = Collections.newSetFromMap(
                ConcurrentHashMap<MarkerWithPosition<Marker, ImageDescriptor>, Boolean>()
            )
            for (c in clusters) {
                val onScreen = visibleBounds.contains(c.position)
                if (zoomingIn && onScreen && SHOULD_ANIMATE && clusterAnimation) {
                    val point = mSphericalMercatorProjection!!.toPoint(c.position)
                    val closest = findClosestCluster(existingClustersOnScreen, point)
                    if (closest != null) {
                        val animateTo = mSphericalMercatorProjection!!.toLatLng(closest)
                        markerModifier.add(true, CreateMarkerTask(c, newMarkers, animateTo))
                    } else {
                        markerModifier.add(true, CreateMarkerTask(c, newMarkers, null))
                    }
                } else {
                    markerModifier.add(onScreen, CreateMarkerTask(c, newMarkers, null))
                }
            }

            // Wait for all markers to be added.
            markerModifier.waitUntilFree()

            // Don't remove any markers that were just added. This is basically anything that had
            // a hit in the MarkerCache.
            markersToRemove.removeAll(newMarkers)

            // Find all of the new clusters that were added on-screen. These are candidates for
            // markers to animate from.
            var newClustersOnScreen: MutableList<Point>? = null
            if (SHOULD_ANIMATE && clusterAnimation) {
                newClustersOnScreen = ArrayList()
                for (c in clusters) {
                    if (shouldRenderAsCluster(c) && visibleBounds.contains(c.position)) {
                        val p = mSphericalMercatorProjection!!.toPoint(c.position)
                        newClustersOnScreen.add(p)
                    }
                }
            }

            // Remove the old markers, animating them into clusters if zooming out.
            for (marker in markersToRemove) {
                val onScreen = visibleBounds.contains(marker.position)
                // Don't animate when zooming out more than 3 zoom levels.
                // TODO: drop animation based on speed of device & number of markers to animate.
                if (!zoomingIn && zoomDelta > -3 && onScreen && SHOULD_ANIMATE && clusterAnimation) {
                    val point = mSphericalMercatorProjection!!.toPoint(marker.position)
                    val closest = findClosestCluster(newClustersOnScreen, point)
                    if (closest != null) {
                        val animateTo = mSphericalMercatorProjection!!.toLatLng(closest)
                        markerModifier.animateThenRemove(marker, marker.position, animateTo)
                    } else {
                        markerModifier.remove(true, marker.tedMarker)
                    }
                } else {
                    markerModifier.remove(onScreen, marker.tedMarker)
                }
            }

            markerModifier.waitUntilFree()

            mMarkers = newMarkers
            this@ClusterRenderer.mClusters = clusters
            mZoom = zoom

            mCallback!!.run()
        }
    }

    internal fun onClustersChanged(clusters: Set<Cluster<T>>) {
        mViewModifier.queue(clusters)
    }

    private fun findClosestCluster(markers: List<Point>?, point: Point): Point? {
        if (markers == null || markers.isEmpty()) return null

        val maxDistance = mClusterManager.algorithm!!.maxDistanceBetweenClusteredItems
        var minDistSquared = (maxDistance * maxDistance).toDouble()
        var closest: Point? = null
        for (candidate in markers) {
            val dist = distanceSquared(candidate, point)
            if (dist < minDistSquared) {
                closest = candidate
                minDistSquared = dist
            }
        }
        return closest
    }

    /**
     * Handles all markerWithPosition manipulations on the tedMap. Work (such as adding, removing, or
     * animating a markerWithPosition) is performed while trying not to block the rest of the app's
     * UI.
     */
    @SuppressLint("HandlerLeak")
    private inner class MarkerModifier : Handler(Looper.getMainLooper()),
        MessageQueue.IdleHandler {

        private val lock = ReentrantLock()
        private val busyCondition = lock.newCondition()

        private val mCreateMarkerTasks = LinkedList<CreateMarkerTask>()
        private val mOnScreenCreateMarkerTasks = LinkedList<CreateMarkerTask>()
        private val mRemoveMarkerTasks = LinkedList<Marker>()
        private val mOnScreenRemoveMarkerTasks = LinkedList<Marker>()
        private val mAnimationTasks = LinkedList<AnimationTask>()

        /**
         * Whether the idle listener has been added to the UI thread's MessageQueue.
         */
        private var mListenerAdded: Boolean = false

        /**
         * @return true if there is still work to be processed.
         */
        val isBusy: Boolean
            get() {
                try {
                    lock.lock()
                    return !(mCreateMarkerTasks.isEmpty() && mOnScreenCreateMarkerTasks.isEmpty() &&
                            mOnScreenRemoveMarkerTasks.isEmpty() && mRemoveMarkerTasks.isEmpty() &&
                            mAnimationTasks.isEmpty())
                } finally {
                    lock.unlock()
                }
            }

        /**
         * Creates markers for a cluster some time in the future.
         *
         * @param priority whether this operation should have priority.
         */
        fun add(priority: Boolean, c: CreateMarkerTask) {
            lock.lock()
            sendEmptyMessage(BLANK)
            if (priority) {
                mOnScreenCreateMarkerTasks.add(c)
            } else {
                mCreateMarkerTasks.add(c)
            }
            lock.unlock()
        }

        /**
         * Removes a markerWithPosition some time in the future.
         *
         * @param priority whether this operation should have priority.
         * @param m        the markerWithPosition to remove.
         */
        fun remove(priority: Boolean, m: Marker) {
            lock.lock()
            sendEmptyMessage(BLANK)
            if (priority) {
                mOnScreenRemoveMarkerTasks.add(m)
            } else {
                mRemoveMarkerTasks.add(m)
            }
            lock.unlock()
        }

        /**
         * Animates a markerWithPosition some time in the future.
         *
         * @param marker the markerWithPosition to animate.
         * @param from   the position to animate from.
         * @param to     the position to animate to.
         */
        fun animate(
            marker: MarkerWithPosition<Marker, ImageDescriptor>,
            from: TedLatLng,
            to: TedLatLng
        ) {
            lock.lock()
            mAnimationTasks.add(AnimationTask(marker, from, to))
            lock.unlock()
        }

        /**
         * Animates a markerWithPosition some time in the future, and removes it when the animation
         * is complete.
         *
         * @param marker the markerWithPosition to animate.
         * @param from   the position to animate from.
         * @param to     the position to animate to.
         */
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        fun animateThenRemove(
            marker: MarkerWithPosition<Marker, ImageDescriptor>,
            from: TedLatLng,
            to: TedLatLng
        ) {
            lock.lock()
            val animationTask = AnimationTask(marker, from, to)
            animationTask.removeOnAnimationComplete(mClusterManager.markerManager)
            mAnimationTasks.add(animationTask)
            lock.unlock()
        }

        override fun handleMessage(msg: Message) {
            if (!mListenerAdded) {
                Looper.myQueue().addIdleHandler(this)
                mListenerAdded = true
            }
            removeMessages(BLANK)

            lock.lock()
            try {

                // Perform up to 10 tasks at once.
                // Consider only performing 10 remove tasks, not adds and animations.
                // Removes are relatively slow and are much better when batched.
                for (i in 0..9) {
                    performNextTask()
                }

                if (!isBusy) {
                    mListenerAdded = false
                    Looper.myQueue().removeIdleHandler(this)
                    // Signal any other threads that are waiting.
                    busyCondition.signalAll()
                } else {
                    // Sometimes the idle queue may not be called - schedule up some work regardless
                    // of whether the UI thread is busy or not.
                    // TODO: try to remove this.
                    sendEmptyMessageDelayed(BLANK, 10)
                }
            } finally {
                lock.unlock()
            }
        }

        /**
         * Perform the next task. Prioritise any on-screen work.
         */
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        private fun performNextTask() {
            if (!mOnScreenRemoveMarkerTasks.isEmpty()) {
                removeMarker(mOnScreenRemoveMarkerTasks.poll())
            } else if (!mAnimationTasks.isEmpty()) {
                mAnimationTasks.poll().perform()
            } else if (!mOnScreenCreateMarkerTasks.isEmpty()) {
                mOnScreenCreateMarkerTasks.poll().perform(this)
            } else if (!mCreateMarkerTasks.isEmpty()) {
                mCreateMarkerTasks.poll().perform(this)
            } else if (!mRemoveMarkerTasks.isEmpty()) {
                removeMarker(mRemoveMarkerTasks.poll())
            }
        }

        private fun removeMarker(m: Marker) {
            val cluster = mMarkerToCluster[m]
            mClusterToMarker.remove(cluster)
            mMarkerCache.remove(m)
            mMarkerToCluster.remove(m)
            mClusterManager.markerManager.remove(m)
        }

        /**
         * Blocks the calling thread until all work has been processed.
         */
        fun waitUntilFree() {
            while (isBusy) {
                // Sometimes the idle queue may not be called - schedule up some work regardless
                // of whether the UI thread is busy or not.
                // TODO: try to remove this.
                sendEmptyMessage(BLANK)
                lock.lock()
                try {
                    if (isBusy) {
                        busyCondition.await()
                    }
                } catch (e: InterruptedException) {
                    throw RuntimeException(e)
                } finally {
                    lock.unlock()
                }
            }
        }

        override fun queueIdle(): Boolean {
            // When the UI is not busy, schedule some work.
            sendEmptyMessage(BLANK)
            return true
        }

        private val BLANK = 0

    }

    /**
     * A cache of markers representing individual ClusterItems.
     */
    private class MarkerCache<T, Marker : TedMarker<ImageDescriptor>, ImageDescriptor> {
        private val mCache = HashMap<T, Marker>()
        private val mCacheReverse = HashMap<Marker, T>()

        operator fun get(item: T): Marker? {
            return mCache[item]
        }

        operator fun get(m: Marker): T {
            return mCacheReverse[m]!!
        }

        fun put(item: T, m: Marker) {
            mCache[item] = m
            mCacheReverse[m] = item
        }

        fun remove(m: Marker) {
            val item = mCacheReverse[m]
            mCacheReverse.remove(m)
            mCache.remove(item)
        }
    }

    /**
     * Called before the tedMarker for a Cluster is added to the tedMap.
     * The default implementation draws a circle with a rough count of the number of items.
     */
    private fun onBeforeClusterRendered(
        cluster: Cluster<T>,
        tedMarker: TedMarker<ImageDescriptor>
    ) {
        val bucket = getBucket(cluster)
        var imageDescriptor: ImageDescriptor? = mIcons.get(bucket)
        if (imageDescriptor == null) {
            val clusterBitmap = builder.clusterMaker?.let {
                val view = it.invoke(cluster)
                IconGenerator.makeIcon(view)
            } ?: getDefaultCluster(bucket)
            imageDescriptor = tedMarker.fromBitmap(clusterBitmap)
            mIcons.put(bucket, imageDescriptor)
        }
        // TODO: consider adding anchor(.5, .5) (Individual markers will overlap more often)
        tedMarker.setImageDescriptor(imageDescriptor!!)
    }

    private fun getDefaultCluster(bucket: Int): Bitmap {
        mColoredCircleBackground!!.paint.color =
            builder.clusterBackground?.invoke(bucket) ?: getDefaultClusterBackground(bucket)
        val clusterText = builder.clusterText?.invoke(bucket) ?: getDefaultClusterText(bucket)
        return mIconGenerator.makeIcon(clusterText)
    }

    /**
     * Get the tedMarker from a TedClusterItem
     * @param clusterItem TedClusterItem which you will obtain its tedMarker
     * @return a tedMarker from a TedClusterItem or null if it does not exists
     */
    fun getMarker(clusterItem: T): Marker? {
        return mMarkerCache[clusterItem]
    }

    /**
     * Get the TedClusterItem from a tedMarker
     * @param tedMarker which you will obtain its TedClusterItem
     * @return a TedClusterItem from a tedMarker or null if it does not exists
     */
    fun getClusterItem(tedMarker: Marker): T? {
        return mMarkerCache[tedMarker]
    }

    /**
     * Get the tedMarker from a Cluster
     * @param cluster which you will obtain its tedMarker
     * @return a tedMarker from a cluster or null if it does not exists
     */
    fun getMarker(cluster: Cluster<T>): Marker? {
        return mClusterToMarker[cluster]
    }

    /**
     * Get the Cluster from a tedMarker
     * @param tedMarker which you will obtain its Cluster
     * @return a Cluster from a tedMarker or null if it does not exists
     */
    fun getCluster(tedMarker: TedMarker<ImageDescriptor>): Cluster<T>? {
        return mMarkerToCluster[tedMarker]
    }

    /**
     * Creates markerWithPosition(s) for a particular cluster, animating it if necessary.
     */
    private inner class CreateMarkerTask
    /**
     * @param cluster      the cluster to render.
     * @param markersAdded a collection of markers to append any created markers.
     * @param animateFrom  the location to animate the markerWithPosition from, or null if no
     * animation is required.
     */
        (
        private val cluster: Cluster<T>,
        private val newMarkers: MutableSet<MarkerWithPosition<Marker, ImageDescriptor>>,
        private val animateFrom: TedLatLng?
    ) {

        fun perform(markerModifier: MarkerModifier) {
            // Don't show small clusters. Render the markers inside, instead.
            if (!shouldRenderAsCluster(cluster)) {
                for (item in cluster.items) {
                    var marker = mMarkerCache[item]
                    val markerWithPosition: MarkerWithPosition<Marker, ImageDescriptor>
                    if (marker == null) {

                        val markerOptions: Marker = builder.getMarker(item)
                        tedMap.addMarkerClickListener(markerOptions, { marker ->
                            mClusterManager.onMarkerClick(marker)
                        })
                        marker = mClusterManager.markerMarkerCollection.addMarker(markerOptions)
                        markerWithPosition = MarkerWithPosition(marker)
                        mMarkerCache.put(item, marker)
                        if (animateFrom != null) {
                            markerModifier.animate(
                                markerWithPosition,
                                animateFrom,
                                item.getTedLatLng()
                            )
                        }
                    } else {
                        markerWithPosition = MarkerWithPosition(marker)
                    }
                    builder.markerAddedListener?.invoke(item, marker)
                    newMarkers.add(markerWithPosition)
                }
                return
            }

            var marker = mClusterToMarker[cluster]
            val markerWithPosition: MarkerWithPosition<Marker, ImageDescriptor>
            if (marker == null) {
                val defaultMarker = tedMap.getMarker()
                defaultMarker.position = animateFrom ?: cluster.position
                onBeforeClusterRendered(cluster, defaultMarker)
                marker = mClusterManager.clusterMarkerMarkerCollection.addMarker(defaultMarker)
                mMarkerToCluster[marker] = cluster
                mClusterToMarker[cluster] = marker
                markerWithPosition = MarkerWithPosition(marker)
                if (animateFrom != null) {
                    markerModifier.animate(markerWithPosition, animateFrom, cluster.position)
                }
            } else {
                markerWithPosition = MarkerWithPosition(marker)
            }
            builder.clusterAddedListener?.invoke(cluster, marker)
            newMarkers.add(markerWithPosition)
            tedMap.addMarkerClickListener(marker, { marker ->
                mClusterManager.onMarkerClick(marker)
            })
        }
    }


    /**
     * A TedMarker and its position. TedMarker.getTedLatLng() must be called from the UI thread, so this
     * object allows lookup from other threads.
     */
    private class MarkerWithPosition<Marker : TedMarker<ImageDescriptor>, ImageDescriptor>(val tedMarker: Marker) {
        var position: TedLatLng = tedMarker.position

        override fun equals(other: Any?): Boolean {
            return if (other is MarkerWithPosition<*, *>) {
                tedMarker == other.tedMarker
            } else false
        }

        override fun hashCode(): Int {
            return tedMarker.hashCode()
        }
    }

    /**
     * Animates a markerWithPosition from one position to another. TODO: improve performance for
     * slow devices (e.g. Nexus S).
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private inner class AnimationTask(
        private val markerWithPosition: MarkerWithPosition<Marker, ImageDescriptor>,
        private val from: TedLatLng,
        private val to: TedLatLng
    ) : AnimatorListenerAdapter(), ValueAnimator.AnimatorUpdateListener {
        private val tedMarker: Marker = markerWithPosition.tedMarker
        private var mRemoveOnComplete: Boolean = false
        private lateinit var mMarkerManager: MarkerManager<RealMarker, Marker, ImageDescriptor>

        fun perform() {
            val valueAnimator = ValueAnimator.ofFloat(0.0f, 1.0f)
            valueAnimator.interpolator = ANIMATION_INTERPOLATOR
            valueAnimator.addUpdateListener(this)
            valueAnimator.addListener(this)
            valueAnimator.start()
        }

        override fun onAnimationEnd(animation: Animator) {
            if (mRemoveOnComplete) {
                val cluster = mMarkerToCluster[tedMarker]
                mClusterToMarker.remove(cluster)
                mMarkerCache.remove(tedMarker)
                mMarkerToCluster.remove(tedMarker)
                mMarkerManager.remove(tedMarker)
            }
            markerWithPosition.position = to
        }

        fun removeOnAnimationComplete(markerManager: MarkerManager<RealMarker, Marker, ImageDescriptor>) {
            mMarkerManager = markerManager
            mRemoveOnComplete = true
        }

        override fun onAnimationUpdate(valueAnimator: ValueAnimator) {
            val fraction = valueAnimator.animatedFraction
            val lat = (to.latitude - from.latitude) * fraction + from.latitude
            var lngDelta = to.longitude - from.longitude

            // Take the shortest path across the 180th meridian.
            if (Math.abs(lngDelta) > 180) {
                lngDelta -= Math.signum(lngDelta) * 360
            }
            val lng = lngDelta * fraction + from.longitude
            tedMarker.position = TedLatLng(lat, lng)
        }
    }

    companion object {
        private val SHOULD_ANIMATE = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB

        private fun distanceSquared(a: Point, b: Point): Double {
            return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y)
        }

        private val ANIMATION_INTERPOLATOR = DecelerateInterpolator()
    }
}
