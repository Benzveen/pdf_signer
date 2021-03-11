package com.benzveen.pdfdigitalsignature.Document;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;

import com.benzveen.pdfdigitalsignature.DigitalSignatureActivity;

import fr.castorflex.android.verticalviewpager.VerticalViewPager;

public class PDSViewPager extends VerticalViewPager {
    private Context mActivityContext = null;
    private boolean mDownReceieved = true;

    public PDSViewPager(Context context) {
        super(context);
        this.mActivityContext = context;
        init();
    }

    public PDSViewPager(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mActivityContext = context;
        init();
    }


    private void init() {
        setOnPageChangeListener(new OnPageChangeListener() {
            public void onPageScrollStateChanged(int i) {
            }

            public void onPageScrolled(int i, float f, int i2) {
            }

            public void onPageSelected(int i) {
                View focusedChild = PDSViewPager.this.getFocusedChild();
                if (focusedChild != null) {
                    PDSPageViewer pDSPageViewer = (PDSPageViewer) ((ViewGroup) focusedChild).getChildAt(0);
                    if (pDSPageViewer != null) {
                        pDSPageViewer.resetScale();
                    }
                }
                if (PDSViewPager.this.mActivityContext != null) {
                    ((DigitalSignatureActivity) PDSViewPager.this.mActivityContext).updatePageNumber(i + 1);
                }
            }
        });
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {
            this.mDownReceieved = true;
        }
        if (motionEvent.getPointerCount() <= 1 && this.mDownReceieved) {
            return super.onInterceptTouchEvent(motionEvent);
        }
        this.mDownReceieved = false;
        return false;

    }
}
