package com.meteology.radarnavi;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
//import android.content.Context;

//import com.baidu.navisdk.R;
import com.baidu.navisdk.adapter.BNRouteGuideManager;
import com.baidu.navisdk.adapter.BNRouteGuideManager.CustomizedLayerItem;
import com.baidu.navisdk.adapter.BNRouteGuideManager.OnNavigationListener;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
/*import com.baidu.navisdk.comapi.mapcontrol.BNMapController;
import com.baidu.navisdk.comapi.mapcontrol.BNMapViewFactory;
import com.baidu.nplatform.comapi.basestruct.GeoPoint;
import com.baidu.nplatform.comapi.map.MapGLSurfaceView;
import com.baidu.nplatform.comapi.map.OverlayItem;
import com.baidu.navisdk.comapi.mapcontrol.BNMapItemizedOverlay;*/

import com.meteology.radarnavi.R;


public class NaviGuideActivity extends Activity {

	private BNRoutePlanNode mBNRoutePlanNode = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		createHandler();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
		}		
		View view = BNRouteGuideManager.getInstance().onCreate(this,
				new OnNavigationListener() {

					@Override
					public void onNaviGuideEnd() {
						finish();
					}

					@Override
					public void notifyOtherAction(int actionType, int arg1,
							int arg2, Object obj) {

					}
				});

		if (view != null) {
			setContentView(view);
		}
		
		Intent intent = getIntent();
		if (intent != null) {
			Bundle bundle = intent.getExtras();
			if (bundle != null) {
				mBNRoutePlanNode = (BNRoutePlanNode) bundle
						.getSerializable(MainActivity.ROUTE_PLAN_NODE);
			}
		}
		
		//MapGLSurfaceView nMapView = BNMapViewFactory.getInstance().getMainMapView();
		/*Context mContext = null;
		Bundle b = null;
		@SuppressWarnings("unused")
		MapGLSurfaceView nMapView =  BNMapController.getInstance().initMapView(mContext, b);
		
		OverlayItem item = new OverlayItem(new GeoPoint((int)mBNRoutePlanNode.getLongitude(),
				(int)mBNRoutePlanNode.getLatitude()),"ICON","NAVI");
		item.setMarker(getResources().getDrawable(R.drawable.ic_launcher));
        item.setAnchor(OverlayItem.ALING_CENTER);
        BNMapItemizedOverlay.getInstance().removeAll();
        BNMapItemizedOverlay.getInstance().addItem(item);
        BNMapItemizedOverlay.getInstance().show();
        BNMapViewFactory.getInstance().getMainMapView().refresh(BNMapItemizedOverlay.getInstance());
        BNMapController.getInstance().locateWithAnimation((int)mBNRoutePlanNode.getLongitude(),
				(int)mBNRoutePlanNode.getLatitude());*/
		
	}

	@Override
	protected void onResume() {
		BNRouteGuideManager.getInstance().onResume();
		super.onResume();

		hd.sendEmptyMessageDelayed(MSG_SHOW, 5000);
	}

	protected void onPause() {
		super.onPause();
		BNRouteGuideManager.getInstance().onPause();
	};

	@Override
	protected void onDestroy() {
		BNRouteGuideManager.getInstance().onDestroy();
		super.onDestroy();
	}

	@Override
	protected void onStop() {
		BNRouteGuideManager.getInstance().onStop();
		super.onStop();
	}

	@Override
	public void onBackPressed() {
		BNRouteGuideManager.getInstance().onBackPressed(false);
	}

	public void onConfigurationChanged(
			android.content.res.Configuration newConfig) {
		BNRouteGuideManager.getInstance().onConfigurationChanged(newConfig);
		super.onConfigurationChanged(newConfig);
	};

	private void addCustomizedLayerItems() {
		List<CustomizedLayerItem> items = new ArrayList<CustomizedLayerItem>();
		CustomizedLayerItem item1 = null;
		if (mBNRoutePlanNode != null) {
			Drawable l_item = getResources().getDrawable(R.drawable.ic_launcher);
			l_item.setAlpha(50);
			item1 = new CustomizedLayerItem(mBNRoutePlanNode.getLongitude(),
					mBNRoutePlanNode.getLatitude(),					
					mBNRoutePlanNode.getCoordinateType(), l_item,
					CustomizedLayerItem.ALIGN_CENTER);
			items.add(item1);

			BNRouteGuideManager.getInstance().setCustomizedLayerItems(items);
		}
		BNRouteGuideManager.getInstance().showCustomizedLayer(true);
	}

	private static final int MSG_SHOW = 1;
	private static final int MSG_HIDE = 2;
	private Handler hd = null;

	private void createHandler() {
		if (hd == null) {
			hd = new Handler(getMainLooper()) {
				public void handleMessage(android.os.Message msg) {
					if (msg.what == MSG_SHOW) {
						//addCustomizedLayerItems();
						// hd.sendEmptyMessageDelayed(MSG_HIDE, 5000);
					} else if (msg.what == MSG_HIDE) {
						//BNRouteGuideManager.getInstance().showCustomizedLayer(
						//		false);
						// hd.sendEmptyMessageDelayed(MSG_SHOW, 5000);
					}

				};
			};
		}
	}
}
