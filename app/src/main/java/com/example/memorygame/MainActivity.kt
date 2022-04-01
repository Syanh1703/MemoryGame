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
import java.util.*

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
        const val ALERT_REFRESH = "Are you sure want to refresh?"
        const val ALERT_REFRESH_VN = "Bạn có muốn tải lại trò chơi?"

        const val ALERT_NEW_LEVEL = "Choose your level"
        const val ALERT_NEW_LEVEL_VN = "Hãy chọn cấp đội chơi"

        const val ALERT_CREATE = "Create your own board"
        const val ALERT_CREATE_VN = "Tạo trò chơi của riêng bạn"

        const val ALERT_FETCH = "Fetch Your Memory Game"
        const val ALERT_FETCH_VN = "Hãy nhập tên trò chơi tại đây"

        const val ALERT_NEGATIVE = "Cancel"
        const val ALERT_NEGATIVE_VN = "Huỷ"

        const val REFRESH_DONE = "Your game has been successfully refreshed"
        const val REFRESH_DONE_VN = "Tải lại trò chơi thành công"

        const val FIND_GAME_NOT_DONE = "We cannot find any such game"
        const val FIND_GAME_NOT_DONE_VN = "Chúng tôi không thể tìm thấy trò chơi bạn yêu cầu"

        const val HAVE_WON_GAME = "You have already won"
        const val HAVE_WON_GAME_VN = "Bạn đã chiến thắng trò chơi"

        const val IS_UP_CARD = "This card is up"
        const val IS_UP_CARD_VN = "Lá bài này đã được mở"

        const val WON_GAME = "Yayy, you won the game"
        const val WON_GAME_VN = "Chúc mừng, bạn đã chiến thắng trò chơi"

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
                    showWaringDialog(alertMessageShow(ALERT_REFRESH, ALERT_REFRESH_VN), null, View.OnClickListener {
                        setUpGame()
                        toastTranslated(REFRESH_DONE, REFRESH_DONE_VN)
                    })
                } else {
                    setUpGame()
                    toastTranslated(REFRESH_DONE, REFRESH_DONE_VN)
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
                    toastTranslated(FIND_GAME_NOT_DONE, FIND_GAME_NOT_DONE_VN)
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
                    "You are playing the custom game: $createdGameName",
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
            toastTranslated(HAVE_WON_GAME, HAVE_WON_GAME_VN)
            return
        }
        //Prevent double click
        if (memoryGame.isCardFacedUp(position)) {
            toastTranslated(IS_UP_CARD, IS_UP_CARD_VN)
            return
        }

        //Determine what happen to the state of the game
        if (memoryGame.flipCard(position)) {
            Log.i(ACTIVITY, "Found a match with num of pairs: ${memoryGame.numPairsFound}")
            movesPairsToVietnamese(memoryGame.numPairsFound, boardSize.getGamePairs())
            if (memoryGame.hasWonGame()) {
                toastTranslated(WON_GAME, WON_GAME_VN)
                CommonConfetti.rainingConfetti(clRoot, intArrayOf(Color.YELLOW, Color.BLUE,
                Color.CYAN)).oneShot()
            }
        }

        //Update the moves
        val numMoves = memoryGame.getNumMoves()
        numMovesTranslate(numMoves)
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
        movesPairsToVietnamese(0, boardSize.getGamePairs())
    }

    private fun showWaringDialog(title: String, view: View?, positiveClick: View.OnClickListener) {
        val warningDialog = AlertDialog.Builder(this)
        warningDialog.setTitle(title)
        warningDialog.setView(view)
        warningDialog.setNegativeButton(alertMessageShow(ALERT_NEGATIVE, ALERT_NEGATIVE_VN), null)
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
        showWaringDialog(alertMessageShow(ALERT_NEW_LEVEL, ALERT_NEW_LEVEL_VN), boardSizeLevel, View.OnClickListener {
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
        showWaringDialog(alertMessageShow(ALERT_CREATE, ALERT_CREATE_VN), boardSizeLevel, View.OnClickListener {
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
        showWaringDialog(alertMessageShow(ALERT_FETCH, ALERT_FETCH_VN), downloadBoard, View.OnClickListener {
            //Grab the text of the game name that user wants to download
            val downloadGameName = etDownloadGame.text.toString()
            downloadGame(downloadGameName)
        })
    }

    private fun movesPairsToVietnamese(num1 :Int, num2:Int)
    {
        if(Locale.getDefault().displayLanguage.equals(Locale.ENGLISH))
        {
            tvPairs.text = "Pairs: $num1/$num2"
            tvPairs.text = "Moves:$num1"
        }
        else
        {
            tvPairs.text = "Cặp đúng: $num1/$num2"
            tvMoves.text = "Lượt: $num1"
        }
    }

    private fun numMovesTranslate(num1:Int)
    {
        if(Locale.getDefault().displayLanguage.equals(Locale.ENGLISH))
        {
            tvPairs.text = "Moves:$num1"
        }
        else
        {
            tvMoves.text = "Lượt: $num1"
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
    
    private fun alertMessageShow(engString: String, vnString: String):String
    {
        if(Locale.getDefault().displayLanguage.equals(Locale.ENGLISH))
        {
            return engString
        }
        else
        {
            return vnString
        }
    }
}