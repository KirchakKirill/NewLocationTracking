package com.example.newlocationtracking.presentation

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.example.newlocationtracking.R
import com.example.newlocationtracking.domain.models.ARPlacementModel
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberOnGestureListener

private data class ZoneAnchor(
    val placementId: Long,
    val anchor: Anchor
)

private class LastFrameHolder(var frame: Frame? = null)

@Composable
fun ARZoneScreen(
    zoneId: Long,
    placements: List<ARPlacementModel>,
    onPlaceCube: (ARPlacementModel) -> Unit,
    onExitZone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lastFrame = remember { LastFrameHolder() }
    val anchors = remember { mutableStateListOf<ZoneAnchor>() }
    val currentSession = remember { mutableStateOf<Session?>(null) }
    val horizontalPlaneDetected = remember { mutableStateOf(false) }
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val cubeMaterial = remember(materialLoader) {
        materialLoader.createColorInstance(Color.Yellow)
    }

    LaunchedEffect(currentSession.value, placements) {
        val session = currentSession.value ?: return@LaunchedEffect
        val placementIds = placements.mapTo(mutableSetOf()) { it.id }

        anchors.filter { it.placementId !in placementIds }.forEach { zoneAnchor ->
            zoneAnchor.anchor.detach()
        }
        anchors.removeAll { it.placementId !in placementIds }

        while (true) {
            val cameraTracking = runCatching {
                lastFrame.frame?.camera?.trackingState == TrackingState.TRACKING
            }.getOrDefault(false)

            if (cameraTracking) {
                var retryNeeded = false
                placements.forEach { placement ->
                    if (anchors.none { it.placementId == placement.id }) {
                        try {
                            val pose = Pose(
                                floatArrayOf(placement.x, placement.y, placement.z),
                                floatArrayOf(placement.qx, placement.qy, placement.qz, placement.qw)
                            )
                            anchors += ZoneAnchor(placement.id, session.createAnchor(pose))
                        } catch (_: Exception) {
                            retryNeeded = true
                        }
                    }
                }

                if (!retryNeeded) break
            }

            delay(250L)
        }
    }

    DisposableEffect(Unit) {
        onDispose { anchors.forEach { it.anchor.detach() } }
    }

    Box(modifier = modifier.fillMaxSize()) {
        ARSceneView(
            modifier = Modifier.fillMaxSize(),
            planeRenderer = true,
            engine = engine,
            materialLoader = materialLoader,
            onSessionUpdated = { session, frame ->
                lastFrame.frame = frame
                if (currentSession.value !== session) {
                    anchors.forEach { it.anchor.detach() }
                    anchors.clear()
                    currentSession.value = session
                }

                val foundPlane = session.getAllTrackables(Plane::class.java).any { plane ->
                    plane.trackingState == TrackingState.TRACKING &&
                        plane.type == Plane.Type.HORIZONTAL_UPWARD_FACING
                }
                if (horizontalPlaneDetected.value != foundPlane) {
                    horizontalPlaneDetected.value = foundPlane
                }
            },
            onGestureListener = rememberOnGestureListener(
                onSingleTapConfirmed = { event, _ ->
                    findHorizontalPlaneHit(lastFrame.frame, event)?.let { hit ->
                        val pose = hit.hitPose
                        onPlaceCube(
                            ARPlacementModel(
                                poiId = zoneId,
                                x = pose.tx(),
                                y = pose.ty(),
                                z = pose.tz(),
                                qx = pose.qx(),
                                qy = pose.qy(),
                                qz = pose.qz(),
                                qw = pose.qw()
                            )
                        )
                    }
                }
            )
        ) {
            anchors.forEach { zoneAnchor ->
                androidx.compose.runtime.key(zoneAnchor.placementId) {
                    AnchorNode(zoneAnchor.anchor) {
                        CubeNode(
                            size = Float3(0.1f, 0.1f, 0.1f),
                            apply = { isEditable = false },
                            materialInstance = cubeMaterial
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.zone_title, zoneId),
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Button(onClick = onExitZone) {
                    Text(stringResource(R.string.exit_zone))
                }
            }
            if (!horizontalPlaneDetected.value) {
                Text(
                    text = stringResource(R.string.ar_surface_prompt),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun CameraPermissionRequired(onRequestPermission: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.camera_permission_required),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRequestPermission) {
                Text(stringResource(R.string.allow_camera))
            }
        }
    }
}

private fun findHorizontalPlaneHit(frame: Frame?, event: MotionEvent): HitResult? =
    frame?.hitTest(event.x, event.y)?.firstOrNull { hit ->
        hit.isValid { it.trackable is Plane } &&
            (hit.trackable as? Plane)?.let { plane ->
                plane.trackingState == TrackingState.TRACKING &&
                    plane.type == Plane.Type.HORIZONTAL_UPWARD_FACING &&
                    plane.isPoseInPolygon(hit.hitPose)
            } == true
    }
