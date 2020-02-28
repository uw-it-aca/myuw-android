package edu.uw.myuw_android

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import edu.my.myuw_android.R
import kotlinx.android.synthetic.main.activity_error.*

class ErrorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_error)

        intent.extras?.let {
            errorTextView.text = it.getString("ErrorMessage")
            errorButton.text = it.getString("ErrorButtonText")

            errorButton.setOnClickListener { view ->
                val serializedFunction = it.getSerializable("ErrorButtonFunc")
                serializedFunction?.also { func ->
                    try {
                        val onClickFunction = func as (View) -> Nothing
                        onClickFunction(view)
                    }
                    catch (e: TypeCastException) {
                        Log.e("ErrorActivity - onCreate", "Failed to cast on click function: $e")
                    }
                } ?: Log.e("ErrorActivity - onCreate", "No ErrorButtonFunc was passed")
            }
        }
    }
}
