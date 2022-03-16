package com.example.fastcampus_11

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import java.util.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //뷰를 초기화해주기
        initOnOffButton()
        initChangeAlarmTimeButton()

        //step1 데이터 가져오기
        val model = fetchDataFromSharedPreferences()

        //step2 데이터를 그려주기
        renderView(model)
    }

    private fun initOnOffButton() {
        val onOffButton = findViewById<Button>(R.id.onOffButton)
        onOffButton.setOnClickListener {
            val model = it.tag as? AlarmDisplayModel ?: return@setOnClickListener
            val newModel = saveAlarmModel(model.hour, model.minute, model.onOff.not())
            renderView(newModel)

            if(newModel.onOff){
                //켜진 경우 -> 알람을 등록
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, newModel.hour)
                    set(Calendar.MINUTE, newModel.minute)

                    if(before(Calendar.getInstance())) {
                        add(Calendar.DATE, 1)
                    }
                }

                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(this, AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(this, BROADCAST_REQUEST_CODE, intent, PendingIntent.FLAG_MUTABLE)

                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )

            } else {
                //꺼진 경우 -> 알람을 제거
                cancelAlarm()
            }
        }
    }
    private fun initChangeAlarmTimeButton() {
        val changeAlarmButton = findViewById<Button>(R.id.changeAlarmButton)
        changeAlarmButton.setOnClickListener {

            val calendar = Calendar.getInstance()

            TimePickerDialog(this, { picker, hour, minute ->
                val model = saveAlarmModel(hour = hour, minute = minute, onOff = false)
                renderView(model)

                cancelAlarm()

            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }
    }

    private fun saveAlarmModel(
        hour: Int,
        minute: Int,
        onOff: Boolean
    ): AlarmDisplayModel {
        val model = AlarmDisplayModel(
            hour = hour,
            minute = minute,
            onOff = onOff
        )

        val sharedPreference = getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE)
        with(sharedPreference.edit()) {
            putString(ALARM_TIME_KEY, model.makeDataForDB())
            putBoolean(ON_OFF_VALUE_KEY, model.onOff)
            commit()
        }

        return model
    }

    private fun fetchDataFromSharedPreferences(): AlarmDisplayModel {
        val sharedPreference = getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE)

        //Nullable이기 때문에 뒤에 조건을 추가해서 null 값이 들어갈 수 없도록 바꿈
        val timeDBValue = sharedPreference.getString(ALARM_TIME_KEY, "9:30") ?: "9:30"
        val onOffDBValue = sharedPreference.getBoolean(ON_OFF_VALUE_KEY, false)
        val alarmData = timeDBValue.split(":")

        val alarmModel =  AlarmDisplayModel(
            hour = alarmData[0].toInt(),
            minute = alarmData[1].toInt(),
            onOff = onOffDBValue
        )

        //예외처리
        val pendingIntent = PendingIntent.getBroadcast(this, BROADCAST_REQUEST_CODE, Intent(this, AlarmReceiver::class.java), PendingIntent.FLAG_IMMUTABLE )
        if((pendingIntent == null) and alarmModel.onOff){
            //알람은 꺼져있는데, 데이터는 켜져있는 경우
            alarmModel.onOff = false
        } else if ((pendingIntent != null) and alarmModel.onOff.not()){
            //알람은 켜져 있는데, 데이터는 꺼져있는 경우
            //알람을 취소함
            pendingIntent.cancel()
        }

        return alarmModel
    }

    private fun renderView(model: AlarmDisplayModel) {
        findViewById<TextView>(R.id.ampmTextView).apply {
            text = model.ampmText
        }

        findViewById<TextView>(R.id.timeTextView).apply {
            text = model.timeText
        }

        findViewById<Button>(R.id.onOffButton).apply {
            text = model.onOffText
            tag = model
        }
    }

    private fun cancelAlarm() {
        val pendingIntent = PendingIntent.getBroadcast(this, BROADCAST_REQUEST_CODE, Intent(this, AlarmReceiver::class.java), PendingIntent.FLAG_IMMUTABLE )
        pendingIntent?.cancel()
    }

    companion object {
        private const val SHARED_PREFERENCE_NAME = "alarmTime"
        private const val ALARM_TIME_KEY = "alarm"
        private const val ON_OFF_VALUE_KEY = "onOff"
        private const val BROADCAST_REQUEST_CODE = 200
    }
}