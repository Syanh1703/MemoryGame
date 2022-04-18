package com.example.memorygame

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import com.facebook.*
import com.facebook.BuildConfig
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_social_sign_in.*

class SocialLogInActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var callbackManager: CallbackManager
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var lastSignedGoogle: GoogleSignInAccount

    private var fbId: String? = null
    private var fbFirstName: String? = null
    private var fbMiddleName: String? = null
    private var fbLastName: String? = null
    private var fbEmail: String? = null

    private var userEmail :String = ""
    private var userPass :String = ""

    companion object {
        const val SOCIAL_ACTIVITY = "SocialLogInActivity"
        var fbName: String? = null
        const val GG_CODE = 1804
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_social_sign_in)

        //Check Logged In By Facebook

        /**
         * Check logged In By google, block Comment the Facebook
         */

        /*if (isLoggedInByFacebook()) {
            Log.i(SOCIAL_ACTIVITY, "Already Logged In by Facebook")
            intentToMain()
            finish()
        } else {
            Log.i(SOCIAL_ACTIVITY, "Not logged in")
        }*/

        //firebaseAuth = FirebaseAuth.getInstance()
        firebaseAuth = Firebase.auth
        callbackManager = CallbackManager.Factory.create()
        /*btnContOnFB.setOnClickListener {
            LoginManager.getInstance()
                .logInWithReadPermissions(this, listOf("public_profile", "email"))
        }
        signInWithFacebook()*/

        //Google
        //isLoggedInByGoogle()
        btnGoogle.setSize(SignInButton.SIZE_STANDARD)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        btnGoogle.setOnClickListener {
            val signingIntent = googleSignInClient.signInIntent
            startActivityForResult(signingIntent, GG_CODE)
        }

        checkUser()
        //Normal Log In
        btnLogIn.setOnClickListener {
            validateLogIn()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //Facebook
        callbackManager.onActivityResult(requestCode, resultCode, data)

        //Google
        if (requestCode == GG_CODE) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val acc = task.getResult(ApiException::class.java)
                Toast.makeText(
                    this,
                    "${stringCovert(R.string.welcome_user)} ${acc.displayName}",
                    Toast.LENGTH_SHORT
                ).show()
                googleAccountToFirebase(acc.idToken!!)
            } catch (e: ApiException) {
                Log.w(SOCIAL_ACTIVITY, "Login by Google Account failed. Code: ${e.statusCode}")
            }
        }
    }

    override fun onStart() {
        super.onStart()

        val user = firebaseAuth.currentUser
        updateUIGoogle(user)
    }

    private fun googleAccountToFirebase(account: String) {
        val googleCred = GoogleAuthProvider.getCredential(account, null)
        firebaseAuth.signInWithCredential(googleCred)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(SOCIAL_ACTIVITY, "Sign in success")
                    val user = firebaseAuth.currentUser
                    updateUIGoogle(user)
                } else {
                    Toast.makeText(this, "Sign in failed", Toast.LENGTH_SHORT).show()
                    Log.d(SOCIAL_ACTIVITY, "Sign In failed")
                    updateUIGoogle(null)
                }
            }

    }

    /*private fun signInWithFacebook() {
        btnContOnFB.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onCancel() {
                Toast.makeText(
                    this@SocialLogInActivity,
                    stringCovert(R.string.login_cancel),
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onError(error: FacebookException) {
                Toast.makeText(this@SocialLogInActivity, error.message, Toast.LENGTH_SHORT).show()
            }

            override fun onSuccess(result: LoginResult) {
                handleFacebookAccess(result.accessToken)
                getUserProfile(result.accessToken, result.accessToken.userId)
            }
        })
    }*/

    private fun handleFacebookAccess(accessToken: AccessToken?) {
        /**
         * Get Credential
         */
        val cred = FacebookAuthProvider.getCredential(accessToken!!.token)
        firebaseAuth.signInWithCredential(cred)
            .addOnFailureListener { e ->
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }
            .addOnSuccessListener { success ->
                //Get User Email
                val email: String? = success.user!!.email
                //Toast.makeText(this, "Log In Successfully with email: $email", Toast.LENGTH_SHORT).show()
                Log.i(SOCIAL_ACTIVITY, "Welcome $email")
            }
    }

    @SuppressLint("LongLogTag")
    fun getUserProfile(token: AccessToken?, userId: String) {
        val params = Bundle()
        params.putString(
            "fields", "id, first_name, middle_name, last_name, name, picture, email"
        )
        /**
         * Use Graph Request
         */
        GraphRequest(token, "/$userId/", params, HttpMethod.GET,
            GraphRequest.Callback { response ->
                val jSonObject = response.jsonObject

                //Can't see by using Log
                if (BuildConfig.DEBUG)//Turn on Debug Mode
                {
                    FacebookSdk.setIsDebugEnabled(true)
                    FacebookSdk.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS)
                }

                //Facebook Id
                if (jSonObject!!.has("id")) {
                    fbId = jSonObject.getString("id")
                    Log.i(SOCIAL_ACTIVITY, "Facebook ID is: $fbId")
                } else {
                    Log.i(SOCIAL_ACTIVITY, "Facebook ID not existed")
                }

                //Facebook First Name
                if (jSonObject!!.has("first_name")) {
                    fbFirstName = jSonObject.getString("first_name")
                    Log.i(SOCIAL_ACTIVITY, "First Name is: $fbFirstName")
                } else {
                    Log.i(SOCIAL_ACTIVITY, "First Name is not existed")
                }

                //Facebook Middle Name
                if (jSonObject!!.has("middle_name")) {
                    fbMiddleName = jSonObject.getString("middle_name")
                    Log.i(SOCIAL_ACTIVITY, "Middle Name is: $fbMiddleName")
                } else {
                    Log.i(SOCIAL_ACTIVITY, "Middle Name is not existed")
                }

                //Facebook Last Name
                if (jSonObject!!.has("last_name")) {
                    fbLastName = jSonObject.getString("last_name")
                    Log.i(SOCIAL_ACTIVITY, "Last Name is: $fbLastName")
                } else {
                    Log.i(SOCIAL_ACTIVITY, "Last Name is not existed")
                }

                //Facebook Name
                if (jSonObject!!.has("name")) {
                    fbName = jSonObject.getString("name")
                    Log.i(SOCIAL_ACTIVITY, "Name is: $fbName")
                } else {
                    Log.i(SOCIAL_ACTIVITY, "Name is not existed")
                }

                //Facebook Picture URL
                if (jSonObject!!.has("picture")) {
                    val fbPicObject = jSonObject.getJSONObject("picture")
                    if (fbPicObject.has("data")) {
                        val fbDataObject = fbPicObject.getJSONObject("data")
                        if (fbDataObject.has("url")) {
                            val fbProfileURL = fbDataObject.getString("url")
                            Log.i(SOCIAL_ACTIVITY, "Facebook Profile Picture URL: $fbProfileURL")
                        }
                    }
                } else {
                    Log.i(SOCIAL_ACTIVITY, "Facebook Profile Picture URL no existed")
                }

                //Facebook Email
                if (jSonObject.has("email")) {
                    fbEmail = jSonObject.getString("email")
                    Log.i(SOCIAL_ACTIVITY, "Facebook Email is: $fbEmail")
                } else {
                    Log.i(SOCIAL_ACTIVITY, "Facebook Email not existed")
                }

                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("Name", fbName)
                Log.e(SOCIAL_ACTIVITY, "Name: $fbName")
                startActivity(intent)

            }).executeAsync()
    }

    private fun isLoggedInByFacebook(): Boolean {
        val presentToken = AccessToken.getCurrentAccessToken()
        return presentToken != null && !presentToken.isExpired
    }

    private fun isLoggedInByGoogle() {
        val userGoogle = firebaseAuth.currentUser
        if (userGoogle != null) {
            //Logged In
            intentToMain()
            Log.i(SOCIAL_ACTIVITY, "Already Logged In")
        }
    }

    private fun stringCovert(string: Int): String {
        return getString(string)
    }

    private fun updateUIGoogle(user: FirebaseUser?) {
        //Intent to Main Activity
        if (user == null) {
            Log.w(SOCIAL_ACTIVITY, "Null user")
            return
        }
        intentToMain()
    }

    private fun intentToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun checkUser()
    {
        val firebaseUser = firebaseAuth.currentUser
        if(firebaseUser!= null)
        {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun validateLogIn()
    {
        userEmail = etUserName.text.toString().trim()
        userPass = etPassword.text.toString().trim()

        if(!Patterns.EMAIL_ADDRESS.matcher(userEmail).matches())
        {
            //Invalid Email
            etUserName.error = getString(R.string.invalid_email)
        }
        else if(TextUtils.isEmpty(userPass)&&userPass.length<8)
        {
            etPassword.error = getString(R.string.incorrect_pass)
            Toast.makeText(this, "Password must be at least 8 letters", Toast.LENGTH_SHORT).show()
        }
        else
        {
            //Valid data
            logInToFirebase()
        }
    }

    private fun logInToFirebase()
    {
        firebaseAuth.signInWithEmailAndPassword(userEmail, userPass)
            .addOnSuccessListener {
                val firebaseUser = firebaseAuth.currentUser
                val name = firebaseUser!!.displayName
                Toast.makeText(this, "${getString(R.string.log_in_success)}, welcome $name", Toast.LENGTH_SHORT).show()
                intentToMain()
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.login_failed_google), Toast.LENGTH_SHORT).show()
            }
    }

    private fun signUpToFirebase()
    {
        //Sign Up first
        firebaseAuth.createUserWithEmailAndPassword(userEmail,userPass)
            .addOnSuccessListener {
                logInToFirebase()
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.log_in_failed), Toast.LENGTH_SHORT).show()
            }
    }
}
