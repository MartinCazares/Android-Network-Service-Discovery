package com.doepiccoding.nsd;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.doepiccoding.nsd.connection.NSDDiscover;
import com.doepiccoding.nsd.connection.NSDListen;


public class NSDMainActivity extends Activity {

    private NSDListen mNSDListener;
    private NSDDiscover mNSDDiscover;

    private Button mRegisterBtn;
    private Button mDiscoverBtn;
    private Button mSayHelloBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nsdmain);

        mNSDListener = new NSDListen(this);
        mNSDDiscover = new NSDDiscover(this, mDiscoveryListener);

        mRegisterBtn = (Button)findViewById(R.id.register);
        mRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNSDListener.registerDevice();
            }
        });

        mDiscoverBtn = (Button)findViewById(R.id.discover);
        mDiscoverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNSDDiscover.discoverServices();
            }
        });

        mSayHelloBtn = (Button)findViewById(R.id.sayHello);
        mSayHelloBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNSDDiscover.sayHello();
            }
        });

        //Show selection alert dialog...
        new AlertDialog.Builder(this)
        .setMessage(getString(R.string.select_mode_dlg_msg))
        .setPositiveButton(getString(R.string.register_dlg_btn), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mDiscoverBtn.setVisibility(View.GONE);
                dialog.dismiss();
            }
        })
        .setNegativeButton(getString(R.string.discover_dlg_btn), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mRegisterBtn.setVisibility(View.GONE);
                dialog.dismiss();
            }
        })
        .setCancelable(false)
        .show();
    }

    private NSDDiscover.DiscoveryListener mDiscoveryListener = new NSDDiscover.DiscoveryListener() {
        @Override
        public void serviceDiscovered(String host, int port) {
            //This callback is on a worker thread...
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    findViewById(R.id.sayHello).setVisibility(View.VISIBLE);
                }
            });
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mNSDListener.shutdown();
        mNSDDiscover.shutdown();
    }
}
