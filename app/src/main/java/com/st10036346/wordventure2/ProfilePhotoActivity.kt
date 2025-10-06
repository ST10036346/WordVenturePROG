package com.st10036346.wordventure2

import android.content.Context
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class ProfilePhotoActivity : AppCompatActivity() {

    // SharedPreferences
    companion object {
        private const val PREFS_NAME = "AppPrefs"
        const val KEY_PROFILE_PIC_RES_ID = "profile_picture_res_id"

        val DEFAULT_PROFILE_PIC_RES_ID = R.mipmap.profile_pic_1_foreground

        /**
         * Helper function to retrieve the current profile picture resource ID from SharedPreferences.
         */

        /**
         * Kotlin Language
         * Object declarations and expressions
         * Available at: https://kotlinlang.org/docs/object-declarations.html#companion-objects
         */
        fun getProfilePicResId(context: Context): Int {
            val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return sharedPrefs.getInt(KEY_PROFILE_PIC_RES_ID, DEFAULT_PROFILE_PIC_RES_ID)
        }
    }

    // map of CardView IDs to their corresponding image IDs
    /**
     * Android Developers, 2025.
     * ImageView
     * Available at: https://developer.android.com/reference/android/widget/ImageView#setImageResource(int)
     */
    private val avatarMap = mapOf(
        R.id.AvatarCard1 to R.mipmap.profile_pic_1_foreground,
        R.id.AvatarCard2 to R.mipmap.profile_pic_3_foreground,
        R.id.AvatarCard3 to R.mipmap.profile_pic_4_foreground,
        R.id.AvatarCard4 to R.mipmap.profile_pic_5_foreground,
        R.id.AvatarCard5 to R.mipmap.profile_pic_6_foreground,
        R.id.AvatarCard6 to R.mipmap.profile_pic_2_foreground
    )

    private lateinit var selectedProfilePicture: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_pics)

        // initialise main profile picture preview ImageView
        selectedProfilePicture = findViewById(R.id.selectedProfilePicture)

        // navigate to previous screen
        findViewById<ImageView>(R.id.backIcon).setOnClickListener {
            finish()
        }

        // load the saved profile picture ID and display it
        loadSelectedProfilePic()

        // set up click listeners for all avatar options
        setupAvatarClickListeners()
    }

    /**
     * Sets up click listeners on all avatar CardViews to handle selection.
     */
    private fun setupAvatarClickListeners() {
        avatarMap.keys.forEach { cardId ->
            val avatarCard = findViewById<CardView>(cardId)

            avatarCard.setOnClickListener {
                // get the resource ID associated with the clicked CardView
                val selectedResId = avatarMap[cardId] ?: DEFAULT_PROFILE_PIC_RES_ID

                // update main preview
                updatePreview(selectedResId)

                // save new selection
                saveProfilePicId(selectedResId)
            }
        }
    }

    /**
     * Updates the main preview ImageView with the selected resource ID
     */
    private fun updatePreview(resId: Int) {
        selectedProfilePicture.setImageResource(resId)
    }

    /**
     * Saves the selected image resource ID to SharedPreferences
     */
    private fun saveProfilePicId(resId: Int) {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putInt(KEY_PROFILE_PIC_RES_ID, resId)
            apply()
        }
    }

    /**
     * Loads the saved profile picture ID from SharedPreferences and displays it in the preview.
     */
    private fun loadSelectedProfilePic() {
        val savedResId = getProfilePicResId(this)
        // check if ID is valid
        if (savedResId != 0) {
            selectedProfilePicture.setImageResource(savedResId)
        } else {
            // default
            selectedProfilePicture.setImageResource(DEFAULT_PROFILE_PIC_RES_ID)
        }
    }
}
