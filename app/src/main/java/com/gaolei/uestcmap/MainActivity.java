package com.gaolei.uestcmap;

import android.Manifest;
import android.app.Activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;



import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.bikenavi.BikeNavigateHelper;
import com.baidu.mapapi.bikenavi.adapter.IBEngineInitListener;
import com.baidu.mapapi.bikenavi.adapter.IBRoutePlanListener;
import com.baidu.mapapi.bikenavi.model.BikeRoutePlanError;
import com.baidu.mapapi.bikenavi.params.BikeNaviLaunchParam;
import com.baidu.mapapi.bikenavi.params.BikeRouteNodeInfo;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;
import com.baidu.mapapi.walknavi.WalkNavigateHelper;
import com.baidu.mapapi.walknavi.adapter.IWEngineInitListener;
import com.baidu.mapapi.walknavi.adapter.IWRoutePlanListener;
import com.baidu.mapapi.walknavi.model.WalkRoutePlanError;
import com.baidu.mapapi.walknavi.params.WalkNaviLaunchParam;
import com.baidu.mapapi.walknavi.params.WalkRouteNodeInfo;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;
import static com.baidu.mapapi.map.MapStatusUpdateFactory.zoomTo;

public class MainActivity extends Activity {

    private MapView mMapView = null;
    private BaiduMap mBaiduMap=null;
    private LocationClient mLocationClient;
    private LatLngBounds latLngBounds;
    private LatLng currentPosition=null;
    private LatLng destinationPosition=null;
    //private LatLng startPosition=null;
    //private Marker mStartMarker=null;
    //private Marker mEndMarker=null;
    private PoiSearch  mPoiSearch = null;
    private LatLngBounds searchBounds=null;
    private Button button=null;
    private AutoCompleteTextView editText=null;
    private SuggestionSearch mSuggestionSearch=null;
    private ArrayAdapter<String> sugAdapter = null;
    private boolean onClickbutton=false;

    private BikeNavigateHelper mNaviHelper;
    private BikeNaviLaunchParam bikeParam;
    private WalkNaviLaunchParam walkParam;

    private static boolean isPermissionRequested = false;
    BitmapDescriptor bdStart = BitmapDescriptorFactory
            .fromResource(R.drawable.icon_start);
    BitmapDescriptor bdEnd = BitmapDescriptorFactory
            .fromResource(R.drawable.icon_end);






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sugAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line);
        initMap();
        initLocation();
        createPOIListener();
        initButton();
        initPlaceSearch();
        initEditText();

        mNaviHelper=BikeNavigateHelper.getInstance();
        /*普通步行导航入口*/
        Button bikeBtn = (Button) findViewById(R.id.btn_bikenavi_normal);

        bikeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(destinationPosition!=null)
                startBikeNavi();
                else{
                    Log.d("error","detination=null");
                }
            }
        });

        Button walkBtn = (Button) findViewById(R.id.btn_walknavi_normal);
        walkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(destinationPosition!=null){
                    walkParam.extraNaviMode(0);
                    startWalkNavi();
                }
            }
        });

        //initButton();




    }
    @Override
    protected void onResume() {
        mMapView.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mLocationClient.stop();
        mBaiduMap.setMyLocationEnabled(false);
        mMapView.onDestroy();
        mMapView = null;
        mPoiSearch.destroy();
        mSuggestionSearch.destroy();
        super.onDestroy();
    }
    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= 23 && !isPermissionRequested) {

            isPermissionRequested = true;

            ArrayList<String> permissions = new ArrayList<>();
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }

            if (permissions.size() == 0) {
                return;
            } else {
                requestPermissions(permissions.toArray(new String[permissions.size()]), 0);
            }
        }
    }


    //设置定位点
    public class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            //mapView 销毁后不在处理新接收的位置
            if (location == null || mMapView == null){
                return;
            }
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(location.getDirection()).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);

            //获取当前位置信息
            currentPosition= new LatLng(location.getLatitude(),location.getLongitude());
        }
    }


    //地图初始化
    private void initMap(){
        //获取地图控件引用
        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap=mMapView.getMap();
        //设置地图范围
        LatLng lat1=new LatLng(30.75382,103.925432);//左下
        LatLng lat2=new LatLng(30.744013,103.943398);//右下
        LatLng lat3=new LatLng(30.767118,103.937307);//左上
        LatLng lat4=new LatLng(30.75742,103.950063);//右上
        latLngBounds=new LatLngBounds.Builder().include(lat1).include(lat2).include(lat3).include(lat4).build();
        mBaiduMap.setMapStatusLimits(latLngBounds);
        //MapStatusUpdate update=MapStatusUpdateFactory.zoomTo(17);
        //设置缩放级别
        mBaiduMap.setMaxAndMinZoomLevel(21,17);
        //mBaiduMap.animateMapStatus(update);
    }


    //定位初始化
    private void initLocation(){
        mBaiduMap.setMyLocationEnabled(true);
        //定位初始化
        mLocationClient = new LocationClient(this);
        //通过LocationClientOption设置LocationClient相关参数
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);

        //设置locationClientOption
        mLocationClient.setLocOption(option);

        //注册LocationListener监听器
        MyLocationListener myLocationListener = new MyLocationListener();
        mLocationClient.registerLocationListener(myLocationListener);
        //开启地图定位图层
        mLocationClient.start();
    }

    //创建POI检索监听器 、设置检索监听器
    private void createPOIListener(){
        OnGetPoiSearchResultListener listener = new OnGetPoiSearchResultListener() {
            @Override
            public void onGetPoiResult(PoiResult poiResult) {

                if (poiResult == null || poiResult.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {
                    Log.d("poi","NO !!!!!");
                    return;
                }
                if (poiResult.error == SearchResult.ERRORNO.NO_ERROR) {
                    mBaiduMap.clear();
                   Log.d("poi","YES !!!!!");

                    //创建PoiOverlay对象
                    com.baidu.mapapi.overlayutil.PoiOverlay poiOverlay = new com.baidu.mapapi.overlayutil.PoiOverlay(mBaiduMap);

                    //设置Poi检索数据
                    poiOverlay.setData(poiResult);
                    List a=poiResult.getAllPoi();
                    Log.d("result",a.toString());

                        //将poiOverlay添加至地图并缩放至合适级别
                        poiOverlay.addToMap();
                        poiOverlay.zoomToSpan();
                    }
            }

            @Override
            public void onGetPoiDetailResult(PoiDetailSearchResult poiDetailSearchResult) {

            }
            @Override
            public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

            }
            //废弃
            @Override
            public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {

            }
        };
        //设置检索监听器
        mPoiSearch = PoiSearch.newInstance();
        mPoiSearch.setOnGetPoiSearchResultListener(listener);
        searchBounds = new LatLngBounds.Builder()
                .include(new LatLng(30.75386,103.926087))
                .include(new LatLng(30.75742,103.950063))
                .build();

    }

    private void initEditText(){
        editText=(AutoCompleteTextView) findViewById(R.id.inputtext);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String keyword=editText.getText().toString();
                mSuggestionSearch.requestSuggestion(new SuggestionSearchOption()
                        .citylimit(true)
                        .city("成都")
                        .keyword(keyword));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


    }

    private void initButton(){
        button=(Button)findViewById(R.id.sbutton);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickbutton=true;
                Log.d("Button:","Click Button");
                String keyword=editText.getText().toString();
                Log.d("EditText:",keyword.toString());
                mSuggestionSearch.requestSuggestion(new SuggestionSearchOption()
                        .citylimit(true)
                        .city("成都")
                        .keyword(keyword));
               /* mPoiSearch.searchNearby(new PoiNearbySearchOption()
                        .location(new LatLng(30.756094,103.937872))
                        .radius(2000)
                        .keyword(keyword)
                        .pageNum(10));*/
            }
        });
    }
    private void initPlaceSearch() {
        mSuggestionSearch = SuggestionSearch.newInstance();


        OnGetSuggestionResultListener listener = new OnGetSuggestionResultListener() {
            @Override
            public void onGetSuggestionResult(SuggestionResult suggestionResult) {
                //处理sug检索结果
                if(suggestionResult==null||suggestionResult.error==SearchResult.ERRORNO.RESULT_NOT_FOUND){
                    Log.d("Search","NO !!!!!");
                    return;
                }
                if(suggestionResult.error == SearchResult.ERRORNO.NO_ERROR)
                {
                    Log.d("Search","YES !!!!!");
                    List<String> suggest = new ArrayList<>();
                    for (SuggestionResult.SuggestionInfo info : suggestionResult.getAllSuggestions()) {
                        if (info.key != null)
                            suggest.add(info.key);
                        if(onClickbutton==true)
                        {
                            destinationPosition=new LatLng(info.getPt().latitude,info.getPt().longitude);

                            MapStatus mMapStatus = new MapStatus.Builder()
                                    .target(destinationPosition)
                                    .zoom(18)
                                    .build();
                            MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
                            //改变地图状态
                            mBaiduMap.setMapStatus(mMapStatusUpdate);
                            onClickbutton=false;
                            mBaiduMap.clear();
                            BitmapDescriptor bitmap = BitmapDescriptorFactory.fromResource(R.drawable.icon_end);

                            //构建MarkerOption，用于在地图上添加Marker
                            OverlayOptions option = new MarkerOptions()
                                    .position(destinationPosition)
                                    .icon(bitmap);

                            //在地图上添加Marker，并显示
                            mBaiduMap.addOverlay(option);
                            BikeRouteNodeInfo bikeStartInfo=new BikeRouteNodeInfo();
                            BikeRouteNodeInfo bikeEndInfo=new BikeRouteNodeInfo();
                            bikeStartInfo.setLocation(currentPosition);
                            bikeEndInfo.setLocation(destinationPosition);

                            WalkRouteNodeInfo walkStartInfo=new WalkRouteNodeInfo();
                            WalkRouteNodeInfo walkEndInfo=new WalkRouteNodeInfo();
                            walkStartInfo.setLocation(currentPosition);
                            walkEndInfo.setLocation(destinationPosition);

                            bikeParam = new BikeNaviLaunchParam().startNodeInfo(bikeStartInfo).endNodeInfo(bikeEndInfo);
                            walkParam=new WalkNaviLaunchParam().startNodeInfo(walkStartInfo).endNodeInfo(walkEndInfo);

                        }
                    }
                    sugAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_dropdown_item_1line, suggest);
                    editText.setAdapter(sugAdapter);
                    sugAdapter.notifyDataSetChanged();


                    return;
                }
            }
        };
        mSuggestionSearch.setOnGetSuggestionResultListener(listener);


    }


    /**
     * 开始骑行导航
     */
    private void startBikeNavi() {
        Log.d(TAG, "startBikeNavi");
        BikeNavigateHelper.getInstance().initNaviEngine(this, new IBEngineInitListener() {
            @Override
            public void engineInitSuccess() {
                //骑行导航初始化成功之后的回调
                routePlanWithBikeParam();
            }

            @Override
            public void engineInitFail() {
                //骑行导航初始化失败之后的回调
            }
        });
    }
    /**
     * 发起骑行导航算路
     */
    private void routePlanWithBikeParam() {
        BikeNavigateHelper.getInstance().routePlanWithRouteNode(bikeParam, new IBRoutePlanListener() {
            @Override
            public void onRoutePlanStart() {
                Log.d(TAG, "BikeNavi onRoutePlanStart");
            }

            @Override
            public void onRoutePlanSuccess() {
                Log.d(TAG, "BikeNavi onRoutePlanSuccess");
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, BNaviGuideActivity.class);
                startActivity(intent);
            }

            @Override
            public void onRoutePlanFail(BikeRoutePlanError error) {
                Log.d(TAG, "BikeNavi onRoutePlanFail");
            }

        });
    }


    /**
     * 开始步行导航
     */
    private void startWalkNavi() {
        Log.d(TAG, "startBikeNavi");
        try {
            WalkNavigateHelper.getInstance().initNaviEngine(this, new IWEngineInitListener() {
                @Override
                public void engineInitSuccess() {
                    Log.d(TAG, "WalkNavi engineInitSuccess");
                    routePlanWithWalkParam();
                }

                @Override
                public void engineInitFail() {
                    Log.d(TAG, "WalkNavi engineInitFail");
                }
            });
        } catch (Exception e) {
            Log.d(TAG, "startBikeNavi Exception");
            e.printStackTrace();
        }
    }

    /**
     * 发起步行导航算路
     */
    private void routePlanWithWalkParam() {
        WalkNavigateHelper.getInstance().routePlanWithRouteNode(walkParam, new IWRoutePlanListener() {
            @Override
            public void onRoutePlanStart() {
                Log.d(TAG, "WalkNavi onRoutePlanStart");
            }

            @Override
            public void onRoutePlanSuccess() {
                Log.d("View", "onRoutePlanSuccess");
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, WNaviGuideActivity.class);
                startActivity(intent);
            }

            @Override
            public void onRoutePlanFail(WalkRoutePlanError error) {
                Log.d(TAG, "WalkNavi onRoutePlanFail");
            }

        });
    }

}
