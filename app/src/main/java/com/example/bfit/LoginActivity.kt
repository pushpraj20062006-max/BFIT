package com.example.bfit

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.bfit.databinding.ActivityLoginBinding
import com.clerk.android.Clerk
import com.clerk.android.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var firestore: FirebaseFirestore

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()

        // Check if user already logged in with Clerk
        if (Clerk.shared.sessions.count() > 0) {
            navigateToMain()
            return
        }

        // Email/Password Login with Clerk
        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                setLoading(true)
                lifecycleScope.launch {
                    try {
                        // Using Clerk's SDK for seamless auth
                        val session = Clerk.shared.signIn(email, password)
                        if (session != null) {
                            navigateToMain()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@LoginActivity, "Clerk Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        setLoading(false)
                    }
                }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        // Email/Password Sign Up with Clerk
        binding.signUpButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                setLoading(true)
                lifecycleScope.launch {
                    try {
                        val session = Clerk.shared.signUp(email, password)
                        if (session != null) {
                            val user = Clerk.shared.currentUser
                            // Sync profile to Firestore (keeping your DB backend)
                            val userProfile = hashMapOf(
                                "email" to email,
                                "uid" to (user?.id ?: "unknown"),
                                "displayName" to (user?.firstName ?: email.substringBefore("@")),
                                "height" to 0,
                                "weight" to 0,
                                "healthGoal" to "",
                                "createdAt" to com.google.firebase.Timestamp.now()
                            )
                            user?.id?.let { uid ->
                                firestore.collection("users").document(uid).set(userProfile).await()
                            }
                            navigateToMain()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@LoginActivity, "Clerk Sign up failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        setLoading(false)
                    }
                }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        // Demo Mode (Bypass everything—ultimate alternative)
        binding.demoModeButton.setOnClickListener {
            Toast.makeText(this, "Welcome to Demo Mode!", Toast.LENGTH_SHORT).show()
            navigateToMain()
        }

        // Google Sign-In with Clerk
        binding.googleSignInButton.setOnClickListener {
            Toast.makeText(this, "Google Sign-In via Clerk is coming soon!", Toast.LENGTH_SHORT).show()
            // Clerk handles OAuth via its own managed UI or custom flow
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }

    private fun setLoading(isLoading: Boolean) {
        binding.loginProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.loginButton.isEnabled = !isLoading
        binding.signUpButton.isEnabled = !isLoading
        binding.googleSignInButton.isEnabled = !isLoading
        binding.demoModeButton.isEnabled = !isLoading
    }
}
