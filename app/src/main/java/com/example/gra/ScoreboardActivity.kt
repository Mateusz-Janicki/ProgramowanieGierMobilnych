package com.example.gra

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ScoreboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scoreboard)

        val listView: ListView = findViewById(R.id.listViewScores)
        val scoresList = mutableListOf<String>()
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, scoresList)
        listView.adapter = adapter

        val database = FirebaseDatabase.getInstance("https://game-6d27f-default-rtdb.europe-west1.firebasedatabase.app/")
        val scoresRef = database.getReference("scores")

        scoresRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val allScores = snapshot.children.mapNotNull { it.getValue(ScoreData::class.java) }

                val sortedScores = allScores.sortedByDescending { it.score }

                scoresList.clear()

                for ((index, scoreData) in sortedScores.withIndex()) {
                    val position = index + 1  // numer w rankingu od 1 w górę
                    scoresList.add("$position. ${scoreData.nickname} - ${scoreData.score}")
                }

                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@ScoreboardActivity,
                    "Error: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    data class ScoreData(
        val nickname: String = "",
        val score: Int = 0
    )
}
