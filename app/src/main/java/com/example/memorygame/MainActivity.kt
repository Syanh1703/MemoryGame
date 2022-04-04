package com.example.memorygame

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    //Create the adaptable board size
    private var boardSize = BoardSize.EASY

    private lateinit var memoryGame: MemoryGame
    private lateinit var gameAdapter: ItemsAdapter

    //Vairables for FireStore
    private val fireStore = Firebase.firestore
    private var gameName: String? = null
    private var customGameImages: List<String>? = null

    companion object {
        const val REQUEST_CODE = 1804
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setUpGame()
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
                    showWaringDialog(getString(R.string.refreshGame), null, View.OnClickListener {
                        setUpGame()
                        Toast.makeText(this, getString(R.string.refresh_done), Toast.LENGTH_SHORT)
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
        //Request game from the FireStore to Main by downloading
        fireStore.collection("games").document(createdGameName).get()
            .addOnSuccessListener { document ->
                val userImageList =
                    document.toObject(UserImageList::class.java)//Take the images instance in the class
                if (userImageList?.images == null) {
                    //Wrong
                    Log.e(ACTIVITY, "Invalid custom game from the FireStore")
                    Toast.makeText(this, getString(R.string.not_find_game), Toast.LENGTH_SHORT)
                        .show()
                    return@addOnSuccessListener
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
                    "${getString(R.string.custom_game)}: $createdGameName",
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
            Toast.makeText(this, getString(R.string.have_won_game), Toast.LENGTH_SHORT).show()
            return
        }
        //Prevent double click
        if (memoryGame.isCardFacedUp(position)) {
            Toast.makeText(this, getString(R.string.is_up_card), Toast.LENGTH_SHORT).show()
            return
        }

        //Determine what happen to the state of the game
        if (memoryGame.flipCard(position)) {
            Log.i(ACTIVITY, "Found a match with num of pairs: ${memoryGame.numPairsFound}")
            tvPairs.text =
                "${getString(R.string.pair)}: ${memoryGame.numPairsFound}/${boardSize.getGamePairs()}"
            tvMoves.text = "${R.string.moves}:0"
            if (memoryGame.hasWonGame()) {
                Toast.makeText(this, getString(R.string.win_game), Toast.LENGTH_SHORT).show()
                CommonConfetti.rainingConfetti(
                    clRoot, intArrayOf(
                        Color.YELLOW, Color.BLUE,
                        Color.CYAN
                    )
                ).oneShot()
            }
        }

        //Update the moves
        val numMoves = memoryGame.getNumMoves()
        tvMoves.text = "${getString(R.string.moves)}:$numMoves"
        //Update itself
        gameAdapter.notifyDataSetChanged()
    }

    private fun setUpGame() {
        //Construct the Memory Game
        memoryGame = MemoryGame(boardSize, customGameImages)
        supportActionBar!!.title = gameName ?: getString(R.string.app_name)
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
        tvPairs.text = "${getString(R.string.pair)}:0/${boardSize.getGamePairs()}"
        tvMoves.text = "${getString(R.string.moves)}:0"
    }

    private fun showWaringDialog(title: String, view: View?, positiveClick: View.OnClickListener) {
        val warningDialog = AlertDialog.Builder(this)
        warningDialog.setTitle(title)
        warningDialog.setView(view)
        warningDialog.setNegativeButton(getString(R.string.cancel), null)
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
        showWaringDialog(getString(R.string.chooseLevel), boardSizeLevel, View.OnClickListener {
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
        //Let the user choose the size
        val boardSizeLevel = LayoutInflater.from(this).inflate(R.layout.dialog_level, null)
        val radioGroupOptions = boardSizeLevel.findViewById<RadioGroup>(R.id.radioGroupOptions)
        showWaringDialog(getString(R.string.create_board), boardSizeLevel, View.OnClickListener {
            val chosenSize = when (radioGroupOptions.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                R.id.rbHard -> BoardSize.HARD
                else -> BoardSize.EXTREMELY_HARD
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
        showWaringDialog(getString(R.string.fetch), downloadBoard, View.OnClickListener {
            //Grab the text of the game name that user wants to download
            val downloadGameName = etDownloadGame.text.toString()
            downloadGame(downloadGameName)
        })
    }

}