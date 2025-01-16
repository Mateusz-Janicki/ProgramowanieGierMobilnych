package com.example.gra

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class GameOverActivity : AppCompatActivity() {

    private lateinit var finalScoreTextView: TextView
    private lateinit var nicknameEditText: EditText
    private lateinit var submitButton: Button
    private lateinit var playAgainButton: Button
    private lateinit var menuButton: Button

    private var finalScore = 0
    private var submitted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_over)

        finalScoreTextView = findViewById(R.id.finalScoreTextView)
        nicknameEditText = findViewById(R.id.nicknameEditText)
        submitButton = findViewById(R.id.submitButton)
        playAgainButton = findViewById(R.id.playAgainButton)
        menuButton = findViewById(R.id.menuButton)

        finalScore = intent.getIntExtra("FINAL_SCORE", 0)
        finalScoreTextView.text = "Your Score: $finalScore"

        submitButton.setOnClickListener {
            if (!submitted) {
                val nickname = nicknameEditText.text.toString()
                if (nickname.isNotEmpty()) {
                    submitScoreToFirebase(nickname, finalScore)
                    submitButton.isEnabled = false
                    submitted = true
                } else {
                    Toast.makeText(this, "Enter a nickname", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Already submitted!", Toast.LENGTH_SHORT).show()
            }
        }

        playAgainButton.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
            finish()
        }

        menuButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun submitScoreToFirebase(nickname: String, score: Int) {
        val database = FirebaseDatabase.getInstance("https://game-6d27f-default-rtdb.europe-west1.firebasedatabase.app/")
        val scoresRef = database.getReference("scores")

        val scoreData = ScoreData(nickname, score)

        scoresRef.push().setValue(scoreData)
            .addOnSuccessListener {
                Toast.makeText(this, "Score submitted!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    data class ScoreData(
        val nickname: String = "",
        val score: Int = 0
    )
}
