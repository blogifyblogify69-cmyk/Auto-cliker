package com.autoclicker.test;

import android.app.*;
import android.content.*;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final int REQ_CAPTURE=4001;
    private String selectedPackage;
    private int trigger=15;
    private int delaySeconds=22;
    private final ArrayList<String> packages=new ArrayList<>();

    @Override public void onCreate(Bundle b){ super.onCreate(b); buildUi(); }

    private TextView tv(String s,int sp){ TextView t=new TextView(this); t.setText(s); t.setTextSize(sp); t.setPadding(24,18,24,18); return t; }
    private void buildUi(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(24,24,24,24);
        TextView title=tv("Auto Clicker Test",26); title.setTextColor(Color.rgb(21,101,192)); root.addView(title);
        TextView info=tv("For testing apps you own or are authorized to test. Select an app, calibrate the countdown and Target B, then start automation.",15); root.addView(info);
        Button access=new Button(this); access.setText("Enable Accessibility Service"); access.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))); root.addView(access);
        Button overlay=new Button(this); overlay.setText("Allow Overlay (if needed)"); overlay.setOnClickListener(v->{try{startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName())));}catch(Exception e){startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));}}); root.addView(overlay);
        TextView apps=tv("Installed apps",19); root.addView(apps);
        Spinner spinner=new Spinner(this); loadApps(spinner); root.addView(spinner);
        EditText triggerEdit=new EditText(this); triggerEdit.setInputType(2); triggerEdit.setHint("Countdown trigger (default 15)"); triggerEdit.setText("15"); root.addView(triggerEdit);
        EditText delayEdit=new EditText(this); delayEdit.setInputType(2); delayEdit.setHint("Delay after first click in seconds (default 22)"); delayEdit.setText("22"); root.addView(delayEdit);
        Button launch=new Button(this); launch.setText("Launch Selected App"); root.addView(launch);
        Button countdown=new Button(this); countdown.setText("Set Countdown Area"); countdown.setOnClickListener(v->{if(AutomationAccessibilityService.instance!=null) AutomationAccessibilityService.instance.startRegionCalibration(); else toast("Enable Accessibility Service first.");}); root.addView(countdown);
        Button target=new Button(this); target.setText("Set Target B"); target.setOnClickListener(v->{if(AutomationAccessibilityService.instance!=null) AutomationAccessibilityService.instance.startTargetCalibration(); else toast("Enable Accessibility Service first.");}); root.addView(target);
        Button start=new Button(this); start.setText("Start Automation"); root.addView(start);
        Button stop=new Button(this); stop.setText("Stop All Active"); stop.setOnClickListener(v->ScreenMonitorService.stop(this)); root.addView(stop);
        TextView note=tv("Workflow: countdown == 15 → click Target B → wait 22 seconds → click Target B → resume monitoring.",14); root.addView(note);
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){} public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){if(pos<packages.size())selectedPackage=packages.get(pos);}});
        launch.setOnClickListener(v->{ if(selectedPackage==null){toast("Select an app");return;} try{Intent i=getPackageManager().getLaunchIntentForPackage(selectedPackage); if(i!=null)startActivity(i); else toast("Cannot launch selected app");}catch(Exception e){toast(e.getMessage());} });
        start.setOnClickListener(v->{ try{trigger=Integer.parseInt(triggerEdit.getText().toString());delaySeconds=Integer.parseInt(delayEdit.getText().toString());}catch(Exception e){toast("Enter valid numbers");return;} if(AutomationAccessibilityService.instance==null){toast("Enable Accessibility Service first.");return;} MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE); startActivityForResult(m.createScreenCaptureIntent(),REQ_CAPTURE); });
        setContentView(root);
    }
    private void loadApps(Spinner spinner){
        List<ApplicationInfo> list=getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA); ArrayList<String> labels=new ArrayList<>();
        for(ApplicationInfo a:list){ if(getPackageManager().getLaunchIntentForPackage(a.packageName)!=null){packages.add(a.packageName); labels.add(a.loadLabel(getPackageManager())+"\n"+a.packageName);} }
        spinner.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labels)); if(!packages.isEmpty())selectedPackage=packages.get(0);
    }
    @Override protected void onActivityResult(int r,int c,Intent data){super.onActivityResult(r,c,data); if(r==REQ_CAPTURE && c==RESULT_OK && data!=null){ScreenMonitorService.start(this,data,trigger,delaySeconds);}}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
