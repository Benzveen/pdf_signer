package com.benzveen.pdfdigitalsignature.Document;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Build;
import android.os.SystemClock;

import androidx.core.view.MotionEventCompat;
import androidx.core.view.ViewCompat;

import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.OverScroller;
import android.widget.RelativeLayout;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.util.SizeF;

import com.benzveen.pdfdigitalsignature.DigitalSignatureActivity;
import com.benzveen.pdfdigitalsignature.PDF.PDSPDFPage;
import com.benzveen.pdfdigitalsignature.PDSModel.PDSElement;
import com.benzveen.pdfdigitalsignature.R;
import com.benzveen.pdfdigitalsignature.Signature.SignatureView;
import com.benzveen.pdfdigitalsignature.utils.PDSSignatureUtils;
import com.benzveen.pdfdigitalsignature.utils.ViewUtils;

import java.io.File;
import java.util.Observable;
import java.util.Observer;

public class PDSPageViewer extends FrameLayout implements Observer {
    private final ImageView mImageView;
    private final LayoutInflater mInflater;
    private final LinearLayout mProgressView;
    private final ScaleGestureListener mScaleGestureListener;
    private ScaleGestureDetector mScaleGestureDetector = null;
    private GestureDetector mGestureDetector = null;
    PDSRenderPageAsyncTask mInitialRenderingTask = null;
    private PDSRenderPageAsyncTask mRenderPageTask = null;
    private final Context mContext;
    private float mScaleFactor = 1.0f;
    private PointF mScroll = new PointF(0.0f, 0.0f);
    private PointF mFocus = new PointF(0.0f, 0.0f);
    private static final int DRAG_SHADOW_OPACITY = 180;
    private float mStartScaleFactor = 1.0f;
    private int mMaxScrollX = 0;
    private int mMaxScrollY = 0;
    private RelativeLayout mPageView = null;
    private RelativeLayout mScrollView = null;
    private OverScroller mScroller = null;
    private long mLastZoomTime = 0;
    private boolean mIsFirstScrollAfterIntercept = false;
    private boolean mIsInterceptedScrolling = false;
    private int mKeyboardHeight = 0;
    private boolean mKeyboardShown = false;
    private boolean mResizeInOperation = false;
    private boolean mRenderPageTaskPending = false;
    private float mBitmapScale = 1.0f;
    private PDSPDFPage mPage;
    SizeF mInitialImageSize = null;
    private Bitmap mImage = null;
    private RectF mImageContentRect = null;
    private Matrix mToPDFCoordinatesMatrix = null;
    private boolean mRenderingComplete = false;
    private Matrix mToViewCoordinatesMatrix = null;
    private float mInterceptedDownX = 0.0f;
    private float mInterceptedDownY = 0.0f;
    private float mTouchSlop = 0.0f;
    private float mLastDragPointX = -1.0f;
    private float mLastDragPointY = -1.0f;
    private View mElementPropMenu = null;
    private PDSElementViewer mLastFocusedElementViewer = null;
    private boolean mElementAlreadyPresentOnTap;
    private View mElementCreationMenu = null;
    private float mTouchX = 0.0f;
    private float mTouchY = 0.0f;
    private ImageView mDragShadowView = null;
    DigitalSignatureActivity activity = null;

    public PDSPageViewer(Context context, DigitalSignatureActivity activity, PDSPDFPage pdfPage) {
        super(context);
        this.mContext = context;
        this.activity = activity;
        this.mPage = pdfPage;
        this.mPage.setPageViewer(this);
        this.mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View inflate = this.mInflater.inflate(R.layout.pdfviewer, null, false);
        addView(inflate);
        this.mScrollView = inflate.findViewById(R.id.scrollview);
        this.mPageView = inflate.findViewById(R.id.pageview);
        this.mImageView = inflate.findViewById(R.id.imageview);
        setHorizontalScrollBarEnabled(true);
        setVerticalScrollBarEnabled(true);
        setScrollbarFadingEnabled(true);
        this.mProgressView = findViewById(R.id.linlaProgress);
        this.mProgressView.setVisibility(VISIBLE);
        this.mScroller = new OverScroller(getContext());
        this.mScaleGestureListener = new ScaleGestureListener(this);
        this.mScaleGestureDetector = new ScaleGestureDetector(this.mContext, this.mScaleGestureListener);
        this.mGestureDetector = new GestureDetector(this.mContext, new GestureListener(this));
        this.mGestureDetector.setIsLongpressEnabled(true);
        requestFocus();
    }

    @Override
    public void update(Observable o, Object arg) {

    }


    private void attachListeners() {
        setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionEvent) {
                boolean z = PDSPageViewer.this.mGestureDetector.onTouchEvent(motionEvent) || PDSPageViewer.this.mScaleGestureDetector.onTouchEvent(motionEvent);
                if (!(z || motionEvent.getAction() == 1)) {
                    motionEvent.getAction();
                }
                return z;
            }
        });
        this.mPageView.setOnDragListener(new OnDragListener() {
            public boolean onDrag(View view, DragEvent dragEvent) {
                switch (dragEvent.getAction()) {
                    case 1:
                        PDSPageViewer.this.mLastDragPointX = -1.0f;
                        PDSPageViewer.this.mLastDragPointY = -1.0f;
                        break;
                    case 2:
                        PDSPageViewer.this.handleDragMove(dragEvent);
                        break;
                    case 3:
                        PDSPageViewer.this.mLastDragPointX = dragEvent.getX();
                        PDSPageViewer.this.mLastDragPointY = dragEvent.getY();
                        break;
                    case 4:
                        PDSPageViewer.this.handleDragEnd(dragEvent);
                        break;
                    case 5:
                        PDSPageViewer.this.mLastDragPointX = -1.0f;
                        PDSPageViewer.this.mLastDragPointY = -1.0f;
                        break;
                }
                return true;
            }
        });
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private GestureListener() {
        }

        GestureListener(PDSPageViewer fASPageViewer) {
            this();
        }

        public boolean onDown(MotionEvent motionEvent) {
            PDSPageViewer.this.mScroller.forceFinished(true);
            ViewCompat.postInvalidateOnAnimation(PDSPageViewer.this);
            return true;
        }

        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
            if (motionEvent2.getPointerCount() > 1 || SystemClock.elapsedRealtime() - PDSPageViewer.this.mLastZoomTime < 200L) {
                return false;
            }
            PDSPageViewer.this.mScroller.abortAnimation();
            PDSPageViewer.this.mScroller.fling(PDSPageViewer.this.mScrollView.getScrollX(), PDSPageViewer.this.mScrollView.getScrollY(), ((int) (-f)) * 2, ((int) (-f2)) * 2, 0, PDSPageViewer.this.getMaxScrollX(), 0, PDSPageViewer.this.getMaxScrollY());
            ViewCompat.postInvalidateOnAnimation(PDSPageViewer.this);
            return true;
        }

        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
            if (PDSPageViewer.this.mIsInterceptedScrolling && PDSPageViewer.this.mIsFirstScrollAfterIntercept) {
                PDSPageViewer.this.mIsFirstScrollAfterIntercept = false;
                return false;
            } else if (motionEvent2.getPointerCount() > 1 || SystemClock.elapsedRealtime() - PDSPageViewer.this.mLastZoomTime < 200L) {
                return false;
            } else {
                PDSPageViewer.this.applyScroll(PDSPageViewer.this.mScrollView.getScrollX() + Math.round(f), PDSPageViewer.this.mScrollView.getScrollY() + Math.round(f2));
                return true;
            }
        }

        public void onLongPress(MotionEvent motionEvent) {
            super.onLongPress(motionEvent);
            PDSPageViewer.this.onTap(motionEvent, true);
        }

        public boolean onSingleTapUp(MotionEvent motionEvent) {
            super.onSingleTapUp(motionEvent);
            PDSPageViewer.this.mElementAlreadyPresentOnTap = false;
            PDSPageViewer.this.onTap(motionEvent, false);
            return true;
        }
    }

    private class ScaleGestureListener extends SimpleOnScaleGestureListener {
        private ScaleGestureListener() {
        }

        ScaleGestureListener(PDSPageViewer fASPageViewer) {
            this();
        }

        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
            return PDSPageViewer.this.scaleBegin(scaleGestureDetector.getFocusX(), scaleGestureDetector.getFocusY());
        }

        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            return PDSPageViewer.this.scale(scaleGestureDetector.getScaleFactor());
        }

        public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
            PDSPageViewer.this.scaleEnd();
        }

        public void resetScale() {
            PDSPageViewer.this.mFocus = new PointF(0.0f, 0.0f);
            PDSPageViewer.this.mScroll = new PointF(0.0f, 0.0f);
            PDSPageViewer.this.mStartScaleFactor = 1.0f;
            PDSPageViewer.this.mScaleFactor = 1.0f;
            PDSPageViewer.this.mMaxScrollX = 0;
            PDSPageViewer.this.mMaxScrollY = 0;
            PDSPageViewer.this.mPageView.setScaleX(PDSPageViewer.this.mScaleFactor);
            PDSPageViewer.this.mPageView.setScaleY(PDSPageViewer.this.mScaleFactor);
            PDSPageViewer.this.mScrollView.scrollTo(0, 0);
            PDSPageViewer.this.updateImageFoScale();
        }
    }

    public void resetScale() {
        this.mScaleGestureListener.resetScale();
    }

    private boolean scaleBegin(float f, float f2) {
        this.mPageView.setPivotX(0.0f);
        this.mPageView.setPivotY(0.0f);
        this.mFocus.set(f + ((float) this.mScrollView.getScrollX()), f2 + ((float) this.mScrollView.getScrollY()));
        this.mScroll.set((float) this.mScrollView.getScrollX(), (float) this.mScrollView.getScrollY());
        this.mStartScaleFactor = this.mScaleFactor;
        if (this.mLastFocusedElementViewer != null) {
            if (this.mElementPropMenu != null) {
                this.mElementPropMenu.setVisibility(INVISIBLE);
            } else if (this.mElementCreationMenu != null) {
                this.mElementCreationMenu.setVisibility(INVISIBLE);
            }
            this.mLastFocusedElementViewer.hideBorder();
        } else if (this.mElementCreationMenu != null) {
            this.mElementCreationMenu.setVisibility(INVISIBLE);
        }
        return true;
    }

    private boolean scale(float f) {
        this.mScaleFactor *= (f / 10000.0f) * 10000.0f;
        this.mScaleFactor = (this.mScaleFactor / 10000.0f) * 10000.0f;
        this.mScaleFactor = Math.max(1.0f, Math.min(this.mScaleFactor, 3.0f));
        this.mMaxScrollX = Math.round(((float) this.mScrollView.getWidth()) * (this.mScaleFactor - 1.0f));
        this.mMaxScrollY = Math.round(((float) this.mScrollView.getHeight()) * (this.mScaleFactor - 1.0f));
        int round = Math.round((this.mFocus.x * ((this.mScaleFactor / this.mStartScaleFactor) - 1.0f)) + this.mScroll.x);
        int round2 = Math.round((this.mFocus.y * ((this.mScaleFactor / this.mStartScaleFactor) - 1.0f)) + this.mScroll.y);
        round = Math.max(0, Math.min(round, getMaxScrollX()));
        round2 = Math.max(0, Math.min(round2, getMaxScrollY()));
        this.mPageView.setScaleX(this.mScaleFactor);
        this.mPageView.setScaleY(this.mScaleFactor);
        this.mScrollView.scrollTo(round, round2);
        invalidate();
        return true;
    }

    private void scaleEnd() {
        this.mLastZoomTime = SystemClock.elapsedRealtime();
        updateImageFoScale();
        if (this.mLastFocusedElementViewer == null) {
            return;
        }
        if (this.mElementPropMenu != null) {
            showElementPropMenu(this.mLastFocusedElementViewer);
        } else if (this.mElementCreationMenu != null) {
            //  showElementCreationMenu(this.mLastFocusedElementViewer);
        }
    }

    private void applyScroll(int i, int i2) {
        this.mScrollView.scrollTo(Math.max(0, Math.min(i, getMaxScrollX())), Math.max(0, Math.min(i2, getMaxScrollY())));
    }

    private int getMaxScrollY() {
        int i = this.mMaxScrollY;
        return this.mKeyboardShown ? i + this.mKeyboardHeight : i;
    }


    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        int actionMasked = MotionEventCompat.getActionMasked(motionEvent);
        if (!this.mResizeInOperation || this.mScaleFactor == 1.0f) {
            return false;
        }


        if (this.mLastFocusedElementViewer != null) {
            Rect rect = new Rect();
            this.mLastFocusedElementViewer.getContainerView().getHitRect(rect);
            if (rect.contains((int) ((motionEvent.getX() + ((float) this.mScrollView.getScrollX())) / this.mScaleFactor), (int) ((motionEvent.getY() + ((float) this.mScrollView.getScrollY())) / this.mScaleFactor))) {
                return false;
            }
        }
        boolean z = true;
        switch (actionMasked) {
            case 1:
            case 3:
                this.mIsInterceptedScrolling = false;
                break;
            case 2:
                if (!this.mIsInterceptedScrolling) {
                    float abs = Math.abs(motionEvent.getX() - this.mInterceptedDownX);
                    float abs2 = Math.abs(motionEvent.getY() - this.mInterceptedDownY);
                    if (abs > this.mTouchSlop || abs2 > this.mTouchSlop) {
                        this.mIsInterceptedScrolling = true;
                        this.mIsFirstScrollAfterIntercept = true;
                        break;
                    }
                }
                break;
            case 0:
                this.mIsInterceptedScrolling = false;
                this.mInterceptedDownX = motionEvent.getX();
                this.mInterceptedDownY = motionEvent.getY();
                this.mTouchSlop = (float) ViewConfiguration.get(getContext()).getScaledTouchSlop();
        }
        z = false;
        return z;
    }

    private void onTap(MotionEvent motionEvent, boolean z) {
        manualScale(motionEvent.getX(), motionEvent.getY());
        //getDocumentViewer().hideTrainingViewIfVisible();
        float x = (motionEvent.getX() + ((float) this.mScrollView.getScrollX())) / this.mScaleFactor;
        float y = (motionEvent.getY() + ((float) this.mScrollView.getScrollY())) / this.mScaleFactor;
        if (!getImageContentRect().contains(new RectF(x, y, (getResources().getDimension(R.dimen.element_min_width) + x) + (getResources().getDimension(R.dimen.element_horizontal_padding) * 2.0f), (getResources().getDimension(R.dimen.element_min_width) + y) + (getResources().getDimension(R.dimen.element_vertical_padding) * 2.0f)))) {
            if (PDSSignatureUtils.isSignatureMenuOpen()) {
                PDSSignatureUtils.dismissSignatureMenu();
            }
            removeFocus();
        } else {
            this.mTouchX = x;
            this.mTouchY = y;
            if (PDSSignatureUtils.isSignatureMenuOpen()) {
                PDSSignatureUtils.dismissSignatureMenu();
            } else if (z) {
                onLongTap(x, y);
            } else {
                onSingleTap(x, y);
            }
        }
    }

    private void onLongTap(float f, float f2) {
        if (this.mLastFocusedElementViewer != null) {

            removeFocus();
        }
    }

    private void onSingleTap(float f, float f2) {
        if (this.mLastFocusedElementViewer != null) {
            removeFocus();
            hideElementCreationMenu();
        }
    }

    public void manualScale(float f, float f2) {
        if (this.mScaleFactor == 1.0f && getDocumentViewer().isFirstTap()) {
            getDocumentViewer().setFirstTap(false);
            scaleBegin(f, f2);
            scale(1.15f);
            scale(1.3f);
            scale(1.45f);
            scaleEnd();
        }
    }

    public DigitalSignatureActivity getDocumentViewer() {
        return (DigitalSignatureActivity) this.mContext;
    }

    private int getMaxScrollX() {
        return this.mMaxScrollX;
    }

    private void setImageBitmap(Bitmap bitmap) {
        if (this.mImage != null) {
            this.mImage.recycle();
        }
        this.mImage = bitmap;
        this.mImageView.setImageBitmap(bitmap);
    }

    public RectF getImageContentRect() {
        return this.mImageContentRect;
    }

    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        if (z && this.mImageView.getWidth() > 0) {
            initRenderingAsync();
        }
    }

    public void onSizeChanged(int i, int i2, int i3, int i4) {
        if (this.mImageView.getWidth() > 0) {
            initRenderingAsync();
        }
       /* if (this.mImage != null && i3 == i) {
            i = i4 - i2;
            if (Math.abs(i) >= 200) {
                this.mKeyboardHeight = Math.abs(i) + ((int) getResources().getDimension(R.dimen.suggestion_bar_height));
                if (i2 < i4 && this.mLastFocusedElementViewer != null && !this.mKeyboardShown) {
                    this.mKeyboardShown = true;
                    float f = getVisibleRect().bottom - (((float) this.mKeyboardHeight) / this.mScaleFactor);
                    View elementView = this.mLastFocusedElementViewer.getElementView();
                    if (this.mLastFocusedElementViewer.getElementView() instanceof FASCombFieldView) {
                        elementView = this.mLastFocusedElementViewer.getContainerView();
                    }
                    float y = elementView.getY() + ((float) elementView.getHeight());
                    if (y > f) {
                        this.mPanY = Math.round(y - f);
                        applyScroll(this.mScrollView.getScrollX(), this.mScrollView.getScrollY() + Math.round(((float) this.mPanY) * this.mScaleFactor));
                    }
                    this.mSuggestionsBarView.setVisibility(0);
                    this.mSuggestionsBarView.invalidate();
                } else if (i2 > i4 && this.mKeyboardShown) {
                    this.mKeyboardShown = false;
                    applyScroll(this.mScrollView.getScrollX(), this.mScrollView.getScrollY() - Math.round(((float) this.mPanY) * this.mScaleFactor));
                    this.mPanY = 0;
                    this.mSuggestionsBarView.setVisibility(4);
                }
            }
        }*/
    }

    private void initRenderingAsync() {
        if (this.mImage == null && this.mImageView.getWidth() > 0 && (this.mInitialRenderingTask == null || this.mInitialRenderingTask.getStatus() == AsyncTask.Status.FINISHED)) {
            this.mInitialRenderingTask = new PDSRenderPageAsyncTask(this.mContext, this.mPage, new SizeF((float) this.mImageView.getWidth(), (float) this.mImageView.getHeight()), 1.0f, false, false, new PDSRenderPageAsyncTask.OnPostExecuteListener() {
                public void onPostExecute(PDSRenderPageAsyncTask fASRenderPageAsyncTask, Bitmap bitmap) {
                    if (bitmap != null && PDSPageViewer.this.mScaleFactor == 1.0f && fASRenderPageAsyncTask.getPage() == PDSPageViewer.this.mPage) {
                        int visibleWindowHeight = PDSPageViewer.this.getDocumentViewer().getVisibleWindowHeight();
                        if (visibleWindowHeight > 0) {
                            PDSPageViewer.this.mScrollView.setLayoutParams(new LayoutParams(-1, visibleWindowHeight));
                        }
                        PDSPageViewer.this.mInitialImageSize = fASRenderPageAsyncTask.getBitmapSize();
                        PDSPageViewer.this.computeImageContentRect();
                        PDSPageViewer.this.computeCoordinateConversionMatrices();
                       /* if (PDSPageViewer.this.mPage.getNumber() == 0) {
                            FASDocStore instance = FASDocStore.getInstance(FASPageViewer.this.mContext);
                            String uuid = FASPageViewer.this.getDocumentViewer().getDocument().getUuid();
                            if (!instance.thumbnailExists(uuid)) {
                                instance.updateThumbnail(uuid);
                            }
                        }*/
                        PDSPageViewer.this.setImageBitmap(bitmap);
                        PDSPageViewer.this.renderElements();
                        PDSPageViewer.this.mProgressView.setVisibility(INVISIBLE);
                        PDSPageViewer.this.attachListeners();
                    } else if (bitmap != null) {
                        bitmap.recycle();
                    }
                    PDSPageViewer.this.mRenderingComplete = true;
                }
            });
            this.mInitialRenderingTask.execute(new Void[0]);
        }
    }

    private void renderElements() {
        for (int i = 0; i < this.mPage.getNumElements(); i++) {
            addElement(this.mPage.getElement(i));
        }
    }

    private void computeImageContentRect() {
        float width;
        float f;
        float width2 = this.mInitialImageSize.getWidth() / this.mInitialImageSize.getHeight();
        float f2 = 0.0f;
        if (width2 >= ((float) this.mImageView.getWidth()) / ((float) this.mImageView.getHeight())) {
            width = (float) this.mImageView.getWidth();
            width2 = width / width2;
            f2 = (((float) this.mImageView.getHeight()) - width2) / 2.0f;
            f = 0.0f;
            float f3 = width;
            width = width2;
            width2 = f3;
        } else {
            width = (float) this.mImageView.getHeight();
            width2 *= width;
            f = (((float) this.mImageView.getWidth()) - width2) / 2.0f;
        }
        this.mImageContentRect = new RectF(f, f2, width2 + f, width + f2);
    }

    private void computeCoordinateConversionMatrices() {
        SizeF pageSize = this.mPage.getPageSize();
        RectF imageContentRect = getImageContentRect();
        this.mToPDFCoordinatesMatrix = new Matrix();
        this.mToPDFCoordinatesMatrix.postTranslate(0.0f - imageContentRect.left, 0.0f - imageContentRect.top);
        this.mToPDFCoordinatesMatrix.postScale(pageSize.getWidth() / imageContentRect.width(), pageSize.getHeight() / imageContentRect.height());
        this.mToViewCoordinatesMatrix = new Matrix();
        this.mToViewCoordinatesMatrix.postScale(imageContentRect.width() / pageSize.getWidth(), imageContentRect.height() / pageSize.getHeight());
        this.mToViewCoordinatesMatrix.postTranslate(imageContentRect.left, imageContentRect.top);
    }


    private synchronized void updateImageFoScale() {
        if (this.mScaleFactor != this.mBitmapScale) {
            if (this.mRenderPageTask != null) {
                this.mRenderPageTask.cancel(false);
                if (this.mRenderPageTask.getStatus() == AsyncTask.Status.RUNNING) {
                    this.mRenderPageTaskPending = true;
                    return;
                }
            }
            this.mRenderPageTask = new PDSRenderPageAsyncTask(this.mContext, this.mPage, new SizeF((float) this.mImageView.getWidth(), (float) this.mImageView.getHeight()), this.mScaleFactor, false, false, new PDSRenderPageAsyncTask.OnPostExecuteListener() {
                public void onPostExecute(PDSRenderPageAsyncTask fASRenderPageAsyncTask, Bitmap bitmap) {
                    if (bitmap != null && PDSPageViewer.this.mScaleFactor == fASRenderPageAsyncTask.getScale()) {
                        PDSPageViewer.this.setImageBitmap(bitmap);
                        PDSPageViewer.this.mBitmapScale = PDSPageViewer.this.mScaleFactor;
                    } else if (bitmap != null) {
                        bitmap.recycle();
                    }
                    if (PDSPageViewer.this.mRenderPageTaskPending) {
                        PDSPageViewer.this.mRenderPageTask = null;
                        PDSPageViewer.this.mRenderPageTaskPending = false;
                        PDSPageViewer.this.updateImageFoScale();
                    }
                }
            });
            this.mRenderPageTask.execute(new Void[0]);
        }
    }

    public RectF getVisibleRect() {
        return new RectF(((float) this.mScrollView.getScrollX()) / this.mScaleFactor, ((float) this.mScrollView.getScrollY()) / this.mScaleFactor, ((float) (this.mScrollView.getScrollX() + this.mPageView.getWidth())) / this.mScaleFactor, ((float) (this.mScrollView.getScrollY() + this.mPageView.getHeight())) / this.mScaleFactor);
    }

    public void cancelRendering() {
        if (mInitialRenderingTask != null) {
            mInitialRenderingTask.cancel(false);
        }
    }

    public void computeScroll() {
        if (!mScroller.isFinished()) {
            mScroller.computeScrollOffset();
            applyScroll(mScroller.getCurrX(), mScroller.getCurrY());
        }
    }

    public int computeHorizontalScrollRange() {
        return Math.round(((float) this.mScrollView.getWidth()) * this.mScaleFactor) - 1;
    }

    public int computeVerticalScrollRange() {
        return Math.round(((float) this.mScrollView.getHeight()) * this.mScaleFactor) - 1;
    }

    public int computeHorizontalScrollOffset() {
        return this.mScrollView.getScrollX();
    }

    public int computeVerticalScrollOffset() {
        return this.mScrollView.getScrollY();
    }

    public float getScaleFactor() {
        return this.mScaleFactor;
    }

    public RelativeLayout getPageView() {
        return this.mPageView;
    }

    public PDSPDFPage getPage() {
        return this.mPage;
    }

    public void hideElementPropMenu() {
        if (this.mElementPropMenu != null) {
            this.mScrollView.removeView(this.mElementPropMenu);
            this.mElementPropMenu = null;
        }
        if (this.mLastFocusedElementViewer != null) {
            this.mLastFocusedElementViewer.hideBorder();
            this.mLastFocusedElementViewer = null;
        }
    }

    public PDSElementViewer getLastFocusedElementViewer() {
        return this.mLastFocusedElementViewer;
    }

    public PDSElement createElement(PDSElement.PDSElementType fASElementType, File file, float f, float f2, float f3, float f4) {
        PDSElement fASElement = new PDSElement(fASElementType, file);
        //fASElement.setContent(fASElementContent);
        fASElement.setRect(mapRectToPDFCoordinates(new RectF(f, f2, f + f3, f2 + f4)));
        PDSElementViewer addElement = addElement(fASElement);
        if (fASElementType == PDSElement.PDSElementType.PDSElementTypeSignature) {
            addElement.getElementView().requestFocus();
        }
        return fASElement;
    }

    public PDSElement createElement(PDSElement.PDSElementType fASElementType, Bitmap bitmap, float f, float f2, float f3, float f4) {
        PDSElement fASElement = new PDSElement(fASElementType, bitmap);
        //fASElement.setContent(fASElementContent);
        fASElement.setRect(mapRectToPDFCoordinates(new RectF(f, f2, f + f3, f2 + f4)));
        PDSElementViewer addElement = addElement(fASElement);
        addElement.getElementView().requestFocus();
        return fASElement;
    }

    private PDSElementViewer addElement(PDSElement fASElement) {
        return new PDSElementViewer(this.mContext, this, fASElement);
    }

    public RectF mapRectToPDFCoordinates(RectF rectF) {
        this.mToPDFCoordinatesMatrix.mapRect(rectF);
        return rectF;
    }

    public float mapLengthToPDFCoordinates(float f) {
        return this.mToPDFCoordinatesMatrix.mapRadius(f);
    }

    public void setElementAlreadyPresentOnTap(boolean z) {
        this.mElementAlreadyPresentOnTap = z;
    }

    public void showElementPropMenu(final PDSElementViewer fASElementViewer) {
        hideElementPropMenu();
        hideElementCreationMenu();
        fASElementViewer.showBorder();
        this.mLastFocusedElementViewer = fASElementViewer;
        View inflate = this.mInflater.inflate(R.layout.element_prop_menu_layout, null, false);
        inflate.setTag(fASElementViewer);
        this.mScrollView.addView(inflate);
        this.mElementPropMenu = inflate;

        ((ImageButton) inflate.findViewById(R.id.delButton)).setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                fASElementViewer.removeElement();
                activity.invokeMenuButton(false);

            }
        });


        setMenuPosition(fASElementViewer.getContainerView(), inflate);

    }

    public void hideElementCreationMenu() {
        if (this.mElementCreationMenu != null) {
            this.mScrollView.removeView(this.mElementCreationMenu);
            this.mElementCreationMenu = null;
        }
    }


    private void setMenuPosition(float f, float f2, View view, boolean z) {
        view.measure(0, 0);
        float f3 = this.mScaleFactor * f;
        if (z) {
            f3 -= (float) (view.getMeasuredWidth() / 2);
        }
        float dimension = (this.mScaleFactor * f2) - ((float) ((int) getResources().getDimension(R.dimen.menu_offset_y)));
        RectF rectF = new RectF(f3, dimension, ((float) view.getMeasuredWidth()) + f3, ((float) view.getMeasuredHeight()) + dimension);
        RectF visibleRect = getVisibleRect();
        visibleRect.intersect(getImageContentRect());
        if (!visibleRect.contains(rectF)) {
            if (f3 > (visibleRect.right * this.mScaleFactor) - ((float) view.getMeasuredWidth())) {
                f3 = (visibleRect.right * this.mScaleFactor) - ((float) view.getMeasuredWidth());
            } else if (f3 < visibleRect.left * this.mScaleFactor && z) {
                f3 = visibleRect.left * this.mScaleFactor;
            }
            if (dimension < visibleRect.top * this.mScaleFactor) {
                if (z) {
                    dimension = (f2 * this.mScaleFactor) + ((float) ((int) getResources().getDimension(R.dimen.menu_offset_x)));
                } else if (f3 > ((visibleRect.left * this.mScaleFactor) + ((float) view.getMeasuredWidth())) + ((float) ((int) getResources().getDimension(R.dimen.menu_offset_x)))) {
                    f3 = ((f * this.mScaleFactor) - ((float) view.getMeasuredWidth())) - ((float) ((int) getResources().getDimension(R.dimen.menu_offset_x)));
                    dimension = f2 * this.mScaleFactor;
                }
            }
        }
        view.setX(f3);
        view.setY(dimension);
    }

    private void setMenuPosition(View view, View view2) {
        setMenuPosition(view.getX(), view.getY(), view2, false);
    }

    public float mapLengthToViewCoordinates(float f) {
        return this.mToViewCoordinatesMatrix.mapRadius(f);
    }


    public Matrix getToViewCoordinatesMatrix() {
        return this.mToViewCoordinatesMatrix;
    }

    public void modifyElementSignatureSize(PDSElement fASElement, View view, RelativeLayout relativeLayout, int i, int i2) {
        RelativeLayout relativeLayout2 = relativeLayout;
        int i3 = i;
        int i4 = i2;
        float f = (float) i4;
        if (getImageContentRect().contains(new RectF(relativeLayout.getX(), relativeLayout.getY() - f, (relativeLayout.getX() + ((float) relativeLayout.getWidth())) + ((float) i3), relativeLayout.getY() + ((float) relativeLayout.getHeight())))) {
            if (view instanceof SignatureView) {
                SignatureView signatureView = (SignatureView) view;
                signatureView.setLayoutParams(view.getWidth() + i3, view.getHeight() + i4);
                relativeLayout2.setLayoutParams(new RelativeLayout.LayoutParams(relativeLayout.getWidth() + i3, relativeLayout.getHeight() + i4));
                relativeLayout2.setY(relativeLayout.getY() - f);
                PDSElement fASElement2 = fASElement;
                this.mPage.updateElement(fASElement2, mapRectToPDFCoordinates(new RectF((float) ((int) relativeLayout.getX()), (float) ((int) relativeLayout.getY()), (float) ((int) (relativeLayout.getX() + ((float) view.getWidth()))), (float) ((int) (relativeLayout.getY() + ((float) view.getHeight()))))), 0.0f, 0.0f, signatureView.getStrokeWidth(), 0.0f);
                setMenuPosition(relativeLayout2, this.mElementPropMenu);
            } else if (view instanceof ImageView) {
                ImageView signatureView = (ImageView) view;
                signatureView.setLayoutParams(new RelativeLayout.LayoutParams(view.getWidth() + i3, view.getHeight() + i4));
                relativeLayout2.setLayoutParams(new RelativeLayout.LayoutParams(relativeLayout.getWidth() + i3, relativeLayout.getHeight() + i4));
                relativeLayout2.setY(relativeLayout.getY() - f);
                PDSElement fASElement2 = fASElement;
                this.mPage.updateElement(fASElement2, mapRectToPDFCoordinates(new RectF((float) ((int) relativeLayout.getX()), (float) ((int) relativeLayout.getY()), (float) ((int) (relativeLayout.getX() + ((float) view.getWidth()))), (float) ((int) (relativeLayout.getY() + ((float) view.getHeight()))))), 0.0f, 0.0f, 0.0f, 0.0f);
                setMenuPosition(relativeLayout2, this.mElementPropMenu);
            }
        }
    }


    public void setResizeInOperation(boolean z) {
        this.mResizeInOperation = z;
    }

    public boolean getResizeInOperation() {
        return this.mResizeInOperation;
    }

    public void removeFocus() {
        if (Build.VERSION.SDK_INT < 28) {
            clearFocus();
        }
        hideElementPropMenu();
        hideElementCreationMenu();
        if (Build.VERSION.SDK_INT >= 28) {
            this.mImageView.requestFocus();
        }
    }

    private void handleDragMove(DragEvent dragEvent) {
        PDSElementViewer.DragEventData dragEventData = (PDSElementViewer.DragEventData) dragEvent.getLocalState();
        PDSElementViewer fASElementViewer = dragEventData.viewer;
        View elementView = fASElementViewer.getElementView();
        this.mLastDragPointX = dragEvent.getX();
        this.mLastDragPointY = dragEvent.getY();
        if (this.mDragShadowView == null) {
            hideDragElement(fASElementViewer);
            fASElementViewer.getElement();
            initDragShadow(elementView);
        }

        RectF rectF = new RectF(this.mLastDragPointX - dragEventData.x, this.mLastDragPointY - dragEventData.y, (((float) elementView.getWidth()) + this.mLastDragPointX) - dragEventData.x, (((float) elementView.getHeight()) + this.mLastDragPointY) - dragEventData.y);
        ViewUtils.constrainRectXY(rectF, getImageContentRect());
        this.mLastDragPointX = rectF.left + dragEventData.x;
        this.mLastDragPointY = rectF.top + dragEventData.y;
        updateDragShadow(this.mLastDragPointX, this.mLastDragPointY, dragEventData.x, dragEventData.y);
    }

    private void updateDragShadow(float f, float f2, float f3, float f4) {
        if (this.mDragShadowView != null) {
            this.mDragShadowView.setX(f - f3);
            this.mDragShadowView.setY(f2 - f4);
        }
    }

    private void handleDragEnd(DragEvent dragEvent) {
        if (this.mLastDragPointX != -1.0f || this.mLastDragPointY != -1.0f) {
            float f;
            PDSElementViewer.DragEventData dragEventData = (PDSElementViewer.DragEventData) dragEvent.getLocalState();
            PDSElementViewer fASElementViewer = dragEventData.viewer;
            View elementView = fASElementViewer.getElementView();
            float f2 = this.mLastDragPointX - dragEventData.x;
            float f3 = this.mLastDragPointY - dragEventData.y;
            int width = elementView.getWidth();
            int height = elementView.getHeight();

            width = fASElementViewer.getContainerView().getWidth();
            height = fASElementViewer.getContainerView().getHeight();

            float f4 = (float) width;
            float f5 = (float) height;
            RectF rectF = new RectF(f2, f3, f2 + f4, f3 + f5);
            RectF imageContentRect = getImageContentRect();
            if (!imageContentRect.contains(rectF)) {
                if (f2 < imageContentRect.left) {
                    f2 = imageContentRect.left;
                } else if (f2 > imageContentRect.right - f4) {
                    f2 = imageContentRect.right - f4;
                }
                if (f3 < imageContentRect.top) {
                    f3 = imageContentRect.top;
                } else if (f3 > imageContentRect.bottom - f5) {
                    f3 = imageContentRect.bottom - f5;
                }
            }

            RelativeLayout containerView = fASElementViewer.getContainerView();
            containerView.setX(f2);
            containerView.setY(f3);
            containerView.setVisibility(VISIBLE);
            /* else {
                elementView.setX(f2);
                elementView.setY(f3);
            }*/
            elementView.setVisibility(VISIBLE);
            f = 0.0f;

            imageContentRect = new RectF((float) ((int) f2), (float) ((int) f3), (float) ((int) (f2 + ((float) elementView.getWidth()))), (float) ((int) (f3 + ((float) elementView.getHeight()))));
            mapRectToPDFCoordinates(imageContentRect);
            this.mPage.updateElement((PDSElement) elementView.getTag(), imageContentRect, 0.0f, f, 0.0f, 0.0f);
            showElementPropMenu(fASElementViewer);

            releaseDragShadow();
        }
    }

    private void hideDragElement(PDSElementViewer fASElementViewer) {
        removeFocus();
        fASElementViewer.showBorder();
        fASElementViewer.getContainerView().setVisibility(INVISIBLE);
        fASElementViewer.getElementView().setVisibility(INVISIBLE);
    }

    private void initDragShadow(View view) {
        if (this.mDragShadowView == null) {
            Bitmap createBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
            view.draw(new Canvas(createBitmap));
            this.mDragShadowView = new ImageView(this.mContext);
            this.mDragShadowView.setImageBitmap(createBitmap);
            this.mDragShadowView.setImageAlpha(DRAG_SHADOW_OPACITY);
            this.mDragShadowView.setLayoutParams(new RelativeLayout.LayoutParams(view.getWidth(), view.getHeight()));
            this.mPageView.addView(this.mDragShadowView);
        }
    }

    private void releaseDragShadow() {
        if (this.mDragShadowView != null) {
            this.mDragShadowView.setVisibility(INVISIBLE);
            this.mPageView.removeView(this.mDragShadowView);
            this.mDragShadowView = null;
        }
    }
}
