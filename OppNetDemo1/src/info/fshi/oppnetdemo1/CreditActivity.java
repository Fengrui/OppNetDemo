package info.fshi.oppnetdemo1;

import info.fshi.oppnetdemo1.bluetooth.BTCom;
import info.fshi.oppnetdemo1.bluetooth.BTController;
import info.fshi.oppnetdemo1.bluetooth.BTScanningAlarm;
import info.fshi.oppnetdemo1.data.QueueManager;
import info.fshi.oppnetdemo1.http.TransactionData;
import info.fshi.oppnetdemo1.http.WebServerConnector;
import info.fshi.oppnetdemo1.log.PeerLog;
import info.fshi.oppnetdemo1.log.PeerLogList;
import info.fshi.oppnetdemo1.packet.BasicPacket;
import info.fshi.oppnetdemo1.utils.Constants;
import info.fshi.oppnetdemo1.utils.Devices;

import java.util.ArrayList;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class CreditActivity extends Activity {

	private static Context mContext;

	private static final String TAG = "CreditActivity";

	// bluetooth
	private static BTController mBTController;
	private BluetoothAdapter mBluetoothAdapter = null;
	private final int REQUEST_BT_ENABLE = 1;
	private final int REQUEST_BT_DISCOVERABLE = 11;
	private int RESULT_BT_DISCOVERABLE_DURATION = 0;

	QueueManager myQueue;

	WebServerConnector mWebConnector;

	private View peerPhone;
	private View myPhone;
	private TextView peerPhoneMac;
	private TextView txMyQueueLen;
	private TextView txPeerQueueLen;

	private ImageView arrowView;
	private TextView byteSent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_credit);
		mContext = this;
		// credit UI activity
		// it shows the current credit the user has received, also notifies any credit change
		// it starts the scanning alarm

		myPhone = (View) findViewById(R.id.my_phone_graph);
		peerPhone = (View) findViewById(R.id.peer_phone_graph);

		TextView myPhoneMac = (TextView) findViewById(R.id.my_phone_mac);
		peerPhoneMac = (TextView) findViewById(R.id.peer_phone_mac);

		arrowView = (ImageView) findViewById(R.id.transmission_signal);
		byteSent = (TextView) findViewById(R.id.byte_sent);

		txMyQueueLen = (TextView) findViewById(R.id.my_queue_len);
		txPeerQueueLen = (TextView) findViewById(R.id.peer_queue_len);

		initBluetoothUtils();

		registerBroadcastReceivers();

		myPhoneMac.setText(mBluetoothAdapter.getAddress());
		txMyQueueLen.setText(String.valueOf(myQueue.getQueueLength()));
		updatePhoneGraph(myQueue.getQueueLength(), myPhone);

		mWebConnector = new WebServerConnector();
	}

	private void updatePhoneGraph(int len, View phone){
		if(len > 10){
			phone.setBackgroundResource(R.drawable.phonered);
		}else if(len > 5){
			phone.setBackgroundResource(R.drawable.phoneyellow);
		}else if(len == -1){
			phone.setBackgroundResource(R.drawable.phoneblue);
		}else{
			phone.setBackgroundResource(R.drawable.phonegreen);
		}
	}

	private void registerBroadcastReceivers(){
		// Register the bluetooth BroadcastReceiver
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothDevice.ACTION_UUID);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		registerReceiver(BTFoundReceiver, filter);
	}

	private void unregisterBroadcastReceivers(){
		unregisterReceiver(BTFoundReceiver);
	}

	private void initBluetoothUtils(){
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			// Device does not support Bluetooth
			Toast.makeText(mContext, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		myQueue = new QueueManager(mBluetoothAdapter.getAddress());	

		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_BT_ENABLE);
		}
		else{
			// start bluetooth utils
			BTServiceHandler handler = new BTServiceHandler();
			mBTController = new BTController(handler);
			mBTController.startBTServer();
			BTScanningAlarm.stopScanning(mContext);
			new BTScanningAlarm(mContext, mBTController);
		}
	}


	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		BTScanningAlarm.stopScanning(mContext);
	}


	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		Log.d(TAG, "onDestroy()");
		super.onDestroy();
		BTScanningAlarm.stopScanning(mContext);
		unregisterBroadcastReceivers();
	}

	ArrayList<PeerLog> peerLogs = new ArrayList<PeerLog>();

	/**
	 * exchange sensor data
	 * @author fshi
	 *
	 */
	private class ExchangeData extends AsyncTask<Void, Void, Void> {
		protected Void doInBackground(Void... voids) {
			// init the counter
			Log.d(TAG, "# of sensors " + String.valueOf(deviceSensor.size()));
			Log.d(TAG, "# of sinks " + String.valueOf(deviceSink.size()));
			Log.d(TAG, "# of relays " + String.valueOf(deviceRelay.size()));

			// send to sink if queue len > 1
			boolean sendToSink = false;
			boolean sendToSensor = false;

			int indexToRemove = -1;
			for(BluetoothDevice device : deviceSink){
				if(((System.currentTimeMillis() - myQueue.sinkTimestamp) > Constants.SINK_CONTACT_INTERVAL) && myQueue.getQueueLength() > 0){
					indexToRemove = deviceSensor.indexOf(device);
					mBTController.connectBTServer(device, Constants.BT_CLIENT_TIMEOUT);
					sendToSink = true;
					break;
				}

			}
			if(indexToRemove >= 0){
				deviceSensor.remove(indexToRemove);
			}
			// receive from sensor if queue len = 0
			if(!sendToSink){
				indexToRemove = -1;
				for(BluetoothDevice device : deviceSensor){
					if((System.currentTimeMillis() - myQueue.sensorTimestamp) > Constants.SENSOR_CONTACT_INTERVAL){
						indexToRemove = deviceSensor.indexOf(device);
						mBTController.connectBTServer(device, Constants.BT_CLIENT_TIMEOUT);
						sendToSensor = true;
						break;
					}
				}
				if(indexToRemove >= 0){
					deviceSensor.remove(indexToRemove);
				}
			}

			if(!sendToSensor){
				indexToRemove = -1;
				for(BluetoothDevice device : deviceRelay){
					indexToRemove = deviceRelay.indexOf(device);
					mBTController.connectBTServer(device, Constants.BT_CLIENT_TIMEOUT);
					break;
				}
				if(indexToRemove >= 0){
					deviceRelay.remove(indexToRemove);
				}
			}
			return null;
		}
	}

	@SuppressLint("HandlerLeak") private class BTServiceHandler extends Handler {

		private int peerQueueLen;		
		private final String TAG = "BTServiceHandler";

		// wrapper class
		class Result
		{
			public int length;
			public String MAC;
			public String data;
		}

		private class ClientConnectionTask extends AsyncTask<String, Void, Result> {

			protected Result doInBackground(String... strings) {
				// init the counter
				String MAC = strings[0];
				Result re = new Result();
				re.MAC = MAC;
				int serverType = Devices.PARTICIPATING_DEVICES.get(MAC.toUpperCase(Locale.ENGLISH));
				if(serverType == Devices.DEVICE_TYPE_RELAY){ // if device is a relay, send queue size request					
					JSONObject data = new JSONObject();
					try {
						data.put(BasicPacket.PACKET_TYPE, BasicPacket.PACKET_TYPE_QUEUE_SIZE_REQUEST);
						mBTController.sendToBTDevice(MAC, data);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}else if(serverType == Devices.DEVICE_TYPE_SINK){ // if device is a sink, send data
					Log.d(Constants.TAG_ACT_TEST, "send data to sink");
					re.length = myQueue.getQueueLength();
					JSONObject data = new JSONObject();
					try {
						data.put(BasicPacket.PACKET_TYPE, BasicPacket.PACKET_TYPE_DATA);
						String[] sinkData = myQueue.getFromQueue();
						data.put(BasicPacket.PACKET_PATH, sinkData[0]);
						data.put(BasicPacket.PACKET_DATA, sinkData[1]);
						data.put(BasicPacket.PACKET_ID, sinkData[2]);
						re.data = sinkData[1];
						mBTController.sendToBTDevice(MAC, data);

						myQueue.sinkTimestamp = System.currentTimeMillis();
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}else{ // if device is a sensor, do nothing here and wait for data
					myQueue.sensorTimestamp = System.currentTimeMillis();
				}
				return re;
			}

			@Override
			protected void onPostExecute(Result re) {
				// TODO Auto-generated method stub
				if(re.length > 0){
					Toast.makeText(mContext, "send data to " + re.MAC, Toast.LENGTH_LONG).show();
					arrowView.setBackgroundResource(R.drawable.arrowright);
					byteSent.setText(re.data + " (" + re.data.length() + " bytes)");
					updatePhoneGraph(-1, peerPhone);
					txPeerQueueLen.setText("K");
					peerPhoneMac.setText(re.MAC);
				}
			}
		}

		private class ServerConnectionTask extends AsyncTask<String, Void, Result> {
			protected Result doInBackground(String... strings) {
				// init the counter
				String MAC = strings[0];
				Result re = new Result();
				re.MAC = MAC;
				int myType = Devices.PARTICIPATING_DEVICES.get(mBluetoothAdapter.getAddress().toUpperCase(Locale.ENGLISH));
				if(myType == Devices.DEVICE_TYPE_RELAY){ // if my device is a relay, do nothing, wait for request
					Log.d(TAG, "I am a relay");
				}else if(myType == Devices.DEVICE_TYPE_SENSOR){ // if my device is a sensor, send data
					Log.d(Constants.TAG_ACT_TEST, "I am a sensor, send data to relay " + myQueue.getQueueLength());
					re.length = myQueue.getQueueLength();
					if(myQueue.getQueueLength() > 0){
						JSONObject data = new JSONObject();
						try {
							data.put(BasicPacket.PACKET_TYPE, BasicPacket.PACKET_TYPE_DATA);
							String[] sensorData = myQueue.getFromQueue();
							data.put(BasicPacket.PACKET_PATH, sensorData[0]);
							data.put(BasicPacket.PACKET_DATA, sensorData[1]);
							data.put(BasicPacket.PACKET_ID, sensorData[2]);
							re.data = sensorData[1];
							mBTController.sendToBTDevice(MAC, data);
							myQueue = new QueueManager(mBluetoothAdapter.getAddress());
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}else{
						mBTController.stopConnection(MAC);
					}
				}else{ // if device is a sink, do nothing here and wait for data
				}
				return re;
			}

			@Override
			protected void onPostExecute(Result re) {
				// TODO Auto-generated method stub
				if(re.length > 0){
					Toast.makeText(mContext, "send data to " + re.MAC, Toast.LENGTH_LONG).show();
					arrowView.setBackgroundResource(R.drawable.arrowright);
					byteSent.setText(re.data + " (" + re.data.length() + " bytes)");
					updatePhoneGraph(-1, peerPhone);
					txPeerQueueLen.setText("R");
					peerPhoneMac.setText(re.MAC);
				}
			}
		}


		@Override
		public void handleMessage(Message msg) {
			Bundle b = msg.getData();
			String MAC = b.getString(BTCom.BT_DEVICE_MAC);
			switch(msg.what){
			case BTCom.BT_CLIENT_ALREADY_CONNECTED:
			case BTCom.BT_CLIENT_CONNECTED:
				// don't continue
				PeerLog cLog = new PeerLog();
				cLog.mac = MAC;
				cLog.sTimestamp = System.currentTimeMillis();
				peerLogs.add(cLog);
				new ClientConnectionTask().execute(MAC);
				break;
			case BTCom.BT_CLIENT_CONNECT_FAILED:
				Log.d(Constants.TAG_ACT_TEST, "client failed");
				new ExchangeData().execute();
				break;
			case BTCom.BT_SUCCESS: // triggered by receiver
				Log.d(Constants.TAG_ACT_TEST, "success");
				break;
			case BTCom.BT_DISCONNECTED:
				Log.d(Constants.TAG_ACT_TEST, "disconnected");
				break;
			case BTCom.BT_SERVER_CONNECTED:
				Log.d(TAG, "server connected");
				PeerLog sLog = new PeerLog();
				sLog.mac = MAC;
				sLog.sTimestamp = System.currentTimeMillis();
				peerLogs.add(sLog);
				// mainly used for test, in the real case, client is always a relay
				new ServerConnectionTask().execute(MAC);
				break;
			case BTCom.BT_DATA:
				JSONObject json;
				int type;
				try{
					json = new JSONObject(b.getString(BTCom.BT_DATA_CONTENT));
					type = json.getInt(BasicPacket.PACKET_TYPE);
					switch(type){
					case BasicPacket.PACKET_TYPE_QUEUE_SIZE_REQUEST:
						JSONObject queueSize = new JSONObject();
						try {
							queueSize.put(BasicPacket.PACKET_TYPE, BasicPacket.PACKET_TYPE_QUEUE_SIZE);
							queueSize.put(BasicPacket.PACKET_DATA, myQueue.getQueueLength());
							Log.d(TAG, "send queue length " + myQueue.getQueueLength());
							mBTController.sendToBTDevice(MAC, queueSize);
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;
					case BasicPacket.PACKET_TYPE_QUEUE_SIZE: // relay queue length received

						peerQueueLen = json.getInt(BasicPacket.PACKET_DATA);
						Log.d(TAG, "receive queue size " + peerQueueLen);

						int queueDiff = (myQueue.getQueueLength() - peerQueueLen);

						String[] packet = null;
						if(queueDiff > 0){
							packet = myQueue.getFromQueue(1, MAC);
						}

						if(packet != null){
							if(packet[0] != null){
								JSONObject data = new JSONObject();
								try {
									data.put(BasicPacket.PACKET_TYPE, BasicPacket.PACKET_TYPE_DATA);
									data.put(BasicPacket.PACKET_PATH, packet[0]);
									data.put(BasicPacket.PACKET_DATA, packet[1]);
									data.put(BasicPacket.PACKET_ID, packet[2]);
								} catch (JSONException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}

								// update UI
								mBTController.sendToBTDevice(MAC, data);
								peerQueueLen += 1;
								Log.d(TAG, "send data to " + MAC);
								arrowView.setBackgroundResource(R.drawable.arrowright);
								byteSent.setText(packet[1] + " (" + packet[1].length() + " bytes)");
								updatePhoneGraph(peerQueueLen, peerPhone);
								txPeerQueueLen.setText(String.valueOf(peerQueueLen));
								peerPhoneMac.setText(MAC);
							}
							else{
								mBTController.stopConnection(MAC);
							}
						}
						else{
							mBTController.stopConnection(MAC);
							new ExchangeData().execute();
						}
						break;
					case BasicPacket.PACKET_TYPE_DATA:
						String id = json.getString(BasicPacket.PACKET_ID);
						String path = json.getString(BasicPacket.PACKET_PATH);
						String data = json.getString(BasicPacket.PACKET_DATA);
						Log.d(TAG, "received packet " + id);
						Log.d(TAG, "path : " + path.toString());

						myQueue.appendToQueue(id, path, data);
						updatePhoneGraph(myQueue.getQueueLength(), myPhone);
						updatePhoneGraph(-1, peerPhone);
						txMyQueueLen.setText(String.valueOf(myQueue.getQueueLength()));
						if(Devices.PARTICIPATING_DEVICES.get(MAC) == Devices.DEVICE_TYPE_RELAY){
							txPeerQueueLen.setText("R");
						}else if(Devices.PARTICIPATING_DEVICES.get(MAC) == Devices.DEVICE_TYPE_SENSOR){
							txPeerQueueLen.setText("S");
						}

						peerPhoneMac.setText(MAC);

						arrowView.setBackgroundResource(R.drawable.arrowleft);
						byteSent.setText(data.toString() + " (" + String.valueOf(data.length()) + " bytes)");

						Log.d(TAG, "receive " + data.length() + " bytes data from " + MAC);
						Log.d(TAG, "new queue size " + myQueue.getQueueLength());
						JSONObject ack = new JSONObject();
						try {
							ack.put(BasicPacket.PACKET_TYPE, BasicPacket.PACKET_TYPE_DATA_ACK);
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						mBTController.sendToBTDevice(MAC, ack);

						// webconnector
						TransactionData txData = new TransactionData();
						txData.senderAddr = MAC;
						txData.receiverAddr = mBluetoothAdapter.getAddress();
						txData.packetSize = data.length();
						txData.packetId = id;
						txData.timestamp = System.currentTimeMillis();
//						mWebConnector.reportTransactionData(txData);

						// add data to log
						for(PeerLog log:peerLogs){
							if(log.mac.equalsIgnoreCase(MAC)){
								log.dir = 1;
								log.qLen = myQueue.getQueueLength();
								log.eTimestamp = System.currentTimeMillis();
								PeerLogList.peerLogList.add(log);
							}
						}

						Log.d(TAG, "send ack to " + MAC);
						break;
					case BasicPacket.PACKET_TYPE_DATA_ACK:
						Log.d(TAG, "receive ack");
						mBTController.stopConnection(MAC);
						updatePhoneGraph(myQueue.getQueueLength(), myPhone);
						txMyQueueLen.setText(String.valueOf(myQueue.getQueueLength()));
						// add data to log
						for(PeerLog log:peerLogs){
							if(log.mac.equalsIgnoreCase(MAC)){
								log.dir = 0;
								log.qLen = myQueue.getQueueLength();
								log.eTimestamp = System.currentTimeMillis();
								PeerLogList.peerLogList.add(log);
							}
						}
						break;
					default:
						break;
					}
				}catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			default:
				break;
			}
		}
	}

	// timestamp to control if it is a new scan
	private long scanStartTimestamp = System.currentTimeMillis() - 100000;
	private long scanStopTimestamp = System.currentTimeMillis() - 100000;

	private ArrayList<BluetoothDevice> deviceSink = new ArrayList<BluetoothDevice>();
	private ArrayList<BluetoothDevice> deviceRelay = new ArrayList<BluetoothDevice>();
	private ArrayList<BluetoothDevice> deviceSensor = new ArrayList<BluetoothDevice>();

	// Create a BroadcastReceiver for actions
	BroadcastReceiver BTFoundReceiver = new BTServiceBroadcastReceiver();

	class BTServiceBroadcastReceiver extends BroadcastReceiver {

		ArrayList<String> devicesFoundStringArray = new ArrayList<String>();

		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) { // check if one device found more than once
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				String deviceMac = device.getAddress();
				Log.d(TAG, "get a device : " + String.valueOf(deviceMac));
				if(Devices.PARTICIPATING_DEVICES.containsKey(deviceMac.toUpperCase(Locale.ENGLISH))){ // only respond when a device is in the list
					if(!devicesFoundStringArray.contains(deviceMac)){
						devicesFoundStringArray.add(deviceMac);
						if(Devices.PARTICIPATING_DEVICES.get(deviceMac.toUpperCase(Locale.ENGLISH)) == Devices.DEVICE_TYPE_SENSOR){
							deviceSensor.add(device);
						}else if(Devices.PARTICIPATING_DEVICES.get(deviceMac.toUpperCase(Locale.ENGLISH)) == Devices.DEVICE_TYPE_SINK){
							deviceSink.add(device);
						}else{
							deviceRelay.add(device);
						}
					}
				}
			}
			else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				if(System.currentTimeMillis() - scanStartTimestamp > Constants.SCAN_DURATION){
					//a new scan has been started
					Log.d(TAG, "Discovery process has been started: " + String.valueOf(System.currentTimeMillis()));

					devicesFoundStringArray = new ArrayList<String>();
					deviceSink = new ArrayList<BluetoothDevice>();
					deviceRelay = new ArrayList<BluetoothDevice>();
					deviceSensor = new ArrayList<BluetoothDevice>();
					scanStartTimestamp = System.currentTimeMillis();
				}
				invalidateOptionsMenu();
			}
			else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				if(System.currentTimeMillis() - scanStopTimestamp > Constants.SCAN_DURATION){
					Log.d(TAG, "Discovery process has been stopped: " + String.valueOf(System.currentTimeMillis()));
					peerLogs = new ArrayList<PeerLog>();
					new ExchangeData().execute();
					scanStopTimestamp = System.currentTimeMillis();
				}
			}
		}
	};

	protected void onActivityResult(int requestCode, int resultCode,
			Intent data) {
		switch (requestCode){
		case REQUEST_BT_ENABLE:
			if (resultCode == RESULT_OK) {
				// start bluetooth utils
				initBluetoothUtils();
				Intent discoverableIntent = new
						Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
				discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, RESULT_BT_DISCOVERABLE_DURATION);
				startActivityForResult(discoverableIntent, REQUEST_BT_DISCOVERABLE);
			}
			else{
				Log.d(TAG, "Bluetooth is not enabled by the user.");
			}
			break;
		case REQUEST_BT_DISCOVERABLE:
			if (resultCode == RESULT_CANCELED){
				Log.d(TAG, "Bluetooth is not discoverable.");
			}
			else{
				Log.d(TAG, "Bluetooth is discoverable by 300 seconds.");
			}
			break;
		default:
			break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.credit, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		switch (id){
		case R.id.action_view_log:
			PeerLogListDialog peerLotListDialog = new PeerLogListDialog(mContext);
			peerLotListDialog.show();
			break;
		default:
			break;
		}
		return true;    }

}
