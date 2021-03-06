package com.emanga.emanga.app.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.emanga.emanga.app.R;

public class Notification {
	public static Toast enjoyReading(Activity activity){
		LayoutInflater inflater = activity.getLayoutInflater();
    	View view = inflater.inflate(R.layout.toast_with_image, (ViewGroup) activity.findViewById(R.id.toast));

    	Toast toast = new Toast(activity);
    	toast.setDuration(Toast.LENGTH_LONG);
    	toast.setView(view);
    	return toast;
	}
	
	public static void errorMessage(Activity activity, String title, String message, int icon){
		Builder builder = new AlertDialog.Builder(activity);
		builder.setIcon(icon);
	    builder.setTitle(title);
		builder.setMessage(message);
	    builder.setCancelable(true);
	    AlertDialog dialog = builder.create();
	    dialog.show();
	}
}
