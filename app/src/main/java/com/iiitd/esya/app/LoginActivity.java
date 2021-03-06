package com.iiitd.esya.app;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;
import com.viewpagerindicator.CirclePageIndicator;


public class LoginActivity extends FragmentActivity implements GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks{

    FragmentPagerAdapter mFragmentPagerAdapter;
    ViewPager mViewPager;

    private String TAG = LoginActivity.class.getSimpleName();

    private final int RC_SIGN_IN = 0;
    private GoogleApiClient mGoogleApiClient;
    private boolean mIsResolving = false;
    private boolean mShouldResolve = false;
    private View loading;

    @Override
    protected void onDestroy() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected())
        {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            mGoogleApiClient.disconnect();
        }
        super.onDestroy();
    }

    public void onSignInClicked(View view, View loading) {
        // User clicked the sign-in button, so begin the sign-in process and automatically
        // attempt to resolve any errors that occur.
        mShouldResolve = true;
        this.loading = loading;
        mGoogleApiClient.connect();
        loading.setVisibility(View.VISIBLE);

        // Show a message to the user that we are signing in.
        Snackbar.make(view, "Logging you in.", Snackbar.LENGTH_LONG).show();
    }

    public void onSkipButtonClicked(View view) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(getString(R.string.pref_login_skipped), true).commit();
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
        finish();
        Snackbar.make(view, "Entering Activity", Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult:" + requestCode + ":" + resultCode + ":" + data);
        super.onActivityResult(requestCode, resultCode, data);
        int AUTH_CODE_REQUEST_CODE = 23619;

        if(requestCode == AUTH_CODE_REQUEST_CODE && resultCode == RESULT_OK)
        {
            Bundle extra = data.getExtras();
            if (extra.containsKey("authtoken"))
            {
                String oneTimeToken = extra.getString("authtoken");
                DataHolder.ONE_TIME_AUTH_TOKEN = oneTimeToken;
                Log.v("AUTH_TOKEN", "Setting one time token.");
            }
        }


        if (requestCode == RC_SIGN_IN) {
            // If the error resolution was not successful we should not resolve further.
            if (resultCode != RESULT_OK) {
                mShouldResolve = false;
            }

            mIsResolving = false;
            mGoogleApiClient.connect();
        }
    }


    @Override
    public void onConnected(Bundle bundle) {
        // onConnected indicates that an account was selected on the device, that the selected
        // account has granted any requested permissions to our app and that we were able to
        // establish a service connection to Google Play services.

        final Context context = this;

        GetAndSendIdTokenTask task = new GetAndSendIdTokenTask(this, mGoogleApiClient) {
            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if (DataHolder.ONE_TIME_AUTH_TOKEN == null)
                {
                    Toast.makeText(context, "Please try again", Toast.LENGTH_SHORT).show();
                    return;
                }

                LoginPingTest loginPingTest = new LoginPingTest(context){
                    @Override
                    protected void onPostExecute(Boolean loggedin) {
                        super.onPostExecute(loggedin);
                        if (loading != null)
                        {
                            loading.setVisibility(View.GONE);
                        }

                        if(loggedin)
                        {
                            PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                                    .edit().putBoolean(getString(R.string.pref_logged_in), true).commit();
                            PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
                                    edit().putBoolean(getString(R.string.pref_login_skipped), false).commit();
                            startActivity(new Intent(getApplicationContext(), MainActivity.class).
                                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                            finish();
                        }
                        else
                        {
                            Toast.makeText(context, "Unable to log you in", Toast.LENGTH_LONG).show();
                        }
                    }
                };
                loginPingTest.execute();
            }
        };
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        mShouldResolve = false;
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "OnConnectionSuspended: " + i);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mGoogleApiClient.isConnected())
        {
            mGoogleApiClient.connect();
        }
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // Could not connect to Google Play Services.  The user needs to select an account,
        // grant permissions or resolve an error in order to sign in. Refer to the javadoc for
        // ConnectionResult to see possible error codes.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);

        if (!mIsResolving && mShouldResolve) {
            if (connectionResult.hasResolution()) {
                try {
                    connectionResult.startResolutionForResult(this, RC_SIGN_IN);
                    mIsResolving = true;
                } catch (IntentSender.SendIntentException e) {
                    loading.setVisibility(View.GONE);
                    Log.e(TAG, "Could not resolve ConnectionResult.", e);
                    mIsResolving = false;
                    mGoogleApiClient.connect();
                }
            } else {
                // Could not resolve the connection result, show the user an
                // error dialog.
                Toast.makeText(this, "Connection error.", Toast.LENGTH_SHORT).show();
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected())
                {
                    Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
                }
            }
        } else {
            // Show the signed-out UI
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(!DataHolder.FORCE_LOGIN_FRAGMENT_TO_SHOW) {

            if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                    getString(R.string.pref_logged_in), false
            ) || (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                    getString(R.string.pref_login_skipped), false
            ))) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        }
        DataHolder.FORCE_LOGIN_FRAGMENT_TO_SHOW = false;


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(new Scope(Scopes.PROFILE))
                .addScope(new Scope(Scopes.PLUS_ME))
                .addScope(new Scope(Scopes.PLUS_LOGIN))
                .addScope(new Scope("https://www.googleapis.com/auth/plus.profile.emails.read"))
                .build();

        setContentView(R.layout.fragment_login_container);

        mFragmentPagerAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
            Fragment landingpage = new LandingPage();
            Fragment aboutiiitd = new AboutIIITDFragment();
            Fragment aboutesya = new AboutEsyaFragment();
            Fragment login = new LoginActivityFragment();

            @Override
            public Fragment getItem(int position) {
                switch (position){
                    case 0:
                        return landingpage;
                    case 1:
                        return aboutesya;
                    case 2:
                        return aboutiiitd;
                    case 3:
                        return login;
                    default:
                        return login;
                }
            }

            @Override
            public int getCount() {
                return 4;
            }
        };
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mFragmentPagerAdapter);
        mViewPager.setCurrentItem(0, false);

        CirclePageIndicator indicator = (CirclePageIndicator)findViewById(R.id.indicator);
        indicator.setViewPager(mViewPager);
    }
}
