package com.st10036346.wordventure2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

// **************** NEW IMPORTS FOR BIOMETRICS ****************
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
// ************************************************************

class Login : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    /**
     * TECH_WORLD, 2023.
     * SIGN IN WITH GOOGLE || FIREBASE ||ANDROID STUDIO KOTLIN TUTORIAL || STEP-BY-STEP IMPLEMENTATION
     * Available at: https://www.youtube.com/watch?v=H_maapn4Q3Q
     */
    private lateinit var googleSignInClient: GoogleSignInClient // Client for Google Sign-In

    companion object {
        private const val TAG = "Login"
    }

    // **************** NEW BIOMETRIC MEMBERS ****************
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    // *******************************************************

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
            try {
                // if Google Sign In was successful, this authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "Google sign-in success: id=${account.id}")
                // Use the ID token to authenticate with Firebase
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign In failed
                Log.w(TAG, "Google sign-in failed", e)
                Toast.makeText(this, "Google sign-in failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Log.d(TAG, "Google sign-in cancelled or failed with result code: ${result.resultCode}")
            Toast.makeText(this, "Google Sign-In Cancelled.", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val emailEditText = findViewById<EditText>(R.id.email_edit_text)
        val passwordEditText = findViewById<EditText>(R.id.password_edit_text)
        val loginButton = findViewById<Button>(R.id.login_button)
        val googleSignInButton = findViewById<SignInButton>(R.id.google_sign_in_button)

        // **************** FIND NEW BIOMETRIC BUTTON ****************
        val fingerprintLoginButton = findViewById<Button>(R.id.fingerprint_login_button)
        // ***********************************************************

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Please fill in both email and password.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "signInWithEmail:success")
                        // Navigate to MainMenu
                        handleSuccessfulLogin()
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                        Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }

        // **************** BIOMETRIC SETUP AND LISTENER ****************
        setupBiometrics()

        fingerprintLoginButton.setOnClickListener {
            checkBiometricSupportAndAuthenticate()
        }
        // **************************************************************
    }


    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }


    private fun firebaseAuthWithGoogle(idToken: String) {
        Log.d(TAG, "firebaseAuthWithGoogle start")

        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Login success navigates to Main Menu
                    Log.d(TAG, "Firebase Google Auth successful")
                    handleSuccessfulLogin()
                } else {
                    // Firebase authentication failed
                    Log.w(TAG, "Firebase Google Auth failed", task.exception)
                    Toast.makeText(this, "Firebase Authentication failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }


    private fun handleSuccessfulLogin() {
        val user = auth.currentUser
        Toast.makeText(this, "Login successful for ${user?.email}", Toast.LENGTH_SHORT).show()

        Log.i(TAG, "Navigating to MainMenu activity. Clearing activity stack.")
        val intent = Intent(this, MainMenu::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }


    // **************** NEW BIOMETRIC FUNCTIONS ****************

    private fun setupBiometrics() {
        // Use a main thread executor, which is required for the BiometricPrompt
        val executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.e(TAG, "Biometric authentication error: $errorCode ($errString)")
                    Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.d(TAG, "Biometric authentication success")
                    // On successful fingerprint login, navigate to MainMenu
                    handleSuccessfulLogin()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.w(TAG, "Biometric authentication failed")
                    Toast.makeText(applicationContext, "Authentication failed. Try again.", Toast.LENGTH_SHORT).show()
                }
            })

        // Configure the dialog box (PromptInfo)
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Fingerprint Login")
            .setSubtitle("Log in using your fingerprint")
            .setNegativeButtonText("Cancel")
            .build()
    }

    private fun checkBiometricSupportAndAuthenticate() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.d(TAG, "App can authenticate using biometrics.")
                // Biometrics are available, show the prompt
                biometricPrompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Toast.makeText(this, "No biometric hardware available.", Toast.LENGTH_LONG).show()
                Log.e(TAG, "No biometric hardware available.")
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Toast.makeText(this, "Biometric features are currently unavailable.", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Biometric features are currently unavailable.")
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(this, "No fingerprints enrolled. Please enroll a biometric credential in device settings.", Toast.LENGTH_LONG).show()
                Log.e(TAG, "No biometric credentials enrolled.")
            }
            else -> {
                Toast.makeText(this, "Biometric authentication is not possible.", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Biometric authentication is not possible (unknown error).")
            }
        }
    }
    // *******************************************************
}