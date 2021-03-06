package info.fshi.oppnetdemo1.bluetooth;

import info.fshi.oppnetdemo1.utils.Constants;
import info.fshi.oppnetdemo1.utils.SharedPreferencesUtil;

import java.util.Random;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class BTScanningAlarm extends BroadcastReceiver {
	// Class constants
	static final String TAG = "ScanningAlarm"; /** for logging */   
	private static WakeLock wakeLock;
	private static final String WAKE_LOCK = "ScanningAlarmWakeLock";

	private static PendingIntent alarmIntent;

	private static long interval;

	private static BTController mBTController = null;

	/**
	 * This constructor is called the alarm manager.
	 */
	public BTScanningAlarm(){}

	/**
	 * Starts the alarm, need to give it a user defined bluetooth controller (define handler e.g.)
	 */

	public BTScanningAlarm(Context context, BTController btController) {          
		// init bt controller
		mBTController = btController;

		if(BluetoothAdapter.getDefaultAdapter().isEnabled()){                           
			scheduleScanning(context, System.currentTimeMillis());
		}
	}
	/**
	 * Acquire the Wake Lock
	 * @param context
	 */
	public static void getWakeLock(Context context){

		releaseWakeLock();

		PowerManager mgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK , WAKE_LOCK); 
		wakeLock.acquire();
	}

	public static void releaseWakeLock(){
		if(wakeLock != null)
			if(wakeLock.isHeld())
				wakeLock.release();
	}


	/**
	 * Stop the scheduled alarm
	 * @param context
	 */
	public static void stopScanning(Context context) {
		AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

		if(alarmMgr != null){
			Intent intent = new Intent(context, BTScanningAlarm.class);
			alarmIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			alarmMgr.cancel(alarmIntent);
		}
		releaseWakeLock();
	}

	/**
	 * Schedules a Scanning communication
	 * @param time after how many milliseconds (0 for immediately)?
	 */
	public void scheduleScanning(Context context, long time) {

		Log.d(TAG, "scheduling a new Bluetooth scanning in " + Long.toString( time ));
		AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

		Intent intent = new Intent(context, BTScanningAlarm.class);
		alarmIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		if(alarmMgr != null){
			alarmMgr.cancel(alarmIntent);
		}
		interval = Constants.SCAN_INTERVAL;
		
		alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, time, interval, alarmIntent);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		// start scan
		Log.d(TAG, "start a scan at " + String.valueOf(System.currentTimeMillis()));
		if(mBTController != null){
			mBTController.startBTScan(true, Constants.SCAN_DURATION);
		}
		// schedule a new scan
		Random r = new Random();
		scheduleScanning(context, System.currentTimeMillis() + interval + (r.nextInt(2000 - 1000) + 1000) * 10);
	}
}
