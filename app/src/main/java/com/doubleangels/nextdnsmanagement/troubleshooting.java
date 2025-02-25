package com.doubleangels.nextdnsmanagement;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.AddTrace;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.perf.metrics.Trace;
import org.apache.commons.io.FileUtils;
import java.util.UUID;

public class troubleshooting extends AppCompatActivity {

    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private FirebaseAnalytics mFirebaseAnalytics;
    private Toolbar toolbar;
    private Window window;
    private ImageView taskbarImage;
    private ImageView statusIcon;
    private String storedUniqueKey;
    private String uniqueKey;
    private Boolean isManualDarkThemeOnSub;
    private Boolean isDarkThemeOn;
    private Boolean isManualDisableAnalytics;

    @Override
    @AddTrace(name = "troubleshooting_create", enabled = true /* optional */)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_troubleshooting);

        try {
            final SharedPreferences sharedPreferences = getSharedPreferences("publicResolverSharedPreferences", MODE_PRIVATE);
            isManualDisableAnalytics = sharedPreferences.getBoolean("manualDisableAnalytics", false);
            storedUniqueKey = sharedPreferences.getString("uuid", "defaultValue");
            if (storedUniqueKey.contains("defaultValue")) {
                uniqueKey = UUID.randomUUID().toString();
                sharedPreferences.edit().putString("uuid", uniqueKey).apply();
                FirebaseCrashlytics.getInstance().setUserId(uniqueKey);
                FirebaseCrashlytics.getInstance().log("Set UUID to: " + uniqueKey);
            } else {
                uniqueKey = sharedPreferences.getString("uuid", "defaultValue");
                FirebaseCrashlytics.getInstance().setUserId(uniqueKey);
                FirebaseCrashlytics.getInstance().log("Set UUID to: " + uniqueKey);
            }
            mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
            if (isManualDisableAnalytics) {
                mFirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(true);
            }

            Trace remoteConfigStartTrace = FirebasePerformance.getInstance().newTrace("remoteConfig_setup");
            remoteConfigStartTrace.start();
            mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
            FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(1800).build();
            mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
            mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
            remoteConfigStartTrace.stop();

            window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            Trace remoteConfigFetchTrace = FirebasePerformance.getInstance().newTrace("remoteConfig_fetch");
            remoteConfigFetchTrace.start();
            mFirebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(this, new OnCompleteListener<Boolean>() {
                @Override
                public void onComplete(@NonNull Task<Boolean> task) {
                    if (task.isSuccessful()) {
                        boolean updated = task.getResult();
                        FirebaseCrashlytics.getInstance().log("Remote config fetch succeeded: " + updated);
                        mFirebaseRemoteConfig.activate();
                    }
                }
            });
            remoteConfigFetchTrace.stop();
            FirebaseCrashlytics.getInstance().setCustomKey("status_bar_background_color", mFirebaseRemoteConfig.getString("status_bar_background_color"));
            window.setStatusBarColor(Color.parseColor(mFirebaseRemoteConfig.getString("status_bar_background_color")));
            toolbar =(Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            FirebaseCrashlytics.getInstance().setCustomKey("toolbar_background_color", mFirebaseRemoteConfig.getString("toolbar_background_color"));
            toolbar.setBackgroundColor(Color.parseColor(mFirebaseRemoteConfig.getString("toolbar_background_color")));
            getSupportActionBar().setDisplayShowTitleEnabled(mFirebaseRemoteConfig.getBoolean("show_title"));

            boolean isDarkThemeOnSub = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            isManualDarkThemeOnSub = sharedPreferences.getBoolean("manualDarkMode", false);
            if (isDarkThemeOnSub || isManualDarkThemeOnSub) {
                isDarkThemeOn = true;
            } else {
                isDarkThemeOn = false;
            }
            FirebaseCrashlytics.getInstance().setCustomKey("isDarkThemeOn", isDarkThemeOn);
            ImageView troubleshootingGithub = (ImageView) findViewById(R.id.helpGithubImageView);
            if (isDarkThemeOn) {
                taskbarImage = (ImageView) findViewById(R.id.taskbarImage);
                taskbarImage.setImageResource(R.drawable.taskbar_dark);
                FirebaseCrashlytics.getInstance().setCustomKey("toolbar_dark_mode_background_color", mFirebaseRemoteConfig.getString("toolbar_dark_mode_background_color"));
                toolbar.setBackgroundColor(Color.parseColor(mFirebaseRemoteConfig.getString("toolbar_dark_mode_background_color")));
                getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.dark_mode_background));
                TextView troubleshootingTitleView = (TextView) findViewById(R.id.troubleshootingTitleView);
                troubleshootingTitleView.setTextColor(getResources().getColor(R.color.white));
                TextView directionsTextView = (TextView) findViewById(R.id.directionsTextView);
                directionsTextView.setTextColor(getResources().getColor(R.color.white));
                TextView helpStep1TextView = (TextView) findViewById(R.id.helpStep1TextView);
                helpStep1TextView.setTextColor(getResources().getColor(R.color.white));
                TextView helpStep2TextView = (TextView) findViewById(R.id.helpStep2TextView);
                helpStep2TextView.setTextColor(getResources().getColor(R.color.white));
                TextView helpStep3TextView = (TextView) findViewById(R.id.helpStep3TextView);
                helpStep3TextView.setTextColor(getResources().getColor(R.color.white));
                troubleshootingGithub.setImageResource(R.drawable.github_dark);
            } else {
                taskbarImage = (ImageView) findViewById(R.id.taskbarImage);
                taskbarImage.setImageResource(R.drawable.taskbar_light);
            }

            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            Network network = connectivityManager.getActiveNetwork();
            LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
            updateVisualIndicator(linkProperties);
            if (connectivityManager != null) {
                connectivityManager.registerNetworkCallback(new NetworkRequest.Builder().build(), new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
                        super.onLinkPropertiesChanged(network, linkProperties);
                        updateVisualIndicator(linkProperties);
                    }
                });
            }

            statusIcon = (ImageView) findViewById(R.id.connectionStatus);
            statusIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Bundle bundle = new Bundle();
                    bundle.putString("id", "help");
                    mFirebaseAnalytics.logEvent("toolbar_action", bundle);
                    Intent helpIntent = new Intent(v.getContext(), help.class);
                    startActivity(helpIntent);
                }
            });

            Button clearCacheButton = (Button) findViewById(R.id.clearCacheButton);
            clearCacheButton.setOnClickListener(new View.OnClickListener(){
                public void onClick(View v){
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                }
            });

            troubleshootingGithub.setOnClickListener(new View.OnClickListener(){
                public void onClick(View v){
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.addCategory(Intent.CATEGORY_BROWSABLE);
                    intent.setData(Uri.parse("https://github.com/mtverlee/NextDNSManager/issues"));
                    startActivity(intent);
                }
            });

        } catch (Exception e) {
                FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    @Override
    @AddTrace(name = "troubleshooting_inflate", enabled = true /* optional */)
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_back_only, menu);
        return true;
    }

    @Override
    @AddTrace(name = "troubleshooting_switch", enabled = true /* optional */)
    public boolean onOptionsItemSelected(MenuItem item) {
        Bundle bundle = new Bundle();
        switch (item.getItemId()) {
            case R.id.back:
                bundle.putString("id", "back");
                mFirebaseAnalytics.logEvent("toolbar_action", bundle);
                Intent mainIntent = new Intent(this, MainActivity.class);
                startActivity(mainIntent);
            default:
                return super.onContextItemSelected(item);
        }
    }

    @AddTrace(name = "update_visual_indicator", enabled = true /* optional */)
    public void updateVisualIndicator(LinkProperties linkProperties) {
        try {
            if (linkProperties.isPrivateDnsActive()) {
                if (linkProperties.getPrivateDnsServerName() != null) {
                    if (linkProperties.getPrivateDnsServerName().contains("nextdns")) {
                        ImageView connectionStatus = (ImageView) findViewById(R.id.connectionStatus);
                        connectionStatus.setImageResource(R.drawable.success);
                        connectionStatus.setColorFilter(getResources().getColor(R.color.green));
                    } else {
                        ImageView connectionStatus = (ImageView) findViewById(R.id.connectionStatus);
                        connectionStatus.setImageResource(R.drawable.success);
                        connectionStatus.setColorFilter(getResources().getColor(R.color.yellow));
                    }
                } else {
                    ImageView connectionStatus = (ImageView) findViewById(R.id.connectionStatus);
                    connectionStatus.setImageResource(R.drawable.success);
                    connectionStatus.setColorFilter(getResources().getColor(R.color.yellow));
                }
            } else {
                ImageView connectionStatus = (ImageView) findViewById(R.id.connectionStatus);
                connectionStatus.setImageResource(R.drawable.failure);
                connectionStatus.setColorFilter(getResources().getColor(R.color.red));
            }
        } catch (Exception e) {
                FirebaseCrashlytics.getInstance().recordException(e);
        }
    }
}