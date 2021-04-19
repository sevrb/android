package mega.privacy.android.app.meeting.fragments

import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_local_camera_call.*
import mega.privacy.android.app.databinding.IndividualCallFragmentBinding
import mega.privacy.android.app.databinding.SelfFeedFloatingWindowFragmentBinding
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaChatRoom


class IndividualCallFragment : MeetingBaseFragment() {

    private var chatId: Long? = null
    private var clientId: Long? = null
    private var chat: MegaChatRoom? = null
    private var isFloatingWindow = false

    private lateinit var surfaceHolder: SurfaceHolder

    var videoAlpha = 255

    var released = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            chatId = it.getLong(Constants.CHAT_ID)
            clientId = it.getLong(Constants.CLIENT_ID)
            isFloatingWindow = it.getBoolean(Constants.IS_FLOATING_WINDOW)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return if (isFloatingWindow)
            SelfFeedFloatingWindowFragmentBinding.inflate(inflater, container, false).root
        else
            IndividualCallFragmentBinding.inflate(inflater, container, false).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        surfaceHolder = video.holder

        if (isFloatingWindow) {
            meetingActivity.bottomFloatingPanelViewHolder.propertyUpdaters.add {
                view.alpha = 1 - it
            }

            meetingActivity.bottomFloatingPanelViewHolder.propertyUpdaters.add {
                videoAlpha = ((1 - it) * 255).toInt()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Start drawing here, so that after a lock screen, the drawing can resume.
        if (isFloatingWindow) {
            handleFloatingWindow()
        }
    }

    private fun handleFloatingWindow() {
        // Set background of SurfaceView as transparent after drew a round corner rect.
        video.setZOrderOnTop(true)
        surfaceHolder.setFormat(PixelFormat.TRANSLUCENT)

        // TODO test start
        surfaceHolder.addCallback(object : SurfaceHolder.Callback {

            override fun surfaceCreated(holder: SurfaceHolder?) {
                released = false

                Thread{
                    while (!released) {
                        Thread.sleep(100)
                        drawFrame()
                    }
                }.start()
            }

            override fun surfaceChanged(
                holder: SurfaceHolder?,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder?) { released = true}

        })
        // TODO test code end
    }

    // TODO test start
    private fun drawFrame() {
        val w = video.width.toFloat()
        val h = video.height.toFloat()
        val rectF = RectF(0f, 0f, w, h)

        val canvas = surfaceHolder.lockCanvas()

        canvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        val paint = Paint()
        paint.apply {
            color = Color.parseColor("#ABCDEF")
            alpha = videoAlpha
        }

        canvas?.drawRoundRect(
            rectF,
            Util.dp2px(16f, outMetrics).toFloat(),
            Util.dp2px(16f, outMetrics).toFloat(),
            paint
        )

        if(!released) {
            surfaceHolder.unlockCanvasAndPost(canvas)
        }
    }
    // TODO test code end

    companion object {

        const val TAG = "IndividualCallFragment"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param chatId Parameter 1.
         * @param clientId Parameter 2.
         * @return A new instance of fragment MeetingFragment.
         */
        @JvmStatic
        fun newInstance(chatId: Long, clientId: Long, isFloatingWindow: Boolean) =
            IndividualCallFragment().apply {
                arguments = Bundle().apply {
                    putLong(Constants.CHAT_ID, chatId)
                    putLong(Constants.CLIENT_ID, clientId)
                    putBoolean(Constants.IS_FLOATING_WINDOW, isFloatingWindow)
                }
            }
    }
}