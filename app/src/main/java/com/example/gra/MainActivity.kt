package com.example.gra

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val buttonPlay: Button = findViewById(R.id.buttonPlay)
        val buttonScoreboard: Button = findViewById(R.id.buttonScoreboard)
        val buttonAbout: Button = findViewById(R.id.button)

        buttonPlay.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }

        buttonScoreboard.setOnClickListener {
            val intent = Intent(this, ScoreboardActivity::class.java)
            startActivity(intent)
        }

        buttonAbout.setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }
    }
}
