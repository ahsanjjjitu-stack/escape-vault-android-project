package com.example.escapevault.sms_setup_pack;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.escapevault.R;

public class FakeChatActivity extends AppCompatActivity {

    TextView sender_message, textView_senderName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_fake_chat);

        textView_senderName = findViewById(R.id.textView_senderName);
        sender_message = findViewById(R.id.sender_message);

        String name = getIntent().getStringExtra("name");
        String body = getIntent().getStringExtra("msg");


        textView_senderName.setText(name);
        sender_message.setText(body);


    }
}