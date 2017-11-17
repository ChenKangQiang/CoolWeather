package edu.tongji.coolweather;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import java.io.IOException;

import edu.tongji.coolweather.gson.Weather;
import edu.tongji.coolweather.util.HttpUtil;
import edu.tongji.coolweather.util.Utility;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 后台自动更新天气服务
 */
public class AutoUpdateService extends Service {
    public AutoUpdateService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateWeather();
        updateBingPic();
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        //每8小时更新一次
        int interval = 8 * 60 * 60 * 1000;
        long triggerAtTime = SystemClock.elapsedRealtime() + interval;
        Intent i = new Intent(this, AutoUpdateService.class);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        alarmManager.cancel(pi);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);

        return super.onStartCommand(intent, flags, startId);
    }


    /**
     * 更新天气信息
     */
    private void updateWeather() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather", null);
        if (weatherString != null) {
            //有缓存时直接解析天气数据，获取weatherId
            Weather weather = Utility.handleWeatherResponse(weatherString);
            String weatherId = weather.basic.weatherId;
            String requestUrl = "http://guolin.tech/api/weather?cityid="
                    + weatherId + "&key=c02aee38906842afb50bcbbb424cb1af";

            HttpUtil.sendOKHttpRequest(requestUrl, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseText = response.body().string();
                    Weather newWeather = Utility.handleWeatherResponse(responseText);
                    if (newWeather != null && "ok".equals(newWeather.status)) {
                        //写入缓存
                        SharedPreferences.Editor editor = PreferenceManager
                                .getDefaultSharedPreferences(AutoUpdateService.this)
                                .edit();
                        editor.putString("weather", responseText);
                        editor.apply();
                    }
                }
            });

        }
    }


    /**
     * 更新必应每日一图
     */
    private void updateBingPic() {
        String requestBingPicUrl = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOKHttpRequest(requestBingPicUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager
                        .getDefaultSharedPreferences(AutoUpdateService.this)
                        .edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();
            }

        });
    }




}
