package com.benzveen.pdfdigitalsignature.Signature;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;


import com.benzveen.pdfdigitalsignature.R;

public class SignatureLayout extends RelativeLayout {

    private static final int REQUEST_LAYOUT_MSG = 1;
    private int mHeight;
    Handler mRequestLayoutHandler = new Handler() {
        public void handleMessage(Message message) {
            if (message.what == 1) {
                SignatureLayout.this.resizeDrawingView();
            }
            super.handleMessage(message);
        }
    };
    private int mWidth;

    public SignatureLayout(Context context) {
        super(context);
    }

    public SignatureLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public SignatureLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    /* Access modifiers changed, original: protected */
    public void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        this.mWidth = i - (((int) getResources().getDimension(R.dimen.signature_panel_horizontal_margin)) * 2);
        this.mHeight = this.mWidth / 2;
        //if (!SignatureUtils.isTablet((Activity) getContext())) {
            i2 -= (int) (getResources().getDimension(R.dimen.signature_view_header) * 2.0f);
            if (this.mHeight > i2) {
                this.mHeight = i2;
                this.mWidth = this.mHeight * 2;
            }
       // }
        this.mRequestLayoutHandler.removeMessages(1);
        Message obtain = Message.obtain();
        obtain.what = 1;
        this.mRequestLayoutHandler.sendMessage(obtain);
    }

    private void resizeDrawingView() {
       /* ViewGroup.LayoutParams layoutParams =findViewById(R.id.drawingView).getLayoutParams();
        layoutParams.height = this.mHeight;
        layoutParams.width = this.mWidth;
        findViewById(R.id.signature_top_bar).getLayoutParams().height = (int) getResources().getDimension(R.dimen.signature_view_header);
        findViewById(R.id.signature_bottom_bar).getLayoutParams().height = (int) getResources().getDimension(R.dimen.signature_view_header);
        findViewById(R.id.signature_panel_layout).requestLayout();*/
    }
}
