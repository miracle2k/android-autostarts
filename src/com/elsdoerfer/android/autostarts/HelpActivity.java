package com.elsdoerfer.android.autostarts;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class HelpActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help);
        ((Button)findViewById(R.id.close)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
        });
    }
}
