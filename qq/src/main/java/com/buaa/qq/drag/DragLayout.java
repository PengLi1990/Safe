package com.buaa.qq.drag;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import com.nineoldandroids.view.ViewHelper;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Created by Administrator on 2016/3/30.
 */
public class DragLayout extends FrameLayout {

    private static final String TAG = "TAG";
    private ViewDragHelper mViewDragHelper;
    private ViewGroup mLeftView;
    private ViewGroup mMainView;
    private int mHeight;
    private int mWidth;
    private int mRange;

    public DragLayout(Context context) {
        this(context, null);
    }

    public DragLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mViewDragHelper = ViewDragHelper.create(this, new ViewDragHelper.Callback() {
            // 1. 根据返回结果决定当前child是否可以拖拽
            // child 当前被拖拽的View
            // pointerId 区分多点触摸的id
            @Override
            public boolean tryCaptureView(View child, int pointerId) {
                Log.d(TAG, "tryCaptureView: " + child);
                return true;
            }

            // 2. 根据建议值 修正将要移动到的(横向)位置   (重要)
            // 此时没有发生真正的移动
            @Override
            public int clampViewPositionHorizontal(View child, int left, int dx) {
                // child: 当前拖拽的View
                // left 新的位置的建议值, dx 位置变化量
                // left = oldLeft + dx;
                Log.d(TAG, "clampViewPositionHorizontal: "
                        + "oldLeft: " + child.getLeft() + " dx: " + dx + " left: " +left);
                if(child == mMainView){
                    left = fixLeft(left);
                }
                return left;
            }

            @Override
            public void onViewCaptured(View capturedChild, int activePointerId) {
                Log.d(TAG, "onViewCaptured: " + capturedChild);
                // 当capturedChild被捕获时,调用.
                super.onViewCaptured(capturedChild, activePointerId);
            }

            @Override
            public int getViewHorizontalDragRange(View child) {
                // 返回拖拽的范围, 不对拖拽进行真正的限制. 仅仅决定了动画执行速度
                return mRange;
            }

            @Override
            public int getViewVerticalDragRange(View child) {
                return super.getViewVerticalDragRange(child);
            }


            @Override
            public int clampViewPositionVertical(View child, int top, int dy) {
                return super.clampViewPositionVertical(child, top, dy);
            }

            // 3. 当View位置改变的时候, 处理要做的事情 (更新状态, 伴随动画, 重绘界面)
            // 此时,View已经发生了位置的改变
            @Override
            public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
                super.onViewPositionChanged(changedView, left, top, dx, dy);

                int newLeft = left;
                if(changedView == mLeftView){
                    newLeft = mMainView.getLeft() + dx;
                }

                newLeft = fixLeft(newLeft);

                if(changedView == mLeftView){
                    //当左面板移动之后再强制放回去
                    mLeftView.layout(0,0,0 + mWidth,0 + mHeight);
                    mMainView.layout(newLeft,0,newLeft + mWidth,0 + mHeight);
                }
                //为了兼容低版本，需要进行重绘
                invalidate();
            }

            //4.释放动画时调用
            @Override
            public void onViewReleased(View releasedChild, float xvel, float yvel) {
                super.onViewReleased(releasedChild, xvel, yvel);
                if(xvel > 0){
                    open();
                }else if(xvel==0 || mMainView.getLeft() > mRange / 2.0f){
                    open();
                }else {
                    close();
                }
            }

            @Override
            public void onViewDragStateChanged(int state) {
                super.onViewDragStateChanged(state);
            }
        });
    }

    //关闭左侧菜单
    public void close() {
        close(true);
    }

    //平滑移动
    public void close(boolean isSmooth){
        // 1. 触发一个平滑动画
        int finalLeft = 0;
        if(isSmooth){
            if(mViewDragHelper.smoothSlideViewTo(mMainView,finalLeft,0)){
                // 返回true代表还没有移动到指定位置, 需要刷新界面.
                // 参数传this(child所在的ViewGroup)
                ViewCompat.postInvalidateOnAnimation(this);
            }else {
                mMainView.layout(finalLeft,0,finalLeft + mWidth,0 + mHeight);
            }
        }

    }

    //打开左侧菜单
    public void open() {
        open(true);
    }

    public void open(boolean isSmooth){
        int finalLeft = mRange;
        if(isSmooth){
            // 1. 触发一个平滑动画
            if(mViewDragHelper.smoothSlideViewTo(mMainView, finalLeft, 0)){
                // 返回true代表还没有移动到指定位置, 需要刷新界面.
                // 参数传this(child所在的ViewGroup)
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }else {
            mMainView.layout(finalLeft, 0, finalLeft + mWidth, 0 + mHeight);
        }
    }

    //修正左边距
    private int fixLeft(int left) {
        if(left < 0){
            return 0;
        }else if(left > mRange){
            return mRange;
        }
        return left;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();

        // 2. 持续平滑动画 (高频率调用)
        if(mViewDragHelper.continueSettling(true)){
            //  如果返回true, 动画还需要继续执行
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }


    public void dispatchDragEvent(int newLeft) {
        float percent = newLeft * 1.0f / mRange;
        //伴随动画
        //左面板平移、缩放、浓淡变化
        animViews(percent);
    }

    private void animViews(float percent) {
        //		> 1. 左面板: 缩放动画, 平移动画, 透明度动画
        // 缩放动画 0.0 -> 1.0 >>> 0.5f -> 1.0f  >>> 0.5f * percent + 0.5f
        //		mLeftContent.setScaleX(0.5f + 0.5f * percent);
        //		mLeftContent.setScaleY(0.5f + 0.5f * percent);
        ViewHelper.setScaleX(mLeftView, evaluate(percent, 0.5f, 1.0f));
        ViewHelper.setScaleY(mLeftView, 0.5f + 0.5f * percent);
        // 平移动画: -mWidth / 2.0f -> 0.0f
        ViewHelper.setTranslationX(mLeftView, evaluate(percent, -mWidth / 2.0f, 0));
        // 透明度: 0.5 -> 1.0f
        ViewHelper.setAlpha(mLeftView, evaluate(percent, 0.5f, 1.0f));

        //		> 2. 主面板: 缩放动画
        // 1.0f -> 0.8f
        ViewHelper.setScaleX(mMainView, evaluate(percent, 1.0f, 0.8f));
        ViewHelper.setScaleY(mMainView, evaluate(percent, 1.0f, 0.8f));

        //		> 3. 背景动画: 亮度变化 (颜色变化)
        getBackground().setColorFilter((Integer)evaluateColor(percent, Color.BLACK, Color.TRANSPARENT), PorterDuff.Mode.SRC_OVER);
    }

    //颜色变化过度
    public Object evaluateColor(float fraction, Object startValue, Object endValue) {
        int startInt = (Integer) startValue;
        int startA = (startInt >> 24) & 0xff;
        int startR = (startInt >> 16) & 0xff;
        int startG = (startInt >> 8) & 0xff;
        int startB = startInt & 0xff;

        int endInt = (Integer) endValue;
        int endA = (endInt >> 24) & 0xff;
        int endR = (endInt >> 16) & 0xff;
        int endG = (endInt >> 8) & 0xff;
        int endB = endInt & 0xff;

        return (int)((startA + (int)(fraction * (endA - startA))) << 24) |
                (int)((startR + (int)(fraction * (endR - startR))) << 16) |
                (int)((startG + (int)(fraction * (endG - startG))) << 8) |
                (int)((startB + (int)(fraction * (endB - startB))));
    }

    //估值器
    public Float evaluate(float fraction, Number startValue, Number endValue) {
        float startFloat = startValue.floatValue();
        return startFloat + fraction * (endValue.floatValue() - startFloat);
    }

    //传递触摸事件
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mViewDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mViewDragHelper.processTouchEvent(event);
        //返回true，持续接收事件
        return true;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if(getChildCount() < 2){
            throw new IllegalStateException("布局至少有俩孩子. Your ViewGroup must have 2 children at least.");
        }
        if(!(getChildAt(0) instanceof ViewGroup && getChildAt(1) instanceof ViewGroup)){
            throw new IllegalArgumentException("子View必须是ViewGroup的子类. Your children must be an instance of ViewGroup");
        }

        mLeftView = (ViewGroup)getChildAt(0);
        mMainView = (ViewGroup)getChildAt(1);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        //当尺寸有变化时调用
        mHeight = getMeasuredHeight();
        mWidth = getMeasuredWidth();

        mRange = (int)(mWidth *0.6f);
    }
}
