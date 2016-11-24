package com.example.administrator.stealwifitest.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.example.administrator.stealwifitest.Utils.L;

/**
 * Created by Zan on 2016/11/22.
 */

public class PostService extends Service {
	public static final String TAG = "PostService";

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		L.e(TAG,"onBind");
		return null;
	}

	@Override
	public void onCreate() {
		L.e(TAG,"onCreate");
		super.onCreate();
	}



	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		L.e(TAG,"onStartCommand");
		String lan = intent.getStringExtra("lan");
		L.e("lan","changed",lan);
		String mac = intent.getStringExtra("mac");
		L.e("mac",mac);
		Toast.makeText(this.getApplicationContext(),mac,Toast.LENGTH_LONG).show();
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public boolean stopService(Intent name) {
		L.e(TAG,"stopService");
		return super.stopService(name);
	}

	@Override
	public void onDestroy() {
		L.e(TAG,"onDestroy");
		super.onDestroy();
	}
}
