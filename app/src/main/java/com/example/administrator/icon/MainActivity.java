package com.example.administrator.icon;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private Button take_photo;
    private Button choose_photo;
    private ImageView photo;

    private Uri imageUri;

    private File outputImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        take_photo = (Button) findViewById(R.id.take_photo);
        take_photo.setOnClickListener(this);

        choose_photo = (Button) findViewById(R.id.choose_photo);
        choose_photo.setOnClickListener(this);

        photo = (ImageView) findViewById(R.id.icon);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.take_photo:
                TakePhoto();
                break;
            case R.id.choose_photo:
                ChoosePhoto();
                break;
            default:
                break;
        }
    }

    //调用手机摄像头
    private void TakePhoto(){
        //创建FILE对象，用于存储拍照后的图片，第二个参数是图片的名字
        //位置是SD卡的应用关联目录，这个是专门用于存放当前应用缓存数据的
        outputImage = new File(getExternalCacheDir(),"outputImage.jpg");
        try{
            if(outputImage.exists()){
                outputImage.delete();
            }
            outputImage.createNewFile();
        }catch (IOException e){
            e.printStackTrace();
        }
        //如果手机系统大于7.0的，就要用到内容提供器
        //否则就直接路径传进去
        if(Build.VERSION.SDK_INT>=24){
            imageUri = FileProvider.getUriForFile(MainActivity.this,"com.example.administrator.icon.fileprovider",outputImage);
        }else{
            imageUri = Uri.fromFile(outputImage);
        }
        //这里的imageUri就是outputImage的路径，也就是说图片要传到这里来

        //下面是启动相机程序
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri); //这里就是指定了图片的输出地址
        //因为这里指定了图片的Uri，所以后面的onActivityResult()返回的data就为空，源码有解释
        startActivityForResult(intent,1);
    }

    //从相册中选取图片
    private void ChoosePhoto(){
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }else{
            openAlbum();
        }
    }

    private void openAlbum(){
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");//指定打开是的是相册
        startActivityForResult(intent,2);
    }

    public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        switch (requestCode){
            case 1:
                if(grantResults.length>0&&grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    openAlbum();
                }else{
                    Toast.makeText(this,"uU denied the permission",Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    //因为前面的程序是有返回数据的，结果是返回到下面的函数中
    //所以就在下面的函数中处理逻辑
    protected void onActivityResult(int requestCode,int resultCode,Intent data){
        switch (requestCode){
            case 1:
                //这个是拍照返回的数据
                if(resultCode == RESULT_OK){
                    //try{
                        //图片压缩为原来的1/4
                        BitmapFactory.Options opts = new BitmapFactory.Options();
                        opts.inSampleSize = 4;
                        Bitmap bitmap = BitmapFactory.decodeFile(imageUri.getPath(),opts);

                        //Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        //因为这种方法不知道怎么压缩图片，所以只好换成decodeFile()

                        photo.setImageBitmap(bitmap);
                    //}catch (FileNotFoundException e){
                        //e.printStackTrace();
                    //}
                }
                break;
            case 2:
                if(resultCode == RESULT_OK){
                    if(Build.VERSION.SDK_INT>=19){
                        //4.4及以上系统的用下面的方法
                        handleOne(data);
                    }else{
                        //4.4以下的系统用下面的方法
                        handleTwo(data);
                    }
                }
                break;
            default:
                break;
        }
    }

    private void handleOne(Intent data){


    }

    private void handleTwo(Intent data){
        Uri uri = data.getData();
        String imagePath = getImagePath(uri,null);
        displayImage(imagePath);
    }

    private String getImagePath(Uri uri,String selection){
        String path = null;
        Cursor cursor = getContentResolver().query(uri,null,selection,null,null);
        if(cursor!=null){
            if(cursor.moveToFirst()){
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    private void displayImage(String imagePath){
        if(imagePath!=null){
            //这里同样要压缩照片，因为这部垃圾三星估计是因为返回的照片内存太大所以显示不了(选截图的图片的时候可以正常显示)
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;

            Bitmap bitmap = BitmapFactory.decodeFile(imagePath,options);
            photo.setImageBitmap(bitmap);
        }else{
            Toast.makeText(this,"failed to get photo",Toast.LENGTH_SHORT).show();
        }
    }
}
