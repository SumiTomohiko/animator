package jp.gr.java_conf.neko_daisuki.android.animator.widget;

import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;

import jp.gr.java_conf.neko_daisuki.android.view.MotionEventDispatcher;

public class FocusAreaView extends View {

    public interface OnAreaChangedListener {

        public void onAreaChanged(FocusAreaView view, List<Camera.Area> areas);
    }

    private class NopListener implements OnAreaChangedListener {

        public void onAreaChanged(FocusAreaView view, List<Camera.Area> areas) {
            // Does nothing.
        }
    }

    private class MotionUpHandler implements MotionEventDispatcher.Proc {

        public boolean run(MotionEvent event) {
            setAreas(computeFocusAreas(event.getX(), event.getY()));
            return true;
        }
    }

    private interface AreaDrawer {

        public void draw(Canvas canvas);
    }

    private class TrueAreaDrawer implements AreaDrawer {

        private class Size {

            int width;
            int height;
        }

        // Helper
        private RectF mRect = new RectF();
        private Size mSize = new Size();

        public void draw(Canvas canvas) {
            Size size = getFrameSize();
            Rect rect = mAreas.get(0).rect;
            RectF holderRect = computeRectInHolder(size, rect);
            RectF surfaceRect = computeRectInSurface(size, holderRect);
            RectF viewRect = computeRect(surfaceRect);
            canvas.drawRect(viewRect, mOutlinePaint);
            canvas.drawRect(viewRect, mBorderPaint);
        }

        private Size getFrameSize() {
            Rect rect = mSurfaceView.getHolder().getSurfaceFrame();
            mSize.width = rect.right - rect.left;
            mSize.height = rect.bottom - rect.top;
            return mSize;
        }

        private RectF computeRect(RectF rect) {
            int deltaX = mSurfaceView.getLeft() - getLeft();
            int deltaY = mSurfaceView.getTop() - getTop();
            rect.left += deltaX;
            rect.top += deltaY;
            rect.right += deltaX;
            rect.bottom += deltaY;
            return rect;
        }

        private RectF computeRectInSurface(Size size, RectF rect) {
            int deltaX = (mSurfaceView.getWidth() - size.width) / 2;
            int deltaY = (mSurfaceView.getHeight() - size.height) / 2;
            rect.left += deltaX;
            rect.top += deltaY;
            rect.right += deltaX;
            rect.bottom += deltaY;
            return rect;
        }

        private RectF computeRectInHolder(Size size, Rect rect) {
            int halfWidth = size.width / 2;
            int halfHeight = size.height / 2;
            final float SIZE = 1000f;
            mRect.left = halfWidth + rect.left / SIZE * halfWidth;
            mRect.top = halfHeight + rect.top / SIZE * halfHeight;
            mRect.right = halfWidth + rect.right / SIZE * halfWidth;
            mRect.bottom = halfHeight + rect.bottom / SIZE * halfHeight;
            return mRect;
        }
    }

    private class FalseAreaDrawer implements AreaDrawer {

        public void draw(Canvas canvas) {
            // Does nothing.
        }
    }

    private static final float AREA_SIZE = 100f;

    // documents
    private SurfaceView mSurfaceView;
    private OnAreaChangedListener mListener;
    private List<Camera.Area> mAreas;
    private Paint mBorderPaint = new Paint();
    private Paint mOutlinePaint = new Paint();
    // helpers
    private MotionEventDispatcher mDispatcher = new MotionEventDispatcher();
    private AreaDrawer mAreaDrawer;

    public FocusAreaView(Context context) {
        super(context);
        initialize();
    }

    public FocusAreaView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public FocusAreaView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    public void setSurfaceView(SurfaceView surfaceView) {
        mSurfaceView = surfaceView;
    }

    public boolean onTouchEvent(MotionEvent event) {
        mDispatcher.dispatch(event);
        return true;
    }

    public void setOnAreaChangedListener(OnAreaChangedListener l) {
        mListener = l != null ? l : new NopListener();
    }

    public void performChanged() {
        mListener.onAreaChanged(this, mAreas);
        invalidate();
    }

    public void showAreas() {
        mAreaDrawer = new TrueAreaDrawer();
        invalidate();
    }

    public void hideAreas() {
        mAreaDrawer = new FalseAreaDrawer();
        invalidate();
    }

    protected void onDraw(Canvas canvas) {
        mAreaDrawer.draw(canvas);
    }

    private List<Camera.Area> computeInitialAreas() {
        final float AREA_HALF_SIZE = AREA_SIZE / 2;
        int size = (int)AREA_HALF_SIZE;
        Rect rect = new Rect();
        rect.left = rect.top = - size;
        rect.right = rect.bottom = size;
        return newAreas(rect);
    }

    private void initializeAreas() {
        setAreas(computeInitialAreas());
    }

    private void setAreas(List<Camera.Area> areas) {
        mAreas = areas;
        performChanged();
    }

    private void initialize() {
        setOnAreaChangedListener(null);
        initializeAreas();
        mDispatcher.setUpProc(new MotionUpHandler());
        initializePaints();
        showAreas();
    }

    private List<Camera.Area> newAreas(Rect rect) {
        List<Camera.Area> areas = new LinkedList<Camera.Area>();
        areas.add(new Camera.Area(rect, 1000));
        return areas;
    }

    private List<Camera.Area> computeFocusAreas(float x, float y) {
        float surfaceX = x + getLeft() - mSurfaceView.getLeft();
        float surfaceY = y + getTop() - mSurfaceView.getTop();

        final float VIEW_SIZE = 2000f;
        final float OFFSET = - VIEW_SIZE / 2;
        float areaX = surfaceX / mSurfaceView.getWidth() * VIEW_SIZE + OFFSET;
        float areaY = surfaceY / mSurfaceView.getHeight() * VIEW_SIZE + OFFSET;

        final float HALF_SIZE = AREA_SIZE / 2;
        Rect rect = new Rect();
        rect.left = (int)(areaX - HALF_SIZE);
        rect.top = (int)(areaY - HALF_SIZE);
        rect.right = (int)(areaX + HALF_SIZE);
        rect.bottom = (int)(areaY + HALF_SIZE);
        if (rect.left < -1000) {
            rect.left = -1000;
            rect.right = rect.left + (int)AREA_SIZE;
        }
        if (rect.top < -1000) {
            rect.top = -1000;
            rect.bottom = rect.top + (int)AREA_SIZE;
        }
        if (1000 < rect.right) {
            rect.right = 1000;
            rect.left = rect.right - (int)AREA_SIZE;
        }
        if (1000 < rect.bottom) {
            rect.bottom = 1000;
            rect.top = rect.bottom - (int)AREA_SIZE;
        }

        return newAreas(rect);
    }

    private void initializePaints() {
        mBorderPaint.setARGB(255, 0, 0, 0);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(3);
        mOutlinePaint.setARGB(255, 255, 255, 255);
        mOutlinePaint.setStyle(Paint.Style.STROKE);
        mOutlinePaint.setStrokeWidth(7);
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
