package com.quickjs.myquickjs;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.quickjs.QuickJs;

public class MainActivity extends AppCompatActivity {
    private EditText ed;
    private Button bu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        QuickJs.open();
        setContentView(R.layout.activity_main);
        ed = findViewById(R.id.editText);
        bu = findViewById(R.id.button);
        //QuickJs.openJsContext();
        bu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                QuickJs.evalString(MainActivity.this, ed.getText().toString());
            }
        });
    }

    @Override
    protected void onDestroy() {
        //QuickJs.closeJs();
        super.onDestroy();
    }
}
