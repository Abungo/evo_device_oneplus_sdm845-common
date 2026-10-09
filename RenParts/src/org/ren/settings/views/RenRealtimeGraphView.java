/*
 * SPDX-FileCopyrightText: 2024 Ren Linux / Android Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.ren.settings.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class RenRealtimeGraphView extends View {

    public static final int MODE_POWER = 0;
    public static final int MODE_CPU = 1;
    public static final int MODE_THERMAL_GPU = 2;

    private static final int HISTORY_CAPACITY = 30;

    private int mGraphMode = MODE_POWER;

    /* Metrics History Buffers */
    private final float[] mPrimaryHistory = new float[HISTORY_CAPACITY];
    private final float[] mSecondaryHistory = new float[HISTORY_CAPACITY];
    private int mHistoryCount = 0;

    /* Graph Paints */
    private final Paint mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mPrimaryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mSecondaryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mLegendTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint mLegendPrimaryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mLegendSecondaryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path mPath = new Path();
    private final Path mSecondaryPath = new Path();
    private final Path mFillPath = new Path();

    public RenRealtimeGraphView(Context context) {
        this(context, null);
    }

    public RenRealtimeGraphView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RenRealtimeGraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaints();
    }

    public void setGraphMode(int mode) {
        mGraphMode = mode;
        initPaintsForMode();
        invalidate();
    }

    private void initPaints() {
        mBorderPaint.setColor(Color.parseColor("#1FFFFFFF"));
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(1.5f);

        mGridPaint.setColor(Color.parseColor("#0FFFFFFF"));
        mGridPaint.setStyle(Paint.Style.STROKE);
        mGridPaint.setStrokeWidth(1f);
        mGridPaint.setPathEffect(new DashPathEffect(new float[]{8f, 8f}, 0));

        mLegendTextPaint.setColor(Color.parseColor("#99FFFFFF"));
        mLegendTextPaint.setTextSize(26f);
        mLegendTextPaint.setFakeBoldText(true);

        initPaintsForMode();
    }

    private void initPaintsForMode() {
        mLegendPrimaryPaint.setTextSize(30f);
        mLegendPrimaryPaint.setFakeBoldText(true);
        mLegendPrimaryPaint.setStyle(Paint.Style.FILL);

        mLegendSecondaryPaint.setTextSize(30f);
        mLegendSecondaryPaint.setFakeBoldText(true);
        mLegendSecondaryPaint.setStyle(Paint.Style.FILL);

        switch (mGraphMode) {
            case MODE_POWER:
                /* Neon Emerald (Primary W) & Soft Cyan (Secondary mA) */
                mPrimaryPaint.setColor(Color.parseColor("#00E676"));
                mPrimaryPaint.setStyle(Paint.Style.STROKE);
                mPrimaryPaint.setStrokeWidth(4.5f);
                mPrimaryPaint.setStrokeCap(Paint.Cap.ROUND);
                mPrimaryPaint.setStrokeJoin(Paint.Join.ROUND);

                mSecondaryPaint.setColor(Color.parseColor("#00E5FF"));
                mSecondaryPaint.setStyle(Paint.Style.STROKE);
                mSecondaryPaint.setStrokeWidth(3.5f);
                mSecondaryPaint.setStrokeCap(Paint.Cap.ROUND);

                mLegendPrimaryPaint.setColor(Color.parseColor("#00E676"));
                mLegendSecondaryPaint.setColor(Color.parseColor("#00E5FF"));
                break;

            case MODE_CPU:
                /* Coral Red (Big Kryo MHz) & Gold Yellow (LITTLE MHz) */
                mPrimaryPaint.setColor(Color.parseColor("#FF5252"));
                mPrimaryPaint.setStyle(Paint.Style.STROKE);
                mPrimaryPaint.setStrokeWidth(4.5f);
                mPrimaryPaint.setStrokeCap(Paint.Cap.ROUND);
                mPrimaryPaint.setStrokeJoin(Paint.Join.ROUND);

                mSecondaryPaint.setColor(Color.parseColor("#FFD740"));
                mSecondaryPaint.setStyle(Paint.Style.STROKE);
                mSecondaryPaint.setStrokeWidth(3.5f);
                mSecondaryPaint.setStrokeCap(Paint.Cap.ROUND);

                mLegendPrimaryPaint.setColor(Color.parseColor("#FF5252"));
                mLegendSecondaryPaint.setColor(Color.parseColor("#FFD740"));
                break;

            case MODE_THERMAL_GPU:
                /* Bright Amber (°C) & Vivid Violet (GPU MHz) */
                mPrimaryPaint.setColor(Color.parseColor("#FF9100"));
                mPrimaryPaint.setStyle(Paint.Style.STROKE);
                mPrimaryPaint.setStrokeWidth(4.5f);
                mPrimaryPaint.setStrokeCap(Paint.Cap.ROUND);
                mPrimaryPaint.setStrokeJoin(Paint.Join.ROUND);

                mSecondaryPaint.setColor(Color.parseColor("#E040FB"));
                mSecondaryPaint.setStyle(Paint.Style.STROKE);
                mSecondaryPaint.setStrokeWidth(3.5f);
                mSecondaryPaint.setStrokeCap(Paint.Cap.ROUND);

                mLegendPrimaryPaint.setColor(Color.parseColor("#FF9100"));
                mLegendSecondaryPaint.setColor(Color.parseColor("#E040FB"));
                break;
        }
    }

    public void addSample(float primary, float secondary) {
        if (mHistoryCount < HISTORY_CAPACITY) {
            mPrimaryHistory[mHistoryCount] = primary;
            mSecondaryHistory[mHistoryCount] = secondary;
            mHistoryCount++;
        } else {
            System.arraycopy(mPrimaryHistory, 1, mPrimaryHistory, 0, HISTORY_CAPACITY - 1);
            System.arraycopy(mSecondaryHistory, 1, mSecondaryHistory, 0, HISTORY_CAPACITY - 1);
            mPrimaryHistory[HISTORY_CAPACITY - 1] = primary;
            mSecondaryHistory[HISTORY_CAPACITY - 1] = secondary;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        float paddingLeft = 4f;
        float paddingTop = 32f;
        float paddingRight = 4f;
        float paddingBottom = 4f;

        float drawWidth = width - paddingLeft - paddingRight;
        float drawHeight = height - paddingTop - paddingBottom;

        /* Grid lines */
        float midY = paddingTop + (drawHeight / 2f);
        float quarterY = paddingTop + (drawHeight / 4f);
        float threeQuarterY = paddingTop + (3f * drawHeight / 4f);
        canvas.drawLine(paddingLeft, quarterY, width - paddingRight, quarterY, mGridPaint);
        canvas.drawLine(paddingLeft, midY, width - paddingRight, midY, mGridPaint);
        canvas.drawLine(paddingLeft, threeQuarterY, width - paddingRight, threeQuarterY, mGridPaint);

        float primaryMin = 0f, primaryMax = 5f;
        float secondaryMin = 0f, secondaryMax = 1000f;

        switch (mGraphMode) {
            case MODE_POWER:
                primaryMin = 0f; primaryMax = 6f;       /* Power Watts */
                secondaryMin = 0f; secondaryMax = 2000f;  /* Current mA */
                canvas.drawText("● Instant Power (W)", paddingLeft + 10, paddingTop - 10, mLegendPrimaryPaint);
                canvas.drawText("● Current (mA)", paddingLeft + (drawWidth * 0.55f), paddingTop - 10, mLegendSecondaryPaint);
                break;

            case MODE_CPU:
                primaryMin = 0f; primaryMax = 2800f;     /* Big Core MHz */
                secondaryMin = 0f; secondaryMax = 1800f;  /* LITTLE Core MHz */
                canvas.drawText("● Big Cores (MHz)", paddingLeft + 10, paddingTop - 10, mLegendPrimaryPaint);
                canvas.drawText("● LITTLE Cores", paddingLeft + (drawWidth * 0.55f), paddingTop - 10, mLegendSecondaryPaint);
                break;

            case MODE_THERMAL_GPU:
                primaryMin = 20f; primaryMax = 90f;      /* Temp °C */
                secondaryMin = 0f; secondaryMax = 800f;   /* GPU MHz */
                canvas.drawText("● SoC Temp (°C)", paddingLeft + 10, paddingTop - 10, mLegendPrimaryPaint);
                canvas.drawText("● GPU Clock (MHz)", paddingLeft + (drawWidth * 0.55f), paddingTop - 10, mLegendSecondaryPaint);
                break;
        }

        if (mHistoryCount < 2) return;

        /* Draw Primary Bezier Smooth Curve with Gradient Fill */
        drawSmoothCurveWithFill(canvas, mPrimaryHistory, primaryMin, primaryMax, paddingLeft, paddingTop, drawWidth, drawHeight, mPrimaryPaint);

        /* Draw Secondary Bezier Curve */
        drawSmoothCurve(canvas, mSecondaryHistory, secondaryMin, secondaryMax, paddingLeft, paddingTop, drawWidth, drawHeight, mSecondaryPaint);
    }

    private void drawSmoothCurveWithFill(Canvas canvas, float[] data, float minY, float maxY,
                                         float left, float top, float width, float height, Paint paint) {
        mPath.reset();
        mFillPath.reset();

        float stepX = width / (HISTORY_CAPACITY - 1);
        float bottomY = top + height;

        mFillPath.moveTo(left, bottomY);

        float prevX = left;
        float prevY = top + height - (((Math.max(minY, Math.min(maxY, data[0])) - minY) / (maxY - minY)) * height);
        mPath.moveTo(prevX, prevY);
        mFillPath.lineTo(prevX, prevY);

        for (int i = 1; i < mHistoryCount; i++) {
            float val = Math.max(minY, Math.min(maxY, data[i]));
            float normalized = (val - minY) / (maxY - minY);
            float currentX = left + (i * stepX);
            float currentY = top + height - (normalized * height);

            /* Cubic Bezier Smooth Control Points */
            float controlX1 = prevX + (stepX / 2f);
            float controlY1 = prevY;
            float controlX2 = prevX + (stepX / 2f);
            float controlY2 = currentY;

            mPath.cubicTo(controlX1, controlY1, controlX2, controlY2, currentX, currentY);
            mFillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, currentX, currentY);

            prevX = currentX;
            prevY = currentY;
        }

        float lastX = left + ((mHistoryCount - 1) * stepX);
        mFillPath.lineTo(lastX, bottomY);
        mFillPath.close();

        /* Premium Modern Gradient Fill */
        int mainColor = paint.getColor();
        int startAlpha = Color.argb(45, Color.red(mainColor), Color.green(mainColor), Color.blue(mainColor));
        mFillPaint.setShader(new LinearGradient(0, top, 0, bottomY, startAlpha, Color.TRANSPARENT, Shader.TileMode.CLAMP));

        canvas.drawPath(mFillPath, mFillPaint);
        canvas.drawPath(mPath, paint);
    }

    private void drawSmoothCurve(Canvas canvas, float[] data, float minY, float maxY,
                                 float left, float top, float width, float height, Paint paint) {
        mSecondaryPath.reset();

        float stepX = width / (HISTORY_CAPACITY - 1);
        float prevX = left;
        float prevY = top + height - (((Math.max(minY, Math.min(maxY, data[0])) - minY) / (maxY - minY)) * height);
        mSecondaryPath.moveTo(prevX, prevY);

        for (int i = 1; i < mHistoryCount; i++) {
            float val = Math.max(minY, Math.min(maxY, data[i]));
            float normalized = (val - minY) / (maxY - minY);
            float currentX = left + (i * stepX);
            float currentY = top + height - (normalized * height);

            float controlX1 = prevX + (stepX / 2f);
            float controlY1 = prevY;
            float controlX2 = prevX + (stepX / 2f);
            float controlY2 = currentY;

            mSecondaryPath.cubicTo(controlX1, controlY1, controlX2, controlY2, currentX, currentY);

            prevX = currentX;
            prevY = currentY;
        }
        canvas.drawPath(mSecondaryPath, paint);
    }
}
