package com.example.card

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.card.Stat.Stats
import com.example.card.Stat.StatsDatabase
import com.example.card.database.AppDatabase
import com.example.card.database.Card
import com.example.card.databinding.ActivityMainBinding
import com.example.card.databinding.DialogCardBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.jvm.java
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
            btnStats.setOnClickListener {
                val intent = Intent(this@MainActivity, StatisticActivity::class.java)
                startActivity(intent)
            }
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
                val question = dialogBinding.etQuestion.text.toString()
                val answer = dialogBinding.etAnswer.text.toString()

                // Проверка ДО создания карточки
                if (question.isBlank() || answer.isBlank()) {
                    Toast.makeText(this@MainActivity, "Заполните все поля", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val card = Card(
                    question = question,
                    answer = answer
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
        val currentCard = cards.getOrNull(currentPosition) ?: return

        AlertDialog.Builder(this)
            .setTitle("Подтверждение удаления")
            .setMessage("Вы точно хотите удалить эту карточку?")
            .setPositiveButton("Да") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    db.cardDao().delete(currentCard)
                    loadDueCards()
                }
            }
            .setNegativeButton("Нет", null)
            .setCancelable(true)
            .show()
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

    private var ratingDialogJob: Job? = null

    private fun flipCard() {
        if (cards.isEmpty()) return

        showingQuestion = !showingQuestion
        updateCardDisplay()

        ratingDialogJob?.cancel()

        if (!showingQuestion) {
            ratingDialogJob = lifecycleScope.launch {
                delay(2000)
                if (!showingQuestion) showRatingDialog()
            }
        }

        // Анимация изменения цвета
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
        val ratings = arrayOf(
            "0 - Совсем не помню",
            "1 - Почти вспомнил",
            "2 - Легко запомнить",
            "3 - Вспомнил с усилием",
            "4 - Вспомнил после паузы",
            "5 - Мгновенно вспомнил"
        )

        AlertDialog.Builder(this)
            .setTitle("Оцените запоминание")
            .setItems(ratings) { _, which ->
                cards.getOrNull(currentPosition)?.let {
                    updateCardSM2(it, which)
                    showNextCardAutomatically()
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
                cardView.setCardBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.card_question))
            } else {
                val card = cards[currentPosition]
                tvCardContent.text = if (showingQuestion) card.question else card.answer
                tvCardStatus.text = getString(R.string.card_status, card.interval, card.repetition)
                val bgColorRes = if (showingQuestion) R.color.card_question else R.color.card_answer
                cardView.setCardBackgroundColor(ContextCompat.getColor(this@MainActivity, bgColorRes))
            }
            tvCardCount.text = getString(R.string.card_count, currentPosition + 1, cards.size)
        }
    }
    private fun showRatingConfirmationDialog() {
        if (showingQuestion) return // Не показывать, если виден вопрос

        AlertDialog.Builder(this)
            .setTitle("Оценка карточки")
            .setMessage("Хотите оценить запоминание этой карточки?")
            .setPositiveButton("Да") { _, _ ->
                showRatingOptionsDialog()
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    private fun showRatingOptionsDialog() {
        val ratings = arrayOf(
            "0 - Совсем не помню",
            "1 - Почти вспомнил",
            "2 - Легко запомнить",
            "3 - Вспомнил с усилием",
            "4 - Вспомнил после паузы",
            "5 - Мгновенно вспомнил"
        )

        AlertDialog.Builder(this)
            .setTitle("Ваша оценка")
            .setItems(ratings) { _, which ->
                cards.getOrNull(currentPosition)?.let {
                    updateCardSM2(it, which)
                    showNextCardAutomatically()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    private fun showNextCardAutomatically() {
        if (cards.isEmpty()) return

        if (currentPosition < cards.size - 1) {
            currentPosition++
        } else {
            AlertDialog.Builder(this)
                .setTitle("Сессия завершена")
                .setMessage("Вы просмотрели все карточки!")
                .setPositiveButton("OK") { _, _ ->
                    currentPosition = 0
                }
                .show()
        }
        showingQuestion = true
        updateCardDisplay()
    }
    private fun updateCardSM2(card: Card, quality: Int) {
        require(quality in 0..5) { "Quality must be between 0 and 5" }

        if (quality < 3) {
            card.interval = 1
            card.repetition = 0
        } else {
            // Исправлено: добавлена недостающая закрывающая скобка для max()
            card.efactor = max(1.3, card.efactor + 0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02))
            card.interval = when (card.repetition) {
                0 -> 1
                1 -> 6
                else -> (card.interval * card.efactor).toInt()
            }
            card.repetition++
        }
        card.nextReviewDate = System.currentTimeMillis() + card.interval * 86400000

        // Сохранение статистики
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val stat = Stats(
            date = dateFormat.format(Date()),
            rating = quality,
            cardId = card.id,
            cardsSolved = 1,
            isCompleted = quality >= 3
        )

        lifecycleScope.launch(Dispatchers.IO) {
            db.cardDao().update(card)
            StatsDatabase.getDatabase(this@MainActivity).statsDao().insert(stat)
        }
    }
}