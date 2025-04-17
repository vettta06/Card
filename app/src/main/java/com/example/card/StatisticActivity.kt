// StatisticActivity.kt
package com.example.card

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.card.Stat.StatsDatabase
import com.example.card.databinding.ActivityStatisticBinding
import kotlinx.coroutines.launch

class StatisticActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStatisticBinding
    private lateinit var statsDb: StatsDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticBinding.inflate(layoutInflater)
        setContentView(binding.root)
        statsDb = StatsDatabase.getDatabase(applicationContext)

        loadStatistics()

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadStatistics() {
        lifecycleScope.launch {
            val stats = statsDb.statsDao().getAll()
            binding.textView.text = stats.joinToString("\n\n") { stat ->
                "Дата: ${stat.date}\nРешено карточек: ${stat.cardsSolved}"
            }
        }
    }
}