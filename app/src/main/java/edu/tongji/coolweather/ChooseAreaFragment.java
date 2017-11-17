package edu.tongji.coolweather;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.tongji.coolweather.db.City;
import edu.tongji.coolweather.db.County;
import edu.tongji.coolweather.db.Province;
import edu.tongji.coolweather.util.HttpUtil;
import edu.tongji.coolweather.util.Utility;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by chen on 2017/11/15.
 */

public class ChooseAreaFragment extends android.support.v4.app.Fragment {

    private static final String TAG = "ChooseAreaFragment";

    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog progressDialog;

    private ProgressBar progressBar;

    private Button backButton;

    private TextView titleText;

    private ListView listView;

    private ArrayAdapter<String> adapter;

    private List<String> dataList = new ArrayList<>();

    /**
     * 省列表
     */
    private List<Province> provinceList;
    /**
     * 市列表
     */
    private List<City> cityList;
    /**
     * 县列表
     */
    private List<County> countyList;
    /**
     * 选中的省份
     */
    private Province selectedProvince;
    /**
     * 选中的城市
     */
    private City selectedCity;
    /**
     * 选中的级别
     */
    private int currentLevel;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        titleText = view.findViewById(R.id.title_text);
        backButton = view.findViewById(R.id.back_button);
        listView = view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        return view;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(position);
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(position);
                    queryCounties();
                } else if (currentLevel == LEVEL_COUNTY) {
                    String weatherId = countyList.get(position).getWeatherId();
                    //判断用户是否在天气展示页面要切换城市
                    if (getActivity() instanceof MainActivity) {
                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
                        Log.d(TAG, "onItemClick: " + weatherId);
                        intent.putExtra("weather_id", weatherId);
                        startActivity(intent);
                        getActivity().finish();

                    } else if (getActivity() instanceof WeatherActivity) {
                        WeatherActivity activity = (WeatherActivity) getActivity();
                        //关闭侧栏
                        activity.drawerLayout.closeDrawers();
                        //开始更新天气
                        activity.swipeRefresh.setRefreshing(true);
                        activity.requestWeather(weatherId);
                        activity.hasChangeCity = true;
                    }

                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLevel == LEVEL_COUNTY) {
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    queryProvinces();
                }
            }
        });

        queryProvinces();
    }

    /**
     * 查询全国所有的省，优先从数据库查询，如果没有再到服务器上查询
     */
    private void queryProvinces() {
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList = DataSupport.findAll(Province.class);
        if (provinceList != null && provinceList.size() > 0) {
            dataList.clear();
            for (Province province : provinceList) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        } else {
            //本地数据库没有数据，从服务器查询
            String address = "http://guolin.tech/api/china";
            queryFromServer(address, LEVEL_PROVINCE);
        }
    }


    /**
     * 查询选中的省下的所有市，优先从数据库查询，如果没有再到服务器上查询
     */
    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceId = ?",
                selectedProvince.getProvinceId() + "").find(City.class);
        if (cityList != null && cityList.size() > 0) {
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        } else {
            //本地数据库没有数据，从服务器查询
            int provinceId = selectedProvince.getProvinceId();
            String address = "http://guolin.tech/api/china" + "/" + provinceId;
            queryFromServer(address, LEVEL_CITY);
        }
    }


    /**
     * 查询选中的市下的所有区县，优先从数据库查询，如果没有再到服务器上查询
     */
    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityId = ?",
                selectedCity.getCityId() + "").find(County.class);
        if (countyList != null && countyList.size() > 0) {
            dataList.clear();
            for (County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        } else {
            //本地数据库没有数据，从服务器查询
            int provinceId = selectedProvince.getProvinceId();
            int cityId = selectedCity.getCityId();
            String address = "http://guolin.tech/api/china" + "/" + provinceId + "/" + cityId;
            queryFromServer(address, LEVEL_COUNTY);
        }
    }


    /**
     * 根据传入的地址和类型从服务器上查询省市县数据
     * @param address
     * @param level
     */
    private void queryFromServer(String address, final int level) {
        showProgressDialog();
        HttpUtil.sendOKHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                //通过runOnUiThread回到主线程，在主线程中更新UI
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                //写入本地数据库中
                if (level == LEVEL_PROVINCE) {
                    result = Utility.handleProvinceResponse(responseText);
                } else if (level == LEVEL_CITY) {
                    result = Utility.handleCityResponse(responseText, selectedProvince.getProvinceId());
                } else if (level == LEVEL_COUNTY) {
                    result = Utility.handleCountyResponse(responseText, selectedCity.getCityId());
                }

                //查询及处理成功
                if (result) {
                    //通过runOnUiThread回到主线程，在主线程中更新UI
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if (level == LEVEL_PROVINCE) {
                                queryProvinces();
                            } else if (level == LEVEL_CITY) {
                                queryCities();
                            } else if (level == LEVEL_COUNTY) {
                                queryCounties();
                            }
                        }
                    });
                }
            }

        });
    }


    /**
     * 显示进度对话框
     */
    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    /**
     * 关闭进度对话框
     */
    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }


}
