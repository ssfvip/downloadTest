package com.example.ssf.downloadtest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    public final static int MSG_UPDATE = 1;
    public final static int MSG_FINISHED = 2;

    private DownloadPercentView mDownloadPercentView;
    private int mDownloadProgress = 0;
    private Handler mHandler = new InnerHandler();
    private boolean downloading = false;

// 下载一张图片
    private ImageView imageView;
    private String[] image_urls =
            {"http://image.tianjimedia.com/uploadImages/2012/010/XC4Y39BYZT9A" +
                    ".jpg","http://image.tianjimedia.com/uploadImages/2015/129/56/J63MI042Z4P8.jpg"};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 图片
        imageView = (ImageView) findViewById(R.id.iv_image);

        mDownloadPercentView = (DownloadPercentView) findViewById(R.id.downloadView);
        mDownloadPercentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mDownloadPercentView.getStatus() == DownloadPercentView.STATUS_PEDDING
                        || mDownloadPercentView.getStatus() == DownloadPercentView.STATUS_PAUSED) {
                    downloading = true;
                    mDownloadPercentView.setStatus(DownloadPercentView.STATUS_DOWNLOADING);

                    // 开启异步下载图片的操作
                    new DownloadAsynTask().execute(image_urls);// 传入图片的URL


                    //模拟下载
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            while (downloading) {
//                                if(mDownloadProgress == 100) {
//                                    mHandler.sendEmptyMessage(MSG_FINISHED);
//                                    return;
//                                }
//                                mDownloadProgress += 1;
//                                mHandler.sendEmptyMessage(MSG_UPDATE);
//                                try{
//                                    Thread.sleep(100);
//                                } catch (Exception e) {
//                                }
//
//                            }
//                        }
//                    }).start();
                } else if(mDownloadPercentView.getStatus() == DownloadPercentView.STATUS_DOWNLOADING){
                    downloading = false;
                    mDownloadPercentView.setStatus(DownloadPercentView.STATUS_PAUSED);
                }
            }
        });
    }


    /**
     * 定义一个类,继承于AsyncTask
     * Params
     * String类型,表示我们传递给异步任务的是一个String类型的参数
     * Prograss
     * Integer类型,进度条一般都是以数字为单位的
     * Result
     * Bitmap类型,根据你的需要,可以自定义
     */
    public class DownloadAsynTask extends AsyncTask<String, Integer, Bitmap>{

        //任务开始前的准备工作,一般都是用于开启进度条的
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
//            mHandler.sendEmptyMessage(MSG_UPDATE);
        }
        //后台线程中,通过Http工具类,访问网络地址,获取bitmap对象
        @Override
        protected Bitmap doInBackground(String... params) {
            //获取地址
            String path = params[1];
            String filename = "local_temp_image";
            try {
                URL url = new URL(path);
                HttpURLConnection connection =
                        (HttpURLConnection) url.openConnection();
                //设置connection的属性
                connection.setDoInput(true);
                connection.setDoOutput(false);
                connection.setConnectTimeout(20 * 1000);

                InputStream in = connection.getInputStream();
                OutputStream out =
                        openFileOutput(filename, Context.MODE_PRIVATE);
                byte[] data = new byte[1024];
                int seg = 0;
                // 获取文件总长度
                long total = connection.getContentLength();
                long current = 0;
                //通过循环请求图片,并计算进度值
                while (!isCancelled() && (seg = in.read(data)) != -1 && downloading) {

                    //开始写文件
                    out.write(data,0,seg);
                    //当前已经获取的长度
                    current += seg;
                    //计算当前进度值
                    mDownloadProgress = (int) ((float) current / total * 100);
                    // 发送handler，进行实时更新进度
                    mHandler.sendEmptyMessage(MSG_UPDATE);
                    //调用pulishProgass方法传递
                    publishProgress(mDownloadProgress);//通知UI线程更新进度
                    SystemClock.sleep(10);
                }
                //关闭流和连接
                connection.disconnect();
                in.close();
                out.close();

                return BitmapFactory
                        .decodeFile(
                                getFileStreamPath(filename)
                                        .getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            //UI线程中,可直接操作控件
            imageView.setImageBitmap(bitmap);
            mHandler.sendEmptyMessage(MSG_FINISHED);
        }
    }
    class InnerHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_FINISHED:
                    // 如果是下载完成的话，发送一个状态
                    mDownloadPercentView.setStatus(DownloadPercentView.STATUS_FINISHED);
                    break;
                case MSG_UPDATE:
                    // 如果任然在下载的过程中的话，传递下载的额进度
                    mDownloadPercentView.setProgress(mDownloadProgress);
                    break;
            }
            super.handleMessage(msg);
        }
    }
}
