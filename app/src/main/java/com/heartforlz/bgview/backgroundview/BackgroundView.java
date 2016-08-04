package com.heartforlz.bgview.backgroundview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lz on 2016/7/24.
 * github地址：https://github.com/lzuntalented/BackgroundView
 * 自定义背景视图
 *
 * 参考canvas中就是绘图，地址：https://github.com/hustcc/canvas-nest.js
 */
public class BackgroundView extends View implements View.OnTouchListener,GestureDetector.OnGestureListener {

    private List<OnTouchListener> mlist = new ArrayList<>();//存储多次点击事件的响应

    private int lineCount = 99;//屏幕出现的点数量
    private Context mContext;
    private List<LineConfig> random_lines = new ArrayList<>();//屏幕出现的点集合

    private GestureDetector mGestureDetector = null;

    private LineConfig currentDown = new LineConfig();//触摸点

    private int color_point = Color.argb(100, 255, 255, 255);//点的颜色
    private int color_line = Color.argb(100,255,0,0);//线的颜色

    public BackgroundView(Context context) {
        super(context);

        mContext = context;
        init();
    }

    public BackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    public BackgroundView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init();
    }

    /**
     * 初始化
     */
    private void init(){
        currentDown.max = 100000;//触摸点与其他点连线的最大距离

        /*添加手势监听*/
        mGestureDetector = new GestureDetector(mContext, this);
        this.setOnTouchListener(this);
        this.setLongClickable(true);

        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);

        int width = wm.getDefaultDisplay().getWidth();
        int height = wm.getDefaultDisplay().getHeight();

        /*初始化点集合*/
        for(int i=0; i < lineCount ; ++i){
            LineConfig l = new LineConfig();
            l.x = (float) (Math.random() * width);
            l.y = (float) (Math.random() * height);
            l.xa = (float) (Math.random() * 2 - 1);
            l.ya = (float) (Math.random() * 2 - 1);
            l.max = 15000;

            random_lines.add(l);
        }

        /*添加触摸点到集合*/
        random_lines.add(currentDown);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);

        int canvas_width = wm.getDefaultDisplay().getWidth();
        int canvas_height = wm.getDefaultDisplay().getHeight();

        Paint paint_blue = new Paint();
        paint_blue.setStyle(Paint.Style.STROKE);
        paint_blue.setStrokeWidth(1);


        float d, x_dist, y_dist, dist;
        for(int i = 0; i < random_lines.size() ; i ++ ){
            LineConfig r = random_lines.get(i);
            r.x += r.xa;
            r.y += r.ya; //移动
            r.xa *= r.x > canvas_width || r.x < 0 ? -1 : 1;
            r.ya *= r.y > canvas_height || r.y < 0 ? -1 : 1; //碰到边界，反向反弹

            paint_blue.setColor(color_point);
            canvas.drawCircle(r.x, r.y, 1, paint_blue);

            for(int j = i + 1; j < random_lines.size() ; j ++ ){
                LineConfig e = random_lines.get(j);
                x_dist = r.x - e.x; //x轴距离
                y_dist = r.y - e.y; //y轴距离
                dist = x_dist * x_dist + y_dist * y_dist; //总距离
                if(dist < e.max){
                    if(e.touch && dist >= e.max / 2){
                        r.x -= 0.03 * x_dist;
                        r.y -= 0.03 * y_dist; //靠近的时候加速
                    }

                    paint_blue.setColor(color_line);
                    canvas.drawLine(r.x,r.y,e.x,e.y,paint_blue);
                }
            }
        }

        new DrawThread().start();//启动定时线程绘制

    }

    /**
     * 重写触摸监听事件，
     * 将添加的触摸事件添加到集合中
     * 防止外部对此视图再次添加事件导致触摸点无效
     */
    @Override
    public void setOnTouchListener(OnTouchListener l) {
        mlist.add(l);
        super.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                for (int i = 0; i < mlist.size(); ++i) {
                    OnTouchListener ml = mlist.get(i);
                    if (ml.onTouch(v, event)) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mGestureDetector.onTouchEvent(event);//分发手势通知
        return false;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        currentDown.touch = true;
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        currentDown.touch = false;
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        currentDown.x = e2.getX();//记录触摸点坐标
        currentDown.y = e2.getY();//记录触摸点坐标
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    /**
     * 定时通知绘制线程
     * */
    private class DrawThread extends Thread{

        @Override
        public void run() {
            super.run();
            try {
                sleep(1000 / 45);//每秒绘制45次
                mHandler.sendEmptyMessage(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    /**绘图通知handler*/
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what == 1) draw_canvas();
        }
    };

    /**
     * 重绘视图通知
     */
    private void draw_canvas() {
        this.invalidate();
    }

    /**
     * 屏幕点对象
     * */
    private class LineConfig{
        public float x;//x左标
        public float y;//y左标
        public float xa;//x增量
        public float ya;//y增量
        public float max;//两点间最大距离，超过此距离不再绘制线段
        public boolean touch = false;//标示是否为触摸点

    }

}
