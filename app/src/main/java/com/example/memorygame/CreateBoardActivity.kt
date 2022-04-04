package com.example.memorygame

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.example.memorygame.models.BoardSize
import com.example.memorygame.utils.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.activity_create_board.*
import java.io.ByteArrayOutputStream
import java.util.*

class CreateBoardActivity : AppCompatActivity() {

    private lateinit var intentBoardSize: BoardSize
    private var numOfRequiredImages: Int = 0
    private var listOfChoosingImages = mutableListOf<Uri>()
    private lateinit var imagerAdapter: ImagePickerAdapter

    //Variables for Firebase
    private val fbStorage = Firebase.storage
    private val fireStore = Firebase.firestore
    private val remoteConfig = Firebase.remoteConfig

    companion object {
        const val PHOTOS_REQUEST_CODE = 1703
        const val READ_PHOTO_PERMISSION = android.Manifest.permission.READ_EXTERNAL_STORAGE
        const val READ_EXTERNAL_PHOTO = 1605
        const val MIN_GAME_NAME_LENGTH = 4
        const val MAX_GAME_NAME_LENGTH = 14

        const val ENCOUNTER_ERROR = "Encounter error while saving game"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_board)

        //Get the intent data from Main
        intentBoardSize = intent.getSerializableExtra(CHOSEN_BOARD_SIZE) as BoardSize

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        //Decide the number of picture
        numOfRequiredImages = intentBoardSize.getGamePairs()
        supportActionBar?.title = "${getString(R.string.choose_pic)} (0/$numOfRequiredImages)"

        //Set the recyclerView
        rvChooseImages.setHasFixedSize(true)
        rvChooseImages.layoutManager = GridLayoutManager(this, intentBoardSize.getGameWidth())
        imagerAdapter = ImagePickerAdapter(
            this,
            listOfChoosingImages,
            intentBoardSize,
            object : ImagePickerAdapter.PickImage {
                override fun onPlaceHolderClick() {
                    //Check for the permission
                    if (isPermissionGranted(this@CreateBoardActivity, READ_PHOTO_PERMISSION)) {
                        launchPhotoIntent()
                    } else {
                        requestPermission(
                            this@CreateBoardActivity,
                            READ_PHOTO_PERMISSION,
                            READ_EXTERNAL_PHOTO
                        )
                    }
                }
            })

        //Add text watcher for the Edit Text
        etGameName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                btnSave.isEnabled = validDataToSave()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {}

        })
        rvChooseImages.adapter = imagerAdapter

        /**
         * Save game to FireBase
         */
        btnSave.setOnClickListener {
            saveGameToFireStore()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == READ_EXTERNAL_PHOTO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchPhotoIntent()
            } else {
               Toast.makeText(this, getString(R.string.permission), Toast.LENGTH_SHORT).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home)//home stands for the back button
        {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun launchPhotoIntent() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"//Take only the images
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.setAction(Intent.ACTION_GET_CONTENT)
        startActivityForResult(
            Intent.createChooser(intent, getString(R.string.choose_pic)),
            PHOTOS_REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //Check the request code first
        if (requestCode != PHOTOS_REQUEST_CODE || resultCode != RESULT_OK || data == null) {
            Log.w(ACTIVITY, "Did not get the data from the user")
            return
        }
        /**
         * Use clipData to save multiple images
         * ClipData determines the number of chosen images in one procedure
         * All the images will be officially stored in the list of chosen images
         */
        val chosenUri = data.data
        val clipData = data.clipData
        if (clipData != null) {
            Log.i(ACTIVITY, "Number of Clip Data: ${clipData.itemCount}: $clipData")
            for (i in 0 until clipData.itemCount) {
                val clipItem = clipData.getItemAt(i)
                //Add the item to the Uri
                if (listOfChoosingImages.size < numOfRequiredImages) {
                    listOfChoosingImages.add(clipItem.uri)
                }
            }
        } else if (chosenUri != null)//In case some phones do not let the user choose multiple images
        {
            listOfChoosingImages.add(chosenUri)
        }
        //Update the title of the activity by process
        supportActionBar?.title =
            "${getString(R.string.choose_pic)} (${listOfChoosingImages.size}/$numOfRequiredImages)"
        imagerAdapter.notifyDataSetChanged()
        //Enable Save Btn
        btnSave.isEnabled = validDataToSave()
    }

    private fun validDataToSave(): Boolean {
        if (listOfChoosingImages.size != numOfRequiredImages) {
            return false
        }
        if (etGameName.text.isEmpty() || etGameName.text.length !in MAX_GAME_NAME_LENGTH downTo MIN_GAME_NAME_LENGTH) {
            Toast.makeText(this, getString(R.string.letter_code), Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun saveGameToFireStore() {
        btnSave.isEnabled = false
        /**
         * When the other user create the game with the same name, everything will be overwritten
         * Check that we do not overwrite their game
         */
        val gameName = etGameName.text.toString()
        fireStore.collection("games").document(gameName).get().addOnSuccessListener {
            document -> {}
            if(document != null && document.data != null)
            {
                AlertDialog.Builder(this)
                    .setTitle("Name taken")
                    .setMessage(getString(R.string.existed_game))
                    .setPositiveButton("OK", null)
                    .show()
            }
            else{
                imageUploading(gameName)
            }
        }.addOnFailureListener {exception ->
            //Not able to retrieve the document
            Log.e(ACTIVITY, ENCOUNTER_ERROR, exception)
            Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show()
            btnSave.isEnabled = true
        }
    }

    private fun downgradeImage(imageUri: Uri): ByteArray {
        /**
         * For newer than  Pie Android Version => Use Bitmap and ImageDecoder to decode the images
         * For other versions => Use getBitmap from Media Store
         */
        val originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, imageUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
        }

        Log.i(
            ACTIVITY,
            "Original width: ${originalBitmap.width}, Original height: ${originalBitmap.height}"
        )

        val scaledBitmap = BitMapScaler.scaleHeight(originalBitmap, 250)

        Log.i(
            ACTIVITY,
            "ScaledBitmap Height: ${scaledBitmap.height}, ScaledBitmap Width: ${scaledBitmap.width}"
        )
        //Compress to JPEG
        val byteOutputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteOutputStream)
        return byteOutputStream.toByteArray()
    }

    private fun handleAllUploadedImages(name: String, imageUrl: MutableList<String>) {
        /**
         * Upload to Fire Store
         * Convert one game into one document
         */
        fireStore.collection("games").document(name)
            .set(mapOf("images" to imageUrl))
            .addOnCompleteListener { gameCreationTask ->
                pbUpload.visibility = View.GONE
                if (!gameCreationTask.isSuccessful) {
                    Log.e(ACTIVITY, "Exception with the game creation")
                    Toast.makeText(this, getString(R.string.not_create), Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }
                Log.i(ACTIVITY, "Successfully create game: $name!!")
                AlertDialog.Builder(this)
                    .setTitle("${getString(R.string.upload_done)} $name")
                    .setPositiveButton("OK")
                    { _, _ ->
                        val resultData = Intent()
                        resultData.putExtra(EXTRA_GAME_NAME, name)
                        setResult(Activity.RESULT_OK, resultData)
                        finish()
                    }.show()
            }
    }
    private fun imageUploading(customGameName :String)
    {
        var didEncounterError = false
        val uploadedUrlList = mutableListOf<String>()
        pbUpload.visibility = View.VISIBLE

        //Downscale the image
        for ((index, imageUri) in listOfChoosingImages.withIndex()) {
            val imageByteArray = downgradeImage(imageUri)
            //Define the file path
            val filePath =
                "images/$customGameName/${System.currentTimeMillis()}-$index.jpg"//Images will be saved here
            val imageReference = fbStorage.reference.child(filePath)
            imageReference.putBytes(imageByteArray)
                .continueWithTask {
                    //Do something when done uploading
                        imageUploadTask ->
                    Log.i(
                        ACTIVITY,
                        "Amount of uploaded images: ${imageUploadTask?.result.bytesTransferred}"
                    )
                    imageReference.downloadUrl
                }.addOnCompleteListener { downloadUrlTask ->
                    if (!downloadUrlTask.isSuccessful) {
                        Log.e(ACTIVITY, "Error in uploading the images", downloadUrlTask.exception)
                        Toast.makeText(this, getString(R.string.fail_upload), Toast.LENGTH_SHORT).show()
                        didEncounterError = true
                        pbUpload.visibility = View.GONE
                        return@addOnCompleteListener
                    }
                    if (didEncounterError)//Return and do nothing when images failed to upload
                    {
                        return@addOnCompleteListener
                    }
                    val downloadedUrl = downloadUrlTask.result.toString()
                    uploadedUrlList.add(downloadedUrl)
                    pbUpload.progress = (uploadedUrlList.size)*100/listOfChoosingImages.size
                    Log.i(
                        ACTIVITY,
                        "Success uploading: $listOfChoosingImages, Number of uploading: ${uploadedUrlList.size}"
                    )
                    //Check the size of the list to know how many images have been successfully uploaded
                    if (uploadedUrlList.size == listOfChoosingImages.size) {
                        handleAllUploadedImages(
                            customGameName,
                            uploadedUrlList
                        )
                    }
                }
        }
    }

    private fun toastTranslated(engString:String, vnString: String):String
    {
        var translatedToast :String = if(Locale.getDefault().displayLanguage.equals(Locale.ENGLISH)) {
            Toast.makeText(this, engString, Toast.LENGTH_SHORT).show().toString()
        } else {
            Toast.makeText(this, vnString, Toast.LENGTH_SHORT).show().toString()
        }
        return translatedToast
    }
}