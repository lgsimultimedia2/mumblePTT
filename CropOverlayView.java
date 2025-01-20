package com.jio.jiotalkie.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class CropOverlayView extends View {
    private Paint borderPaint;
    private Paint overlayPaint;
    private RectF cropRect;
    private RectF imageRect;
    private static final float TOUCH_RADIUS = 30f;
    private boolean isDragging = false;
    private boolean isResizing = false;
    private float touchOffsetX, touchOffsetY;
    private Edge touchedEdge = Edge.NONE;
    private Bitmap imageBitmap;
    private Matrix imageToScreenMatrix;
    private Matrix screenToImageMatrix;

    private enum Edge {
        NONE,
        LEFT,
        RIGHT,
        TOP,
        BOTTOM,
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    public CropOverlayView(Context context) {
        super(context);
        init();
    }

    public CropOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CropOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        borderPaint = new Paint();
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(5f);
        borderPaint.setAntiAlias(true);

        overlayPaint = new Paint();
        overlayPaint.setColor(Color.parseColor("#80000000"));
        overlayPaint.setStyle(Paint.Style.FILL);

        imageRect = new RectF();
        cropRect = new RectF();
        imageToScreenMatrix = new Matrix();
        screenToImageMatrix = new Matrix();
    }

    public void setImageBitmap(Bitmap bitmap) {
        this.imageBitmap = bitmap;
        if (bitmap != null) {
            resetState();
            imageToScreenMatrix.reset();
            screenToImageMatrix.reset();
            updateImageRect();
            initializeCropRect();
            invalidate();
        }
    }

    private void updateImageRect() {
        if (imageBitmap == null || getWidth() == 0 || getHeight() == 0) return;

        float viewWidth = getWidth();
        float viewHeight = getHeight();

        float scale = Math.min(
                viewWidth / imageBitmap.getWidth(),
                viewHeight / imageBitmap.getHeight()
        );

        float left = (viewWidth - imageBitmap.getWidth() * scale) / 2;
        float top = (viewHeight - imageBitmap.getHeight() * scale) / 2;

        imageRect.set(
                left,
                top,
                left + imageBitmap.getWidth() * scale,
                top + imageBitmap.getHeight() * scale
        );

        imageToScreenMatrix.reset();
        imageToScreenMatrix.setRectToRect(
                new RectF(0, 0, imageBitmap.getWidth(), imageBitmap.getHeight()),
                imageRect,
                Matrix.ScaleToFit.CENTER
        );

        screenToImageMatrix.reset();
        imageToScreenMatrix.invert(screenToImageMatrix);
    }

    private void initializeCropRect() {
        float padding = imageRect.width() * 0.1f;
        cropRect.set(
                imageRect.left + padding,
                imageRect.top + padding,
                imageRect.right - padding,
                imageRect.bottom - padding
        );
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateImageRect();
        initializeCropRect();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw the semi-transparent overlay
        canvas.drawRect(0, 0, getWidth(), cropRect.top, overlayPaint);
        canvas.drawRect(0, cropRect.bottom, getWidth(), getHeight(), overlayPaint);
        canvas.drawRect(0, cropRect.top, cropRect.left, cropRect.bottom, overlayPaint);
        canvas.drawRect(cropRect.right, cropRect.top, getWidth(), cropRect.bottom, overlayPaint);

        // Draw crop rectangle
        canvas.drawRect(cropRect, borderPaint);

        // Draw corner indicators
        float cornerSize = 20f;
        // Top-left corner
        canvas.drawLine(cropRect.left, cropRect.top, cropRect.left + cornerSize, cropRect.top, borderPaint);
        canvas.drawLine(cropRect.left, cropRect.top, cropRect.left, cropRect.top + cornerSize, borderPaint);
        // Top-right corner
        canvas.drawLine(cropRect.right - cornerSize, cropRect.top, cropRect.right, cropRect.top, borderPaint);
        canvas.drawLine(cropRect.right, cropRect.top, cropRect.right, cropRect.top + cornerSize, borderPaint);
        // Bottom-left corner
        canvas.drawLine(cropRect.left, cropRect.bottom - cornerSize, cropRect.left, cropRect.bottom, borderPaint);
        canvas.drawLine(cropRect.left, cropRect.bottom, cropRect.left + cornerSize, cropRect.bottom, borderPaint);
        // Bottom-right corner
        canvas.drawLine(cropRect.right - cornerSize, cropRect.bottom, cropRect.right, cropRect.bottom, borderPaint);
        canvas.drawLine(cropRect.right, cropRect.bottom - cornerSize, cropRect.right, cropRect.bottom, borderPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (imageBitmap == null) return false;

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchedEdge = getTouchedEdge(x, y);
                if (touchedEdge != Edge.NONE) {
                    isResizing = true;
                } else if (cropRect.contains(x, y)) {
                    isDragging = true;
                    touchOffsetX = x - cropRect.left;
                    touchOffsetY = y - cropRect.top;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (isResizing) {
                    resizeCropRect(x, y);
                } else if (isDragging) {
                    moveCropRect(x, y);
                }
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                isResizing = false;
                touchedEdge = Edge.NONE;
                break;
        }

        return true;
    }

    private Edge getTouchedEdge(float x, float y) {
        if (Math.abs(x - cropRect.left) < TOUCH_RADIUS && Math.abs(y - cropRect.top) < TOUCH_RADIUS) {
            return Edge.TOP_LEFT;
        } else if (Math.abs(x - cropRect.right) < TOUCH_RADIUS && Math.abs(y - cropRect.top) < TOUCH_RADIUS) {
            return Edge.TOP_RIGHT;
        } else if (Math.abs(x - cropRect.left) < TOUCH_RADIUS && Math.abs(y - cropRect.bottom) < TOUCH_RADIUS) {
            return Edge.BOTTOM_LEFT;
        } else if (Math.abs(x - cropRect.right) < TOUCH_RADIUS && Math.abs(y - cropRect.bottom) < TOUCH_RADIUS) {
            return Edge.BOTTOM_RIGHT;
        } else if (Math.abs(x - cropRect.left) < TOUCH_RADIUS) {
            return Edge.LEFT;
        } else if (Math.abs(x - cropRect.right) < TOUCH_RADIUS) {
            return Edge.RIGHT;
        } else if (Math.abs(y - cropRect.top) < TOUCH_RADIUS) {
            return Edge.TOP;
        } else if (Math.abs(y - cropRect.bottom) < TOUCH_RADIUS) {
            return Edge.BOTTOM;
        }
        return Edge.NONE;
    }

    private void moveCropRect(float x, float y) {
        float newLeft = x - touchOffsetX;
        float newTop = y - touchOffsetY;
        float width = cropRect.width();
        float height = cropRect.height();

        // Constrain to image bounds
        newLeft = Math.max(imageRect.left, Math.min(newLeft, imageRect.right - width));
        newTop = Math.max(imageRect.top, Math.min(newTop, imageRect.bottom - height));

        cropRect.set(newLeft, newTop, newLeft + width, newTop + height);
    }

    private void resizeCropRect(float x, float y) {
        float minWidth = imageRect.width() * 0.1f;
        float minHeight = imageRect.height() * 0.1f;

        // Constrain coordinates to image bounds
        x = Math.max(imageRect.left, Math.min(x, imageRect.right));
        y = Math.max(imageRect.top, Math.min(y, imageRect.bottom));

        switch (touchedEdge) {
            case LEFT:
                cropRect.left = Math.min(x, cropRect.right - minWidth);
                cropRect.left = Math.max(cropRect.left, imageRect.left);
                break;
            case RIGHT:
                cropRect.right = Math.max(x, cropRect.left + minWidth);
                cropRect.right = Math.min(cropRect.right, imageRect.right);
                break;
            case TOP:
                cropRect.top = Math.min(y, cropRect.bottom - minHeight);
                cropRect.top = Math.max(cropRect.top, imageRect.top);
                break;
            case BOTTOM:
                cropRect.bottom = Math.max(y, cropRect.top + minHeight);
                cropRect.bottom = Math.min(cropRect.bottom, imageRect.bottom);
                break;
            case TOP_LEFT:
                cropRect.left = Math.min(x, cropRect.right - minWidth);
                cropRect.top = Math.min(y, cropRect.bottom - minHeight);
                cropRect.left = Math.max(cropRect.left, imageRect.left);
                cropRect.top = Math.max(cropRect.top, imageRect.top);
                break;
            case TOP_RIGHT:
                cropRect.right = Math.max(x, cropRect.left + minWidth);
                cropRect.top = Math.min(y, cropRect.bottom - minHeight);
                cropRect.right = Math.min(cropRect.right, imageRect.right);
                cropRect.top = Math.max(cropRect.top, imageRect.top);
                break;
            case BOTTOM_LEFT:
                cropRect.left = Math.min(x, cropRect.right - minWidth);
                cropRect.bottom = Math.max(y, cropRect.top + minHeight);
                cropRect.left = Math.max(cropRect.left, imageRect.left);
                cropRect.bottom = Math.min(cropRect.bottom, imageRect.bottom);
                break;
            case BOTTOM_RIGHT:
                cropRect.right = Math.max(x, cropRect.left + minWidth);
                cropRect.bottom = Math.max(y, cropRect.top + minHeight);
                cropRect.right = Math.min(cropRect.right, imageRect.right);
                cropRect.bottom = Math.min(cropRect.bottom, imageRect.bottom);
                break;
        }
    }

    public RectF getCropRect() {
        RectF imageCropRect = new RectF();
        screenToImageMatrix.mapRect(imageCropRect, cropRect);
        return imageCropRect;
    }

    // Optional: Add method to get crop rect relative to the image's displayed position
    public RectF getScreenCropRect() {
        return new RectF(cropRect);
    }

    public void resetState() {
        isDragging = false;
        isResizing = false;
        touchedEdge = Edge.NONE;
        touchOffsetX = 0;
        touchOffsetY = 0;
        invalidate();
    }
}