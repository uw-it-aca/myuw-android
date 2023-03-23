package edu.uw.myuw_android

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import edu.my.myuw_android.R
import kotlinx.android.synthetic.main.activity_error.*

const val EXTRA_ERROR_MESSAGE_HEADING = "edu.uw.myuw_andorid.ERROR_MESSAGE_HEADING"
const val EXTRA_ERROR_MESSAGE = "edu.uw.myuw_andorid.ERROR_MESSAGE"
const val EXTRA_ERROR_BUTTON_TEXT = "edu.uw.myuw_andorid.ERROR_BUTTON_TEXT"
const val EXTRA_ERROR_BUTTON_FUNC = "edu.uw.myuw_andorid.ERROR_BUTTON_FUNC"

class ErrorActivity : AppCompatActivity() {
    enum class ErrorHandlerEnum {
        RETRY_LOGIN,
        RELOAD_PAGE,
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AuthStateWrapper.tryAuthServiceInit(this)

        setContentView(R.layout.activity_error)

        intent.let {
            errorTextHeading.text = it.getStringExtra(EXTRA_ERROR_MESSAGE_HEADING)
            errorTextView.text = it.getStringExtra(EXTRA_ERROR_MESSAGE)
            errorButton.text = it.getStringExtra(EXTRA_ERROR_BUTTON_TEXT)

            errorButton.setOnClickListener { _ ->
                val serializedHandler = it.getSerializableExtra(EXTRA_ERROR_BUTTON_FUNC)
                Log.d("ErrorActivity - onCreate", serializedHandler.toString())
                serializedHandler?.also { errorHandler ->
                    when (errorHandler as ErrorHandlerEnum) {
                        ErrorHandlerEnum.RETRY_LOGIN -> retryLogin()
                        ErrorHandlerEnum.RELOAD_PAGE -> reloadPage()
                    }
                } ?: Log.e("ErrorActivity - onCreate", "No ErrorButtonFunc was passed")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AuthStateWrapper.tryAuthServiceInit(this)
    }

    override fun onPause() {
        super.onPause()
        AuthStateWrapper.tryAuthServiceDispose()
    }

    // Error Handling Functions
    private fun retryLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun reloadPage() {
        retryLogin()
    }

    companion object {
        fun showError(msgHeading:String, msg: String, buttonText: String, errorHandler: ErrorHandlerEnum, activity: Activity) {
            Log.e("ErrorActivity - showError", activity.localClassName)
            Log.e("ErrorActivity - showError", msgHeading)
            Log.e("ErrorActivity - showError", msg)
            Log.e("ErrorActivity - showError", buttonText)

            val intent = Intent(activity, ErrorActivity::class.java).apply {
                putExtra(EXTRA_ERROR_MESSAGE_HEADING, msgHeading)
                putExtra(EXTRA_ERROR_MESSAGE, msg)
                putExtra(EXTRA_ERROR_BUTTON_TEXT, buttonText)
                putExtra(EXTRA_ERROR_BUTTON_FUNC, errorHandler)
            }

            activity.startActivity(intent)
            activity.finish()
        }
    }
}
