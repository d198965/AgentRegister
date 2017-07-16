package com.test.stk.apttestmoon;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {
    LinearLayout contentLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        contentLayout = (LinearLayout) findViewById(R.id.keyvalue_content);
        aptTestUpdateList();
    }

    private void aptTestUpdateList() {
        BufferedReader reader = null;
        try {
            String[] fileList = MainActivity.this.getAssets().list("agentkeyvalue");
            for (String fileName : fileList) {
                String temFileName = "agentkeyvalue"+"/"+fileName;
                reader = new BufferedReader(new InputStreamReader( MainActivity.this.getAssets().open(temFileName)));
                String line = reader.readLine();
                while (line != null && !"".equals(line)) {
                    String[] keyValue = line.split(";");
                    TextView temView = new TextView(MainActivity.this);
                    temView.setText("AgentName: " + keyValue[1] + " KeyValue:" + keyValue[0]);
                    contentLayout.addView(temView);
                    line = reader.readLine();
                }
                reader.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e("onDestroy","MainActivity");
    }
}
