package com.st10036346.wordventure2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.st10036346.wordventure2.LocaleManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class Register : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Variable to hold the selected language code from the Spinner
    private var selectedLanguageCode: String = LocaleManager.DEFAULT_LANG

    // Language codes corresponding to the order in R.array.language_options
    private val languageCodes = listOf("en", "af", "zu", "xh")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = Firebase.auth
        db = Firebase.firestore

        val usernameEditText = findViewById<EditText>(R.id.username_edit_text)
        val emailEditText = findViewById<EditText>(R.id.email_edit_text)
        val passwordEditText = findViewById<EditText>(R.id.password_edit_text)
        val signupButton = findViewById<Button>(R.id.signup_button)
        val loginLink = findViewById<TextView>(R.id.login_link)
        val languageSpinner = findViewById<Spinner>(R.id.language_spinner)

        // Set up Spinner selection listener to capture the chosen language code
        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Map the spinner position to the corresponding language code
                selectedLanguageCode = languageCodes.getOrElse(position) { LocaleManager.DEFAULT_LANG }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedLanguageCode = LocaleManager.DEFAULT_LANG
            }
        }

        signupButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val username = usernameEditText.text.toString()

            if (email.isBlank() || password.isBlank() || username.isBlank()) {
                // Use localized string resource
                Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { authTask ->
                    if (authTask.isSuccessful) {
                        // SUCCESS: Save the user's preferred language code persistently
                        LocaleManager.saveLanguage(this, selectedLanguageCode)

                        val user = auth.currentUser
                        user?.let {
                            val userId = it.uid
                            val userData = hashMapOf(
                                "username" to username,
                                "email" to email,
                                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                            )

                            // 1. ATTEMPT TO SAVE DATA TO FIRESTORE
                            db.collection("users").document(userId)
                                .set(userData)
                                .addOnSuccessListener {
                                    Log.d("Firestore", "User data added successfully.")
                                }
                                .addOnFailureListener { e ->
                                    Log.w("Firestore", "Error writing document, continuing navigation.", e)
                                }

                            // 2. IMMEDIATE REDIRECTION TO MainActivity AFTER SUCCESSFUL FIREBASE AUTH
                            // Use localized string resource
                            Toast.makeText(this, R.string.registration_success, Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        // Registration failed
                        Toast.makeText(this, "Registration failed: ${authTask.exception?.message}", Toast.LENGTH_LONG).show()
                        Log.e("Firebase", "createUserWithEmail:failure", authTask.exception)
                    }
                }
        }

        loginLink.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }
    }
}
