package info.fshi.oppnetdemo1;

import info.fshi.oppnetdemo1.log.PeerLogListAdapter;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.widget.ListView;

public class PeerLogListActivity extends Activity {

	private PeerLogListAdapter peerLogListAdapter;
	
	private Context mContext;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_peer_log);
		mContext = this;
		peerLogListAdapter = new PeerLogListAdapter(mContext, R.layout.log);
		
		ListView peerLogLv = (ListView) findViewById(R.id.log_list);
		peerLogLv.setAdapter(peerLogListAdapter);
		
		peerLogListAdapter.notifyDataSetChanged();
	}
	
}
