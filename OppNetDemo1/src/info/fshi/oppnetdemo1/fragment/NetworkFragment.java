package info.fshi.oppnetdemo1.fragment;

import info.fshi.oppnetdemo1.R;
import info.fshi.oppnetdemo1.data.QueueManager;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class NetworkFragment extends Fragment {

	Context mContext;
	
	private final String TAG = "NetworkFragment";

	public NetworkFragment(Context context){
		mContext = context;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View v = inflater.inflate(R.layout.network_fragment_layout, container, false);
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
		
		Log.d(TAG, "create views");
		
		TextView queueLenTv = (TextView) getView().findViewById(R.id.network_queue_len);
		queueLenTv.setText(String.valueOf(QueueManager.getInstance(mContext).getQueueLength()));
		
		TextView receivedTv = (TextView) getView().findViewById(R.id.network_received);
		receivedTv.setText(String.valueOf(QueueManager.getInstance(mContext).packetsReceived));
	
		TextView sentTv = (TextView) getView().findViewById(R.id.network_sent);
		sentTv.setText(String.valueOf(QueueManager.getInstance(mContext).packetsSent));
		
		TextView creditTv = (TextView) getView().findViewById(R.id.network_credit);
		creditTv.setText(String.valueOf(QueueManager.getInstance(mContext).packetsSent + QueueManager.getInstance(mContext).packetsReceived));	
	}
}
