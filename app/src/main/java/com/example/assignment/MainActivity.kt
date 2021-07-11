package com.example.assignment

import android.content.pm.PackageManager
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : AppCompatActivity() {
    val REQUEST_CODE_ASK_PERMISSIONS = 20
    var ph_no: TextInputEditText? = null
    var days_no: TextInputEditText? = null
    var button: AppCompatButton? = null
    var text: MaterialTextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Initializing the Values at the time of activity creation
        ph_no = findViewById(R.id.phone_no)
        days_no = findViewById(R.id.days)
        button = findViewById(R.id.submit_button)
        text = findViewById(R.id.message_count)

        //Requesting Permission
        ActivityCompat.requestPermissions(this@MainActivity, arrayOf("android.permission.READ_SMS"), REQUEST_CODE_ASK_PERMISSIONS)

        button?.setOnClickListener(View.OnClickListener {
            //Checking for Permission to read SMS
            if (ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED) {
                //Requesting Permission
                if (ph_no?.text.toString().isNotEmpty() && days_no?.text.toString().isNotEmpty()) {
                    //Using Kotlin Coroutines
                    GlobalScope.launch(Dispatchers.Main) {
                        val count = async(Dispatchers.IO) { messageCount(ph_no?.text.toString(), days_no?.text.toString()) } //Working on background thread
                        count.await()?.let { it1 -> displayMessageCount(it1) } // back on UI thread
                    }
                } else {
                    Toast.makeText(this, "Fields cannot be blank", Toast.LENGTH_LONG).show()
                }
            } else {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf("android.permission.READ_SMS"), REQUEST_CODE_ASK_PERMISSIONS)
            }

        })
    }

    private fun displayMessageCount(count: String) {
        text?.text = count

    }

    private fun messageCount(phNo: String, daysNo: String): String? {
        //Subtracting from current day
        val k = Calendar.getInstance()
        k.add(Calendar.DAY_OF_YEAR, -(daysNo.toInt()));

        var c = 0; //Counter to count the number of message
        var SMS_URI_INBOX = "content://sms/inbox"
        try {
            val uri = Uri.parse(SMS_URI_INBOX)
            val projection = arrayOf("date", "type")
            var cur = contentResolver.query(uri, projection, "address='$phNo'", null, "date desc")
            if (cur!!.moveToFirst()) {
                val index_Date = cur.getColumnIndex("date")
                val index_Type = cur.getColumnIndex("type")
                do {
                    var longDate: Long = cur.getLong(index_Date)
                    val int_Type = cur.getInt(index_Type)
                    if (longDate > k.timeInMillis) {
                        c++;
                    }
                } while (cur.moveToNext())
                if (!cur.isClosed) {
                    cur.close()
                    cur = null
                }
            } else {
                c = 0;
            } // end if
        } catch (ex: SQLiteException) {
            Log.d("SQLiteException", ex.message!!)
        }

        Log.d("TAG", "MessageCount: " + c)
        return c.toString()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_ASK_PERMISSIONS -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {

                } else {
                    Toast.makeText(this, "Permission Denied Cannot Read the Messages", Toast.LENGTH_LONG).show()
                }
                return
            }
        }
    }
}