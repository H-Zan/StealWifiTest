package com.example.administrator.stealwifitest;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

	private String postion;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
//		MaiManager.getInstance(this).getSysInfo(this);
//		postion=MaiManager.getPostion();
//		L.e("gps-已打开", postion);
	}
}
