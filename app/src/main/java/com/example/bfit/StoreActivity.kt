package com.example.bfit

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.bfit.database.FirestoreRepository
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

data class Supplement(
    val id: String = "",
    val name: String,
    val description: String,
    val price: Double = 0.0,
    val url: String = ""
)

class StoreActivity : AppCompatActivity() {

    private lateinit var firestoreRepository: FirestoreRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store)

        firestoreRepository = FirestoreRepository()

        // Back button
        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        // Purchase history button
        findViewById<Button>(R.id.purchaseHistoryBtn).setOnClickListener {
            showPurchaseHistory()
        }

        val supplementsRecyclerView = findViewById<RecyclerView>(R.id.supplementsRecyclerView)

        // Try loading from Firestore first, fall back to defaults
        lifecycleScope.launch {
            val firestoreSupplements = firestoreRepository.getSupplements()
            val supplements = if (firestoreSupplements.isNotEmpty()) {
                firestoreSupplements.map { data ->
                    Supplement(
                        id = data["id"] as? String ?: "",
                        name = data["name"] as? String ?: "Unknown",
                        description = data["description"] as? String ?: "",
                        price = (data["price"] as? Number)?.toDouble() ?: 0.0,
                        url = data["url"] as? String ?: ""
                    )
                }
            } else {
                getDefaultSupplements()
            }
            supplementsRecyclerView.adapter = SupplementsAdapter(supplements) { supplement ->
                if (supplement.url.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(supplement.url))
                    startActivity(intent)
                } else {
                    Toast.makeText(this@StoreActivity, "Product link coming soon!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun showPurchaseHistory() {
        lifecycleScope.launch {
            try {
                val purchases = firestoreRepository.getPurchaseHistory()
                if (purchases.isEmpty()) {
                    Toast.makeText(this@StoreActivity, getString(R.string.no_purchase_history), Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val items = purchases.map { purchase ->
                    val name = purchase["supplementName"] as? String ?: "Unknown"
                    val price = (purchase["price"] as? Number)?.toDouble() ?: 0.0
                    val status = purchase["status"] as? String ?: "unknown"
                    "$name — $${"%.2f".format(price)} ($status)"
                }.toTypedArray()

                AlertDialog.Builder(this@StoreActivity)
                    .setTitle(getString(R.string.purchase_history))
                    .setItems(items, null)
                    .setPositiveButton("Close", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(this@StoreActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getDefaultSupplements(): List<Supplement> {
        val amazonUrl = "https://www.amazon.com/s?k=" // Global Amazon search redirect
        return listOf(
            Supplement("on_whey", "Optimum Nutrition Gold Standard Whey", "Top-rated protein powder for muscle recovery.", 34.99, "${amazonUrl}optimum+nutrition+gold+standard+whey"),
            Supplement("muscletech_creatine", "MuscleTech Platinum Creatine", "Highly pure micronized creatine for performance.", 19.99, "${amazonUrl}muscletech+platinum+creatine"),
            Supplement("muscleblaze_bcaas", "MuscleBlaze BCAA Powder", "Intra-workout support for muscle recovery.", 24.99, "${amazonUrl}muscleblaze+bcaa"),
            Supplement("c4_pre", "Cellucor C4 Pre-Workout", "The original explosive pre-workout energy.", 29.99, "${amazonUrl}cellucor+c4+pre+workout"),
            Supplement("multivitamin", "Garden of Life Multivitamin", "Organic whole food multivitamin for daily health.", 14.99, "${amazonUrl}garden+of+life+multivitamin")
        )
    }
}

class SupplementsAdapter(
    private val supplements: List<Supplement>,
    private val onBuyClicked: (Supplement) -> Unit
) :
    RecyclerView.Adapter<SupplementsAdapter.SupplementViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SupplementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_supplement, parent, false)
        return SupplementViewHolder(view)
    }

    override fun onBindViewHolder(holder: SupplementViewHolder, position: Int) {
        val supplement = supplements[position]
        holder.bind(supplement)
    }

    override fun getItemCount() = supplements.size

    inner class SupplementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.supplementName)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.supplementDescription)
        private val priceTextView: TextView = itemView.findViewById(R.id.supplementPrice)
        private val buyButton: Button = itemView.findViewById(R.id.buyButton)

        fun bind(supplement: Supplement) {
            nameTextView.text = supplement.name
            descriptionTextView.text = supplement.description
            priceTextView.text = if (supplement.price > 0) "$${String.format("%.2f", supplement.price)}" else ""
            buyButton.setOnClickListener {
                onBuyClicked(supplement)
            }
        }
    }
}