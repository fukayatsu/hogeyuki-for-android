package com.fukayatsu.hogeyuki;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends Activity implements View.OnTouchListener {
    static final int REQUEST_GALLERY = 990;
    static final int REQUEST_CAMERA  = 991;

    Uri mImageUri;
    ImageView mImageView;
    ImageView mKao;
    RelativeLayout mTouchArea;

    private List<Integer> originalLayout;
    private int offsetX;
    private int offsetY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageView  = (ImageView) findViewById(R.id.imageView);
        mKao = (ImageView) findViewById(R.id.kao);
        mTouchArea = (RelativeLayout) findViewById(R.id.touchArea);
        mTouchArea.setOnTouchListener(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Intent intent = null;
        switch (item.getItemId()) {
            case R.id.action_camera:
                String fileName = "hogeyuki_new_photo.jpg";
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.TITLE, fileName);
                values.put(MediaStore.Images.Media.DESCRIPTION,"Image capture by camera");
                mImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                intent = new Intent();
                intent.setAction("android.media.action.IMAGE_CAPTURE");
                intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
                intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
                startActivityForResult(intent, REQUEST_CAMERA);
                return true;
            case R.id.action_gallery:
                intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Select picture"), REQUEST_GALLERY);
                return true;
            case R.id.action_reset:
                if (originalLayout != null) {
                    mKao.layout(originalLayout.get(0), originalLayout.get(1), originalLayout.get(2), originalLayout.get(3));
                }

                return true;
            case R.id.action_send:
                return true;
            case R.id.action_save:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_GALLERY) {
                mImageUri = intent.getData();
            } else if (requestCode == REQUEST_CAMERA) {
                if (mImageUri == null) { return ; }
            }
            mImageView.setImageURI(mImageUri);

        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
//        Log.d("touch", String.valueOf(event.getX()));
        int x = (int) event.getRawX();
        int y = (int) event.getRawY();

        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            int diffX = x - offsetX;
            int diffY = y - offsetY;

            mKao.layout(mKao.getLeft()+diffX, mKao.getTop()+diffY, mKao.getRight()+diffX, mKao.getBottom()+diffY);

            offsetX = x;
            offsetY = y;
        } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (originalLayout == null) {
                originalLayout = Arrays.asList(mKao.getLeft(), mKao.getTop(), mKao.getRight(), mKao.getBottom());
            }
            offsetX = x;
            offsetY = y;
        }
        return false;
    }
}
