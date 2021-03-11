package com.benzveen.pdfdigitalsignature.Document;

import android.util.SizeF;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.os.AsyncTask;
import android.util.SizeF;

import com.benzveen.pdfdigitalsignature.PDF.PDSPDFPage;

public class PDSRenderPageAsyncTask extends AsyncTask<Void, Void, Bitmap>  {
    private static final int MAX_BITMAP_SIZE = 3072;
    private SizeF mBitmapSize = null;
    private Context mContext = null;
    private boolean mForPrint = false;
    private SizeF mImageViewSize = null;
    private boolean mIncludePageElements = false;
    private OnPostExecuteListener mListener = null;
    private final PDSPDFPage mPage;
    private float mScale = 1.0f;

    public interface OnPostExecuteListener {
        void onPostExecute(PDSRenderPageAsyncTask fASRenderPageAsyncTask, Bitmap bitmap);
    }

    PDSRenderPageAsyncTask(Context context, PDSPDFPage fASPage, SizeF sizeF, float f, boolean z, boolean z2, OnPostExecuteListener onPostExecuteListener) {
        this.mContext = context;
        this.mPage = fASPage;
        this.mImageViewSize = sizeF;
        this.mScale = f;
        this.mIncludePageElements = z;
        this.mForPrint = z2;
        this.mListener = onPostExecuteListener;
    }

    /* Access modifiers changed, original: protected|varargs */
    public Bitmap doInBackground(Void... voidArr) {
        if (isCancelled() || this.mImageViewSize.getWidth() <= 0.0f) {
            return null;
        }
        this.mBitmapSize = computePageBitmapSize();
        if (isCancelled()) {
            return null;
        }
        float width = this.mBitmapSize.getWidth() * this.mScale;
        float height = this.mBitmapSize.getHeight() * this.mScale;
        float f = width / height;
        if (width > 3072.0f && width > height) {
            height = 3072.0f / f;
            width = 3072.0f;
        } else if (height > 3072.0f && height > width) {
            width = f * 3072.0f;
            height = 3072.0f;
        }
        try {
            Bitmap createBitmap = Bitmap.createBitmap(Math.round(width), Math.round(height), Config.ARGB_8888);
            if (isCancelled()) {
                return null;
            }
            createBitmap.setHasAlpha(false);
            createBitmap.eraseColor(-1);
            if (isCancelled()) {
                createBitmap.recycle();
                return null;
            }
            this.mPage.renderPage(this.mContext, createBitmap, this.mIncludePageElements, this.mForPrint);
            if (!isCancelled()) {
                return createBitmap;
            }
            createBitmap.recycle();
            return null;
        } catch (OutOfMemoryError unused) {
            return null;
        }
    }

    /* Access modifiers changed, original: protected */
    public void onPostExecute(Bitmap bitmap) {
        if (this.mListener != null) {
            this.mListener.onPostExecute(this, bitmap);
        }
    }

    /* Access modifiers changed, original: protected */
    public void onCancelled(Bitmap bitmap) {
        if (this.mListener != null) {
            this.mListener.onPostExecute(this, null);
        }
    }

    public SizeF getBitmapSize() {
        return this.mBitmapSize;
    }

    public float getScale() {
        return this.mScale;
    }

    public PDSPDFPage getPage() {
        return this.mPage;
    }

    private SizeF computePageBitmapSize() {
        float width;
        SizeF pageSize = this.mPage.getPageSize();
        float width2 = pageSize.getWidth() / pageSize.getHeight();
        if (width2 > this.mImageViewSize.getWidth() / this.mImageViewSize.getHeight()) {
            width = this.mImageViewSize.getWidth();
            if (width > 3072.0f) {
                width = 3072.0f;
            }
            width2 = (float) Math.round(width / width2);
        } else {
            width = this.mImageViewSize.getHeight();
            if (width > 3072.0f) {
                width = 3072.0f;
            }
            float f = width2 * width;
            width2 = width;
            width = f;
        }
        return new SizeF(width, width2);
    }
}
