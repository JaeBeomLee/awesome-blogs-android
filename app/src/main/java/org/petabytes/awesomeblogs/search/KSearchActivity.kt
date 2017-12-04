package org.petabytes.awesomeblogs.search

import android.content.Context
import android.content.Intent
import org.petabytes.awesomeblogs.R
import org.petabytes.awesomeblogs.base.AwesomeActivity
import org.petabytes.coordinator.ActivityGraph

/**
 * Created by leejaebeom on 2017. 11. 29..
 */
class KSearchActivity : AwesomeActivity(){
    override fun createActivityGraph(): ActivityGraph =
            ActivityGraph.Builder()
                    .layoutResId(R.layout.search)
                    .coordinator(R.id.container, KSearchCoordinator(this, this::finish))
                    .build()

    companion object {
        fun intent(context: Context) = Intent(context, SearchActivity::class.java)
    }


}