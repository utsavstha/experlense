package fr.pchab.androidrtc;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class DrawingView extends View {


    private static float STROKE_WIDTH = 15f;

    private List<Path> paths = new ArrayList<>();
    private Paint paint = null;

    private Path path = new Path();

    private ThirdEyeDrawEvent.ThirdEyeEventListener listener = null;

    private boolean canDraw = false;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);

//        int[] colors = new int[]{Color.BLUE, Color.CYAN, Color.GREEN,
//                Color.MAGENTA, Color.YELLOW, Color.RED, Color.WHITE};


            paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(Color.BLUE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeWidth(STROKE_WIDTH);

    }

    public void setPaintColor(int color){
        paint.setColor(color);
    }

    public void setCanDraw(boolean canDraw) {
        this.canDraw = canDraw;
    }

    public boolean canDraw(){
        return this.canDraw;
    }

    /**
     * Erases the signature.
     */
    public void clear() {
        for (Path path : paths) {
            path.reset();
        }
        // Repaints the entire view.
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isInEditMode()) {
            canvas.drawARGB(255, 255, 0, 0);
        }
        super.onDraw(canvas);
        int i = 0;
        for (Path path : paths) {
            canvas.drawPath(path, paint);
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if(!canDraw)return false;

        float eventX = event.getX();
        float eventY = event.getY();

        ThirdEyeDrawEvent thirdEyeDrawEvent = new ThirdEyeDrawEvent();
        thirdEyeDrawEvent.eventX = eventX / getWidth();
        thirdEyeDrawEvent.eventY = eventY / getHeight();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                path = new Path();
                paths.add(path);
                path.moveTo(eventX, eventY);

                thirdEyeDrawEvent.drawMode = ThirdEyeDrawEvent.ACTION_DOWN;

                if (this.listener != null){
                    listener.onThirdEyeEvent(thirdEyeDrawEvent);
                }

                return true;

            case MotionEvent.ACTION_MOVE:
                path.lineTo(eventX, eventY);

                thirdEyeDrawEvent.drawMode = ThirdEyeDrawEvent.ACTION_MOVE;

                if (this.listener != null){
                    listener.onThirdEyeEvent(thirdEyeDrawEvent);
                }

            case MotionEvent.ACTION_UP:
                // After replaying history, connect the line to the touch point.
                break;

            default:
                return false;
        }

        // Include half the stroke width to avoid clipping.
        invalidate();

        return true;
    }


    public void implementThirdEyeEvent(@NonNull ThirdEyeDrawEvent event){
        switch (event.drawMode){
            case ThirdEyeDrawEvent.ACTION_DOWN:
                path = new Path();
                paths.add(path);
                path.moveTo(event.eventX * getWidth(),event.eventY * getHeight());
            case ThirdEyeDrawEvent.ACTION_MOVE:
                path.lineTo(event.eventX * getWidth(),event.eventY * getHeight());
            default:
                break;
        }
        invalidate();
    }

    public void setThirdEyeEventListener(ThirdEyeDrawEvent.ThirdEyeEventListener listener){
        this.listener = listener;
    }

}