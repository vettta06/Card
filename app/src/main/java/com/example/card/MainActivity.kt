// app/src/main/java/com/example/card/MainActivity.kt
package com.example.card

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.card.database.AppDatabase
import com.example.card.database.Card
import com.example.card.databinding.ActivityMainBinding
import com.example.card.databinding.DialogCardBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase
    private var cards = emptyList<Card>()
    private var currentPosition = 0
    private var showingQuestion = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)
        setupClickListeners()
        loadCards()
    }

    private fun setupClickListeners() {
        binding.apply {
            button3.setOnClickListener { showAddDialog() }
            button4.setOnClickListener { showEditDialog() }
            button5.setOnClickListener { deleteCurrentCard() }
            button.setOnClickListener { showPreviousCard() }
            button2.setOnClickListener { showNextCard() }
            textView.setOnClickListener { flipCard() }
        }
    }

    private fun loadCards() {
        lifecycleScope.launch(Dispatchers.IO) { // Используем IO-диспетчер
            try {
                val cardsFromDb = db.cardDao().getAllCards()
                withContext(Dispatchers.Main) { // Возвращаемся в главный поток
                    cards = cardsFromDb
                    updateCardCount()
                    updateCardDisplay()
                }
            } catch (e: Exception) {
            }
        }
    }
    private fun showAddDialog() {
        val dialogBinding = DialogCardBinding.inflate(layoutInflater)
        AlertDialog.Builder(this)
            .setTitle("Новая карточка")
            .setView(dialogBinding.root)
            .setPositiveButton("Добавить") { _, _ ->
                val card = Card(
                    question = dialogBinding.etQuestion.text.toString(),
                    answer = dialogBinding.etAnswer.text.toString()
                )
                lifecycleScope.launch {
                    db.cardDao().insert(card)
                    loadCards()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showEditDialog() {
        if (cards.isEmpty()) return
        val currentCard = cards[currentPosition]
        val dialogBinding = DialogCardBinding.inflate(layoutInflater)

        dialogBinding.etQuestion.setText(currentCard.question)
        dialogBinding.etAnswer.setText(currentCard.answer)

        AlertDialog.Builder(this)
            .setTitle("Редактировать карточку")
            .setView(dialogBinding.root)
            .setPositiveButton("Сохранить") { _, _ ->
                val updatedCard = currentCard.copy(
                    question = dialogBinding.etQuestion.text.toString(),
                    answer = dialogBinding.etAnswer.text.toString()
                )
                lifecycleScope.launch {
                    db.cardDao().update(updatedCard)
                    loadCards()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteCurrentCard() {
        if (cards.isEmpty()) return
        lifecycleScope.launch(Dispatchers.IO) {
            db.cardDao().delete(cards[currentPosition])
            loadCards() // После удаления перезагружаем список
        }
    }
    private fun showPreviousCard() {
        if (currentPosition > 0) {
            currentPosition--
            updateCardDisplay()
        }
    }

    private fun showNextCard() {
        if (currentPosition < cards.size - 1) {
            currentPosition++
            updateCardDisplay()
        }
    }

    private fun flipCard() {
        if (cards.isEmpty()) return
        showingQuestion = !showingQuestion
        updateCardDisplay()
    }

    private fun updateCardDisplay() {
        binding.textView.text = if (cards.isEmpty()) {
            "Нет карточек"
        } else {
            val card = cards[currentPosition]
            if (showingQuestion) card.question else card.answer
        }
    }

    private fun updateCardCount() {
        binding.textView2.text = "Количество карточек: ${cards.size}"
    }
}