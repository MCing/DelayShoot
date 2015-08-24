package cn.delayshoot;

import java.util.Calendar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;
import android.widget.Toast;


public class ChooseDate extends Activity
{
	// 定义5个记录当前时间的变量
	private int year;
	private int month;
	private int day;
	private int hour;
	private int minute;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settime_layout);
		setResult(Activity.RESULT_CANCELED);
		
		DatePicker datePicker = (DatePicker)
			findViewById(R.id.datePicker);
		TimePicker timePicker = (TimePicker) 
			findViewById(R.id.timePicker);
		// 获取当前的年、月、日、小时、分钟
		Calendar c = Calendar.getInstance();
		year = c.get(Calendar.YEAR);
		month = c.get(Calendar.MONTH);
		day = c.get(Calendar.DAY_OF_MONTH);
		hour = c.get(Calendar.HOUR);
		minute = c.get(Calendar.MINUTE);
		// 初始化DatePicker组件，初始化时指定监听器
		datePicker.init(year, month, day, new OnDateChangedListener()
		{

			@Override
			public void onDateChanged(DatePicker arg0, int year
					, int month, int day)
			{
				ChooseDate.this.year = year;
				ChooseDate.this.month = month;
				ChooseDate.this.day = day;
				// 显示当前日期、时间
				showDate(year, month, day, hour, minute);
				Toast.makeText(ChooseDate.this,"您选择的日期："+year+"年  "
				+month+"月  "+day+"日", Toast.LENGTH_SHORT).show();
			}
		});
		// 为TimePicker指定监听器
		timePicker.setOnTimeChangedListener(new OnTimeChangedListener()
		{

			@Override
			public void onTimeChanged(TimePicker view
					, int hourOfDay, int minute)
			{
				ChooseDate.this.hour = hourOfDay;
				ChooseDate.this.minute = minute;
				// 显示当前日期、时间
				showDate(year, month, day, hour, minute);
				Toast.makeText(ChooseDate.this,"您选择的时间："+hourOfDay+"时  "
						+minute+"分", Toast.LENGTH_SHORT).show();
//				
			}
		});
		
		
		Button confirmBtn = (Button) findViewById(R.id.btn_confirm);
		confirmBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
	            intent.putExtra("YEAR", ChooseDate.this.year);
	            intent.putExtra("MONTH", ChooseDate.this.month);
	            intent.putExtra("DAY", ChooseDate.this.day);
	            intent.putExtra("HOUR", ChooseDate.this.hour);
	            intent.putExtra("MIN", ChooseDate.this.minute);
	            setResult(Activity.RESULT_OK, intent);
	            finish();
				
			}
		});
	}

	// 定义在EditText中显示当前日期、时间的方法
	private void showDate(int year, int month
			, int day, int hour, int minute)
	{
		EditText show = (EditText) findViewById(R.id.show);
		show.setText("您选择的日期和时间为：" + year + "年" 
				+ (month + 1) + "月" + day + "日  "
				+ hour + "时" + minute + "分");
	}
}