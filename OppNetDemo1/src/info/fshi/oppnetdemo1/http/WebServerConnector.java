package info.fshi.oppnetdemo1.http;

import info.fshi.oppnetdemo1.utils.Constants;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import android.os.AsyncTask;

public class WebServerConnector {

//	private final static String TAG = "web server connector";

	public WebServerConnector(){

	}

	private class SendTransactionRecordTask extends AsyncTask<TransactionData, Void, Void> {
		protected Void doInBackground(TransactionData... data) {
			// init the counter
			TransactionData txData = data[0];
			String topic = "/data/hydeparkcomms";
			String content = "{ \"source\": \"" + String.valueOf(txData.senderAddr) + "\","
					+ "\"destination\": \""+ String.valueOf(txData.receiverAddr) + "\","
					+ "\"packetid\": \""+ String.valueOf(txData.packetId) + "\","
					+ "\"path\": \""+ String.valueOf(txData.path) + "\","
					+ "\"packetsize\": \""+ String.valueOf(txData.packetSize) +"\"}";
			sendMqtt(topic, content);

			return null;
		}
	}

	public void reportTransactionData(TransactionData data) {
		// Create a new HttpClient and Post Header
		new SendTransactionRecordTask().execute(data);
	}

	public void sendMqtt(String topic, String content) {
		// QoS supported at level 0 and 1
		int qos             = 0;

		// Connection URI (ssl is also supported)
		String broker	    = Constants.MQTT_BROKER_ADDR;
		// Possible values ["tcp://api.cloudplugs.com:1883","ssl://api.cloudplugs.com:8883"]

		try {
			MemoryPersistence persistence = new MemoryPersistence();
			MqttClient sampleClient = new MqttClient(broker,MqttClient.generateClientId(), persistence);
			MqttConnectOptions	connOpts = new MqttConnectOptions();
			connOpts.setCleanSession(true);

			sampleClient.connect(connOpts);

			MqttMessage message = new MqttMessage(content.getBytes());
			message.setQos(qos);
			sampleClient.publish(topic, message);
			sampleClient.disconnect();

		} catch(MqttException me) {
			System.out.println("reason "+me.getReasonCode());
			System.out.println("msg "+me.getMessage());
			System.out.println("loc "+me.getLocalizedMessage());
			System.out.println("cause "+me.getCause());
			System.out.println("excep "+me);
			me.printStackTrace();
		}
	}
}