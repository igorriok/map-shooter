package com.solonari.igor.mapshooter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;


public class AimView extends View {

    Paint mPaint = new Paint();
    private float sW, sH = 0;
    private static final String Tag = "Aim";


    public AimView(Context c, Paint paint) {
        super(c);
    }

    public AimView(Context context, AttributeSet set) {
        super(context, set);
        mPaint.setColor(Color.BLUE);
        mPaint.setTextSize(40);
        mPaint.setStrokeWidth(DpiUtils.getPxFromDpi(getContext(), 1));
        mPaint.setAntiAlias(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.d(Tag, "onSizeChanged in here w=" + w + " h=" + h);
        sW = (float) w;
        sH = (float) h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = 20;
        float h = 40;
        canvas.drawLine(sW/w, sH/2, sW*19/w, sH/2, mPaint);

        canvas.drawLine(sW/2, sH/h, sW/2, sH*39/h, mPaint);
        canvas.drawLine(sW*9/w, sH*16/h, sW*9/w, sH*24/h, mPaint);
        canvas.drawLine(sW*11/w, sH*16/h, sW*11/w, sH*24/h, mPaint);
        canvas.drawLine(sW*8/w, sH*18/h, sW*8/w, sH*22/h, mPaint);
        canvas.drawLine(sW*12/w, sH*18/h, sW*12/w, sH*22/h, mPaint);

        for (int i = 2; i < 19; i++) {
            canvas.drawLine(sW*i/w, sH*19/h, sW*i/w, sH*21/h, mPaint);
        }
        for (int l = 2; l < 39; l = l+2) {
            canvas.drawLine(sW/2, sH*l/h, sW/2+10, sH*l/h, mPaint);
        }
    }

}

