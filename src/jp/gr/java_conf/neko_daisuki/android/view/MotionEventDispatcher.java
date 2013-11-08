package jp.gr.java_conf.neko_daisuki.android.view;

import android.util.SparseArray;
import android.view.MotionEvent;

public class MotionEventDispatcher {

    public interface Proc {

        public boolean run(MotionEvent event);
    }

    private SparseArray<Proc> mMap;

    public MotionEventDispatcher() {
        mMap = new SparseArray<Proc>();
    }

    public void setDownProc(Proc proc) {
        setProc(MotionEvent.ACTION_DOWN, proc);
    }

    public void setUpProc(Proc proc) {
        setProc(MotionEvent.ACTION_UP, proc);
    }

    public void setMoveProc(Proc proc) {
        setProc(MotionEvent.ACTION_MOVE, proc);
    }

    public void removeDownProc() {
        removeProc(MotionEvent.ACTION_DOWN);
    }

    public void removeUpProc() {
        removeProc(MotionEvent.ACTION_UP);
    }

    public void removeMoveProc() {
        removeProc(MotionEvent.ACTION_MOVE);
    }

    public boolean dispatch(MotionEvent event) {
        Proc proc = mMap.get(event.getActionMasked());
        return proc != null ? proc.run(event) : false;
    }

    private void setProc(int action, Proc proc) {
        mMap.put(action, proc);
    }

    private void removeProc(int action) {
        mMap.remove(action);
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
