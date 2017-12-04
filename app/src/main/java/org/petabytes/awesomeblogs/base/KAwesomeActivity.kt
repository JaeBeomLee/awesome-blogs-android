package org.petabytes.awesomeblogs.base

import android.content.Context
import org.petabytes.awesomeblogs.AwesomeBlogsApp
import org.petabytes.coordinator.Activity
import org.petabytes.coordinator.ActivityLayoutBinder
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper

/**
 * Created by leejaebeom on 2017. 11. 29..
 */
abstract class KAwesomeActivity : Activity(){

    override fun attachBaseContext(context: Context) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(context))
    }

    override fun createActivityLayoutBinder(): ActivityLayoutBinder {
        return AwesomeBlogsApp.get().activityLayoutBinder()
    }
}