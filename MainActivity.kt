package com.example.rist33

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Color
import android.graphics.Color.*
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.ValueFormatter
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import android.Manifest
import android.content.pm.PackageManager
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.app.AlertDialog
import android.content.Intent
import android.util.Log
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    private lateinit var dbHelper: ProductDatabaseHelper
    private lateinit var chart: BarChart
    private lateinit var listView: ListView
    private lateinit var productNameInput: EditText
    private lateinit var productExpirationInput: EditText
    private lateinit var productQuantityInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // パーミッションリクエストの実行
        requestNotificationPermission(this)

        // WorkManagerの制約とスケジュールの設定
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<ExpirationCheckWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueue(workRequest)

        // SQLiteデータベースのヘルパーを初期化
        dbHelper = ProductDatabaseHelper(this)

        // BarChartの初期化
        chart = findViewById(R.id.barchart)

        // リストビューの初期化
        listView = findViewById(R.id.listView)


        // データベースからすべての商品を取得してグラフに表示
        val products = dbHelper.getAllProducts()

        // データをグラフに変換して表示
        setUpBarChart(products)

        // リストビューを設定
        setupListView(products)

        // 入力フィールドとボタンの初期化
        productNameInput = findViewById(R.id.productNameInput)
        productExpirationInput = findViewById(R.id.productExpirationInput)
        productQuantityInput = findViewById(R.id.productQuantityInput) // 新しいEditTextを初期化

        val addProductButton: Button = findViewById(R.id.addProductButton)
        addProductButton.setOnClickListener {
            addProduct()
        }

    }

    private fun addProduct() {
        val name = productNameInput.text.toString()
        val expirationDateStr = productExpirationInput.text.toString()
        val quantityStr = productQuantityInput.text.toString() // 数量を取得

        // 日付のパース
        val expirationDate = LocalDate.parse(expirationDateStr)

        // 数量を整数に変換
        val quantity = quantityStr.toIntOrNull()

        if (quantity == null || quantity <= 0) {
            Toast.makeText(this, "数量は1以上の整数を入力してください", Toast.LENGTH_SHORT).show()
            return // 数量が無効な場合は処理を中断
        }
        Log.d("MainActivity", "Adding product: $name, Expiration Date: $expirationDate, Quantity: $quantity")

        // 商品をデータベースに追加
        dbHelper.addProduct(name, expirationDate, quantity)

        // リストとグラフを更新
        refreshProductList()

        // 入力フィールドをクリア
        productNameInput.text.clear()
        productExpirationInput.text.clear()
        productQuantityInput.text.clear()
    }


    private fun refreshProductList() {
        val products = dbHelper.getAllProducts()
        setupListView(products)
    }




    private fun setupListView(products: List<Product>) {
        // ProductAdapterを使用してリストビューを設定
        val adapter = ProductAdapter(this, products.toMutableList(), dbHelper)
        listView.adapter = adapter

        // クリックしたときに詳細画面に遷移
        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedProduct = products[position]

            // ここで選択した商品の数量をログに出力
            Log.d("MainActivity", "Selected Product Quantity: ${selectedProduct.quantity}")

            val intent = Intent(this, DetailActivity::class.java).apply {
                putExtra("productId", selectedProduct.id)
                putExtra("productName", selectedProduct.name)
                putExtra("productExpirationDate", selectedProduct.expirationDate.toString())
                putExtra("productQuantity", selectedProduct.quantity) // 数量を渡す
            }
            startActivity(intent)
        }

    }

    private fun setUpBarChart(products: List<Product>) {
        val entries = ArrayList<BarEntry>()

        // データをEntryに変換（消費期限までの日数をY値として使用）
        products.forEachIndexed { index, product ->
            val daysUntilExpiration = ChronoUnit.DAYS.between(LocalDate.now(), product.expirationDate).toFloat()
            entries.add(BarEntry(index.toFloat(), daysUntilExpiration))
        }

        // BarDataSetの作成
        val dataSet = BarDataSet(entries, "消費期限までの日数")
        dataSet.color = BLUE

        // BarDataの作成
        val barData = BarData(dataSet)
        barData.barWidth = 0.9f // 棒の幅の調整

        // グラフの設定
        chart.data = barData
        chart.setFitBars(true)
        chart.description.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.animateY(1000)
        chart.invalidate() // グラフの更新
    }
}

class ProductDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "products.db"
        private const val DATABASE_VERSION = 7
        private const val TABLE_PRODUCTS = "products"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_EXPIRATION_DATE = "expirationDate"
        private const val COLUMN_QUANTITY = "quantity" // ここを追加
    }

    override fun onCreate(db: SQLiteDatabase) {
        // テーブルの作成
        val createTable = """
        CREATE TABLE $TABLE_PRODUCTS (
            $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COLUMN_NAME TEXT,
            $COLUMN_EXPIRATION_DATE TEXT,
            $COLUMN_QUANTITY INTEGER
        )
    """
        db.execSQL(createTable)
        // 初期データの挿入
        insertInitialData(db)
    }

    private fun insertInitialData(db: SQLiteDatabase) {
        val initialProducts = listOf(
            Triple("食料", LocalDate.of(2024, 8, 30),5),
            Triple("衣服", LocalDate.of(2024, 9, 4),3),
            Triple("食料", LocalDate.of(2024, 9, 10),7)
        )

        for (product in initialProducts) {
            val values = ContentValues().apply {
                put(COLUMN_NAME, product.first)
                put(COLUMN_EXPIRATION_DATE, product.second.toString())
                put(COLUMN_QUANTITY, product.third) // 数量を追加
            }
            db.insert(TABLE_PRODUCTS, null, values)
        }
    }
    //商品を削除するメソッド
    fun deleteProduct(product: Product) {
        val db = this.writableDatabase
        db.delete(TABLE_PRODUCTS, "$COLUMN_ID=?", arrayOf(product.id.toString()))
        db.close()
    }
  //リストビューの商品の追加（データベースの項目の追加と同じ）
  fun addProduct(name: String, expirationDate: LocalDate, quantity: Int) {
      val db = this.writableDatabase
      val values = ContentValues().apply {
          put(COLUMN_NAME, name)
          put(COLUMN_EXPIRATION_DATE, expirationDate.toString())
          put(COLUMN_QUANTITY, quantity) // 数量を保存
      }

      db.insert(TABLE_PRODUCTS, null, values)
      db.close()
  }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PRODUCTS")
        onCreate(db)
    }

    fun getAllProducts(): List<Product> {
        val products = mutableListOf<Product>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_PRODUCTS,
            arrayOf(COLUMN_ID, COLUMN_NAME, COLUMN_EXPIRATION_DATE, COLUMN_QUANTITY),
            null, null, null, null, null
        )

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME))
                val expirationDate = LocalDate.parse(
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXPIRATION_DATE))
                )
                val quantity = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_QUANTITY)) // 数量を取得

                // 確認用ログ出力
                Log.d("ProductDatabaseHelper", "Loaded product: ID=$id, Name=$name, Expiration=$expirationDate, Quantity=$quantity")

                products.add(Product(id, name, expirationDate, quantity))
            } while (cursor.moveToNext())
        }

        cursor.close()
        return products
    }

}

// 商品のデータクラス
data class Product(val id: Int, val name: String, val expirationDate: LocalDate, val quantity: Int)

class ExpirationCheckWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val dbHelper = ProductDatabaseHelper(applicationContext)
        val products = dbHelper.getAllProducts()
        val currentDate = LocalDate.now()

        // 消費期限が3日以内の商品の通知
        for (product in products) {
            val daysUntilExpiration = ChronoUnit.DAYS.between(currentDate, product.expirationDate)
            if (daysUntilExpiration in 0..3) {
                sendNotification(
                    applicationContext,
                    "消費期限が近づいています",
                    "${product.name}の消費期限が${daysUntilExpiration}日後です"
                )
            }
        }

        return Result.success()
    }
}

private fun sendNotification(context: Context, title: String, message: String) {
    val channelId = "expiration_notifications"
    val notificationId = 1

    // NotificationChannelの作成
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "Expiration Notifications"
        val descriptionText = "Notifications for products nearing expiration"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, name, importance).apply {
            description = descriptionText
        }

        // NotificationManagerの取得とチャンネルの登録
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.createNotificationChannel(channel)
    }

    // 通知の表示前にパーミッションを確認
    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        // 通知を表示
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        NotificationManagerCompat.from(context).apply {
            notify(notificationId, builder.build())
        }
    } else {
        requestNotificationPermission(context)
    }
}

private fun requestNotificationPermission(context: Context) {
    if (context is MainActivity) {
        val requestPermissionLauncher = context.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                sendNotification(context, "通知の設定", "アクセスの許可を確認しました")
            }
        }

        // パーミッションリクエストの実行
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}