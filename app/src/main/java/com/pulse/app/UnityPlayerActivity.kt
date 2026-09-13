package com.pulse.app

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import java.lang.reflect.Method

/**
 * Activity that hosts the Unity arena game inside Pulse.
 * Uses reflection so the app compiles without Unity SDK.
 * Once Unity is exported as an Android Library, this will work out of the box.
 */
class UnityPlayerActivity : ComponentActivity() {

    private var unityPlayer: Any? = null
    private var methodPause: Method? = null
    private var methodResume: Method? = null
    private var methodDestroy: Method? = null
    private var methodOnKeyDown: Method? = null
    private var methodOnKeyUp: Method? = null
    private var methodOnTouchEvent: Method? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = FrameLayout(this)
        setContentView(container)

        try {
            // Load UnityPlayer class via reflection
            val unityPlayerClass = Class.forName("com.unity3d.player.UnityPlayer")

            // Get constructor: UnityPlayer(Activity, UnityPlayer.UnityPlayerInitListener)
            val constructors = unityPlayerClass.constructors
            // Try the two-arg constructor first, fall back to single-arg
            val constructor = constructors.find { it.parameterCount == 2 }
                ?: constructors.find { it.parameterCount == 1 }
                ?: throw NoSuchMethodException("No suitable UnityPlayer constructor")

            unityPlayer = if (constructor.parameterCount == 2) {
                constructor.newInstance(this, null)
            } else {
                constructor.newInstance(this)
            }

            // Cache frequently called methods
            methodPause = unityPlayerClass.getMethod("pause")
            methodResume = unityPlayerClass.getMethod("resume")
            methodDestroy = unityPlayerClass.getMethod("destroy")
            methodOnKeyDown = try { unityPlayerClass.getMethod("onKeyDown", Int::class.java, KeyEvent::class.java) } catch (e: Exception) { null }
            methodOnKeyUp = try { unityPlayerClass.getMethod("onKeyUp", Int::class.java, KeyEvent::class.java) } catch (e: Exception) { null }
            methodOnTouchEvent = try { unityPlayerClass.getMethod("onTouchEvent", MotionEvent::class.java) } catch (e: Exception) { null }

            // Add Unity view to the container
            val unityView = unityPlayer as View
            container.addView(unityView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
            unityView.requestFocus()

            Log.d("UnityPlayerActivity", "Unity player initialized successfully")

            // Pass player snapshot to Unity if available
            val snapshotJson = intent.getStringExtra(EXTRA_PLAYER_SNAPSHOT)
            if (snapshotJson != null) {
                unityView.post {
                    sendToUnity("PulseBridge", "LoadPlayerSnapshot", snapshotJson)
                }
            }

        } catch (e: ClassNotFoundException) {
            Log.w("UnityPlayerActivity", "Unity SDK not found. Export Unity as Android Library first.")
            showUnityMissing(container)
        } catch (e: Exception) {
            Log.e("UnityPlayerActivity", "Failed to initialize Unity", e)
            showUnityMissing(container)
        }
    }

    private fun showUnityMissing(container: FrameLayout) {
        val textView = android.widget.TextView(this).apply {
            text = "Unity Arena not available.\n\n" +
                    "To enable:\n" +
                    "1. Open Unity Editor\n" +
                    "2. Menu: Pulse > Export Android Library\n" +
                    "3. Copy export to Pulse/unity/\n" +
                    "4. Rebuild Pulse app"
            textSize = 18f
            setPadding(48, 48, 48, 48)
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#1a1a2e"))
            gravity = android.view.Gravity.CENTER
        }
        container.addView(textView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
    }

    private fun sendToUnity(gameObjectName: String, methodName: String, message: String) {
        try {
            val unitySendMessage = Class.forName("com.unity3d.player.UnityPlayer")
                .getMethod("UnitySendMessage", String::class.java, String::class.java, String::class.java)
            unitySendMessage.invoke(null, gameObjectName, methodName, message)
        } catch (e: Exception) {
            Log.e("UnityPlayerActivity", "Failed to send message to Unity", e)
        }
    }

    override fun onDestroy() {
        try { methodDestroy?.invoke(unityPlayer) } catch (_: Exception) {}
        unityPlayer = null
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        try { methodPause?.invoke(unityPlayer) } catch (_: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        try { methodResume?.invoke(unityPlayer) } catch (_: Exception) {}
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finishFight("abandoned")
            return true
        }
        return try {
            (methodOnKeyDown?.invoke(unityPlayer, keyCode, event) as? Boolean) == true || super.onKeyDown(keyCode, event)
        } catch (_: Exception) {
            super.onKeyDown(keyCode, event)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return try {
            (methodOnKeyUp?.invoke(unityPlayer, keyCode, event) as? Boolean) == true || super.onKeyUp(keyCode, event)
        } catch (_: Exception) {
            super.onKeyUp(keyCode, event)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return try {
            (methodOnTouchEvent?.invoke(unityPlayer, event) as? Boolean) == true || super.onTouchEvent(event)
        } catch (_: Exception) {
            super.onTouchEvent(event)
        }
    }

    /**
     * Called from Unity when fight ends.
     */
    fun finishFight(resultJson: String) {
        setResult(RESULT_OK, intent.putExtra(EXTRA_FIGHT_RESULT, resultJson))
        finish()
    }

    companion object {
        const val EXTRA_PLAYER_SNAPSHOT = "player_snapshot"
        const val EXTRA_FIGHT_RESULT = "fight_result"
    }
}
