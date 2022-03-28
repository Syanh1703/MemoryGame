package com.example.memorygame

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import com.example.memorygame.models.BoardSize
import com.example.memorygame.models.MemoryGame
import com.example.memorygame.utils.CHOSEN_BOARD_SIZE
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    //Create the adaptable board size
    private var boardSize: BoardSize = BoardSize.MEDIUM

    private lateinit var memoryGame: MemoryGame
    private lateinit var gameAdapter: ItemsAdapter

    companion object {
        const val ALERT_REFRESH = "Are you sure want to refresh?"
        const val ALERT_NEW_LEVEL = "Choose your level"
        const val ALERT_CREATE = "Create your own board"
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
                    showWaringDialog(ALERT_REFRESH, null, View.OnClickListener {
                        setUpGame()
                        Toast.makeText(
                            this,
                            "Your game has been successfully refreshed",
                            Toast.LENGTH_SHORT
                        ).show()
                    })
                } else {
                    setUpGame()
                    Toast.makeText(
                        this,
                        "Your game has been successfully refreshed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return true
            }
            R.id.miChooseLevel -> {
                showLevelDialog()
                return true
            }
            R.id.miCreateGame ->{
                //Create the custom game here
                showCreateDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
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
            Toast.makeText(this, "You have already won", Toast.LENGTH_SHORT).show()
            return
        }
        //Prevent double click
        if (memoryGame.isCardFacedUp(position)) {
            Toast.makeText(this, "This card is up", Toast.LENGTH_SHORT).show()
            return
        }

        //Determine what happen to the state of the game
        if (memoryGame.flipCard(position)) {
            Log.i("SyAnh", "Found a match with num of pairs: ${memoryGame.numPairsFound}")
            tvPairs.text = "Pairs: ${memoryGame.numPairsFound}/${boardSize.getGamePairs()}"
            if (memoryGame.hasWonGame()) {
                Toast.makeText(this, "Yayy, you won the game", Toast.LENGTH_SHORT).show()
            }
        }

        //Update the moves
        tvMoves.text = "Moves: ${memoryGame.getNumMoves()}"
        //Update itself
        gameAdapter.notifyDataSetChanged()
    }

    private fun setUpGame() {
        //Construct the Memory Game
        memoryGame = MemoryGame(boardSize)

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
        tvMoves.text = "Moves: 0"
        tvPairs.text = "Pairs: 0/${boardSize.getGamePairs()}"
    }

    private fun showWaringDialog(title: String, view: View?, positiveClick: View.OnClickListener) {
        val warningDialog = AlertDialog.Builder(this)
        warningDialog.setTitle(title)
        warningDialog.setView(view)
        warningDialog.setNegativeButton("Cancel", null)
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
        showWaringDialog(ALERT_NEW_LEVEL, boardSizeLevel, View.OnClickListener {
            boardSize = when (radioGroupOptions.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                R.id.rbHard -> BoardSize.HARD
                else -> BoardSize.EXTREMELY_HARD
            }
            setUpGame()
        })
    }

    private fun showCreateDialog()
    {
        //Let the user choose the size
        val boardSizeLevel = LayoutInflater.from(this).inflate(R.layout.dialog_level, null)
        val radioGroupOptions = boardSizeLevel.findViewById<RadioGroup>(R.id.radioGroupOptions)
        showWaringDialog(ALERT_CREATE, boardSizeLevel, View.OnClickListener {
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
}