package com.st10036346.wordventure2

import android.content.Context
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.st10036346.wordventure2.R

class ProfilePhotoActivity : AppCompatActivity() {

    // Define SharedPreferences constants
    companion object {
        private const val PREFS_NAME = "AppPrefs"
        const val KEY_PROFILE_PIC_RES_ID = "profile_picture_res_id"

        // Resource IDs (R.mipmap.xxx) are not compile-time constants, so 'const' is removed.
        val DEFAULT_PROFILE_PIC_RES_ID = R.mipmap.profile_pic_1_foreground

        /**
         * Helper function to retrieve the current profile picture resource ID from SharedPreferences.
         */
        fun getProfilePicResId(context: Context): Int {
            val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return sharedPrefs.getInt(KEY_PROFILE_PIC_RES_ID, DEFAULT_PROFILE_PIC_RES_ID)
        }
    }

    // Map of CardView IDs to their corresponding *ImageView* IDs
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

        // Initialize the main profile picture preview ImageView
        selectedProfilePicture = findViewById(R.id.selectedProfilePicture)

        // Set up the back button
        findViewById<ImageView>(R.id.backIcon).setOnClickListener {
            // Navigate back to the previous activity (ProfileActivity)
            finish()
        }

        // Load the currently saved profile picture ID and display it
        loadSelectedProfilePic()

        // Set up click listeners for all avatar options
        setupAvatarClickListeners()
    }

    /**
     * Sets up click listeners on all avatar CardViews to handle selection.
     */
    private fun setupAvatarClickListeners() {
        avatarMap.keys.forEach { cardId ->
            val avatarCard = findViewById<CardView>(cardId)

            avatarCard.setOnClickListener {
                // 1. Get the resource ID associated with the clicked CardView
                val selectedResId = avatarMap[cardId] ?: DEFAULT_PROFILE_PIC_RES_ID

                // 2. Update the main preview
                updatePreview(selectedResId)

                // 3. Save the new selection
                saveProfilePicId(selectedResId)
            }
        }
    }

    /**
     * Updates the main preview ImageView with the selected resource ID.
     */
    private fun updatePreview(resId: Int) {
        selectedProfilePicture.setImageResource(resId)
    }

    /**
     * Saves the selected image resource ID to SharedPreferences.
     */
    private fun saveProfilePicId(resId: Int) {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putInt(KEY_PROFILE_PIC_RES_ID, resId)
            apply() // Use apply() for asynchronous write
        }
    }

    /**
     * Loads the saved profile picture ID from SharedPreferences and displays it in the preview.
     */
    private fun loadSelectedProfilePic() {
        val savedResId = getProfilePicResId(this)
        // Check if the resource ID is valid before setting the image
        if (savedResId != 0) {
            selectedProfilePicture.setImageResource(savedResId)
        } else {
            // Fallback to default
            selectedProfilePicture.setImageResource(DEFAULT_PROFILE_PIC_RES_ID)
        }
    }
}
