package com.autoclicker.test;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;

public class AutomationAccessibilityService extends AccessibilityService {
    public static AutomationAccessibilityService instance;
    private WindowManager wm; private View bubble; private View calibration; private boolean calibratingTarget=false, calibratingRegion=false; private float targetX=500,targetY=500; private Rect region=new Rect(0,0,500,300);
    @Override public void onServiceConnected(){super.onServiceConnected(); instance=this; wm=(WindowManager)getSystemService(WINDOW_SERVICE); showBubble();}
    @Override public void onDestroy(){remove(bubble);remove(calibration);instance=null;super.onDestroy();}
    @Override public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent e){}
    @Override public void onInterrupt(){}
    private WindowManager.LayoutParams lp(int type){WindowManager.LayoutParams p=new WindowManager.LayoutParams(type==WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY?WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY:WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,android.graphics.PixelFormat.TRANSLUCENT);p.gravity=Gravity.TOP|Gravity.START;return p;}
    private void showBubble(){ if(bubble!=null)return; TextView b=new TextView(this); b.setText("A"); b.setTextColor(Color.WHITE);b.setTextSize(18);b.setGravity(Gravity.CENTER); GradientDrawable g=new GradientDrawable();g.setColor(Color.rgb(21,101,192));g.setShape(GradientDrawable.OVAL);b.setBackground(g); WindowManager.LayoutParams p=lp(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);p.width=64;p.height=64;p.x=20;p.y=140; b.setOnClickListener(v->showMenu());bubble=b;wm.addView(b,p); }
    private void showMenu(){ final LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(18,12,18,12);box.setBackgroundColor(Color.WHITE); TextView status=new TextView(this);status.setText("AUTO TEST\nStatus: "+(ScreenMonitorService.running?"ACTIVE":"STOPPED"));status.setTextSize(16);box.addView(status); Button a=new Button(this);a.setText("Activate");a.setOnClickListener(v->{ScreenMonitorService.resume();remove(box);});box.addView(a);Button s=new Button(this);s.setText("Stop");s.setOnClickListener(v->{ScreenMonitorService.pause();remove(box);});box.addView(s);Button all=new Button(this);all.setText("Stop All Active");all.setOnClickListener(v->{ScreenMonitorService.stop(this);remove(box);});box.addView(all);Button close=new Button(this);close.setText("Close");close.setOnClickListener(v->remove(box));box.addView(close);WindowManager.LayoutParams p=lp(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);p.width=620;p.height=WindowManager.LayoutParams.WRAP_CONTENT;p.x=20;p.y=220;wm.addView(box,p);}
    public void startTargetCalibration(){remove(calibration);calibratingTarget=true; TextView v=calibrationView("Tap the center of Target B\nTap once to save target"); calibration=v; WindowManager.LayoutParams p=lp(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);p.width=-1;p.height=-1;p.flags=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;v.setOnTouchListener((view,event)->{if(event.getAction()==MotionEvent.ACTION_UP){targetX=event.getRawX();targetY=event.getRawY();calibratingTarget=false;remove(calibration);Toast.makeText(this,"Target B saved: "+(int)targetX+","+(int)targetY,Toast.LENGTH_SHORT).show();}return true;});wm.addView(v,p);}
    public void startRegionCalibration(){remove(calibration);calibratingRegion=true; TextView v=calibrationView("Drag a rectangle around the countdown timer"); calibration=v; WindowManager.LayoutParams p=lp(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);p.width=-1;p.height=-1;wm.addView(v,p); final float[] st=new float[2];v.setOnTouchListener((view,event)->{if(event.getAction()==MotionEvent.ACTION_DOWN){st[0]=event.getRawX();st[1]=event.getRawY();return true;}if(event.getAction()==MotionEvent.ACTION_UP){float x=Math.min(st[0],event.getRawX()),y=Math.min(st[1],event.getRawY()),r=Math.max(st[0],event.getRawX()),b=Math.max(st[1],event.getRawY());region.set((int)x,(int)y,(int)r,(int)b);calibratingRegion=false;remove(calibration);Toast.makeText(this,"Countdown area saved",Toast.LENGTH_SHORT).show();return true;}return true;});}
    private TextView calibrationView(String s){TextView v=new TextView(this);v.setText(s);v.setTextColor(Color.WHITE);v.setTextSize(22);v.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);v.setPadding(20,100,20,20);v.setBackgroundColor(0x55000000);return v;}
    public Rect getRegion(){return new Rect(region);} public float getTargetX(){return targetX;} public float getTargetY(){return targetY;}
    public void clickTarget(){click(targetX,targetY);} public void click(float x,float y){Path path=new Path();path.moveTo(x,y);GestureDescription.StrokeDescription stroke=new GestureDescription.StrokeDescription(path,0,80);dispatchGesture(new GestureDescription.Builder().addStroke(stroke).build(),null,null);}
    private void remove(View v){if(v!=null&&wm!=null){try{wm.removeView(v);}catch(Exception ignored){}}}
}
