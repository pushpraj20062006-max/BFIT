package com.example.bfit

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
                showCheckoutDialog(supplement)
            }
        }
    }

    private fun showCheckoutDialog(supplement: Supplement) {
        val priceText = if (supplement.price > 0) "$${String.format("%.2f", supplement.price)}" else "Free"
        AlertDialog.Builder(this)
            .setTitle("Checkout")
            .setMessage("Purchase ${supplement.name} for $priceText?\n\n${supplement.description}")
            .setPositiveButton("Buy Now") { _, _ ->
                lifecycleScope.launch {
                    firestoreRepository.recordPurchase(
                        supplementName = supplement.name,
                        supplementId = supplement.id,
                        price = supplement.price
                    )
                    Toast.makeText(
                        this@StoreActivity,
                        "✅ Purchase recorded: ${supplement.name}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getDefaultSupplements(): List<Supplement> {
        return listOf(
            Supplement("whey_protein", "Whey Protein", "A high-quality protein for muscle growth and repair.", 29.99),
            Supplement("creatine", "Creatine", "Improves strength, power, and muscle mass.", 19.99),
            Supplement("bcaas", "BCAAs", "Reduces muscle soreness and exercise fatigue.", 24.99),
            Supplement("pre_workout", "Pre-Workout", "Boosts energy and focus for your workouts.", 34.99),
            Supplement("multivitamin", "Multivitamin", "Ensures you get all the essential vitamins and minerals.", 14.99)
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
        private val buyButton: Button = itemView.findViewById(R.id.buyButton)

        fun bind(supplement: Supplement) {
            nameTextView.text = supplement.name
            descriptionTextView.text = supplement.description
            val priceText = if (supplement.price > 0) "Buy - $${String.format("%.2f", supplement.price)}" else "Buy Now"
            buyButton.text = priceText
            buyButton.setOnClickListener {
                onBuyClicked(supplement)
            }
        }
    }
}