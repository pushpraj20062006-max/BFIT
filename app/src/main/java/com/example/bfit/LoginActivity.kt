package com.example.bfit

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.bfit.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firestore: FirebaseFirestore

    companion object {
        private const val TAG = "LoginActivity"
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { firebaseAuthWithGoogle(it) }
        } catch (e: ApiException) {
            Log.w(TAG, "Google sign in failed", e)
            Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            setLoading(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Check if user already logged in
        if (auth.currentUser != null) {
            navigateToMain()
            return
        }

        // Email/Password Login
        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                setLoading(true)
                lifecycleScope.launch {
                    try {
                        auth.signInWithEmailAndPassword(email, password).await()
                        navigateToMain()
                    } catch (e: Exception) {
                        Toast.makeText(this@LoginActivity, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        setLoading(false)
                    }
                }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        // Email/Password Sign Up
        binding.signUpButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                if (password.length < 6) {
                    Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                setLoading(true)
                lifecycleScope.launch {
                    try {
                        val result = auth.createUserWithEmailAndPassword(email, password).await()
                        result.user?.let { user ->
                            // Create initial profile in Firestore
                            val userProfile = hashMapOf(
                                "email" to email,
                                "uid" to user.uid,
                                "displayName" to (user.displayName ?: email.substringBefore("@")),
                                "height" to 0,
                                "weight" to 0,
                                "healthGoal" to "",
                                "createdAt" to com.google.firebase.Timestamp.now()
                            )
                            firestore.collection("users").document(user.uid).set(userProfile).await()
                        }
                        Toast.makeText(this@LoginActivity, "Sign up successful!", Toast.LENGTH_SHORT).show()
                        navigateToMain()
                    } catch (e: Exception) {
                        Toast.makeText(this@LoginActivity, "Sign up failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        setLoading(false)
                    }
                }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        // Google Sign-In
        binding.googleSignInButton.setOnClickListener {
            setLoading(true)
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        lifecycleScope.launch {
            try {
                val result = auth.signInWithCredential(credential).await()
                val user = result.user
                val isNewUser = result.additionalUserInfo?.isNewUser ?: false

                if (isNewUser && user != null) {
                    // Create profile in Firestore for new Google users
                    val userProfile = hashMapOf(
                        "email" to (user.email ?: ""),
                        "uid" to user.uid,
                        "displayName" to (user.displayName ?: ""),
                        "photoUrl" to (user.photoUrl?.toString() ?: ""),
                        "height" to 0,
                        "weight" to 0,
                        "healthGoal" to "",
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )
                    firestore.collection("users").document(user.uid).set(userProfile).await()
                }
                navigateToMain()
            } catch (e: Exception) {
                Log.w(TAG, "signInWithCredential:failure", e)
                Toast.makeText(this@LoginActivity, "Authentication failed: ${e.message}", Toast.LENGTH_SHORT).show()
                setLoading(false)
            }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun setLoading(isLoading: Boolean) {
        binding.loginProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.loginButton.isEnabled = !isLoading
        binding.signUpButton.isEnabled = !isLoading
        binding.googleSignInButton.isEnabled = !isLoading
    }
}
