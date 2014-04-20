package com.gyrovague.dualnback;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * @author asimihsan
 *
 */
public class DrawView extends View {
    private String TAG = "DrawView";
    /**
     *  Public bit field flags for determining which square in the grid
     *  to draw.
     */
    public static final int TOP_LEFT        = 1 << 0;
    public static final int TOP_MIDDLE        = 1 << 1;
    public static final int TOP_RIGHT        = 1 << 2;
    public static final int MIDDLE_LEFT        = 1 << 3;
    public static final int MIDDLE_RIGHT    = 1 << 4;
    public static final int BOTTOM_LEFT        = 1 << 5;
    public static final int BOTTOM_MIDDLE    = 1 << 6;
    public static final int BOTTOM_RIGHT    = 1 << 7;
    public static final int[] mPossibleSquares = {TOP_LEFT, TOP_MIDDLE, TOP_RIGHT, MIDDLE_LEFT, MIDDLE_RIGHT, BOTTOM_LEFT, BOTTOM_MIDDLE, BOTTOM_RIGHT};

    /**
     * Hash map to store drawing rectangles for each square location.
     */
    private HashMap<Integer, Rect> mSquareToRect = new HashMap<Integer, Rect>();
    private int mSquareSize;

    /**
     * Handle the perpetual white cross.
     */
    private Rect mRectCross;
    private Paint mPaintWhiteCross = new Paint();

    /**
     * Track the current drawing state in a bit-field.
     */
    private int mSquaresDrawn = 0;

    private Paint   mPaint = new Paint();
    private String    mTag;
    private Resources mResources;
    private Handler mHandlerUI;

    public DrawView(Context context) {
        super(context);
        commonConstructor(context);
    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        commonConstructor(context);
    }

    private void commonConstructor(Context context) {
        setFocusable(true);
        mTag = context.getString(R.string.app_name);
        mResources = context.getResources();
        mPaintWhiteCross.setColor(mResources.getColor(R.color.solid_white));

        // force a re-draw of the display.  eventually triggers onDraw().
        invalidate();
    }

    public void setmHandlerUI(Handler mHandlerUI) {
        this.mHandlerUI = mHandlerUI;
    }

    public void setmSquaresDrawnFromIndex(int index) {
        this.mSquaresDrawn = 1 << index;
    }

    public void disableAllSquares() {
        mSquaresDrawn = 0;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final String FTAG = TAG + "::onDraw()";
        Log.d(FTAG, "entry");
        draw(canvas);
    }

    public void draw(Canvas canvas) {
        final String FTAG = TAG + "::draw()";
        Log.d(FTAG, "entry");
        drawWhiteCross(canvas);

        // for each possible square, clear those not enabled and draw those enabled.
        for (Map.Entry<Integer, Rect> entry : mSquareToRect.entrySet()) {
            int square = entry.getKey();
            Rect rect = entry.getValue();
            if ((mSquaresDrawn & square) != 0) {
                // visible.
                mPaint.setColor(mResources.getColor(R.color.solid_blue));
            } else {
                // invisible
                mPaint.setColor(mResources.getColor(R.drawable.transparent_background));
            }
            canvas.drawRect(rect, mPaint);
        }

        if (mHandlerUI != null) {
            mHandlerUI.sendEmptyMessage(GameActivity.MSG_TYPE_DRAWING_DONE);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        setupDrawingCoordinates();
        mPaintWhiteCross.setStrokeWidth(mSquareSize / 16);
    }

    /**
     * Draw the perpetual white cross.
     */
    private void drawWhiteCross(Canvas canvas) {
        canvas.drawLine(mRectCross.left + mRectCross.width() / 2,
                        mRectCross.top,
                        mRectCross.left + mRectCross.width() / 2,
                        mRectCross.bottom,
                        mPaintWhiteCross);
        canvas.drawLine(mRectCross.left,
                        mRectCross.top + mRectCross.height() / 2,
                        mRectCross.right,
                        mRectCross.top + mRectCross.height() / 2,
                        mPaintWhiteCross);
    }

    /**
     * Set up internal tracking of where the squares in the grid, for later
     * painting.
     */
    private void setupDrawingCoordinates() {
        /**
         * Measurements.
         */
        int width = getWidth();
        int height = getHeight();
        int top = getTop();
        int left = getLeft();

        /**
         * Calculations.
         */
        int canvas = Math.min(width, height);
        int square = canvas / 4;
        mSquareSize = square;
        int gap = square / 4;

        /**
         * Set up all of the squares' locations.
         * Rect(int left, int top, int right, int bottom)
         */
        mSquareToRect.clear();
        mSquareToRect.put(TOP_LEFT,            new Rect(left+1*gap+0*square, top+1*gap+0*square, left+1*gap+1*square, top+1*gap+1*square));
        mSquareToRect.put(TOP_MIDDLE,        new Rect(left+2*gap+1*square, top+1*gap+0*square, left+2*gap+2*square, top+1*gap+1*square));
        mSquareToRect.put(TOP_RIGHT,        new Rect(left+3*gap+2*square, top+1*gap+0*square, left+3*gap+3*square, top+1*gap+1*square));
        mSquareToRect.put(MIDDLE_LEFT,        new Rect(left+1*gap+0*square, top+2*gap+1*square, left+1*gap+1*square, top+2*gap+2*square));
        mSquareToRect.put(MIDDLE_RIGHT,     new Rect(left+3*gap+2*square, top+2*gap+1*square, left+3*gap+3*square, top+2*gap+2*square));
        mSquareToRect.put(BOTTOM_LEFT,         new Rect(left+1*gap+0*square, top+3*gap+2*square, left+1*gap+1*square, top+3*gap+3*square));
        mSquareToRect.put(BOTTOM_MIDDLE,     new Rect(left+2*gap+1*square, top+3*gap+2*square, left+2*gap+2*square, top+3*gap+3*square));
        mSquareToRect.put(BOTTOM_RIGHT,     new Rect(left+3*gap+2*square, top+3*gap+2*square, left+3*gap+3*square, top+3*gap+3*square));

        /**
         * Set the middle-middle square for the perpetual white cross.
         */
        mRectCross = new Rect(left+2*gap+1*square, top+2*gap+1*square, left+2*gap+2*square, top+2*gap+2*square);
    } // private void setupDrawingCoordinates()

}
