package info.fshi.oppnetdemo1.utils;

public abstract class Constants {

	public static long SCAN_INTERVAL = 10000;
	public static long SCAN_DURATION = 5000;

	public static boolean DEBUG = false;

	public static String TAG_ACT_TEST = "test";

	public static long BT_CLIENT_TIMEOUT = 5000;

	public static long SENSOR_CONTACT_INTERVAL = 60000;
	public static long SINK_CONTACT_INTERVAL = 600000;

	public static int MAX_RELAY_NUM = 2;

	public static final long WEB_LOCATION_REPORT_INTERVAL = 10 * 60 * 1000; // 10 minutes
	public static final long WEB_LOCATION_DEVICE_LIST_UPDATE_INTERVAL = 30 * 60 * 1000; // 30 minutes

	public static final double LOCATION_UPDATE_SENSITIVITY = 0.00001;

	public static final String WEB_SERVER_ADDR = "http://hydeparkdemo.herokuapp.com/";
	public static final String WEB_SERVER_TX_ADDR = "http://hydeparkdemo.herokuapp.com/tx";
	public static final String WEB_SERVER_LOC_ADDR = "http://hydeparkdemo.herokuapp.com/loc";
	public static final String WEB_SERVER_DEVICELIST_ADDR = "http://hydeparkdemo.herokuapp.com/devicelist";
	public static final String WEB_SERVER_TYPELIST_ADDR = "http://hydeparkdemo.herokuapp.com/typelist";

	public static final String MQTT_BROKER_ADDR = "tcp://achilles.doc.ic.ac.uk:1883";


}
