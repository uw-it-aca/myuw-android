package edu.uw.myuw_android

import android.app.SearchManager
import android.content.AsyncQueryHandler
import android.content.Intent
import android.database.Cursor
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.BaseColumns
import android.util.Log
import edu.my.myuw_android.R
import java.lang.ref.WeakReference

class SearchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        if (Intent.ACTION_SEARCH == intent.action) {
            intent.getStringExtra(SearchManager.QUERY)?.also { query ->
                updateWithQuery(query)
            }
        }
    }

    private fun updateWithQuery(query: String) {
        Log.d("SearchActivity - updateWithQuery", "query: $query")
    }
}
