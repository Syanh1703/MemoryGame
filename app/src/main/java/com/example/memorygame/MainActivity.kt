package com.example.memorygame

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import com.example.memorygame.models.BoardSize
import com.example.memorygame.models.MemoryGame
import com.example.memorygame.utils.ACTIVITY
import com.example.memorygame.utils.CHOSEN_BOARD_SIZE
import com.example.memorygame.utils.EXTRA_GAME_NAME
import com.facebook.login.LoginManager
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class MainActivity : AppCompatActivity() {

    //Create the adaptable board size
    private var boardSize = BoardSize.EASY

    private lateinit var memoryGame: MemoryGame
    private lateinit var gameAdapter: ItemsAdapter

    //Vairables for FireStore
    private val fireStore = Firebase.firestore
    private var gameName: String? = null
    private var customGameImages: List<String>? = null
    private val remoteConfig = Firebase.remoteConfig
    private val firebaseAnalytics = Firebase.analytics

    //Audio effect
    private var mediaPlayer :MediaPlayer? = null

    companion object {
        const val REQUEST_CODE = 1804
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        facebookKeyHash()

        remoteConfig.setDefaultsAsync(mapOf("about_link" to "https://github.com/Syanh1703/MemoryGame", "scale_height" to 250L, "scale_width" to 60L))
        remoteConfig.fetchAndActivate().addOnCompleteListener {
            task -> if(task.isSuccessful)
        {
                Log.i(ACTIVITY, "Fetch/activate succeeded, did config get updated? ${task.result}")
        }
            else
        {
            Log.w(ACTIVITY, "Fetch failed")
        }
        }
        setUpGame()

        if(FacebookLogInActivity.fbName != null)
        {
            Toast.makeText(this, "Welcome : ${FacebookLogInActivity.fbName}", Toast.LENGTH_SHORT).show()
        }
        else
        {
            Toast.makeText(this, "Welcome", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.refresh_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.miRefresh -> {
                //Reset the game
                if (memoryGame.getNumMoves() > 0 && !memoryGame.hasWonGame()) {
                    showWaringDialog(stringConvert(R.string.refresh_game), null, View.OnClickListener {
                        setUpGame()
                        Toast.makeText(this, stringConvert(R.string.refresh_done), Toast.LENGTH_SHORT)
                            .show()
                    })
                } else {
                    setUpGame()
                }
                return true
            }
            R.id.miChooseLevel -> {
                showLevelDialog()
                return true
            }
            R.id.miCreateGame -> {
                //Create the custom game here
                showCreateDialog()
                return true
            }
            R.id.miPlayCustomGame -> {
                //Play the created game by user
                showGameToDownloadDialog()
                return true
            }
            R.id.miAbout -> {
                //Give information about the app
                firebaseAnalytics.logEvent("open_about_link", null)
                val aboutLink = remoteConfig.getString("about_link")
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(aboutLink)))
                return true
            }
            R.id.miFBLogOut -> {
                FacebookLogInActivity.userLogOut()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val customGamaName = data?.getStringExtra(EXTRA_GAME_NAME)
            if (customGamaName == null) {
                //Something went wrong
                Log.e(ACTIVITY, "Something went wrong")
                return
            }
            //Nothing went wrong
            downloadGame(customGamaName)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun downloadGame(createdGameName: String) {
        firebaseAnalytics.logEvent("download_game_attempt") {
            param("game_name", createdGameName)
        }
        //Request game from the FireStore to Main by downloading
        fireStore.collection("games").document(createdGameName).get()
            .addOnSuccessListener { document ->
                val userImageList =
                    document.toObject(UserImageList::class.java)//Take the images instance in the class
                if (userImageList?.images == null) {
                    //Wrong
                    Log.e(ACTIVITY, "Invalid custom game from the FireStore")
                    Toast.makeText(this, stringConvert(R.string.not_find_game), Toast.LENGTH_SHORT)
                        .show()
                    return@addOnSuccessListener
                }
                firebaseAnalytics.logEvent("download_game_success") {
                    param("game_name", createdGameName)
                }
                //If success, re set up the game
                val numCards =
                    (userImageList.images.size * 2) //Find out the board size by doubling the images
                boardSize = BoardSize.getCardsByValue(numCards)
                customGameImages = userImageList.images //Get the images

                //Fetch the images to Picasso
                for (url in userImageList.images) {
                    Picasso.get().load(url).placeholder(R.drawable.ic_cover_image).fetch()
                }

                Snackbar.make(
                    clRoot,
                    "${stringConvert(R.string.custom_game)}: $createdGameName",
                    Snackbar.LENGTH_SHORT
                ).show()
                gameName = createdGameName
                setUpGame()
            }.addOnFailureListener { exception ->
                Log.e(ACTIVITY, "Error in loading  starting game", exception)
            }
    }

    private fun updateGameWithFilliped(position: Int) {
        /**
         * Error during checking
         * User double click the card => Prevent that happen
         * User wins the game
         */
        //User wins the game
        if (memoryGame.hasWonGame()) {
            //Show a Snackbar
            Toast.makeText(this, stringConvert(R.string.have_won_game), Toast.LENGTH_SHORT).show()
            return
        }
        //Prevent double click
        if (memoryGame.isCardFacedUp(position)) {
            Toast.makeText(this, stringConvert(R.string.is_up_card), Toast.LENGTH_SHORT).show()
            return
        }

        //Determine what happen to the state of the game
        if (memoryGame.flipCard(position)) {
            Log.i(ACTIVITY, "Found a match with num of pairs: ${memoryGame.numPairsFound}")
            tvPairs.text =
                "${getString(R.string.pair)}: ${memoryGame.numPairsFound}/${boardSize.getGamePairs()}"
            tvMoves.text = "${R.string.moves}:0"
            if (memoryGame.hasWonGame()) {
                Toast.makeText(this, stringConvert(R.string.win_game), Toast.LENGTH_SHORT).show()
                //Play the sound
                playMusic(R.raw.yayyy)
                CommonConfetti.rainingConfetti(
                    clRoot, intArrayOf(
                        Color.YELLOW, Color.BLUE,
                        Color.CYAN
                    )
                ).oneShot()
                firebaseAnalytics.logEvent("won_game") {
                    param("game_name", gameName ?: "[default]")
                    param("board_size", boardSize.name)
                }
            }
        }

        //Update the moves
        val numMoves = memoryGame.getNumMoves()
        tvMoves.text = "${stringConvert(R.string.moves)}:$numMoves"
        //Update itself
        gameAdapter.notifyDataSetChanged()
    }

    private fun setUpGame() {
        //Construct the Memory Game
        memoryGame = MemoryGame(boardSize, customGameImages)
        supportActionBar!!.title = gameName ?: stringConvert(R.string.app_name)
        //Set up the Recycler View
        rvBoard.setHasFixedSize(true)
        gameAdapter = ItemsAdapter(
            this,
            boardSize,
            memoryGame.cards,
            object : ItemsAdapter.CardClickListener {
                override fun onCardClick(position: Int) {
                    //Add logic for toggling card
                    updateGameWithFilliped(position)
                }
            })
        rvBoard.adapter = gameAdapter
        rvBoard.layoutManager = GridLayoutManager(this, boardSize.getGameWidth())
        //Span count stands for the number of columns
        tvPairs.text = "${stringConvert(R.string.pair)}:0/${boardSize.getGamePairs()}"
        tvMoves.text = "${stringConvert(R.string.moves)}:0"
    }

    private fun showWaringDialog(title: String, view: View?, positiveClick: View.OnClickListener) {
        val warningDialog = AlertDialog.Builder(this)
        warningDialog.setTitle(title)
        warningDialog.setView(view)
        warningDialog.setNegativeButton(stringConvert(R.string.cancel), null)
        warningDialog.setPositiveButton("OK")
        { _, _ ->
            positiveClick.onClick(null)
        }.show()
    }

    private fun showLevelDialog() {
        //Create a new View to choose the level
        val boardSizeLevel = LayoutInflater.from(this).inflate(R.layout.dialog_level, null)
        val radioGroupOptions = boardSizeLevel.findViewById<RadioGroup>(R.id.radioGroupOptions)
        //Set the current board size is selected by default
        when (boardSize) {
            BoardSize.EASY -> radioGroupOptions.check(R.id.rbEasy)
            BoardSize.MEDIUM -> radioGroupOptions.check(R.id.rbMedium)
            BoardSize.HARD -> radioGroupOptions.check(R.id.rbHard)
            BoardSize.EXTREMELY_HARD -> radioGroupOptions.check(R.id.rbSuperHard)
        }
        showWaringDialog(stringConvert(R.string.chooseLevel), boardSizeLevel, View.OnClickListener {
            boardSize = when (radioGroupOptions.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                R.id.rbHard -> BoardSize.HARD
                else -> BoardSize.EXTREMELY_HARD
            }
            /**
             * Reset all the value to avoid overwrite
             */
            gameName = null
            customGameImages = null
            setUpGame()
        })
    }

    private fun showCreateDialog() {
        firebaseAnalytics.logEvent("create_dialog", null)
        //Let the user choose the size
        val boardSizeLevel = LayoutInflater.from(this).inflate(R.layout.dialog_level, null)
        val radioGroupOptions = boardSizeLevel.findViewById<RadioGroup>(R.id.radioGroupOptions)
        showWaringDialog(stringConvert(R.string.create_board), boardSizeLevel, View.OnClickListener {
            val chosenSize = when (radioGroupOptions.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                R.id.rbHard -> BoardSize.HARD
                else -> BoardSize.EXTREMELY_HARD
            }
            firebaseAnalytics.logEvent("create_start)activity") {
                param("board_size", chosenSize.name)
            }
            //Navigate the user to the new activity
            val intent = Intent(this, CreateBoardActivity::class.java)
            intent.putExtra(CHOSEN_BOARD_SIZE, chosenSize)
            startActivityForResult(intent, REQUEST_CODE)

        })
    }

    private fun showGameToDownloadDialog() {
        val downloadBoard = LayoutInflater.from(this).inflate(R.layout.download_board, null)
        val etDownloadGame = downloadBoard.findViewById<EditText>(R.id.etDownloadGame)
        showWaringDialog(stringConvert(R.string.fetch), downloadBoard, View.OnClickListener {
            //Grab the text of the game name that user wants to download
            val downloadGameName = etDownloadGame.text.toString()
            downloadGame(downloadGameName)
        })
    }

    private fun playMusic(mp3:Int)
    {
        //Play the sound
        mediaPlayer = MediaPlayer.create(this, mp3)
        Toast.makeText(this, "The music is playing", Toast.LENGTH_SHORT).show()
        mediaPlayer!!.start()
        mediaPlayer!!.setVolume(100F,100F)
        mediaPlayer!!.pause()
    }

    private fun facebookKeyHash()
    {
        try{
            val info = packageManager.getPackageInfo("com.example.memorygame", PackageManager.GET_SIGNATURES)
            for(signature in info.signatures)
            {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                Log.e(ACTIVITY, Base64.encodeToString(md.digest(), Base64.DEFAULT))
            }
        }
        catch (e: PackageManager.NameNotFoundException)
        {

        }
        catch (e: NoSuchAlgorithmException)
        {

        }
    }

    private fun stringConvert(string:Int):String
    {
        return getString(string)
    }
}