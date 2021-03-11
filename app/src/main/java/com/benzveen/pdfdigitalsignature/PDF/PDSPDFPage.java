package com.benzveen.pdfdigitalsignature.PDF;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.graphics.pdf.PdfRenderer;
import android.util.SizeF;

import com.benzveen.pdfdigitalsignature.Document.PDSPageViewer;
import com.benzveen.pdfdigitalsignature.PDSModel.PDSElement;

import java.util.ArrayList;

public class PDSPDFPage {
    private static final transient SizeF DEF_PAGE_SIZE = new SizeF(595.0f, 842.0f);
    private static final transient Object sSynchronizedObject = new Object();
    private ArrayList<PDSElement> mElements;
    private SizeF mPageSize = null;
    private PDSPDFDocument mPDFDocument;
    private int mpageNumber;
    private PDSPageViewer mviewer;

    public PDSPDFPage(int i, PDSPDFDocument fASPDFDocument) {
        this.mpageNumber = i;
        this.mPDFDocument = fASPDFDocument;
        this.mElements = new ArrayList();

    }

    public SizeF getPageSize() {
        if (this.mPageSize == null) {
            synchronized (PDSPDFDocument.getLockObject()) {
                synchronized (getDocument()) {
                    PdfRenderer.Page openPage = ((PDSPDFDocument) getDocument()).getRenderer().openPage(getNumber());
                    this.mPageSize = new SizeF((float) openPage.getWidth(), (float) openPage.getHeight());
                    openPage.close();
                }
            }
        }
        return this.mPageSize;
    }


    public void renderPage(Context context, Bitmap bitmap, boolean z, boolean z2) {
        int i = z2 ? 2 : 1;
        synchronized (PDSPDFDocument.getLockObject()) {
            synchronized (getDocument()) {

                PdfRenderer.Page openPage = ((PDSPDFDocument) getDocument()).getRenderer().openPage(getNumber());
                this.mPageSize = new SizeF((float) openPage.getWidth(), (float) openPage.getHeight());
                openPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                openPage.close();
            }
        }
    }

    public void setPageViewer(PDSPageViewer mviewer) {
        this.mviewer = mviewer;
    }

    public PDSPageViewer getPageViewer() {
        return mviewer;
    }

    public PDSPDFDocument getDocument() {
        return this.mPDFDocument;
    }

    public int getNumber() {
        return this.mpageNumber;
    }

    public void removeElement(PDSElement fASElement) {
        this.mElements.remove(fASElement);
    }

    public void addElement(PDSElement fASElement) {
        this.mElements.add(fASElement);
    }


    public int getNumElements() {
        return this.mElements.size();
    }

    public PDSElement getElement(int i) {
        return (PDSElement) this.mElements.get(i);
    }

    public ArrayList<PDSElement> getElements() {
        return this.mElements;
    }

    public void updateElement(PDSElement fASElement, RectF rectF, float f, float f2, float f3, float f4) {
        Object obj;
        if (rectF == null || rectF.equals(fASElement.getRect())) {
            obj = null;
        } else {
            fASElement.setRect(rectF);
            obj = 1;
        }
        if (!(f == 0.0f || f == fASElement.getSize())) {
            fASElement.setSize(f);
            obj = 1;
        }
        if (!(f2 == 0.0f || f2 == fASElement.getMaxWidth())) {
            fASElement.setMaxWidth(f2);
            obj = 1;
        }
        if (!(f3 == 0.0f || f3 == fASElement.getStrokeWidth())) {
            fASElement.setStrokeWidth(f3);
            obj = 1;
        }
        if (!(f4 == 0.0f || f4 == fASElement.getLetterSpace())) {
            fASElement.setLetterSpace(f4);
            obj = 1;
        }
        if (obj != null) {
            //this.mPDFDocument.setChanged();
        }
    }
}
