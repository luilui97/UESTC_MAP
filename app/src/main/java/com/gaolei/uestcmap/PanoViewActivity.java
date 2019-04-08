package com.gaolei.uestcmap;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.lbsapi.BMapManager;
import com.baidu.lbsapi.panoramaview.ImageMarker;
import com.baidu.lbsapi.panoramaview.OnTabMarkListener;
import com.baidu.lbsapi.panoramaview.PanoramaView;
import com.baidu.lbsapi.panoramaview.PanoramaViewListener;
import com.baidu.lbsapi.panoramaview.TextMarker;
import com.baidu.lbsapi.tools.Point;

import static com.baidu.lbsapi.panoramaview.PanoramaView.ImageDefinition.ImageDefinitionHigh;

public class PanoViewActivity extends Activity {

    private static final String LTAG = "BaiduPanoSDKDemo";

    private PanoramaView mPanoView;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);

        // 先初始化BMapManager
        Log.d("SSSSSSSSS","sssssssssssssssssssssssssssss");
        initBMapManager();
        setContentView(R.layout.activity_pano_view);

        mPanoView=(PanoramaView)findViewById(R.id.panorama) ;
        double latitude= getIntent().getDoubleExtra("latitude",0);

        double longitude= getIntent().getDoubleExtra("longitude",0);
        Log.d("SSSSSSSSS","s"+latitude+"ssss"+longitude);
        mPanoView.setPanoramaImageLevel(ImageDefinitionHigh);
        mPanoView.setPanoramaViewListener(new PanoramaViewListener() {
            @Override
            public void onDescriptionLoadEnd(String s) {

            }

            @Override
            public void onLoadPanoramaBegin() {

            }

            @Override
            public void onLoadPanoramaEnd(String s) {

            }

            @Override
            public void onLoadPanoramaError(String s) {

            }

            @Override
            public void onMessage(String s, int i) {

            }

            @Override
            public void onCustomMarkerClick(String s) {

            }

            @Override
            public void onMoveStart() {

            }

            @Override
            public void onMoveEnd() {

            }
        });
        mPanoView.setPanorama(longitude,latitude);
        //mPanoView.setPanorama(103.936093,30.756464);

    }

    private void initBMapManager() {
        MapApplication app = (MapApplication) this.getApplication();
        if (app.mBMapManager == null) {
            app.mBMapManager = new BMapManager(app);
            app.mBMapManager.init(new MapApplication.MyGeneralListener());
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        mPanoView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPanoView.onResume();
    }

    @Override
    protected void onDestroy() {
        mPanoView.destroy();
        super.onDestroy();
    }
}
