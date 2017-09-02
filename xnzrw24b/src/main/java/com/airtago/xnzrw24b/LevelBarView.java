package com.airtago.xnzrw24b;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/**
 * Created by alexe on 07.02.2017.
 */

public class LevelBarView extends View {
    public LevelBarView(Context context) {
        super(context);

        barFill = new Paint();
        barFill.setColor(Color.YELLOW);
        //_circleFill.setStrokeWidth(5);
        barFill.setStyle(Paint.Style.FILL);
        barFill.setAntiAlias(true);
        barFill.setAlpha(95);

        barStroke = new Paint();
        barStroke.setColor(Color.YELLOW);
        barStroke.setStrokeWidth(5);
        barStroke.setStyle(Paint.Style.STROKE);
        barStroke.setAntiAlias(true);

        mTextPaint = new Paint();
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(30);
        mTextPaint.setStyle(Paint.Style.STROKE);

        barPath = new Path();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBarRelativeHeight > 0) {
            barPath.reset();
            int height, width;
            if (mHasSize) {
                height = mHeight;
                width = mWidth;
            } else {
                height = getHeight();
                width = getWidth();
            }
            int barTop = (int)(height * (1d - mBarRelativeHeight));
            RectF rect = new RectF(width - BAR_WIDTH, barTop, width, height);
            barPath.addRect(rect, Path.Direction.CW);
            canvas.drawPath(barPath, barFill);
            canvas.drawRect(rect, barStroke);

            String levelText = String.format("%.1f", mLevel);
            float textX = rect.left + rect.width() / 2 - mTextPaint.measureText(levelText) / 2;
            canvas.drawText(levelText, textX, rect.top, mTextPaint);
        }
    }

    private Paint barStroke;
    private Paint barFill;
    private Paint mTextPaint;
    private Path barPath;

    private double mLevel;
    private double mBarRelativeHeight;
    private boolean mHasSize = false;
    private int mWidth;
    private int mHeight;

    private final int BAR_WIDTH = 50;

    public void setLevel(double level) {
        mLevel = level;
        if (level > 0) {
            mBarRelativeHeight = Math.abs(100 - level) / 100d;
        } else {
            mBarRelativeHeight = Math.abs(-100 - level) / 100d;
        }
    }

    public void setSizes(int width, int height) {
        mHasSize = true;
        mWidth = width;
        mHeight = height;
    }

    public void Reset() {
        mBarRelativeHeight = 0;
    }
}
