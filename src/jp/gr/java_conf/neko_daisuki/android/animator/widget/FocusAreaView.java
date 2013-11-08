package jp.gr.java_conf.neko_daisuki.android.animator.widget;

import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.MotionEvent;
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

    private interface Helper {

        public List<Camera.Area> computeFocusAreas(float x, float y);
        public List<Camera.Area> computeInitialAreas();
        public void draw(Canvas canvas);
    }

    private class DisabledHelper implements Helper {

        public List<Camera.Area> computeFocusAreas(float x, float y) {
            return null;
        }

        public List<Camera.Area> computeInitialAreas() {
            return null;
        }

        public void draw(Canvas canvas) {
            // Does nothing.
        }
    }

    private class EnabledHelper implements Helper {

        private Paint mBorderPaint = new Paint();
        private Paint mOutlinePaint = new Paint();

        public EnabledHelper() {
            mBorderPaint.setARGB(255, 0, 0, 0);
            mBorderPaint.setStyle(Paint.Style.STROKE);
            mBorderPaint.setStrokeWidth(3);
            mOutlinePaint.setARGB(255, 255, 255, 255);
            mOutlinePaint.setStyle(Paint.Style.STROKE);
            mOutlinePaint.setStrokeWidth(7);
        }

        public List<Camera.Area> computeFocusAreas(float x, float y) {
            final float VIEW_SIZE = 2000f;
            final float OFFSET = - VIEW_SIZE / 2;
            float areaX = x / getWidth() * VIEW_SIZE + OFFSET;
            float areaY = y / getHeight() * VIEW_SIZE + OFFSET;

            final float HALF_SIZE = AREA_SIZE / 2;
            Rect rect = new Rect();
            rect.left = (int)(areaX - HALF_SIZE);
            rect.top = (int)(areaY - HALF_SIZE);
            rect.right = (int)(areaX + HALF_SIZE);
            rect.bottom = (int)(areaY + HALF_SIZE);

            return newAreas(rect);
        }

        public List<Camera.Area> computeInitialAreas() {
            int size = (int)AREA_HALF_SIZE;
            Rect rect = new Rect();
            rect.left = rect.top = - size;
            rect.right = rect.bottom = size;
            return newAreas(rect);
        }

        public void draw(Canvas canvas) {
            Rect rect = mAreas.get(0).rect;
            final float SIZE = 1000f;
            int halfHeight = getHeight() / 2;
            int halfWidth = getWidth() / 2;
            float left = halfWidth + rect.left / SIZE * halfWidth;
            float top = halfHeight + rect.top / SIZE * halfHeight;
            float right = halfWidth + rect.right / SIZE * halfWidth;
            float bottom = halfHeight + rect.bottom / SIZE * halfHeight;
            canvas.drawRect(left, top, right, bottom, mOutlinePaint);
            canvas.drawRect(left, top, right, bottom, mBorderPaint);
        }

        private List<Camera.Area> newAreas(Rect rect) {
            List<Camera.Area> areas = new LinkedList<Camera.Area>();
            areas.add(new Camera.Area(rect, 1000));
            return areas;
        }
    }

    private class MotionUpHandler implements MotionEventDispatcher.Proc {

        public boolean run(MotionEvent event) {
            setAreas(mHelper.computeFocusAreas(event.getX(), event.getY()));
            return true;
        }
    }

    private static final float AREA_SIZE = 100f;
    private static final float AREA_HALF_SIZE = AREA_SIZE / 2;

    // documents
    private View mSurfaceView;
    private OnAreaChangedListener mListener;
    private List<Camera.Area> mAreas;
    // helpers
    private MotionEventDispatcher mDispatcher = new MotionEventDispatcher();
    private Helper mHelper;

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

    public void setSurfaceView(View surfaceView) {
        mSurfaceView = surfaceView;
    }

    public boolean onTouchEvent(MotionEvent event) {
        mDispatcher.dispatch(event);
        return true;
    }

    public void setEnabled(boolean enabled) {
        setHelper(enabled);
        initializeAreas();
        mDispatcher.setUpProc(enabled ? new MotionUpHandler() : null);
    }

    public void setOnAreaChangedListener(OnAreaChangedListener l) {
        mListener = l != null ? l : new NopListener();
    }

    public void performChanged() {
        mListener.onAreaChanged(this, mAreas);
        invalidate();
    }

    protected void onDraw(Canvas canvas) {
        mHelper.draw(canvas);
    }

    private void initializeAreas() {
        setAreas(mHelper.computeInitialAreas());
    }

    private void setAreas(List<Camera.Area> areas) {
        mAreas = areas;
        performChanged();
    }

    private void initialize() {
        setOnAreaChangedListener(null);
        setEnabled(true);
    }

    private void setHelper(boolean enabled) {
        mHelper = enabled ? new EnabledHelper() : new DisabledHelper();
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
