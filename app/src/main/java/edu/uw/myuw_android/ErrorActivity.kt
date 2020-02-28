package edu.uw.myuw_android

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import edu.my.myuw_android.R
import kotlinx.android.synthetic.main.activity_error.*
import java.io.Serializable

const val EXTRA_ERROR_MESSAGE_HEADING = "edu.uw.myuw_andorid.ERROR_MESSAGE_HEADING"
const val EXTRA_ERROR_MESSAGE = "edu.uw.myuw_andorid.ERROR_MESSAGE"
const val EXTRA_ERROR_BUTTON_TEXT = "edu.uw.myuw_andorid.ERROR_BUTTON_TEXT"
const val EXTRA_ERROR_BUTTON_FUNC = "edu.uw.myuw_andorid.ERROR_BUTTON_FUNC"

class ErrorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_error)

        intent.let {
            errorTextHeading.text = it.getStringExtra(EXTRA_ERROR_MESSAGE_HEADING)
            errorTextView.text = it.getStringExtra(EXTRA_ERROR_MESSAGE)
            errorButton.text = it.getStringExtra(EXTRA_ERROR_BUTTON_TEXT)

            errorButton.setOnClickListener { view ->
                val serializedFunction = it.getSerializableExtra(EXTRA_ERROR_BUTTON_FUNC)
                serializedFunction?.also { func ->
                    try {
                        val onClickFunction = func as (View) -> Unit
                        onClickFunction(view)
                    }
                    catch (e: TypeCastException) {
                        Log.e("ErrorActivity - onCreate", "Failed to cast on click function: $e")
                    }
                } ?: Log.e("ErrorActivity - onCreate", "No ErrorButtonFunc was passed")
            }
        }
    }

    companion object {
        fun showError(msgHeading:String, msg: String, buttonText: String, buttonFunction: Serializable, activity: Activity) {
            val intent = Intent(activity, ErrorActivity::class.java).apply {
                putExtra(EXTRA_ERROR_MESSAGE_HEADING, msgHeading)
                putExtra(EXTRA_ERROR_MESSAGE, msg)
                putExtra(EXTRA_ERROR_BUTTON_TEXT, buttonText)
                putExtra(EXTRA_ERROR_BUTTON_FUNC, buttonFunction)
            }

            activity.startActivity(intent)
            activity.finish()
        }
    }
}
