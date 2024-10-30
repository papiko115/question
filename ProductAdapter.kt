package com.example.rist33

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class ProductAdapter(
    private val context: Context,
    private val products: MutableList<Product>,
    private val dbHelper: ProductDatabaseHelper
) : ArrayAdapter<Product>(context, 0, products) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val itemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item, parent, false)

        val product = products[position]
        val productNameTextView: TextView = itemView.findViewById(R.id.productNameTextView)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)

        productNameTextView.text = product.name

        // ゴミ箱ボタンのクリックリスナーを設定
        deleteButton.setOnClickListener {
            dbHelper.deleteProduct(product)
            products.removeAt(position)
            notifyDataSetChanged()
            Toast.makeText(context, "${product.name} を削除しました", Toast.LENGTH_SHORT).show()
        }

        // リストアイテムのタップで詳細画面に遷移
        itemView.setOnClickListener {
            val intent = Intent(context, DetailActivity::class.java).apply {
                putExtra("productId", product.id)
                putExtra("productName", product.name)
                putExtra("productExpirationDate", product.expirationDate.toString())
            }
            context.startActivity(intent)
        }

        return itemView
    }
}