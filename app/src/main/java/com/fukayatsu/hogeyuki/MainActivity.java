package com.fukayatsu.hogeyuki;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


public class MainActivity extends Activity implements View.OnTouchListener,
        ScaleGestureDetector.OnScaleGestureListener,
        GestureDetector.OnGestureListener {
    static final int REQUEST_GALLERY = 990;
    static final int REQUEST_CAMERA  = 991;

    Uri mImageUri;
    ImageView mImageView;
    ImageView mKao;
    RelativeLayout mTouchArea;

    List<Integer> originalLayout;
    int mOriginalWidth;
    int mOriginalHeight;
    float mScaleFactor = 1.0f;

    ScaleGestureDetector mScaleGestureDetector;
    GestureDetector mGestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageView  = (ImageView) findViewById(R.id.imageView);
        mKao = (ImageView) findViewById(R.id.kao);
        mTouchArea = (RelativeLayout) findViewById(R.id.touchArea);
        mTouchArea.setOnTouchListener(this);

        mScaleGestureDetector = new ScaleGestureDetector(this, this);
        mGestureDetector = new GestureDetector(this, this);
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
                    mScaleFactor = 1.0f;
                    mKao.layout(originalLayout.get(0), originalLayout.get(1), originalLayout.get(2), originalLayout.get(3));
                }

                return true;
            case R.id.action_send:
                String filename = saveFacedImage();
                if (filename == null) {return false;}
                intent = new Intent(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filename)));
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_TEXT, " #ほげゆき ");
                startActivity(Intent.createChooser(intent, "compatible apps:"));
                return true;
            case R.id.action_save:
                saveFacedImage();
                return true;
            case R.id.action_licences:
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setTitle("Licences");
                alertDialogBuilder.setMessage(".+ゆき: (c) awayuki All rights reserved.");
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String saveFacedImage() {
        if (mImageView.getDrawable() == null) { return null; }
        Bitmap originalBitmap = ((BitmapDrawable)mImageView.getDrawable()).getBitmap();
        Bitmap faceBitmap = ((BitmapDrawable)mKao.getDrawable()).getBitmap();

        float scale;
        if (originalBitmap.getWidth() > originalBitmap.getHeight()) {
            scale = 1.0f * originalBitmap.getWidth() / mImageView.getWidth();
        } else {
            scale = 1.0f * originalBitmap.getHeight() / mImageView.getHeight();
        }
        faceBitmap = Bitmap.createScaledBitmap(faceBitmap,
                (int) (mKao.getWidth() * scale),
                (int) (mKao.getWidth() * scale * faceBitmap.getHeight() / faceBitmap.getWidth()),
                false);

        Bitmap canvasBitmap = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(canvasBitmap);
        canvas.drawBitmap(originalBitmap, 0, 0, null);

        if (originalBitmap.getWidth() > originalBitmap.getHeight()) {
             float diffY = (mImageView.getHeight() - mImageView.getWidth() * originalBitmap.getHeight() / originalBitmap.getWidth()) / 2;
            canvas.drawBitmap(faceBitmap, mKao.getLeft() * scale, (mKao.getTop() - diffY) * scale, null);
        } else {
            float diffX = (mImageView.getWidth() - mImageView.getHeight() * originalBitmap.getWidth() / originalBitmap.getHeight()) / 2 ;
            canvas.drawBitmap(faceBitmap, (mKao.getLeft() - diffX) * scale, mKao.getTop() * scale, null);
        }

        File dir = new File(Environment.getExternalStorageDirectory().getPath() + "/Hogeyuki/");
        if (!dir.exists()) { dir.mkdir(); }

        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
        String filename = format.format(date) + ".jpg";
        String fullname = dir.getAbsolutePath() + "/" + filename;

        try {
            FileOutputStream os = new FileOutputStream(fullname);
            canvasBitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "failure :(", Toast.LENGTH_SHORT).show();
            return null;
        }

        ContentValues values = new ContentValues();
        ContentResolver contentResolver = getContentResolver();
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.TITLE, filename);
        values.put("_data", fullname);
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Toast.makeText(this, "saved :)", Toast.LENGTH_SHORT).show();
        return fullname;
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

        if (originalLayout == null) {
            originalLayout = Arrays.asList(mKao.getLeft(), mKao.getTop(), mKao.getRight(), mKao.getBottom());
            mOriginalWidth = mKao.getWidth();
            mOriginalHeight = mKao.getHeight();
        }


        if (mScaleGestureDetector != null) {
            final boolean isInProgres = mScaleGestureDetector.isInProgress();
            mScaleGestureDetector.onTouchEvent(event);
            if (isInProgres || mScaleGestureDetector.isInProgress()) {
                return true;
            }
        }
        if (mGestureDetector != null) {
            return mGestureDetector.onTouchEvent(event);
        }

        return false;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        scaleKao(detector.getScaleFactor() * mScaleFactor);
        return false;
    }

    private void scaleKao(float scaleFactor) {

        int centerX = (mKao.getLeft() + mKao.getRight()) / 2;
        int centerY = (mKao.getTop() + mKao.getBottom()) / 2;
        int width = (int) (mOriginalWidth * scaleFactor);
        int height = (int) (mOriginalHeight * scaleFactor);

        mKao.layout(centerX - width/2,
                centerY - height/2,
                centerX + width/2,
                centerY + height/2);
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        mScaleFactor *= detector.getScaleFactor();
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (Math.abs(distanceX) > mKao.getWidth() /3 || Math.abs(distanceY) > mKao.getHeight()/3) {
            return false;
        }

        mKao.layout(mKao.getLeft() - (int) distanceX,
                mKao.getTop() - (int) distanceY,
                mKao.getRight() - (int) distanceX,
                mKao.getBottom() - (int) distanceY);
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }
}
