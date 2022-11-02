package com.example.weatherapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //０)準備（APIキーと、URL（の基本部分）を定義）
        val apiKey = "ba9c5bdd572614ff12c2e51900c74da3"
        val mainUrl = "https://api.openweathermap.org/data/2.5/weather?lang=ja"

        //０)準備　viewを取得
        val btnTokyo: Button = findViewById(R.id.btnTokyo)
        val btnOkinawa: Button = findViewById(R.id.btnOkinawa)
        val tvCityName: TextView = findViewById(R.id.tvCityName)
        val tvCityWeather: TextView = findViewById(R.id.tvCityWeather)
        val tvMax: TextView = findViewById(R.id.tvMax)
        val tvMin: TextView = findViewById(R.id.tvMin)
        val btnClear: Button = findViewById(R.id.btnClear)

        //１）btnTokyoが押されたら
        btnTokyo.setOnClickListener {
            //[1-1]東京のお天気URLを取得して
            //val weatherUrl ="https://api.openweathermap.org/data/2.5/weather?lang=ja&q=tokyo&appid=ba9c5bdd572614ff12c2e51900c74da3"
            val weatherUrl = "$mainUrl&q=tokyo&appid=$apiKey"

            //[1-2]そのＵＲＬを元に得られた情報の結果を表示
            //２)コルーチンを作る⇒３)HTTP通信(ワーカースレッド)⇒４)お天気データ表示（メインスレッド）
            weatherTask(weatherUrl) //中身は「２」へ
        }

        //５）btnOkinawaが押されたら
        btnOkinawa.setOnClickListener {
            //[5-1]沖縄のURLを取得して
            val weatherUrl = "$mainUrl&q=okinawa&appid=$apiKey"

            //[5-2]そのＵＲＬを元に得られた情報の結果を表示
            weatherTask(weatherUrl) //中身は「２」へ
        }

        //６）クリアボタンで元に戻す
        btnClear.setOnClickListener {
            tvCityName.text="都市名"
            tvCityWeather.text="都市の天気"
            tvMax.text="最高気温"
            tvMin.text="最低気温"
        }
    }

    //２）weatherTask()の中身
    private fun weatherTask(weatherUrl: String) {
        //コルーチンスコープ（非同期処理の領域）を用意
        //GlobalScope.launch(Dispatchers.Main,CoroutineStart.DEFAULT){
        lifecycleScope.launch {
            //３）HTTP通信（ワーカースレッド）
            val result = weatherBackgroundTask(weatherUrl)

            //４）３を受けて、お天気データ(JSONデータ)を表示（UIスレッド）
            weatherJsonTask(result)

        }
    }

    //３）HTTP通信（ワーカースレッド）の中身(※suspend＝中断する可能性がある関数につける)
    private suspend fun weatherBackgroundTask(weatherUrl:String):String{
        //withContext=スレッドを分離しますよ、Dispatchers.IO＝ワーカースレッド
        val response = withContext(Dispatchers.IO){
            // 天気情報サービスから取得した結果情報（JSON文字列）を後で入れるための変数（いったん空っぽ）を用意。
            var httpResult = ""

            //  try{エラーがあるかもしれない処理を実行}catch{実際エラーがあった場合}
            try{
                //ただのURL文字列をURLオブジェクトに変換（文字列にリンクを付けるイメージ）
                val urlObj = URL(weatherUrl)

                // アクセスしたAPIから情報を取得
                //テキストファイルを読み込むクラス(文字コードを読めるようにする準備(URLオブジェクト))
                val br = BufferedReader(InputStreamReader(urlObj.openStream()))

                //読み込んだデータを文字列に変換して代入
                //httpResult =br.toString()
                httpResult = br.readText()
            }catch (e:IOException){//IOExceptionとは例外管理するクラス
                e.printStackTrace() //エラーが発生したよって言う
            }catch (e:JSONException){ //JSONデータ構造に問題が発生した場合の例外
                e.printStackTrace()
            }
            //HTTP接続の結果、取得したJSON文字列httpResultを戻り値とする
            return@withContext httpResult
        }

        return response
    }

    //４）３のHTTP通信を受けて、お天気データ(JSONデータ)を表示（UIスレッド）の中身
    private fun weatherJsonTask(result:String){
        val tvCityName: TextView = findViewById(R.id.tvCityName)
        val tvCityWeather: TextView = findViewById(R.id.tvCityWeather)
        val tvMax: TextView = findViewById(R.id.tvMax)
        val tvMin: TextView = findViewById(R.id.tvMin)

        // まずは「3」で取得した、JSONオブジェクト一式を生成。
        val jsonObj =JSONObject(result)

        // JSONオブジェクトの、都市名のキーを取得。⇒tvに代入して表示
        val cityName =jsonObj.getString("name")
        tvCityName.text =cityName

        // JSONオブジェクトの、天気情報JSON配列オブジェクトを取得。
        val weatherJSONArray =jsonObj.getJSONArray("weather")
        // 現在の天気情報JSONオブジェクト(配列の0番目)を取得。
        val  weatherJSON =weatherJSONArray.getJSONObject(0)
        // お天気の説明（description）を取得。
        val weather =weatherJSON.getString("description")
        // TextViewに、お天気結果を表示
        tvCityWeather.text =weather

        //JSONオブジェクトの、mainオブジェクトを取得
        val main = jsonObj.getJSONObject("main")
        //tvMaxに最高気温を表示
        tvMax.text ="最高気温：${main.getInt("temp_max")-273}℃"
        //tvMinに最低気温を表示"
        tvMin.text ="最低気温：${main.getInt("temp_min")-273}℃"
    }

}