package cn.delayshoot;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnticipateOvershootInterpolator;
import android.widget.Button;
import android.widget.TextView;
import cn.wheel.widget.NumericWheelAdapter;
import cn.wheel.widget.WheelView;

@SuppressLint("NewApi")
public class MainActivity extends Activity implements OnClickListener, Callback{
	// Debugging
    private static final String TAG = "DelayShoot";
    private static final boolean D = true;
	private static final int REQUEST_STARTTIME = 0;
    public static void log(String msg){
    	if(D){
    		Log.e(TAG, msg);
    	}
    }
	
	private SurfaceView mSurface = null;
	private SurfaceHolder mHolder = null;
	private Camera mCamera = null;
	private int pictureWidth = 1920;
	private int pictureHeight = 1080;
	private int previewWidth = 1280;
	private int previewHeight = 720;
	private int pictureQulity = 100;
	//camera线程
	private CameraThread mCameraThread;
    private Handler mCameraHandler;
	//layout view
	private Button picTimeBtn;
	private Button startBtn;
	private TextView countTv;
	private Button picStartTimeBtn;
	private TextView startTimeTv;
	//参数
	private int interval = 60;  //时长，初始值为60秒
	private Date startTime;
	private int totalToken;
	
	private Timer timer;
	private TimerTask shootTask;
	class ShootTimerTask extends TimerTask{
		@Override
		public void run() {
			if(!isCameraOpened){
				openCamera();
			}
			takePicture();
		}
	}
	private boolean isStart = false;
	private boolean isCameraOpened = false;
	private String savePath = Environment.getExternalStorageDirectory()+"/0824/";
	PowerManager.WakeLock wl;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
               WindowManager.LayoutParams.FLAG_FULLSCREEN);
//		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);  //常亮
		setContentView(R.layout.activity_main_ui);
		//init view
		picTimeBtn = (Button) findViewById(R.id.btn_picktime);
		picTimeBtn.setOnClickListener(this);
		startBtn = (Button) findViewById(R.id.btn_start);
		startBtn.setOnClickListener(this);
		picStartTimeBtn = (Button) findViewById(R.id.btn_startTime);
		picStartTimeBtn.setOnClickListener(this);
		countTv = (TextView) findViewById(R.id.tv_counter);
		startTimeTv = (TextView) findViewById(R.id.tv_startTime);
		
		this.mSurface = (SurfaceView)findViewById(R.id.surfaceview);
        this.mHolder = this.mSurface.getHolder();
        this.mHolder.addCallback(this);
        this.mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        //thread
        mCameraThread = new CameraThread("camerathread");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper()){
        	@Override
        	public void handleMessage(Message msg) {
        		switch(msg.what){
        		case CameraThread.OPENCAMERA:  //打开相机
        			mCameraThread.openCamera();
        			break;
        		case CameraThread.CLOSECAMERA:  //关闭相机
        			mCameraThread.closeCamera();
        			break;
        		case CameraThread.TAKEPICTURE:  //关闭相机
        			mCameraThread.takePicture();
        			break;	
        		}
        		super.handleMessage(msg);
        	}
        };
        
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"bright");  
        //点亮屏幕  
        wl.acquire();
	}
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		closeCamera();
		mCameraThread.quitSafely();
		wl.release(); 
	}
	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.btn_picktime:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			LayoutInflater factory = LayoutInflater.from(this);
            final View textEntryView = factory.inflate(R.layout.timer_layout, null);
            initPickTime(textEntryView);
                builder.setTitle("请选择时长");
                builder.setView(textEntryView);
                builder.setNegativeButton("重置", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	initWheel(60);
                    	//点击重置后滑轮归回初始化位置，对话框不退出
                    	setQuitable(dialog, false);
                    }
                });
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                   	 int hours = wheelHour.getCurrentItem();
                	 int mins = wheelMin.getCurrentItem();
                	 int secs = wheelSec.getCurrentItem();
                	 interval = hours*3600 + mins*60 + secs;
                	 setQuitable(dialog, true);
                    }
                });
                builder.create().show();
			break;
		case R.id.btn_start:
			if(!isStart){
				isStart = true;
				startTask();
				startBtn.setText("stop");
			}else{
				isStart = false;
				startBtn.setText("start");
				closeCamera();
				stopTask();
			}
			
			break;
		case R.id.btn_startTime:
			Intent intent = new Intent(this, ChooseDate.class);
            startActivityForResult(intent, REQUEST_STARTTIME);
			break;
		}
	}
	private void stopTask() {
		if(timer != null){
			timer.cancel();
			timer = null;
		}
		if(shootTask != null){
			shootTask.cancel();
			shootTask = null;
		}
	}
	private void startTask() {
		stopTask();
		shootTask = new ShootTimerTask();
		timer = new Timer();
		if(startTime == null){
			startTime = new Date();
		}
		timer.schedule(shootTask, startTime, interval*1000);
		totalToken = 0;
	}
	public int[] secToDet(long seconds){
		int[] det = new int[4];
		det[0] = (int) (seconds/(24*60*60));  //days
        det[1] = (int) ((seconds - det[0]*(24*60*60))/3600);  //hours
        det[2] = (int) ((seconds - det[0]*(24*60*60) - det[1]*3600)/60);  //minutes
        det[3] = (int) (seconds - det[0]*(24*60*60) - det[1]*3600 - det[2]*60);   //seconds
        return det;
	}
	///////////////////选择时长
	private WheelView wheelHour;
	private WheelView wheelMin;
	private WheelView wheelSec;
	private void initPickTime(View view){
		wheelHour = getWheel(view, R.id.hours);
		wheelMin = getWheel(view, R.id.minutes);
		wheelSec = getWheel(view, R.id.secs);
		initWheel(interval);
	}
	@SuppressLint("NewApi")
	private void initWheel(long seconds) {
		int[] detail = secToDet(seconds);
		wheelHour.setAdapter(new NumericWheelAdapter(0, 23));
		wheelMin.setAdapter(new NumericWheelAdapter(0, 59));
		wheelSec.setAdapter(new NumericWheelAdapter(0, 59));
		wheelHour.setCurrentItem(detail[1]);
		wheelMin.setCurrentItem(detail[2]);
		wheelSec.setCurrentItem(detail[3]);
		wheelHour.setCyclic(true);
		wheelHour.setInterpolator(new AnticipateOvershootInterpolator());
		wheelMin.setCyclic(true);
		wheelMin.setInterpolator(new AnticipateOvershootInterpolator());
		wheelSec.setCyclic(true);
		wheelSec.setInterpolator(new AnticipateOvershootInterpolator());
		
		wheelHour.setLabel("小时");
		wheelMin.setLabel("分钟");
		wheelSec.setLabel("秒");
	}
	private WheelView getWheel(View view, int id) {
		return (WheelView) view.findViewById(id);
	}
	/**
	 * 设置点击对话框按钮后是否退出对话框（重置选项需要不退出）
	 * @param dialog
	 * @param quit  true：退出  false：不退出
	 */
	private void setQuitable(DialogInterface dialog, boolean quit){
		try  
        {  
            Field field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");  
            field.setAccessible(true);  
             //设置mShowing值，欺骗android系统  
            field.set(dialog, quit);  
        }catch(Exception e) {  
            e.printStackTrace();  
        }  
	}
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
//		openCamera();
	}
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
	}
	
private class CameraThread extends HandlerThread{
		
		public final static int OPENCAMERA = 0;
		public final static int CLOSECAMERA = 1;
		public final static int TAKEPICTURE = 2;
		
		public CameraThread(String name) {
			super(name);
		}
		private void openCamera() {
			try {
				closeCamera();
				mCamera = Camera.open();
				mCamera.setDisplayOrientation(90); //设置横行录制
				Camera.Parameters params = mCamera.getParameters();
				params.setPictureSize(pictureWidth, pictureHeight);  //照片分辨率
				params.setPreviewSize(previewWidth, previewHeight);   //预览分辨率
				mCamera.setParameters(params);
				mCamera.setPreviewDisplay(mHolder);
				mCamera.startPreview();
				mCamera.autoFocus(null);
				isCameraOpened = true;
				log("open camera");
			} catch (IOException e) { 
				log("开启Camera exception");
				log(e.getMessage());
			}catch(Exception e){
				log("open 异常");
				log(e.getMessage());
			}
		}
		private void closeCamera() {
			if (mCamera == null) { return; }
			try {
				mCamera.reconnect();
				mCamera.stopPreview();
				mCamera.setPreviewCallback(null);
				mCamera.release();
				mCamera = null;
				isCameraOpened = false;
				log("close camera");
			} 
			catch (IOException e) { log(e.getMessage()); }
			  catch (RuntimeException e) { log(e.getMessage()); }
		}
		private void takePicture() {
			mCamera.takePicture(null, null, myjpegCalback);
		}
    }
	
	private void openCamera() {
		mCameraHandler.sendEmptyMessage(CameraThread.OPENCAMERA);
	}
	protected void savePicture(Bitmap bm) {
		
		File f = new File(savePath);
		if(!f.exists()){
		    f.mkdir();
		}
		BufferedOutputStream bos = null;
		try{
			DateFormat df = new SimpleDateFormat("MMddHHmmss");
			File fTest = new File(savePath + df.format(new Date()) + ".jpg");
			FileOutputStream fout = new FileOutputStream(fTest);
			bos = new BufferedOutputStream(fout);
			bm.compress(Bitmap.CompressFormat.JPEG, pictureQulity, bos);
			totalToken++;
			displayCounter();
		}catch(Exception e){
			
		}finally{
			try{
				bos.flush();
				bos.close();
			}catch(IOException e){
				
			}
		}
	}
	private void closeCamera() {
		mCameraHandler.sendEmptyMessage(CameraThread.CLOSECAMERA);
	}
	private void takePicture() {
		mCameraHandler.sendEmptyMessage(CameraThread.TAKEPICTURE);
	}
	//拍照
	PictureCallback myjpegCalback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
			log("图片大小:"+data.length);
			savePicture(bitmap);
			openCamera();
		}
	};
	public void displayCounter(){
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				countTv.setText("拍摄数量:" + totalToken);
			}
		});
	}
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_STARTTIME:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
            	int year;
            	int month;
            	int day;
            	int hour;
            	int minute;
                year = data.getExtras().getInt("YEAR");
                month = data.getExtras().getInt("MONTH");
                day = data.getExtras().getInt("DAY");
                hour = data.getExtras().getInt("HOUR");
                minute = data.getExtras().getInt("MIN");
                Calendar cal = Calendar.getInstance();
                cal.set(year, month, day, hour, minute);
                startTime = cal.getTime();
                displayStartTime();
            }
            break;
        }
    }
	public void displayStartTime(){
		if(startTime != null){
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
			startTimeTv.setText(sdf.format(startTime));
		}
	}

}
