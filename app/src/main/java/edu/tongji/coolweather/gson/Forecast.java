package edu.tongji.coolweather.gson;

import com.google.gson.annotations.SerializedName;

import java.security.PublicKey;

/**
 * Created by chen on 2017/11/16.
 */

public class Forecast {

    public String date;

    @SerializedName("tmp")
    public Temperate temperate;

    @SerializedName("cond")
    public More more;

    public class Temperate {

        public String max;

        public String min;
    }


    public class More {

        @SerializedName("txt_d")
        public String info;

    }


}
