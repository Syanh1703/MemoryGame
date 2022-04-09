package com.example.memorygame

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.facebook.*
import com.facebook.BuildConfig
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_facebook_log_in.*
import kotlin.math.sign

class FacebookLogInActivity : AppCompatActivity() {

    private lateinit var firebaseAuth : FirebaseAuth
    private lateinit var callbackManager : CallbackManager

    private var fbId :String? = null
    private var fbFirstName :String? = null
    private var fbMiddleName:String? = null
    private var fbLastName:String? = null
    private var fbEmail:String? = null

    companion object{
        const val FB_ACTIVITY = "Facebook Log In Activity"
        var fbName :String? = null

        fun userLogOut()
        {
            LoginManager.getInstance().logOut()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_facebook_log_in)

        //Check Logged In
        if(isLoggedIn())
        {
            Log.i(FB_ACTIVITY, "Already Logged In")
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        else
        {
            Log.i(FB_ACTIVITY, "Not logged in")
        }

        firebaseAuth = FirebaseAuth.getInstance()
        callbackManager = CallbackManager.Factory.create()
        btnContOnFB.setOnClickListener {
            LoginManager.getInstance().logInWithReadPermissions(this, listOf("public_profile", "email"))
        }
        signIn()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //Facebook
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun signIn()
    {
        btnContOnFB.registerCallback(callbackManager,object : FacebookCallback<LoginResult> {
            override fun onCancel() {
                Toast.makeText(this@FacebookLogInActivity, "Login Cancelled", Toast.LENGTH_SHORT).show()
            }

            override fun onError(error: FacebookException) {
                Toast.makeText(this@FacebookLogInActivity, error.message, Toast.LENGTH_SHORT).show()
            }

            override fun onSuccess(result: LoginResult) {
                handleFacebookAccess(result.accessToken)
                getUserProfile(result?.accessToken, result?.accessToken?.userId)
            }
        })
    }

    private fun handleFacebookAccess(accessToken: AccessToken?)
    {
        /**
         * Get Credential
         */
        val cred = FacebookAuthProvider.getCredential(accessToken!!.token)
        firebaseAuth.signInWithCredential(cred)
            .addOnFailureListener {e ->
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }
            .addOnSuccessListener { success ->
                //Get User Email
                val email:String? = success.user!!.email
                //Toast.makeText(this, "Log In Successfully with email: $email", Toast.LENGTH_SHORT).show()
                Log.i(FB_ACTIVITY,"Welcome $email")
            }
    }

    @SuppressLint("LongLogTag")
    fun getUserProfile(token: AccessToken?, userId:String)
    {
        val params = Bundle()
        params.putString(
            "fields","id, first_name, middle_name, last_name, name, picture, email"
        )
        /**
         * Use Graph Request
         */
        GraphRequest(token, "/$userId/", params, HttpMethod.GET,
            GraphRequest.Callback {
                    response -> val jSonObject = response.jsonObject

                //Can't see by using Log
                if(BuildConfig.DEBUG)//Turn on Debug Mode
                {
                    FacebookSdk.setIsDebugEnabled(true)
                    FacebookSdk.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS)
                }

                //Facebook Id
                if(jSonObject!!.has("id"))
                {
                    fbId = jSonObject.getString("id")
                    Log.i(FB_ACTIVITY, "Facebook ID is: $fbId")
                }
                else
                {
                    Log.i(FB_ACTIVITY, "Facebook ID not existed")
                }

                //Facebook First Name
                if(jSonObject!!.has("first_name"))
                {
                    fbFirstName = jSonObject.getString("first_name")
                    Log.i(FB_ACTIVITY, "First Name is: $fbFirstName")
                }
                else
                {
                    Log.i(FB_ACTIVITY, "First Name is not existed")
                }

                //Facebook Middle Name
                if(jSonObject!!.has("middle_name"))
                {
                    fbMiddleName = jSonObject.getString("middle_name")
                    Log.i(FB_ACTIVITY, "Middle Name is: $fbMiddleName")
                }
                else
                {
                    Log.i(FB_ACTIVITY, "Middle Name is not existed")
                }

                //Facebook Last Name
                if(jSonObject!!.has("last_name"))
                {
                    fbLastName = jSonObject.getString("last_name")
                    Log.i(FB_ACTIVITY, "Last Name is: $fbLastName")
                }
                else
                {
                    Log.i(FB_ACTIVITY, "Last Name is not existed")
                }

                //Facebook Name
                if(jSonObject!!.has("name"))
                {
                    fbName = jSonObject.getString("name")
                    Log.i(FB_ACTIVITY, "Name is: $fbName")
                }
                else
                {
                    Log.i(FB_ACTIVITY, "Name is not existed")
                }

                //Facebook Picture URL
                if(jSonObject!!.has("picture"))
                {
                    val fbPicObject = jSonObject.getJSONObject("picture")
                    if(fbPicObject.has("data"))
                    {
                        val fbDataObject = fbPicObject.getJSONObject("data")
                        if(fbDataObject.has("url"))
                        {
                            val fbProfileURL = fbDataObject.getString("url")
                            Log.i(FB_ACTIVITY, "Facebook Profile Picture URL: $fbProfileURL")
                        }
                    }
                }
                else
                {
                    Log.i(FB_ACTIVITY, "Facebook Profile Picture URL no existed")
                }

                //Facebook Email
                if(jSonObject.has("email"))
                {
                    fbEmail = jSonObject.getString("email")
                    Log.i(FB_ACTIVITY, "Facebook Email is: $fbEmail")
                }
                else
                {
                    Log.i(FB_ACTIVITY, "Facebook Email not existed")
                }

                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("Name", fbName)
                Log.e(FB_ACTIVITY, "Name: $fbName")
                startActivity(intent)

            }).executeAsync()
    }

    private fun isLoggedIn(): Boolean {
        val presentToken = AccessToken.getCurrentAccessToken()
        return presentToken != null && !presentToken.isExpired
    }
}