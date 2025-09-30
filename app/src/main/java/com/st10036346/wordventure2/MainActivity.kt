package com.st10036346.wordventure2


import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This links the Kotlin code to your XML layout file (activity_main.xml)
        setContentView(R.layout.activity_main)

        // 1. Get references to the buttons using their IDs from the XML
        val registerButton: Button = findViewById(R.id.register_button)
        val loginButton: Button = findViewById(R.id.login_button)

        // 2. Set the click listener for the Register button
        registerButton.setOnClickListener {
            // Create an Intent to start the RegisterActivity
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }

        // 3. Set the click listener for the Login button
        loginButton.setOnClickListener {
            // Create an Intent to start the LoginActivity
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }
    }
}