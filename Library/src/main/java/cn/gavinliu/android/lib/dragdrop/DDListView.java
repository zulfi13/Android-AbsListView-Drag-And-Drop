package cn.gavinliu.android.lib.dragdrop;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

/**
 * Created by gavin on 15-5-13.
 */
public class DDListView extends ListView implements
        DragDropController.DragDropListener,
        AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, AbsListView.OnScrollListener {

    private DragDropController mDDController;

    protected OnDragDropListener onDragDropListener;

    private ActionMode mActionMode;

    private SelectionMode mSelectionMode;

    public enum SelectionMode {
        Custom, Official
    }

    public DDListView(Context context) {
        super(context);
        init();
    }

    public DDListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DDListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mDDController = new DragDropController(getContext());
        mDDController.setDragDropListener(this);
        setOnItemLongClickListener(this);
        setOnScrollListener(this);
    }

    public interface OnDragDropListener {

        void onDragStart();

        void onDragEnter();

        void onDragExit();

        void onDragEnd();

        /**
         * @param id The id is ListView item id
         */
        void onSelect(long id);

        /**
         * @param id Menu View id
         */
        void onDrop(int id);
    }

    public void addMenu(View v, MenuType menuType) {
        MenuZone menuZone = new MenuZone(v, menuType);
        mDDController.addMenuZone(menuZone);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        mDDController.startDrag(view);

        if (onDragDropListener != null) {
            onDragDropListener.onSelect(id);
        }

        return true;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

    }

    @Override
    public void setMultiChoiceModeListener(MultiChoiceModeListener listener) {
        super.setMultiChoiceModeListener(new MultiChoiceModeWrapper(listener));
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {

        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        mDDController.onInterceptTouchEvent(ev);

        return super.onInterceptTouchEvent(ev);
    }

    CheckLongClick mCheckLongClick;

    private static final int TOUCH_SLOP = 20;

    private int mLastMotionX, mLastMotionY;

    // 是否移动了
    private boolean isMoved;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        mDDController.onTouchEvent(ev);

        int x = (int) ev.getX();
        int y = (int) ev.getY();

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:

                final int longPressTimeout = ViewConfiguration.getLongPressTimeout();

                if (mCheckLongClick == null) {
                    mCheckLongClick = new CheckLongClick();
                }

                mLastMotionX = x;
                mLastMotionY = y;

                if (mActionMode != null)
                    postDelayed(mCheckLongClick, longPressTimeout);

                break;

            case MotionEvent.ACTION_MOVE:
                if (Math.abs(mLastMotionX - x) > TOUCH_SLOP || Math.abs(mLastMotionY - y) > TOUCH_SLOP) {
                    removeCallbacks(mCheckLongClick);
                }

                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                removeCallbacks(mCheckLongClick);
                break;
        }

        return super.onTouchEvent(ev);
    }

    @Override
    public void onDragStart() {

    }

    @Override
    public void onDragEnter() {

    }

    @Override
    public void onDragExit() {

    }

    @Override
    public void onDragEnd() {

    }

    @Override
    public void onDrop(int id) {
        if (onDragDropListener != null) {
            onDragDropListener.onDrop(id);
        }

        clearChoices();
        exitMultiChoiceMode();
    }

    public void exitMultiChoiceMode() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    private class MultiChoiceModeWrapper implements MultiChoiceModeListener {
        MultiChoiceModeListener mWrapped;
        boolean isDraggable;

        public MultiChoiceModeWrapper(MultiChoiceModeListener listener) {
            this.mWrapped = listener;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            if (mWrapped.onCreateActionMode(mode, menu)) {
                isDraggable = true;
                return true;
            }
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return mWrapped.onActionItemClicked(mode, item);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            mActionMode = mode;
            setLongClickable(true);
            return mWrapped.onPrepareActionMode(mode, menu);
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            isDraggable = true;
            mActionMode = null;
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            if (isDraggable) {
                int item = position - getFirstVisiblePosition();
                View v = getChildAt(item);
                mDDController.startDrag(v);
                isDraggable = false;
            }
        }
    }

    public void setOnDragDropListener(OnDragDropListener onDragDropListener) {
        this.onDragDropListener = onDragDropListener;
    }

    public void setSelectionMode(SelectionMode selectionMode) {
        this.mSelectionMode = selectionMode;
    }

    private class CheckLongClick implements Runnable {

        @Override
        public void run() {
            Toast.makeText(getContext(), "CheckLongClick", Toast.LENGTH_LONG).show();

            int position = pointToPosition(mLastMotionX, mLastMotionY);
            long id = getAdapter().getItemId(position);
            int itemNum = position - getFirstVisiblePosition();
            View selectedView = getChildAt(itemNum);

            setItemChecked(position, true);

            onItemLongClick(DDListView.this, selectedView, position, id);
        }
    }
}
