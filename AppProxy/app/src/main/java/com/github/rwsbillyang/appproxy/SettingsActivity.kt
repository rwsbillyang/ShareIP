package com.github.rwsbillyang.appproxy


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlin.Boolean





class SettingsActivity : AppCompatActivity() {
    companion object {
        private const val TITLE_TAG = "Settings"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.activity_settings, SettingsFragment(), "preference_root")
                .commit()
        } else {
            title = savedInstanceState.getCharSequence(TITLE_TAG)
        }
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                setTitle(R.string.title_activity_settings)
            }
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharSequence(TITLE_TAG, title)
    }

    override fun onSupportNavigateUp(): Boolean {
        return if (supportFragmentManager.popBackStackImmediate()) {
            true
        } else super.onSupportNavigateUp()
    }
}

