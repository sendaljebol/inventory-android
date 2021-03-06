package com.hartz.inventory;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hartz.inventory.model.User;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;


/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {


    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;
    private User user;
    // UI references.
    private EditText mUserView, mServerView, mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private EditText debugBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        // Set up the login form.
        mServerView = (EditText) findViewById(R.id.server);
        String lastServer = SharedPrefsHelper.lastServer(getApplicationContext());
        if(lastServer != null){
            mServerView.setText(lastServer);
        }else{
            mServerView.setText("");
        }

        mUserView = (EditText) findViewById(R.id.user);
        String lastUser = SharedPrefsHelper.lastName(getApplicationContext());
        if(lastUser != null){
            mUserView.setText(lastUser);
        }else{
            mUserView.setText("");
        }

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setText("");

        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        //logged in automatically if prefs exist
        if(SharedPrefsHelper.isLoggedIn(getApplicationContext())){
            showProgress(true);
            mAuthTask = new UserLoginTask(
                    SharedPrefsHelper.readPrefs(SharedPrefsHelper.SERVER_PREFS, getApplicationContext()),
                    SharedPrefsHelper.readPrefs(SharedPrefsHelper.NAME_PREFS, getApplicationContext()),
                    SharedPrefsHelper.readPrefs(SharedPrefsHelper.PASSWORD_PREFS, getApplicationContext()));
            mAuthTask.execute((Void) null);
        }

        debugBox = (EditText)findViewById(R.id.debug_box);
        debugBox.setVisibility(EditText.GONE);
    }



    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
//        if (mAuthTask != null) {
//            return;
//        }

        // Reset errors.
        mUserView.setError(null);
        mPasswordView.setError(null);
        mServerView.setError(null);

        // Store values at the time of the login attempt.
        String server = mServerView.getText().toString();
        String email = mUserView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }

        if (TextUtils.isEmpty(server)) {
            mServerView.setError(getString(R.string.error_field_required));
            focusView = mServerView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mUserView.setError(getString(R.string.error_field_required));
            focusView = mUserView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(server, email, password);
            mAuthTask.execute((Void) null);
        }
    }


    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }


    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mServer;
        private final String mEmail;
        private final String mPassword;
        private boolean connectionProblem;
        private String connectionText;

        UserLoginTask(String server, String email, String password) {
            mServer = server;
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            SharedPrefsHelper.saveToPrefs(SharedPrefsHelper.SERVER_PREFS, mServer, getApplicationContext());
            SharedPrefsHelper.saveToPrefs(SharedPrefsHelper.LAST_SERVER_PREFS, mServer, getApplicationContext());
            SharedPrefsHelper.saveToPrefs(SharedPrefsHelper.PASSWORD_PREFS, mPassword, getApplicationContext());
            SharedPrefsHelper.saveToPrefs(SharedPrefsHelper.NAME_PREFS, mEmail, getApplicationContext());

            //access the network
            HttpHandler handler = new HttpHandler(getApplicationContext());
            LinkedHashMap<String, Object> parameter = new LinkedHashMap<>();
            parameter.put("username", mEmail);
            parameter.put("password", mPassword);

            String loginResult = null;
            try {
                loginResult = handler.makePostCall(parameter, handler.LINK_LOGIN);
            } catch (IOException e) {
                connectionProblem = true;

                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                connectionText = sw.toString();
                return false;
            }
            Log.v("POST RESULT", loginResult);

            user = new User();
            return user.createNew(loginResult, getApplicationContext());
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            DownloadMrmartTask  mrmartTask = null;
            showProgress(false);

            if (success) {
                showProgress(true);
                mrmartTask = new DownloadMrmartTask();
                mrmartTask.execute((Void) null);
            } else {
                if(connectionProblem){
                    mPasswordView.setError(getString(R.string.error_network_problem));
                    debugBox.setVisibility(View.VISIBLE);
                    debugBox.setText(connectionText);
                }else{
                    mPasswordView.setError(getString(R.string.error_incorrect_password));
                }

                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }


    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class DownloadMrmartTask extends AsyncTask<Void, Void, Boolean> {

        private String connectionText;
        private boolean connectionProblem;

        @Override
        protected Boolean doInBackground(Void... params) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String mServer = preferences.getString("ServerName", "");


            // Simulate network access.
            HttpHandler handler = new HttpHandler(getApplicationContext());

            String mrmartResult = null;
            String satuanResult = null;
            String mfgartResult = null;
            String clientResult = null;
            try {
                mrmartResult = handler.makeGetCall(handler.LINK_MRMART_GET);
                mfgartResult = handler.makeGetCall(handler.LINK_MFGART_GET);
                satuanResult = handler.makeGetCall(handler.LINK_SATUAN_GET);
                clientResult = handler.makeGetCall(handler.LINK_CUSTOMER_GET);
            } catch (IOException e) {
                connectionProblem = true;
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                connectionText = sw.toString();
                return false;
            }
            SharedPrefsHelper.saveToPrefs(SharedPrefsHelper.MFGART_PREFS,
                    mfgartResult, getApplicationContext());
            SharedPrefsHelper.saveToPrefs(SharedPrefsHelper.MRMART_PREFS,
                    mrmartResult, getApplicationContext());
            SharedPrefsHelper.saveToPrefs(SharedPrefsHelper.SATUAN_PREFS,
                    satuanResult, getApplicationContext());
            SharedPrefsHelper.saveToPrefs(SharedPrefsHelper.CUSTOMER_PREFS,
                    clientResult, getApplicationContext());

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                Toast.makeText(getApplicationContext(), "Data Updated", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                if(connectionProblem){
                    mPasswordView.setError(getString(R.string.error_network_problem));
                    debugBox.setVisibility(View.VISIBLE);
                    debugBox.setText(connectionText);
                }else{
                    mPasswordView.setError(getString(R.string.error_incorrect_password));
                }

                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

