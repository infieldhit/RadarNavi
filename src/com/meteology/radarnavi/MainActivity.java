package com.meteology.radarnavi;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.meteology.radarnavi.R;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BaiduMap.OnMarkerDragListener;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.GroundOverlayOptions;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.MyLocationConfiguration.LocationMode;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.baidu.mapapi.overlayutil.DrivingRouteOverlay;
import com.baidu.mapapi.overlayutil.OverlayManager;
import com.baidu.mapapi.search.core.RouteLine;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.route.DrivingRoutePlanOption;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRouteResult;
import com.baidu.navisdk.adapter.BNOuterTTSPlayerCallback;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BNRoutePlanNode.CoordinateType;
import com.baidu.navisdk.adapter.BaiduNaviManager;
import com.baidu.navisdk.adapter.BaiduNaviManager.NaviInitListener;
import com.baidu.navisdk.adapter.BaiduNaviManager.RoutePlanListener;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPHTTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;


@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class MainActivity extends Activity implements OnGetRoutePlanResultListener {
	
	public static final String TAG = "RadarNavi";
	private static final String APP_FOLDER_NAME = "RadarNaviApp";
	public static final String ROUTE_PLAN_NODE = "routePlanNode";
	
    /**
     * 地图控件
     */
    private MapView mMapView = null;
    /**
     * 地图实例
     */
    private BaiduMap mBaiduMap;
    /**
     * 定位的客户端
     */
    private LocationClient mLocationClient;
    /**
     * 当前定位的模式
     */
    private LocationMode mCurrentMode = LocationMode.NORMAL;
    /**
     * 定位的监听器
     */
    public MyLocationListener mMyLocationListener;
    /***
     * 是否是第一次定位
     */
    private volatile boolean isFirstLocation = true;
    /**
     * 最新一次的经纬度
     */
    private double mCurrentLantitude;
    private double mCurrentLongitude;
    /**
     * 当前的精度
     */
    private float mCurrentAccracy;
    
    RoutePlanSearch mSearch = null;
    Marker mMarkerDest/*,mMarkerCand*/;
    RouteLine route = null;
    OverlayManager routeOverlay = null;
    
    private String mSDCardPath = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        //注意该方法要再setContentView方法之前实现
        SDKInitializer.initialize(getApplicationContext());
        
		setContentView(R.layout.activity_main);
		
        if (initDirs())
        {
        	initNavi();
        }
		
		findViewById(R.id.marker_progress).setVisibility(View.GONE);
		//findViewById(R.id.btn_navi).setVisibility(View.GONE);
		//findViewById(R.id.btn_plan).setVisibility(View.GONE);
		
        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        
        // add dest loc markers        
        BitmapDescriptor bdDest = BitmapDescriptorFactory.fromResource(R.drawable.icon_geo);
        //BitmapDescriptor bdCand = BitmapDescriptorFactory.fromResource(R.drawable.icon_geo);
        
        LatLng llDest = new LatLng(29.293, 113.088);
        //LatLng llCand = new LatLng(39.942821, 116.369199);
        
        OverlayOptions ooDest = new MarkerOptions().position(llDest).icon(bdDest).zIndex(5).draggable(true);
        mMarkerDest = (Marker)(mBaiduMap.addOverlay(ooDest));
        //OverlayOptions ooCand = new MarkerOptions().position(llCand).icon(bdCand).zIndex(5);
        //mMarkerCand = (Marker)(mBaiduMap.addOverlay(ooCand));
        
	    // add ground overlay
        //BitmapDescriptor bdGround = BitmapDescriptorFactory
    	//		.fromResource(R.drawable.ground_overlay);
        //BitmapDescriptor bdRadar = BitmapDescriptorFactory.fromPath(mSDCardPath+"/"+APP_FOLDER_NAME+"/radar.dat");
        BitmapDescriptor bdRadar = BitmapDescriptorFactory.fromResource(R.drawable.radar);
	    //LatLng southwest = new LatLng(38.92235, 115.380338);
	    //LatLng northeast = new LatLng(40.947246, 117.414977);
        //LatLng southwest = new LatLng(31.40404,110.6146);
	    //LatLng northeast = new LatLng(26.0789,117.0162);
        LatLng northeast = new LatLng(31.40404,117.0162);
        LatLng southwest = new LatLng(26.0789,110.6146);
	    LatLngBounds bounds = new LatLngBounds.Builder().include(northeast)
	    		.include(southwest).build();
	    OverlayOptions ooGround = new GroundOverlayOptions()
	    		.positionFromBounds(bounds).image(bdRadar).transparency(0.8f);
	    mBaiduMap.addOverlay(ooGround);
        
	    //MapStatusUpdate u = MapStatusUpdateFactory
	    //		.newLatLng(bounds.getCenter());
	    MapStatusUpdate u = MapStatusUpdateFactory
	    		.newLatLng(new LatLng(29.293,113.088));
	    
	    mBaiduMap.setMapStatus(u);
	    
	    mBaiduMap.setOnMarkerDragListener(new OnMarkerDragListener() {
			public void onMarkerDrag(Marker marker) {
			}

			public void onMarkerDragEnd(Marker marker) {
				Toast.makeText(MainActivity.this,
						"拖拽结束，新位置：" + marker.getPosition().latitude + ", "
								+ marker.getPosition().longitude,
						Toast.LENGTH_LONG).show();
			}

			public void onMarkerDragStart(Marker marker) {
			}
	    });
        
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
        
		mSearch = RoutePlanSearch.newInstance();
		mSearch.setOnGetRoutePlanResultListener(this);
        
        final Button reqPlanBtn = (Button)findViewById(R.id.btn_plan);
        final OnClickListener btnPlanClickListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				PlanNode stNode = PlanNode.withLocation(new LatLng(mCurrentLantitude, mCurrentLongitude));
        		PlanNode enNode = PlanNode.withLocation(mMarkerDest.getPosition());
        		mSearch.drivingSearch((new DrivingRoutePlanOption()).from(stNode).to(enNode));
        		findViewById(R.id.marker_progress).setVisibility(View.VISIBLE);
			}
        };
        reqPlanBtn.setOnClickListener(btnPlanClickListener);
        
        Button reqResetBtn = (Button)findViewById(R.id.btn_rst);
        OnClickListener btnResetClickListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				reqPlanBtn.setText("plan");
				reqPlanBtn.setOnClickListener(btnPlanClickListener);
				routeOverlay.removeFromMap();
			}
        };
        reqResetBtn.setOnClickListener(btnResetClickListener);
        
        //Button reqNaviBtn = (Button)findViewById(R.id.btn_navi);
        
        //reqNaviBtn.setOnClickListener(btnNaviClickLIstener);
        
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();   
        StrictMode.setThreadPolicy(policy);
	}
	
	/**
	 * @Title: initLocation
	 * @Description: 初始化定位相关代码
	 * @return: void
	 */
	private void initLocation() {
	    // 定位初始化
	    mLocationClient = new LocationClient(this);
	    mMyLocationListener = new MyLocationListener();
	    mLocationClient.registerLocationListener(mMyLocationListener);
	    // 设置定位的相关配置
	    LocationClientOption option = new LocationClientOption();
	    option.setOpenGps(true);// 打开gps
	    option.setCoorType("gcj02"); // 设置坐标类型
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
	        	ftp.enterLocalPassiveMode();
	        	Log.e("mylog", "before file transfer");
	        	File local_file = new File(local);
	        	if (local_file.exists())
	        	{
	        		InputStream input;
	        		input=new FileInputStream(local);
	        		ftp.storeFile(remote, input);
	        		input.close();
	        	}
	        	else
	        	{
		        	OutputStream output;
		        	output=new FileOutputStream(local);
		        	ftp.retrieveFile(remote, output);
		        	output.close();
	        	}
	        	Log.e("mylog", "before ftp logout");
	        	ftp.logout();
	        	ftp.disconnect();
        	}
        	catch (Exception e)
        	{        		
        		Log.e("mylog", "caught exception " + e.getMessage() + " " + ftp.getReplyCode());
        		Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
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
        // 开启图层定位
        // 这段代码非常重要
        mBaiduMap.setMyLocationEnabled(true);
        if (!mLocationClient.isStarted())
            mLocationClient.start();
        //mLocationClient.requestLocation();
        super.onStart();
    }
	
    @Override
    protected void onStop() {
        // 关闭图层定位
        mBaiduMap.setMyLocationEnabled(false);
        mLocationClient.stop();
        super.onStop();
    }*/

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationClient.stop();
        mBaiduMap.setMyLocationEnabled(false);
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
        mMapView = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }
	
    class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
        	//Log.e("mylog", "in onReceiveLocation");
            // map view 销毁后不在处理新接收的位置
            if (location == null || mMapView == null)
                return;
            // 构造定位数据
            MyLocationData locData = new MyLocationData.Builder().accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(100).latitude(location.getLatitude()).longitude(location.getLongitude()).build();
            //mCurrentAccracy = location.getRadius();
            // 设置定位数据
            mBaiduMap.setMyLocationData(locData);
            mCurrentLantitude = location.getLatitude();
            mCurrentLongitude = location.getLongitude();
            
            // 设置自定义图标
            //BitmapDescriptor mCurrentMarker = BitmapDescriptorFactory.fromResource(R.drawable.navi_map_gps_locked);
            //BitmapDescriptor mCurrentMarker = BitmapDescriptorFactory.fromResource(R.mipmap.navi_map_gps_locked);
            //MyLocationConfiguration config = new MyLocationConfiguration(mCurrentMode, true, mCurrentMarker);
            MyLocationConfiguration config = new MyLocationConfiguration(mCurrentMode, true, null);
            mBaiduMap.setMyLocationConfigeration(config);
            //Log.e("mylog", "address:"+location.getAddrStr());
            // 第一次定位时，将地图位置移动到当前位置
            /*if (isFirstLocation) {
                isFirstLocation = false;
                LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
                MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
                mBaiduMap.animateMapStatus(u);
            }*/

        }

    }

	@Override
	public void onGetDrivingRouteResult(DrivingRouteResult result) {
		// TODO Auto-generated method stub
		findViewById(R.id.marker_progress).setVisibility(View.GONE);
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(MainActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
        }
        if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
            //起终点或途经点地址有岐义，通过以下接口获取建议查询信息
            //result.getSuggestAddrInfo()
            return;
        }
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {
            /*nodeIndex = -1;
            mBtnPre.setVisibility(View.VISIBLE);
            mBtnNext.setVisibility(View.VISIBLE);*/
        	final Button reqNaviBtn = (Button)findViewById(R.id.btn_plan);
        	reqNaviBtn.setText("navi");
        	final OnClickListener btnNaviClickListener = new OnClickListener() {

    			@Override
    			public void onClick(View v) {
    				// TODO Auto-generated method stub
    				Log.e("mylog", "mCurrentLantitude="+mCurrentLantitude+" mCurrentLongtitude="+mCurrentLongitude);
    				Log.e("mylog", "mMarkerLan="+mMarkerDest.getPosition().latitude+" mMarkerLong="+mMarkerDest.getPosition().longitude);
    				BNRoutePlanNode stNode = new BNRoutePlanNode(mCurrentLongitude, mCurrentLantitude, "start", null,CoordinateType.GCJ02);
            		BNRoutePlanNode enNode = new BNRoutePlanNode(mMarkerDest.getPosition().longitude,mMarkerDest.getPosition().latitude,"end",null,CoordinateType.GCJ02);
    				//BNRoutePlanNode stNode = new BNRoutePlanNode(116.201427, 23.050877, 
    			    //		"百度大厦", null, CoordinateType.GCJ02);
    				//BNRoutePlanNode enNode = new BNRoutePlanNode(116.397507, 23.798827, 
    			    //		"北京天安门", null, CoordinateType.GCJ02);
            		List<BNRoutePlanNode> list = new ArrayList<BNRoutePlanNode>();
        			list.add(stNode);
        			list.add(enNode);
        			Log.e("mylog", "before entering navi activity");
        			findViewById(R.id.marker_progress).setVisibility(View.VISIBLE);
        			BaiduNaviManager.getInstance().launchNavigator(MainActivity.this, list, 1, true,
        					new DemoRoutePlanListener(stNode));
    			}        	
            };
        	reqNaviBtn.setOnClickListener(btnNaviClickListener);
        	//findViewById(R.id.btn_navi).setVisibility(View.VISIBLE);
        	//findViewById(R.id.btn_plan).setVisibility(View.GONE);
            route = result.getRouteLines().get(0);
            DrivingRouteOverlay overlay = new MyDrivingRouteOverlay(mBaiduMap);
            routeOverlay = overlay;
            mBaiduMap.setOnMarkerClickListener(overlay);
            overlay.setData(result.getRouteLines().get(0));
            overlay.addToMap();
            overlay.zoomToSpan();
        }        
	}

	@Override
	public void onGetTransitRouteResult(TransitRouteResult arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onGetWalkingRouteResult(WalkingRouteResult arg0) {
		// TODO Auto-generated method stub
		
	}
	
    //定制RouteOverly
    private class MyDrivingRouteOverlay extends DrivingRouteOverlay {

        public MyDrivingRouteOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public BitmapDescriptor getStartMarker() {
            //if (useDefaultIcon) {
                return BitmapDescriptorFactory.fromResource(R.drawable.icon_st);
            //}
            //return null;
        }

        @Override
        public BitmapDescriptor getTerminalMarker() {
            //if (useDefaultIcon) {
                return BitmapDescriptorFactory.fromResource(R.drawable.icon_en);
            //}
            //return null;
        }
    }
    
	private boolean initDirs() {
		mSDCardPath = getSdcardDir();
		if (mSDCardPath == null) {
			return false;
		}
		File f = new File(mSDCardPath, APP_FOLDER_NAME);
		if (!f.exists()) {
			try {
				f.mkdir();
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	String authinfo = null;

	private void initNavi() {
		BaiduNaviManager.getInstance().setNativeLibraryPath(
				mSDCardPath + "/BaiduNaviSDK_SO");
		BaiduNaviManager.getInstance().init(this, mSDCardPath, APP_FOLDER_NAME,
				new NaviInitListener() {
					@Override
					public void onAuthResult(int status, String msg) {
						if (0 == status) {
							authinfo = "key校验成功!";
						} else {
							authinfo = "key校验失败, " + msg;
						}
						MainActivity.this.runOnUiThread(new Runnable() {

							@Override
							public void run() {
								Toast.makeText(MainActivity.this,
										authinfo, Toast.LENGTH_LONG).show();
							}
						});

					}

					public void initSuccess() {
						Toast.makeText(MainActivity.this, "百度导航引擎初始化成功",
								Toast.LENGTH_SHORT).show();
					}

					public void initStart() {
						Toast.makeText(MainActivity.this, "百度导航引擎初始化开始",
								Toast.LENGTH_SHORT).show();
					}

					public void initFailed() {
						Toast.makeText(MainActivity.this, "百度导航引擎初始化失败",
								Toast.LENGTH_SHORT).show();
					}
				}, null /* mTTSCallback */);
	}

	private String getSdcardDir() {
		if (Environment.getExternalStorageState().equalsIgnoreCase(
				Environment.MEDIA_MOUNTED)) {
			return Environment.getExternalStorageDirectory().toString();
		}
		return null;
	}
	
	public class DemoRoutePlanListener implements RoutePlanListener {

		private BNRoutePlanNode mBNRoutePlanNode = null;

		public DemoRoutePlanListener(BNRoutePlanNode node) {
			mBNRoutePlanNode = node;
		}

		@Override
		public void onJumpToNavigator() {
			Log.e("mylog", "try to enter guide act");
			findViewById(R.id.marker_progress).setVisibility(View.GONE);
			Intent intent = new Intent(MainActivity.this,
					NaviGuideActivity.class);
			Bundle bundle = new Bundle();
			bundle.putSerializable(ROUTE_PLAN_NODE,
					(BNRoutePlanNode) mBNRoutePlanNode);
			intent.putExtras(bundle);
			startActivity(intent);
		}

		@Override
		public void onRoutePlanFailed() {
			// TODO Auto-generated method stub
			Log.e("mylog", "route plan failed");
			findViewById(R.id.marker_progress).setVisibility(View.GONE);
		}
	}
	
private BNOuterTTSPlayerCallback mTTSCallback = new BNOuterTTSPlayerCallback() {
		
		@Override
		public void stopTTS() {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void resumeTTS() {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void releaseTTSPlayer() {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public int playTTSText(String speech, int bPreempt) {
			// TODO Auto-generated method stub
			return 0;
		}
		
		@Override
		public void phoneHangUp() {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void phoneCalling() {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void pauseTTS() {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void initTTSPlayer() {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public int getTTSState() {
			// TODO Auto-generated method stub
			return 0;
		}
	};
}


