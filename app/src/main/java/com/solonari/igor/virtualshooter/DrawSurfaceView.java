package com.solonari.igor.virtualshooter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;


public class DrawSurfaceView extends View {
    Point me = new Point(47.034590d, 28.880315d, "Me");
    Paint mPaint = new Paint();
    private double OFFSET = 0d;
    private double screenWidth, screenHeight = 0d;
    private Bitmap[] mSpots;
    public ArrayList<Point> props = new ArrayList<>();
    private static final String Tag = "DrawSurface";
    Bitmap spot  = BitmapFactory.decodeResource(getResources(), R.drawable.dot);


    public DrawSurfaceView(Context c, Paint paint) {
        super(c);
    }

    public DrawSurfaceView(Context context, AttributeSet set) {
        super(context, set);
        mPaint.setColor(Color.GREEN);
        mPaint.setTextSize(50);
        mPaint.setStrokeWidth(DpiUtils.getPxFromDpi(getContext(), 2));
        mPaint.setAntiAlias(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.d(Tag, "onSizeChanged in here w=" + w + " h=" + h);
        screenWidth = (double) w;
        screenHeight = (double) h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
            for (Point i : props) {

                double angle = MathUtils.getBearing(me.latitude, me.longitude, i.latitude, i.longitude);
                double delta;


                if ((angle >= 35 && angle < 225 && angle >= OFFSET ) ||
                        (angle >= 0 && angle < 35 && OFFSET < 225 && angle >= OFFSET) ||
                        (angle >= 225 && angle < 360 && OFFSET > 35 && angle >= OFFSET)) {

                    delta = angle - OFFSET;
                    i.x = (float) (screenWidth/2 + screenWidth/2 * delta/35);
                }
                else if ((angle >= 0 && angle < 35 && OFFSET >= 225)){
                    delta = 360 - OFFSET + angle;
                    i.x = (float) (screenWidth/2 + screenWidth/2 * delta/35);
                }
                else if (angle >= 225 && angle < 360 && OFFSET < 35){
                    delta = 360 - angle + OFFSET;
                    i.x = (float) (screenWidth/2 - screenWidth/2 * delta/35);
                }
                else {
                    delta = OFFSET - angle;
                    i.x = (float) (screenWidth/2 - screenWidth/2 * delta/35);
                }
                    i.y = (float) screenHeight/2 - spot.getHeight()/2;
                    canvas.drawBitmap(spot, i.x, i.y, mPaint); //camera spot
                    canvas.drawText(i.description, i.x + spot.getWidth(), i.y, mPaint); //text
        }
        canvas.drawLine(0.0f, (float) screenHeight/2, (float) screenWidth, (float) screenHeight/2, mPaint);
        canvas.drawText(Double.toString(OFFSET), 10, 100, mPaint); //text
    }

    public void setOffset(float offset) {

        this.OFFSET = offset;
    }

    public void setMyLocation(double latitude, double longitude) {
        me.latitude = latitude;
        me.longitude = longitude;
    }
    
    public void setPoints(ArrayList<Point> points) {
        props = points;
        //TODO: Exclude self location for Array
    }

}
