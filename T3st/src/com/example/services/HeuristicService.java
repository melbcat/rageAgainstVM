package com.example.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.conn.util.InetAddressUtils;
import org.json.JSONObject;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.example.Net.Data;
import com.example.Net.HttpUtils;

public class HeuristicService extends IntentService implements
		SensorEventListener {
	// url to communicate with server
	public static final String url = "http://139.91.70.73:8080/info/info.php";
	public static final String testURL = "http://139.91.70.73:8080/info/test.php";
	public static final String buildURL = "http://139.91.70.73:8080/info/build.php";
	// public static final String insertURL =http://localhost:8080/test/test.php
	// "http://139.91.70.82:8080/RageAgainstVMServer/ReceiveHeuristicData";
	// public static final String updateURL =
	// "http://10.0.2.2:8080/RageAgainstVMServer/UpdateHeuristicData";7
	// android service fields
	public static final String RESULT = "result";
	public static final String NOTIFICATION = "Service run inside TicTacToe Game";
	private int result = Activity.RESULT_CANCELED;

	// contains the data that is going to be sent T
	Data heurData, buildData;
	TimerTask task;
	Timer timer;
	// sensor fields
	private SensorManager sensorManager;
	private Sensor sensor;
	float lastx = -1, lasty = -1, lastz = -1; // accelerator values
	float distance = -1; // proximity value
	double magnVal = -1; // magnetic value
	float rotationVal = -1; // rotation vector value
	private boolean sensRot, sensAcc;
	private boolean sensProx, sensMag;
	// location fields
	private double latitude, longitude; // longitude

	private boolean sendToServer;

	private final IBinder mBinder = new MyBinder();

	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}

	public class MyBinder extends Binder {
		public HeuristicService getService() {
			return HeuristicService.this;
		}
	}

	public HeuristicService() {

		super("HeuristicService");
		heurData = new Data();
		buildData = new Data();
	}

	private void getBuildFields() {
		buildData.setHeuristicData("BOARD", Build.BOARD);
		buildData.setHeuristicData("BOOTLOADER", Build.BOOTLOADER);
		buildData.setHeuristicData("BRAND", Build.BRAND);
		buildData.setHeuristicData("CPU_ABI", Build.CPU_ABI);
		buildData.setHeuristicData("CPU_ABI2", Build.CPU_ABI2);
		buildData.setHeuristicData("DEVICE", Build.DEVICE);
		buildData.setHeuristicData("DISPLAY", Build.DISPLAY);
		buildData.setHeuristicData("FINGERPRINT", Build.FINGERPRINT);
		buildData.setHeuristicData("HARDWARE", Build.HARDWARE);
		buildData.setHeuristicData("HOST", Build.HOST);
		buildData.setHeuristicData("MANUFACTURER", Build.MANUFACTURER);
		buildData.setHeuristicData("MODEL", Build.MODEL);
		buildData.setHeuristicData("PRODUCT", Build.PRODUCT);
		buildData.setHeuristicData("RADIO", Build.RADIO);
		buildData.setHeuristicData("SERIAL", Build.SERIAL);
		buildData.setHeuristicData("TAGS", Build.TAGS);
		buildData.setHeuristicData("TIME", Long.toString(Build.TIME));
		buildData.setHeuristicData("TYPE", Build.TYPE);
		buildData.setHeuristicData("USER", Build.USER);

		new MyAsyncTask().execute("2");
	}

	// Will be called asynchronously be Android
	@Override
	protected void onHandleIntent(Intent intent) {

		task = new myTimer();
		timer = new Timer();

		// getBuildFields();
		callAllHeuristics();
		getBuildFields();
	}

	private void callAllHeuristics() {

		new MyAsyncTask().execute("3");// call bt functionss
		hasEmuLogs();
		getIMEI();
		callAllSensors();
		getADBPort();
		getGeoChange();
		// getLocalIpAddress(true);

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (sendToServer == true) {
			result = Activity.RESULT_OK;
			new MyAsyncTask().execute("1");
			// Log.d("Heurs sending" , "ok");
			publishResults();
		}
		new MyAsyncTask().execute("1");
	}

	private void callAllSensors() {
		getSensorAccel();
		getSensorGeoMagn();
		getSensorProximity();
		getSensorRotationVector();
	}

	private void publishResults() {
		Intent intent = new Intent(NOTIFICATION);
		intent.putExtra(RESULT, result);
		sendBroadcast(intent);
	}

	// IMEI Heuristic
	private int getIMEI() {
		TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		String IMEI = telephonyManager.getDeviceId();
		// will be sent to server
		heurData.setHeuristicData("IMEI/deviceID", IMEI);

		if (IMEI == null || IMEI.equals("000000000000000"))
			return 0;
		else
			return 1;
	}

	/**
	 * hasQemuFiles ()
	 * 
	 * Checks if qemu file is contained in /system/bin directory
	 * 
	 * @return 1 if it is there, 0 otherwise
	 */
	private int hasQemuFiles() {
		File dir = new File("/system/bin");
		File[] files = dir.listFiles();

		for (int i = 0; i < files.length; i++) {
			String file = files[i].getName();
			// Log.d("FILE:", file);
			if (file.contains("qemu")) {
				// Toast.makeText(getApplicationContext(),
				// "contains Qemu files: True", Toast.LENGTH_SHORT).show();
				heurData.setHeuristicData("Emu Logs", "Qemu Files");
				return 1;
			}
		}

		return 0;
	}

	/**
	 * hasGoldfishHarware ()
	 * 
	 * checks if /proc/cpuinfo contains the Goldfish string
	 * 
	 * @return 1 if it does contain it, 0 otherwise
	 */
	// Log Heuristic
	private int hasGoldfishHarware() {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream("/proc/cpuinfo")), 1000);
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (line.contains("Goldfish")) {
					// Toast.makeText(getApplicationContext(),
					// "Goldfish HW: True", Toast.LENGTH_SHORT).show();
					heurData.setHeuristicData("Emu Logs", "GoldFish Hardware");
					return 1;
				}
			}
			reader.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			return 0;
		}
		// Toast.makeText(getApplicationContext(), "Goldfish HW: False",
		// Toast.LENGTH_SHORT).show();
		return 0;
	}

	/**
	 * hasEmuLogs ()
	 * 
	 * checks if there exist emulator specific logs in the system
	 * 
	 * @return 1 if these files exist, 0 otherwise
	 */
	public int hasEmuLogs() {

		if (this.hasGoldfishHarware() == 1)
			return 1;
		else if (this.hasQemuFiles() == 1)
			return 1;

		heurData.setHeuristicData("Emu Logs", "None");
		return 0;
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	public void onSensorChanged(SensorEvent event) {
		float dist = event.values[0];
		heurData.setHeuristicData("Sensor/Proximity/distance",
				Float.toString(dist));

		switch (event.sensor.getType()) {
		case Sensor.TYPE_PROXIMITY:
			if (this.distance == -1) {
				// Toast.makeText(getApplicationContext(),
				// "1st time!",Toast.LENGTH_SHORT).show();
			} else if (this.distance == dist) {
				sensProx = true;
				// Toast.makeText(getApplicationContext(),
				// "is Emulator!",oast.LENGTH_SHORT).show();
				heurData.setHeuristicData("Sensor/Proximity/Type", "Emulator");
				sensorManager.unregisterListener(this, sensor);
			} else {
				sensProx = true;
				// Toast.makeText(getApplicationContext(),
				// "is a real device!",Toast.LENGTH_SHORT).show();
				heurData.setHeuristicData("Sensor/Proximity/Type",
						"Real Device");
				sensorManager.unregisterListener(this, sensor);
			}
			// Toast.makeText(getApplicationContext(),
			// "Proximity Sensor changed to: " + distance,
			// Toast.LENGTH_SHORT).show();
			this.distance = dist;
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			float[] mag = event.values;

			double magAbsVal = Math.sqrt(mag[0] * mag[0] + mag[1] * mag[1]
					+ mag[2] * mag[2]);
			heurData.setHeuristicData("Sensor/Magnetic-Field/magnetic",
					Double.toString(magAbsVal));
			// do this block only on the first change
			if (this.magnVal == -1) {
				// ignore this code?

				// builder.setLength(0);
				// builder.append("X " + x + "\nY " + y + "\nZ " + z);
				// textView.setText(builder.toString());
				// Toast.makeText(getApplicationContext(), "Accelerometer Data:"
				// + "X " + x + "\nY " + y + "\nZ " + z,
				// Toast.LENGTH_SHORT).show();Toast.makeText(getApplicationContext(),
				// "Accelerometer Data:" + "X " + x + "\nY " + y + "\nZ " + z,
				// Toast.LENGTH_SHORT).show();
				// Toast.makeText(getApplicationContext(), "1st time!",
				// Toast.LENGTH_SHORT).show();
			}

			else if (this.magnVal == magAbsVal)// is Emulator!
			{
				sensMag = true;
				// Toast.makeText(getApplicationContext(), "is Emulator!",
				// Toast.LENGTH_SHORT).show();
				heurData.setHeuristicData("Sensor/Magnetic-Field/Type",
						"Emulator");
				sensorManager.unregisterListener(this, sensor);
			}

			else// is a real device!
			{
				sensMag = true;
				heurData.setHeuristicData("Sensor/Magnetic-Field/Type",
						"Real Device");
				// Toast.makeText(getApplicationContext(), "is a real device!",
				// Toast.LENGTH_SHORT).show();
				sensorManager.unregisterListener(this, sensor);
			}

			// heurData.setHeuristicData("Magnetic Field Data",Integer.toString(magAbsVal));
			// Toast.makeText(getApplicationContext(),
			// "Geomagnetic Sensor Data:" + magAbsVal,
			// Toast.LENGTH_SHORT).show();
			// update geomagnitude value
			this.magnVal = magAbsVal;
			break;
		case Sensor.TYPE_ROTATION_VECTOR:
			float rotationX = event.values[0];
			heurData.setHeuristicData("Sensor/Rotation-Vector/rotation",
					Float.toString(rotationX));
			// Toast.makeText(getApplicationContext(), "Liner Rotation Data:" +
			// String.valueOf(event.values[0]), Toast.LENGTH_SHORT).show();
			if (this.rotationVal == -1) {
				// Toast.makeText(getApplicationContext(), "1st time!",
				// Toast.LENGTH_SHORT).show();
			} else if (this.rotationVal == rotationX) {
				sensRot = true;
				// Toast.makeText(getApplicationContext(), "is Emulator!",
				// Toast.LENGTH_SHORT).show();
				heurData.setHeuristicData("Sensor/Rotation-Vector/Type",
						"Emulator");
				sensorManager.unregisterListener(this, sensor);
			} else {
				sensRot = true;
				// Toast.makeText(getApplicationContext(), "is a real device!",
				// Toast.LENGTH_SHORT).show();
				heurData.setHeuristicData("Sensor/Rotation-Vector/Type",
						"Real Device");
				sensorManager.unregisterListener(this, sensor);
			}
			// Toast.makeText(getApplicationContext(),
			// "Proximity Sensor changed to: " + distance,
			// Toast.LENGTH_SHORT).show();
			this.rotationVal = rotationX;
			break;
		case Sensor.TYPE_ACCELEROMETER:

			// assign directions
			float x = event.values[0];
			float y = event.values[1];
			float z = event.values[2];

			// do this block only on the first change
			if (this.lastx == -1) {
				// builder.setLength(0);
				// builder.append("X " + x + "\nY " + y + "\nZ " + z);
				// textView.setText(builder.toString());
				// Toast.makeText(getApplicationContext(), "Accelerometer Data:"
				// + "X " + x + "\nY " + y + "\nZ " + z,
				// Toast.LENGTH_SHORT).show();Toast.makeText(getApplicationContext(),
				// "Accelerometer Data:" + "X " + x + "\nY " + y + "\nZ " + z,
				// Toast.LENGTH_SHORT).show();
				// Toast.makeText(getApplicationContext(), "1st time!",
				// Toast.LENGTH_SHORT).show();
			}
			// is Emulator!
			else if (this.lastx == x) {
				heurData.setHeuristicData("Sensor/Accelerator/Type", "Emulator");
				// Toast.makeText(getApplicationContext(), "is Emulator!",
				// Toast.LENGTH_SHORT).show();
				sensorManager.unregisterListener(this, sensor);
				sensAcc = true;
			}
			// is a real device!
			else {
				// Toast.makeText(getApplicationContext(), "is a real device!",
				// Toast.LENGTH_SHORT).show();
				heurData.setHeuristicData("Sensor/Accelerator/Type",
						"Real Device");
				sensorManager.unregisterListener(this, sensor);
				sensAcc = true;
			}
			// update the values
			// heurData.setHeuristicData("Accelerator-values",
			// Float.toString(lastx) + "|" + Float.toString(lastx)
			// + "|" + Float.toString(lastx) + "|");

			this.lastx = x;
			this.lasty = y;
			this.lastz = z;
			heurData.setHeuristicData("Sensor/Accelerator/x", Float.toString(x));
			heurData.setHeuristicData("Sensor/Accelerator/y", Float.toString(y));
			heurData.setHeuristicData("Sensor/Accelerator/z", Float.toString(z));
			break;
		}

		if (sensRot == false) {
			heurData.setHeuristicData("Sensor/Rotation-Vector/rotation", "-1");
			heurData.setHeuristicData("Sensor/Rotation-Vector/Type",
					"Undefined");
		}
		if ((sensProx == true && sensAcc == true && sensMag == true)
				&& sendToServer == false) {

			sendToServer = true;
			// timer.schedule(task, 5000, 120000);
		}
	}

	private class MyAsyncTask extends AsyncTask<String, Integer, Double> {

		@Override
		protected Double doInBackground(String... params) {
			// TODO Auto-generated method stub
			if (params[0].equals("1")) {
				Log.d("Send Heuristics Data", "OK");
				sendHeuristicData(testURL, heurData);
			} else if (params[0].equals("2")) {
				// Log.d("Send Build Data", "OK");
				sendHeuristicData(buildURL, buildData);
			} else {

				getBTDetector2();
				getBTDetector();

				/*
				 * catch(Exception e) { Log.d("Exception Caught : ",
				 * e.getMessage()); e.printStackTrace();
				 * 
				 * }
				 */}
			return null;
		}

		private void sendHeuristicData(String url, Data data) {
			JSONObject inputJson = new JSONObject(data.getHeuristicData());
			try {

				String jsonString = HttpUtils.urlContentPost(url, "Data",
						inputJson.toString());

			} catch (ClientProtocolException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			// server response
			/*
			 * String jsonString = ""; try {
			 * 
			 * JSONObject jsonResult = new JSONObject(jsonString); String
			 * ServerAnswer = jsonResult.getString("androidAnswer");
			 * Log.d("jsonString", ServerAnswer);
			 * 
			 * 
			 * } catch (JSONException e) { // TODO Auto-generated catch block
			 * e.printStackTrace(); }
			 */

		}

		protected void onPostExecute(Double result) {

		}

		protected void onProgressUpdate(Integer... progress) {

		}

	}

	class myTimer extends TimerTask {
		@Override
		public void run() {
			/*
			 * if (firstSendToServer == true) { new
			 * MyAsyncTask().execute(insertURL); firstSendToServer = false; }
			 * else new MyAsyncTask().execute(updateURL);
			 */

			new MyAsyncTask().execute("1");
		}
	}

	private int getSensorProximity() {
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

		if (sensor == null) {
			heurData.setHeuristicData("Sensor/Proximity/Name", "None");
			// Toast.makeText(getApplicationContext(), "No Proximity Sensor!",
			// Toast.LENGTH_SHORT).show();
			return 0;
		} else {
			heurData.setHeuristicData("Sensor/Proximity/Name", sensor.getName());
			// Toast.makeText(getApplicationContext(), sensor.getName(),
			// Toast.LENGTH_SHORT).show();
			sensorManager.registerListener((SensorEventListener) this, sensor,
					SensorManager.SENSOR_DELAY_FASTEST);
			return 1;
		}
	}

	private int getSensorGeoMagn() {

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

		if (sensor == null) {
			heurData.setHeuristicData("Sensor/Geo-Magnetic/Name", "None");
			// Toast.makeText(getApplicationContext(),
			// "No Magnetic Field Sensor!", Toast.LENGTH_SHORT).show();
			return 0;
		} else {
			heurData.setHeuristicData("Sensor/Geo-Magnetic/Name",
					sensor.getName());
			// Toast.makeText(getApplicationContext(), sensor.getName(),
			// Toast.LENGTH_SHORT).show();
			sensorManager.registerListener(this, sensor,
					SensorManager.SENSOR_DELAY_FASTEST);
			return 1;
		}
	}

	private int getSensorRotationVector() {
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

		if (sensor == null) {
			heurData.setHeuristicData("Sensor/Rotation-Vector/Name", "None");
			// Toast.makeText(getApplicationContext(),
			// "No Rotation Vector Sensor!", Toast.LENGTH_SHORT).show();
			return 0;
		} else {
			// Toast.makeText(getApplicationContext(),
			// sensor.getName(), Toast.LENGTH_SHORT).show();
			heurData.setHeuristicData("Sensor/Rotation-Vector/Name",
					sensor.getName());
			sensorManager.registerListener(this, sensor,
					SensorManager.SENSOR_DELAY_FASTEST);
			return 1;
		}

	}

	private int getSensorAccel() {
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		// Log.d("INFO: ", "manager:" + manager);
		// Log.d("INFO: ", "get SENSOR SERVICE!");
		sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		if (sensor == null) {
			heurData.setHeuristicData("Sensor/Accelerator/Name", "None");
			// Toast.makeText(getApplicationContext(),
			// "No Magnetic Field Sensor!", Toast.LENGTH_SHORT).show();
			return 0;
		} else {
			heurData.setHeuristicData("Sensor/Accelerator/Name",
					sensor.getName());
			// Toast.makeText(getApplicationContext(), sensor.getName(),
			// Toast.LENGTH_SHORT).show();
			sensorManager.registerListener(this, sensor,
					SensorManager.SENSOR_DELAY_FASTEST);
			return 1;
		}
	}

	private int checkADBPort() {
		boolean first_line = true;
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream("/proc/net/tcp")), 1000);
			String line = null;
			// Java dictionary with int to int mapping
			// Map<Integer, Integer> map = new HashMap<Integer, Integer>();
			// by definition, always preserves the order of the elements
			ArrayList<Integer> keys = new ArrayList();
			ArrayList<Integer> values = new ArrayList();

			while ((line = reader.readLine()) != null) {
				// skip first line
				if (first_line) {
					first_line = false;
					continue;
				}
				String[] dic = line.split("\\W+");
				int ip = Integer.parseInt(dic[2], 16);
				int port = Integer.parseInt(dic[3], 16);

				// map.put(ip, port);
				keys.add(ip);
				values.add(port);

				// Toast.makeText(getApplicationContext(), ip+" "+port,
				// Toast.LENGTH_SHORT).show();
			}
			int adbport = -1;
			try {
				// adbport = map.get(0);
				adbport = values.get(0);
				heurData.setHeuristicData("ADB/Port", Integer.toString(adbport));
				// Toast.makeText(getApplicationContext(), "* adbport:" +
				// adbport,
				// Toast.LENGTH_SHORT).show();
			} catch (Exception e) {
				Log.d("PORT EXCEPTION", e.toString());
				return 0;
			}
			// iterate through hashmap
			// Iterator<Entry<Integer, Integer>> it = map.entrySet().iterator();
			Iterator<Integer> key_it = keys.iterator();
			Iterator<Integer> val_it = values.iterator();
			int isEmu = 0;
			while (key_it.hasNext()) {
				Integer ip = key_it.next();
				Integer port = val_it.next();
				// Entry<Integer, Integer> pairs = it.next();
				// skip 0 ip
				// Log.d("PORT", pairs.getValue().toString());
				Log.d("PORT", port.toString());
				// if (pairs.getKey() == 0)
				if (ip == 0)
					continue;
				// if (pairs.getValue() == adbport) {
				if (port == adbport) {
					isEmu = 1;
					break;
				}
				key_it.remove();
			}
			reader.close();
			return isEmu;
		} catch (IOException ex) {
			Log.d("PORT EXCEPTION", ex.toString());
			return 0;
		}
	}

	private int getADBPort() {

		int isEmu = checkADBPort();

		if (isEmu == 1) {
			// Toast.makeText(getApplicationContext(), "* is Emulator!",
			// Toast.LENGTH_SHORT).show();
			// Log.d("PATCHED CODE", "* is Emulator!");
			heurData.setHeuristicData("ADB/Type", "Emulator");
			return 0;
		} else {
			// Toast.makeText(getApplicationContext(), "* is a real device!",
			// Toast.LENGTH_SHORT).show();
			// Log.d("PATCHED CODE", "* is a real device!");
			heurData.setHeuristicData("ADB/Type", "Device");
			return 1;
		}
	}

	private void getBTDetector() {

		int isEmu = -1;
		String exception = "no error";
		try{
		 isEmu = BinaryTranslationDetection.is_in_emu();
		// Log.i("JNIDemo", String.format("is Emu: %d", isEmu));
		}
		catch (UnsatisfiedLinkError e) {
			System.out.println("BT/UnsatisfiedLinkError in Service : Couldn't load CallApi");
			exception = new String("Exception : UnsatisfiedLinkError");
			isEmu = -1;
			e.printStackTrace();
			//e.printStackTrace();
			//Log.d("ERROR-ALERT----", e.getMessage());
			//Log.d("ERROR-ALERT------", e.getCause().toString());
			// heurData.setHeuristicData("BTDetector2/Type",
			// e.getMessage());
		} catch (ExceptionInInitializerError e) {
			e.printStackTrace();
			exception = new String("Exception : ExceptionInInitializerError");
			System.out.println("BT/ExceptionInInitializerError in Service : Couldn't load CallApi");
			System.out.println(e.getMessage());
			isEmu = -1;
			// e.printStackTrace();
			// Log.d("ERROR-ALERT----", e.getMessage());
			// Log.d("ERROR-ALERT------", e.getCause().toString());

		}
		// tv.setText( String.format("is Emulator: %d", isEmu) );

		switch(isEmu)
		{
		case 1: 
			heurData.setHeuristicData("BTDetector/Type", "Emulator");
			Log.d("BTDEC1 : ", "emulator");
			break;
		case -1:
			Log.d("BTDEC1 : ", "Error");
			heurData.setHeuristicData("BTDetector/Type", exception);
			 break;
		default:
			Log.d("BTDEC1 : ", "Device");
			heurData.setHeuristicData("BTDetector/Type", "Device");
			break;
		
		}
	}

	private void getBTDetector2() {
		int isEmu = -1;
		String exception = "no error";
		// Log.i("JNIDemo", String.format("is Emu: %d", isEmu));

		// tv.setText( String.format("is Emulator: %d", isEmu) );

		final String PREFS_NAME = "MyPrefsFile";

		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

		if (settings.getBoolean("first_time", true)) {

			// the app is being launched for first time, do something
			try {
				isEmu = NativeWrapper.is_in_emu();
			}

			catch (UnsatisfiedLinkError e) {
				System.out.println("BT2/UnsatisfiedLinkError in Service : Couldn't load CallApi");
				isEmu = -1;
				exception = new String("Exception : UnsatisfiedLinkError");
				e.printStackTrace();
				//e.printStackTrace();
				//Log.d("ERROR-ALERT----", e.getMessage());
				//Log.d("ERROR-ALERT------", e.getCause().toString());
				// heurData.setHeuristicData("BTDetector2/Type",
				// e.getMessage());
			} catch (ExceptionInInitializerError e) {
				isEmu = -1;
				exception = new String("Exception : UnsatisfiedLinkError");
				System.out.println("BT2/ExceptionInInitializerError in Service : Couldn't load CallApi");
				System.out.println(e.getMessage());
				e.printStackTrace();
				// e.printStackTrace();
				// Log.d("ERROR-ALERT----", e.getMessage());
				// Log.d("ERROR-ALERT------", e.getCause().toString());

			}
			// first time task

			// record the fact that the app has been started at least once
			settings.edit().putBoolean("first_time", false).commit();
			settings.edit().putInt("result", isEmu).commit();
		} else {
			isEmu = settings.getInt("result", 666);
			/*
			 * Toast.makeText(getApplicationContext(), "Emulator!",
			 * Toast.LENGTH_LONG).show();
			 */}

		
		switch(isEmu)
		{
		case 1: 
			heurData.setHeuristicData("BTDetector2/Type", "Emulator");
			Log.d("BT2", "Emu");
			break;
		case -1:
			Log.d("BT2 : ", "Error");
			heurData.setHeuristicData("BTDetector2/Type", exception);
			 break;
		default:
			Log.d("BT2", "device");
			heurData.setHeuristicData("BTDetector/Type", "Device");
			break;
		
		}

	}

	private void getGeoChange() {
		String context = Context.LOCATION_SERVICE;
		LocationManager locationManager = (LocationManager) getSystemService(context);

		// Get the last known location using the location manager
		// test
		// Criteria criteria = new Criteria();
		// String provider = locationManager.getBestProvider(criteria, false);
		// Location location = locationManager.getLastKnownLocation(provider);

		heurData.setHeuristicData("GeoLocation/lon", Double.toString(longitude));
		heurData.setHeuristicData("GeoLocation/lat", Double.toString(latitude));

		final LocationListener locationListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				longitude = location.getLongitude();
				latitude = location.getLatitude();

				heurData.setHeuristicData("GeoLocation/lon",
						Double.toString(longitude));
				heurData.setHeuristicData("GeoLocation/lat",
						Double.toString(latitude));

				/*
				 * for (Map.Entry<String, String> entry : heurData
				 * .getHeuristicData().entrySet()) { Log.d("MAP : ", "Key : " +
				 * entry.getKey() + " Value : " + entry.getValue()); }
				 */

				Toast.makeText(getApplicationContext(), "GPS change!",
						Toast.LENGTH_SHORT).show();

			}

			@Override
			public void onProviderDisabled(String provider) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onProviderEnabled(String provider) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onStatusChanged(String provider, int status,
					Bundle extras) {
				// TODO Auto-generated method stub

			}
		};

		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				100, 0, locationListener, Looper.getMainLooper());
	}

	private void getLocalIpAddress(boolean useIPv4) {
		try {
			List<NetworkInterface> interfaces = Collections
					.list(NetworkInterface.getNetworkInterfaces());
			for (NetworkInterface intf : interfaces) {
				List<InetAddress> addrs = Collections.list(intf
						.getInetAddresses());
				for (InetAddress addr : addrs) {
					if (!addr.isLoopbackAddress()) {
						String sAddr = addr.getHostAddress().toUpperCase();
						boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
						if (useIPv4) {
							if (isIPv4) {
								// Log.i("IP", "***** IP=" + sAddr);
								heurData.setHeuristicData("IP", sAddr);
								;
							}
						} else {
							if (!isIPv4) {
								int delim = sAddr.indexOf('%'); // drop ip6 port
																// suffix
								heurData.setHeuristicData(
										"IP",
										delim < 0 ? sAddr : sAddr.substring(0,
												delim));
							}
						}
					}
				}
			}
		} catch (Exception ex) {
		} // for now eat exceptions

		heurData.setHeuristicData("IP", "Undefined");

	}
}
