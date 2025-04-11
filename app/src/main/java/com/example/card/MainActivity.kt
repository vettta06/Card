package com.example.card

import android.animation.ValueAnimator
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.card.database.AppDatabase
import com.example.card.database.Card
import com.example.card.databinding.ActivityMainBinding
import com.example.card.databinding.DialogCardBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

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
        loadDueCards()
        if (savedInstanceState != null) {
            currentPosition = savedInstanceState.getInt("currentPosition")
            showingQuestion = savedInstanceState.getBoolean("showingQuestion")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentPosition", currentPosition)
        outState.putBoolean("showingQuestion", showingQuestion)
    }
    private fun setupClickListeners() {
        binding.apply {
            btnAdd.setOnClickListener { showAddDialog() }
            btnEdit.setOnClickListener { showEditDialog() }
            btnDelete.setOnClickListener { deleteCurrentCard() }
            btnBack.setOnClickListener { showPreviousCard() }
            btnNext.setOnClickListener { showNextCard() }
            tvCardContent.setOnClickListener { flipCard() }
        }
    }

    private fun loadDueCards() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val dueCards = db.cardDao().getDueCards(System.currentTimeMillis())
                withContext(Dispatchers.Main) {
                    cards = dueCards
                    currentPosition = 0
                    updateCardDisplay()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.tvCardContent.text = getString(R.string.error_loading)
                }
            }
        }
    }

    private fun showAddDialog() {
        val dialogBinding = DialogCardBinding.inflate(layoutInflater)
        AlertDialog.Builder(this)
            .setTitle(R.string.new_card)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.add) { _, _ ->
                val card = Card(
                    question = dialogBinding.etQuestion.text.toString(),
                    answer = dialogBinding.etAnswer.text.toString()
                )
                lifecycleScope.launch(Dispatchers.IO) {
                    db.cardDao().insert(card)
                    loadDueCards()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showEditDialog() {
        val currentCard = cards.getOrNull(currentPosition) ?: return
        val dialogBinding = DialogCardBinding.inflate(layoutInflater)

        dialogBinding.etQuestion.setText(currentCard.question)
        dialogBinding.etAnswer.setText(currentCard.answer)

        AlertDialog.Builder(this)
            .setTitle(R.string.edit_card)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val updatedCard = currentCard.copy(
                    question = dialogBinding.etQuestion.text.toString(),
                    answer = dialogBinding.etAnswer.text.toString()
                )
                lifecycleScope.launch(Dispatchers.IO) {
                    db.cardDao().update(updatedCard)
                    loadDueCards()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteCurrentCard() {
        cards.getOrNull(currentPosition)?.let {
            lifecycleScope.launch(Dispatchers.IO) {
                db.cardDao().delete(it)
                loadDueCards()
            }
        }
    }

    private fun showPreviousCard() {
        if (currentPosition > 0) {
            currentPosition--
            showingQuestion = true
            updateCardDisplay()
        }
    }

    private fun showNextCard() {
        if (currentPosition < cards.size - 1) {
            currentPosition++
            showingQuestion = true
            updateCardDisplay()
        }
    }

    private fun flipCard() {
        if (cards.isEmpty()) return
        showingQuestion = !showingQuestion
        updateCardDisplay()
        if (!showingQuestion) showRatingDialog()
        val startColor = if (showingQuestion) {
            ContextCompat.getColor(this, R.color.card_question)
        } else {
            ContextCompat.getColor(this, R.color.card_answer)
        }

        val endColor = if (showingQuestion) {
            ContextCompat.getColor(this, R.color.card_answer)
        } else {
            ContextCompat.getColor(this, R.color.card_question)
        }

        ValueAnimator.ofArgb(startColor, endColor).apply {
            duration = 300
            addUpdateListener {
                binding.cardView.setCardBackgroundColor(it.animatedValue as Int)
            }
            start()
        }
    }

    private fun showRatingDialog() {
        val ratings = resources.getStringArray(R.array.ratings)
        AlertDialog.Builder(this)
            .setTitle(R.string.rate_dialog_title)
            .setItems(ratings) { _, which ->
                cards.getOrNull(currentPosition)?.let {
                    updateCardSM2(it, which)
                    showNextCard()
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun updateCardDisplay() {
        binding.apply {
            if (cards.isEmpty()) {
                tvCardContent.text = getString(R.string.no_cards)
                tvCardStatus.text = ""
                // Устанавливаем цвет по умолчанию для пустого состояния
                cardView.setCardBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.card_question))
            } else {
                val card = cards[currentPosition]

                // Устанавливаем текст
                tvCardContent.text = if (showingQuestion) card.question else card.answer
                tvCardStatus.text = getString(R.string.card_status, card.interval, card.repetition)

                // Меняем цвет карточки
                val bgColorRes = if (showingQuestion) {
                    R.color.card_question // Цвет вопроса
                } else {
                    R.color.card_answer   // Цвет ответа
                }
                cardView.setCardBackgroundColor(ContextCompat.getColor(this@MainActivity, bgColorRes))
            }

            // Обновляем счетчик карточек
            tvCardCount.text = getString(R.string.card_count, cards.size, currentPosition + 1)
        }
    }
    private fun updateCardSM2(card: Card, quality: Int) {
        require(quality in 0..5) { "Quality must be between 0 and 5" }

        if (quality < 3) {
            card.interval = 1
            card.repetition = 0
        } else {
            card.efactor = max(1.3, card.efactor + 0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02))
            card.interval = when (card.repetition) {
                0 -> 1
                1 -> 6
                else -> (card.interval * card.efactor).toInt()
            }
            card.repetition++
        }
        card.nextReviewDate = System.currentTimeMillis() + card.interval * 86400000

        lifecycleScope.launch(Dispatchers.IO) {
            db.cardDao().update(card)
        }
    }
}