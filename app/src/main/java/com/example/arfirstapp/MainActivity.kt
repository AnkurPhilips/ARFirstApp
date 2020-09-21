package com.example.arfirstapp

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.SkeletonNode
import com.google.ar.sceneform.animation.ModelAnimator
import com.google.ar.sceneform.math.Vector3Evaluator
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private lateinit var arFragment:ArFragment
    private lateinit var modelRenderable: ModelRenderable
    private var animationCount:Int = 0
    private var animationIndex:Int = 0
    private var modelAnimator:ModelAnimator? = null
    private lateinit var skeletonNode: SkeletonNode
    private var startAnchorNode:AnchorNode? = null
    private var endAnchorNode:AnchorNode? = null
    private var xTranslation = 0.0f
    private lateinit var transformableNode:TransformableNode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupModel()

        arFragment = supportFragmentManager.findFragmentById(R.id.arFragment) as ArFragment
        arFragment.planeDiscoveryController.hide()
        arFragment.planeDiscoveryController.setInstructionView(null)
        arFragment.arSceneView.scene.addOnUpdateListener { frameTime ->
            arFragment.onUpdate(frameTime)


            // If there is no frame then don't process anything.
            if (arFragment.arSceneView.arFrame == null) {
                return@addOnUpdateListener
            }

            // If ARCore is not tracking yet, then don't process anything.
            if (arFragment.arSceneView.arFrame!!.camera
                    .trackingState != TrackingState.TRACKING
            ) {
                return@addOnUpdateListener
            }

            // Place the anchor 1m in front of the camera if anchorNode is null.
            if (startAnchorNode==null) {
                val session: Session? = arFragment.arSceneView.session
                val startPos = floatArrayOf(-0.3f, 0f, -1f)
                val endPos = floatArrayOf(0.3f, 0f, -1f)
                val widgetPos = floatArrayOf(0f, 0f, -0.5f)
                val rotation = floatArrayOf(0f, 0f, 0f, 1f)
                val startAnchor: Anchor? = session?.createAnchor(Pose(startPos, rotation))
                val endAnchor: Anchor? = session?.createAnchor(Pose(endPos, rotation))
                startAnchorNode = AnchorNode(startAnchor)
                endAnchorNode = AnchorNode(endAnchor)
                startAnchorNode?.setParent(arFragment.arSceneView.scene)
                endAnchorNode?.setParent(arFragment.arSceneView.scene)

                transformableNode = TransformableNode(arFragment.transformationSystem)
                transformableNode.scaleController.minScale = 0.05f
                transformableNode.scaleController.maxScale = 0.1f
                transformableNode.renderable = modelRenderable
                transformableNode.setParent(startAnchorNode)

                transformableNode.setOnTapListener { _, _ ->
                    textView2.visibility = View.VISIBLE
                    val mp = MediaPlayer.create(this, R.raw.tiger)
                    mp.start()
//                    val anchor = session?.createAnchor(Pose(widgetPos, floatArrayOf(0f,0f,0f,0f)))
//                    val anchorNode = AnchorNode(anchor)
//                    anchorNode.setParent(arFragment.arSceneView.scene)
//                    val node = Node()
//
//                    ModelRenderable.builder()
//                        .setSource(this,R.id.textView)
//                        .build()
//                        .thenAccept {
//                                renderable ->
//                            node.renderable = renderable
//                        }
//                        .exceptionally {
//                            it.printStackTrace()
//                            null
//                        }
//                    node.setParent(anchorNode)
                }

//                val filamentAsset = modelRenderable.

                val objectAnimator = ObjectAnimator()
                objectAnimator.setAutoCancel(true)
                objectAnimator.target = transformableNode

                objectAnimator.setObjectValues(transformableNode.worldPosition, endAnchorNode!!.worldPosition)

                objectAnimator.setPropertyName("worldPosition")
                objectAnimator.setEvaluator(Vector3Evaluator())
                objectAnimator.interpolator = LinearInterpolator()
                objectAnimator.repeatCount = ValueAnimator.INFINITE
                objectAnimator.repeatMode = ObjectAnimator.RESTART
                objectAnimator.duration = 1500
                objectAnimator.start()
            }
            else{
                if (modelRenderable!=null) {
                    modelAnimator?.let {
                        if (it.isRunning) {
                            it.end()
                        }
                    }
                    val animationData = modelRenderable.getAnimationData(animationIndex)
                    animationIndex = (animationIndex + 1) % animationCount
                    modelAnimator = ModelAnimator(animationData, modelRenderable)
                    modelAnimator?.start()
                }
            }

        }
    }

    private fun setupPlane() {
        arFragment.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane, motionEvent:MotionEvent ->
            var anchor = hitResult.createAnchor()
            var anchorNode = AnchorNode(anchor)
            anchorNode.setParent(arFragment.arSceneView.scene)
            createModel(anchorNode)
            Log.i("Scale",skeletonNode.localScale.toString())
            val scale = skeletonNode.localScale
            scale.x = (0.1f * scale.x)
            scale.y = (0.1f * scale.y)
            scale.z = (0.1f * scale.z)
//            Log.i("Plane position",skeletonNode.localPosition.toString())
            skeletonNode.localScale = scale

            for(i in 1..20) {
                Handler().postDelayed({
                    val pos = skeletonNode.worldPosition
                    Log.i("Plane position",pos.toString())
//                    pos.x = pos.x+ 0.1f
//                    pos.y = pos.y+0.1f
                    pos.z = pos.z-0.001f
                    skeletonNode.worldPosition = pos
                    anchorNode = AnchorNode(anchor)
                    skeletonNode.setParent(anchorNode)
                }, 1000)
            }
        }
    }

    private fun createModel(anchorNode: AnchorNode):SkeletonNode {
        skeletonNode = SkeletonNode()
        skeletonNode.setParent(anchorNode)
        skeletonNode.renderable = modelRenderable
//        arFragment.arSceneView.scene.addChild(skeletalNode)
        return skeletonNode
    }

    private fun setupModel() {
        ModelRenderable.builder()
            .setSource(this,R.raw.plane)
            .build()
            .thenAccept {
                renderable ->
                modelRenderable = renderable
                animationCount = modelRenderable.animationDataCount
            }
            .exceptionally {
                it.printStackTrace()
                null
            }
    }
}