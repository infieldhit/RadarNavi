package com.meteology.radarnavi;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.meteology.radarnavi.R;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.GroundOverlayOptions;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.MyLocationConfiguration.LocationMode;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPHTTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;


@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class MainActivity extends Activity {
	
    /**
     * ��ͼ�ؼ�
     */
    private MapView mMapView = null;
    /**
     * ��ͼʵ��
     */
    private BaiduMap mBaiduMap;
    /**
     * ��λ�Ŀͻ���
     */
    private LocationClient mLocationClient;
    /**
     * ��ǰ��λ��ģʽ
     */
    private LocationMode mCurrentMode = LocationMode.NORMAL;
    /**
     * ��λ�ļ�����
     */
    public MyLocationListener mMyLocationListener;
    /***
     * �Ƿ��ǵ�һ�ζ�λ
     */
    private volatile boolean isFristLocation = true;
    /**
     * ����һ�εľ�γ��
     */
    private double mCurrentLantitude;
    private double mCurrentLongitude;
    /**
     * ��ǰ�ľ���
     */
    private float mCurrentAccracy;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        //��ʹ��SDK�����֮ǰ��ʼ��context��Ϣ������ApplicationContext
        //ע��÷���Ҫ��setContentView����֮ǰʵ��
        SDKInitializer.initialize(getApplicationContext());
        
		setContentView(R.layout.activity_main);
		findViewById(R.id.marker_progress).setVisibility(View.GONE);
		
        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        
	    // add ground overlay
        BitmapDescriptor bdGround = BitmapDescriptorFactory
    			.fromResource(R.drawable.ground_overlay);
	    LatLng southwest = new LatLng(38.92235, 115.380338);
	    LatLng northeast = new LatLng(40.947246, 117.414977);
	    LatLngBounds bounds = new LatLngBounds.Builder().include(northeast)
	    		.include(southwest).build();
	    OverlayOptions ooGround = new GroundOverlayOptions()
	    		.positionFromBounds(bounds).image(bdGround).transparency(0.8f);
	    mBaiduMap.addOverlay(ooGround);
	    MapStatusUpdate u = MapStatusUpdateFactory
	    		.newLatLng(bounds.getCenter());
	    mBaiduMap.setMapStatus(u);
        
        mBaiduMap.setMyLocationEnabled(true);
        //MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(15.0f);
        //mBaiduMap.setMapStatus(msu);
        MyLocationConfiguration config = new MyLocationConfiguration(mCurrentMode, true, null);
        mBaiduMap.setMyLocationConfigeration(config);
        initLocation();
        //Log.e("mylog", "test log");
        //LatLng ll = new LatLng(mCurrentLantitude, mCurrentLongitude);
        //MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
        //mBaiduMap.animateMapStatus(u);
        
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();   
        StrictMode.setThreadPolicy(policy);
	}
	
	/**
	 * @Title: initLocation
	 * @Description: ��ʼ����λ��ش���
	 * @return: void
	 */
	private void initLocation() {
	    // ��λ��ʼ��
	    mLocationClient = new LocationClient(this);
	    mMyLocationListener = new MyLocationListener();
	    mLocationClient.registerLocationListener(mMyLocationListener);
	    // ���ö�λ���������
	    LocationClientOption option = new LocationClientOption();
	    option.setOpenGps(true);// ��gps
	    option.setCoorType("bd09ll"); // ������������
	    //option.setAddrType("all");
	    option.setScanSpan(1000);
	    mLocationClient.setLocOption(option);
	    mLocationClient.start();
	    //mLocationClient.requestLocation();
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_syncdata) {        	
        	findViewById(R.id.marker_progress).setVisibility(View.VISIBLE);
        	FTPClient ftp = new FTPClient();
        	try
        	{
        		
        		String local = Environment.getExternalStorageDirectory().getAbsolutePath() + "/dn.sh";
        		String remote = "test.sh";
        		Log.e("mylog", "before ftp connect");
        		ftp.connect("192.168.0.100");
        		Log.e("mylog", "connected to server");
	        	ftp.login("rtr", "Gtrt*62??");
	        	//ftp.changeWorkingDirectory("");
	        	ftp.setFileType(FTP.BINARY_FILE_TYPE);
	        	OutputStream output;
	        	output=new FileOutputStream(local);
	        	ftp.enterLocalPassiveMode();
	        	Log.e("mylog", "before file transfer");
	        	ftp.retrieveFile(remote, output);
	        	output.close();
	        	Log.e("mylog", "before ftp logout");
	        	ftp.logout();
	        	ftp.disconnect();
        	}
        	/*catch (IOException e)
        	{
        		Log.e("mylog", "caught exception");
        		if (ftp.isConnected())
        		{
	                try
	                {
	                    ftp.disconnect();
	                }
	                catch (IOException f)
	                {
	                    // do nothing
	                }
        		}
        	}*/
        	catch (Exception e)
        	{
        		Log.e("mylog", "caught exception " + e.getMessage() + " " + ftp.getReplyCode());
        	}
        	findViewById(R.id.marker_progress).setVisibility(View.GONE);
            return true;
        }
        else if (id == R.id.action_settings) {
        	return true;
        }
        return super.onOptionsItemSelected(item);
    }
	
    /*@Override
    protected void onStart() {
        // ����ͼ�㶨λ
        // ��δ���ǳ���Ҫ
        mBaiduMap.setMyLocationEnabled(true);
        if (!mLocationClient.isStarted())
            mLocationClient.start();
        //mLocationClient.requestLocation();
        super.onStart();
    }
	
    @Override
    protected void onStop() {
        // �ر�ͼ�㶨λ
        mBaiduMap.setMyLocationEnabled(false);
        mLocationClient.stop();
        super.onStop();
    }*/

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationClient.stop();
        mBaiduMap.setMyLocationEnabled(false);
        //��activityִ��onDestroyʱִ��mMapView.onDestroy()��ʵ�ֵ�ͼ�������ڹ���
        mMapView.onDestroy();
        mMapView = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        //��activityִ��onResumeʱִ��mMapView. onResume ()��ʵ�ֵ�ͼ�������ڹ���
        mMapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //��activityִ��onPauseʱִ��mMapView. onPause ()��ʵ�ֵ�ͼ�������ڹ���
        mMapView.onPause();
    }
	
    class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
        	//Log.e("mylog", "in onReceiveLocation");
            // map view ���ٺ��ڴ����½��յ�λ��
            if (location == null || mMapView == null)
                return;
            // ���춨λ����
            MyLocationData locData = new MyLocationData.Builder().accuracy(location.getRadius())
                    // �˴����ÿ����߻�ȡ���ķ�����Ϣ��˳ʱ��0-360
                    .direction(100).latitude(location.getLatitude()).longitude(location.getLongitude()).build();
            //mCurrentAccracy = location.getRadius();
            // ���ö�λ����
            mBaiduMap.setMyLocationData(locData);
            //mCurrentLantitude = location.getLatitude();
            //mCurrentLongitude = location.getLongitude();
            // �����Զ���ͼ��
            //BitmapDescriptor mCurrentMarker = BitmapDescriptorFactory.fromResource(R.drawable.navi_map_gps_locked);
            //BitmapDescriptor mCurrentMarker = BitmapDescriptorFactory.fromResource(R.mipmap.navi_map_gps_locked);
            //MyLocationConfiguration config = new MyLocationConfiguration(mCurrentMode, true, mCurrentMarker);
            MyLocationConfiguration config = new MyLocationConfiguration(mCurrentMode, true, null);
            mBaiduMap.setMyLocationConfigeration(config);
            //Log.e("mylog", "address:"+location.getAddrStr());
            // ��һ�ζ�λʱ������ͼλ���ƶ�����ǰλ��
            /*if (isFristLocation) {
                isFristLocation = false;
                LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
                MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
                mBaiduMap.animateMapStatus(u);
            }*/

        }

    }
}


