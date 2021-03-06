package com.mpvreeken.rpgcompanion;


import android.content.Intent;
import android.os.Bundle;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends RPGCActivity {

    private TextView errors_tv;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setupLoadingAnim();

        errors_tv = findViewById(R.id.login_errors_tv);

        //If brought here by another activity with alert data show it
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if(extras != null) {
            String alert = extras.getString("ALERT");
            if (!alert.isEmpty()) {
                errors_tv.setText(alert);
            }
        }

        Button login_btn = findViewById(R.id.login_login_btn);
        login_btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showLoadingAnim();

                EditText email_et = findViewById(R.id.login_email_et);
                EditText password_et = findViewById(R.id.login_password_et);
                String email = email_et.getText().toString();
                String password = password_et.getText().toString();

                final RequestBody postBody = new FormBody.Builder()
                        .add("email", email)
                        .add("password", password)
                        .build();

                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(getResources().getString(R.string.url_login))
                        .post(postBody)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        hideLoadingAnim();
                        onHttpResponseError(e.getMessage());
                    }

                    @Override
                    public void onResponse(Call call, final Response response) throws IOException {
                        hideLoadingAnim();
                        if (!response.isSuccessful()) {
                            onHttpResponseError("Unexpected error");
                            throw new IOException("Unexpected code " + response);
                        }
                        else {
                            /*
                             *    possible responses:
                             * { "error":"invalid_credentials" }
                             * { "error":"could_not_create_token" }
                             * { "error":"account_unconfirmed" }
                             * { "jwt":"<TOKEN>" }
                             *
                             */
                            try {
                                JSONObject r = new JSONObject(response.body().string());
                                if (r.has("jwt")) {
                                    //Success
                                    onResponseSuccess(r.getString("jwt"));
                                }
                                else {
                                    //Error
                                    String error = r.has("error") ? r.getString("error") : "unknown_error";
                                    String readable_error;
                                    switch (error) {
                                        case "invalid_credentials":
                                            readable_error = "The email or password you entered is incorrect";
                                            break;
                                        case "could_not_create_token":
                                            readable_error = "An unknown error has occurred. Please try again.";
                                            break;
                                        case "account_unconfirmed":
                                            readable_error = "Your account hasn't been confirmed yet. Please check your email.";
                                            break;
                                        default:
                                            readable_error = "An unknown error has occurred. Please try again.";
                                            break;
                                    }
                                    onLoginError(readable_error);
                                }
                            }
                            catch (JSONException e) {
                                onHttpResponseError(e.getMessage());
                            }
                        }
                    }
                });
            }
        });
    }

    public void onResponseSuccess(final String token) {
        //If login is successful, flag as logged in in application,
        // finish this activity, and start the MyAccount activity
        LoginActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                application.login(token);
                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                finish();
                startActivity(intent);
            }
        });
    }

    public void onLoginError(final String error) {
        LoginActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                errors_tv.setText(error);
            }
        });
        onHttpResponseError(error);
    }
}
