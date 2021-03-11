package com.benzveen.pdfdigitalsignature.Document;

import android.content.ClipData;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;

import androidx.core.view.ViewCompat;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.benzveen.pdfdigitalsignature.PDSModel.PDSElement;
import com.benzveen.pdfdigitalsignature.R;
import com.benzveen.pdfdigitalsignature.Signature.SignatureView;
import com.benzveen.pdfdigitalsignature.utils.ViewUtils;

public class PDSElementViewer {
    private static int MOTION_THRESHOLD = 3;
    private static int MOTION_THRESHOLD_LONG_PRESS = 12;
    private boolean mBorderShown = false;
    private RelativeLayout mContainerView = null;
    private Context mContext = null;
    private PDSElement mElement = null;
    private View mElementView = null;
    private boolean mHasDragStarted = false;
    private ImageButton mImageButton = null;
    private float mLastMotionX = 0.0f;
    private float mLastMotionY = 0.0f;
    private boolean mLongPress = false;
    public PDSPageViewer mPageViewer = null;
    private float mResizeInitialPos = 0.0f;

    class CustomDragShadowBuilder extends View.DragShadowBuilder {
        int mX;
        int mY;

        public void onDrawShadow(Canvas canvas) {
        }

        public CustomDragShadowBuilder(View view, int i, int i2) {
            super(view);
            this.mX = i;
            this.mY = i2;
        }

        public void onProvideShadowMetrics(Point point, Point point2) {
            super.onProvideShadowMetrics(point, point2);
            point2.set((int) (((float) this.mX) * PDSElementViewer.this.mPageViewer.getScaleFactor()), (int) (((float) this.mY) * PDSElementViewer.this.mPageViewer.getScaleFactor()));
            point.set((int) (((float) getView().getWidth()) * PDSElementViewer.this.mPageViewer.getScaleFactor()), (int) (((float) getView().getHeight()) * PDSElementViewer.this.mPageViewer.getScaleFactor()));
        }
    }

    class DragEventData {
        public PDSElementViewer viewer;
        public float x;
        public float y;

        public DragEventData(PDSElementViewer fASElementViewer, float f, float f2) {
            this.viewer = fASElementViewer;
            this.x = f;
            this.y = f2;
        }
    }

    public PDSElementViewer(Context context, PDSPageViewer fASPageViewer, PDSElement fASElement) {
        this.mContext = context;
        this.mPageViewer = fASPageViewer;
        this.mElement = fASElement;
        fASElement.mElementViewer = this;
        createElement(fASElement);
    }

    public PDSPageViewer getPageViewer() {
        return this.mPageViewer;
    }

    public PDSElement getElement() {
        return this.mElement;
    }

    public View getElementView() {
        return this.mElementView;
    }

    public RelativeLayout getContainerView() {
        return this.mContainerView;
    }

    public ImageButton getImageButton() {
        return this.mImageButton;
    }

    private void createElement(PDSElement fASElement) {
        this.mElementView = createElementView(fASElement);
        this.mPageViewer.getPageView().addView(this.mElementView);
        this.mElementView.setTag(fASElement);
        if (!isElementInModel()) {
            addElementInModel(fASElement);
        }
        setListeners();
    }

    public void removeElement() {
        if (this.mElementView.getParent() != null) {
            this.mPageViewer.getPage().removeElement((PDSElement) this.mElementView.getTag());
            this.mPageViewer.hideElementPropMenu();
            this.mPageViewer.getPageView().removeView(this.mElementView);
        }
    }

    private View createElementView(PDSElement fASElement) {
        switch (fASElement.getType()) {
            case PDSElementTypeSignature:
                SignatureView createSignatureView = ViewUtils.createSignatureView(this.mContext, fASElement, this.mPageViewer.getToViewCoordinatesMatrix());
                fASElement.setRect(new RectF(fASElement.getRect().left, fASElement.getRect().top, fASElement.getRect().left + this.mPageViewer.mapLengthToPDFCoordinates((float) createSignatureView.getSignatureViewWidth()), fASElement.getRect().bottom));
                fASElement.setStrokeWidth(this.mPageViewer.mapLengthToPDFCoordinates(createSignatureView.getStrokeWidth()));
                createSignatureView.setFocusable(true);
                createSignatureView.setFocusableInTouchMode(true);
                createSignatureView.setClickable(true);
                createSignatureView.setLongClickable(true);
                createResizeButton(createSignatureView);
                return createSignatureView;
            case PDSElementTypeImage:
                ImageView imageView = ViewUtils.createImageView(this.mContext, fASElement, this.mPageViewer.getToViewCoordinatesMatrix());
                imageView.setImageBitmap(fASElement.getBitmap());
                fASElement.setRect(new RectF(fASElement.getRect().left, fASElement.getRect().top, fASElement.getRect().left + this.mPageViewer.mapLengthToPDFCoordinates((float) imageView.getWidth()), fASElement.getRect().bottom));
                imageView.setFocusable(true);
                imageView.setFocusableInTouchMode(true);
                imageView.setClickable(true);
                imageView.setLongClickable(true);
                imageView.invalidate();
                createResizeButton(imageView);
                return imageView;
            default:
                return null;
        }
    }


    private void addElementInModel(PDSElement fASElement) {
        this.mPageViewer.getPage().addElement(fASElement);
    }

    private boolean isElementInModel() {
        for (int i = 0; i < this.mPageViewer.getPage().getNumElements(); i++) {
            if (this.mPageViewer.getPage().getElement(i) == this.mElementView.getTag()) {
                return true;
            }
        }
        return false;
    }


    public void setListeners() {
        setTouchListener();
        setFocusListener();
    }

    private void setTouchListener() {
        this.mElementView.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                view.requestFocus();
                PDSElementViewer.this.mLongPress = true;
                return true;
            }
        });
        this.mElementView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                switch (action & 255) {
                    case 0:
                        PDSElementViewer.this.mHasDragStarted = false;
                        PDSElementViewer.this.mLongPress = false;
                        PDSElementViewer.this.mLastMotionX = motionEvent.getX();
                        PDSElementViewer.this.mLastMotionY = motionEvent.getY();
                        break;
                    case 1:
                        PDSElementViewer.this.mHasDragStarted = false;
                        PDSElementViewer.this.mPageViewer.setElementAlreadyPresentOnTap(true);
                        if (!(view instanceof SignatureView)) {
                            view.setVisibility(View.VISIBLE);
                            break;
                        }
                        PDSElementViewer.this.mContainerView.setVisibility(View.VISIBLE);
                        break;
                    case 2:
                        if (!PDSElementViewer.this.mHasDragStarted) {
                            action = Math.abs((int) (motionEvent.getX() - PDSElementViewer.this.mLastMotionX));
                            int abs = Math.abs((int) (motionEvent.getY() - PDSElementViewer.this.mLastMotionY));
                            int access$700;
                            if (PDSElementViewer.this.mLongPress) {
                                access$700 = PDSElementViewer.MOTION_THRESHOLD_LONG_PRESS;
                            } else {
                                access$700 = PDSElementViewer.MOTION_THRESHOLD;
                            }
                            if (motionEvent.getX() >= 0.0f && motionEvent.getY() >= 0.0f && PDSElementViewer.this.mBorderShown && (action > access$700 || abs > access$700)) {
                                float x = motionEvent.getX();
                                float y = motionEvent.getY();
                                view.startDrag(ClipData.newPlainText("pos", String.format("%d %d", new Object[]{Integer.valueOf(Math.round(x)), Integer.valueOf(Math.round(y))})), new CustomDragShadowBuilder(view, Math.round(x), Math.round(y)), new DragEventData(PDSElementViewer.this, x, y), 0);
                                PDSElementViewer.this.mHasDragStarted = true;
                            }
                            return true;
                        }
                        break;
                }
                return false;
            }
        });
    }

    private void setFocusListener() {
        this.mElementView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View view, boolean z) {

                if (z) {
                    PDSElementViewer.this.assignFocus();
                }
            }
        });
    }

    public void assignFocus() {
        this.mPageViewer.showElementPropMenu(this);
    }


    public void relayoutContainer() {
        this.mElementView.measure(Math.round(-2.0f), Math.round(-2.0f));
        this.mElementView.layout(0, 0, this.mElementView.getMeasuredWidth(), this.mElementView.getMeasuredHeight());
        this.mImageButton.measure(Math.round(-2.0f), Math.round(-2.0f));
        this.mImageButton.layout(0, 0, this.mImageButton.getMeasuredWidth(), this.mImageButton.getMeasuredHeight());
        int measuredWidth = this.mElementView.getMeasuredWidth() + (this.mImageButton.getMeasuredHeight() / 2);
        int measuredHeight = this.mElementView.getMeasuredHeight();
        this.mImageButton.setVisibility(View.VISIBLE);

        this.mContainerView.setLayoutParams(new ViewGroup.LayoutParams(measuredWidth, measuredHeight));
    }

    public void setResizeButtonImage() {

        this.mImageButton.setImageResource(R.drawable.ic_resize);
    }

    private void createResizeButton(View view) {
        this.mImageButton = new ImageButton(this.mContext);
        this.mImageButton.setImageResource(R.drawable.ic_resize);

        this.mImageButton.setBackgroundColor(0);
        this.mImageButton.setPadding(0, 0, 0, 0);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(-2, -2);
        layoutParams.addRule(11);
        layoutParams.addRule(15);
        this.mImageButton.setLayoutParams(layoutParams);
        this.mImageButton.measure(Math.round(-2.0f), Math.round(-2.0f));
        this.mImageButton.layout(0, 0, this.mImageButton.getMeasuredWidth(), this.mImageButton.getMeasuredHeight());
        this.mContainerView = new RelativeLayout(this.mContext);
        this.mContainerView.setFocusable(false);
        this.mContainerView.setFocusableInTouchMode(false);
        this.mImageButton.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionEvent) {

                switch (motionEvent.getAction()) {
                    case 0:
                        PDSElementViewer.this.mResizeInitialPos = motionEvent.getRawX();
                        PDSElementViewer.this.mPageViewer.setResizeInOperation(true);
                        break;
                    case 1:
                    case 3:
                        PDSElementViewer.this.mPageViewer.setResizeInOperation(false);
                        break;
                    case 2:
                        if (PDSElementViewer.this.mPageViewer.getResizeInOperation()) {
                            float rawX = (motionEvent.getRawX() - PDSElementViewer.this.mResizeInitialPos) / 2.0f;
                            if ((rawX <= (-PDSElementViewer.this.mContext.getResources().getDimension(R.dimen.sign_field_step_size)) || rawX >= PDSElementViewer.this.mContext.getResources().getDimension(R.dimen.sign_field_step_size)) && ((float) PDSElementViewer.this.mElementView.getHeight()) + rawX >= PDSElementViewer.this.mContext.getResources().getDimension(R.dimen.sign_field_min_height) && ((float) PDSElementViewer.this.mElementView.getHeight()) + rawX <= PDSElementViewer.this.mContext.getResources().getDimension(R.dimen.sign_field_max_height)) {
                                PDSElementViewer.this.mResizeInitialPos = motionEvent.getRawX();
                                PDSElementViewer.this.mPageViewer.modifyElementSignatureSize((PDSElement) PDSElementViewer.this.mElementView.getTag(), PDSElementViewer.this.mElementView, PDSElementViewer.this.mContainerView, (int) ((((float) PDSElementViewer.this.mElementView.getWidth()) * rawX) / ((float) PDSElementViewer.this.mElementView.getHeight())), (int) rawX);
                                break;
                            }
                        }
                        break;
                }
                return true;
            }
        });
    }

    public void showBorder() {
        changeColor(true);
        if (this.mContainerView.getParent() == null) {
            int signatureViewWidth;
            int signatureViewHeight;
            if (this.mElementView.getParent() == this.mPageViewer.getPageView()) {
                this.mElementView.setOnFocusChangeListener(null);
                this.mPageViewer.getPageView().removeView(this.mElementView);
                this.mContainerView.addView(this.mElementView);
            }
            this.mContainerView.addView(this.mImageButton);
            this.mContainerView.setX(this.mElementView.getX());
            this.mContainerView.setY(this.mElementView.getY());
            this.mElementView.setX(0.0f);
            this.mElementView.setY(0.0f);
            if (this.mElementView instanceof SignatureView) {
                signatureViewWidth = ((SignatureView) this.mElementView).getSignatureViewWidth() + (this.mImageButton.getMeasuredWidth() / 2);
                signatureViewHeight = ((SignatureView) this.mElementView).getSignatureViewHeight();
            } else {
                // this.mElementView.measure(Math.round(-2.0f), Math.round(-2.0f));
                // this.mElementView.layout(0, 0, this.mElementView.getMeasuredWidth(), this.mElementView.getMeasuredHeight());
                signatureViewWidth = this.mElementView.getLayoutParams().width + (this.mImageButton.getMeasuredWidth() / 2);
                signatureViewHeight = this.mElementView.getLayoutParams().height;
            }
            this.mContainerView.setLayoutParams(new RelativeLayout.LayoutParams(signatureViewWidth, signatureViewHeight));
            this.mPageViewer.getPageView().addView(this.mContainerView);
        }
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setStroke(2, this.mContext.getResources().getColor(R.color.colorAccent));
        this.mElementView.setBackground(gradientDrawable);
        this.mBorderShown = true;
    }

    public void hideBorder() {
        changeColor(false);
        if (this.mContainerView.getParent() == this.mPageViewer.getPageView()) {
            this.mElementView.setX(this.mContainerView.getX());
            this.mElementView.setY(this.mContainerView.getY());
            this.mPageViewer.getPageView().removeView(this.mContainerView);
            this.mContainerView.removeView(this.mImageButton);
            if (this.mElementView.getParent() == this.mContainerView) {
                this.mContainerView.removeView(this.mElementView);
                this.mPageViewer.getPageView().addView(this.mElementView);
                setFocusListener();
            }
        }
        this.mElementView.setBackground(null);
        this.mBorderShown = false;
    }

    public void changeColor(boolean z) {
        int color = z ? this.mContext.getResources().getColor(R.color.colorAccent) : ViewCompat.MEASURED_STATE_MASK;
        if (this.mElementView instanceof SignatureView) {
            color = ((SignatureView) this.mElementView).getActualColor();
            ((SignatureView) this.mElementView).setStrokeColor(color);
        } else if (this.mElementView instanceof ImageView) {
            //((ImageView) this.mElementView).setColorFilter(color);
        }
    }

    public boolean isBorderShown() {
        return this.mBorderShown;
    }
}
