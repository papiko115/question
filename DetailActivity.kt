package com.example.rist33


import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import java.time.LocalDate

class DetailActivity : AppCompatActivity() {
    private lateinit var productNameTextView: TextView
    private lateinit var productExpirationDateTextView: TextView
    private lateinit var productQuantityTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        // インテントの中身を確認するためのログ
        Log.d("DetailActivity", "Intent extras: ${intent.extras}")

        productNameTextView = findViewById(R.id.productNameTextView)
        productExpirationDateTextView = findViewById(R.id.productExpirationDateTextView)
        productQuantityTextView = findViewById(R.id.productQuantityTextView)

        // Intentから商品情報を取得
        val productId = intent.getIntExtra("productId", -1)
        val productName = intent.getStringExtra("productName") ?: ""
        val productExpirationDate = intent.getStringExtra("productExpirationDate") ?: ""
        val quantity = intent.getIntExtra("productQuantity", 1) // 数量の取得を確認

        // 受け取った数量をログに出力
        Log.d("DetailActivity", "Received quantity: $quantity")


        // テキストビューに設定
        productNameTextView.text = productName
        productExpirationDateTextView.text = productExpirationDate
        productQuantityTextView.text = "数量: $quantity" // 数量を表示
    }
}