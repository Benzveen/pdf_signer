package com.benzveen.pdfdigitalsignature.PDF;

import android.content.Context;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class PDSPDFDocument {
    private int mNumPages;
    private static final transient Object sLockObject = new Object();
    private HashMap<Integer, PDSPDFPage> mPages;
    private transient PdfRenderer mRenderer;
    Uri pdfDocument = null;
    public InputStream stream;
    Context context = null;

    public PDSPDFDocument(Context context, Uri document) throws FileNotFoundException {
        this.mPages = null;
        this.mNumPages = -1;
        this.mRenderer = null;
        this.mPages = new HashMap();
        this.pdfDocument = document;
        this.context = context;
        stream = context.getContentResolver().openInputStream(document);

    }

    public void open() throws IOException {
        ParcelFileDescriptor open = context.getContentResolver().openFileDescriptor(this.pdfDocument, "r");
        if (isValidPDF(open)) {
            synchronized (sLockObject) {
                try {
                    this.mRenderer = new PdfRenderer(open);
                    this.mNumPages = this.mRenderer.getPageCount();
                } catch (Exception unused) {
                    if (open != null) {
                        open.close();
                    }
                    throw new IOException();
                } catch (Throwable th) {
                }
            }
            return;
        }
        open.close();
        throw new IOException();
    }

    public void close() {
        if (this.mRenderer != null) {
            this.mRenderer.close();
            this.mRenderer = null;
        }
    }

    public PDSPDFPage getPage(int i) {
        if (i >= this.mNumPages || i < 0) {
            return null;
        }
        PDSPDFPage fASPDFPage = (PDSPDFPage) this.mPages.get(Integer.valueOf(i));
        if (fASPDFPage != null) {
            return fASPDFPage;
        }
        PDSPDFPage fASPDFPage2 = new PDSPDFPage(i, this);
        this.mPages.put(Integer.valueOf(i), fASPDFPage2);
        return fASPDFPage2;
    }

    public static Object getLockObject() {
        return sLockObject;
    }

    public PdfRenderer getRenderer() {
        return this.mRenderer;
    }

    public int getNumPages() {
        return this.mNumPages;
    }

    private boolean isValidPDF(ParcelFileDescriptor parcelFileDescriptor) {
        try {
            byte[] bArr = new byte[4];
            if (new FileInputStream(parcelFileDescriptor.getFileDescriptor()).read(bArr) == 4 && bArr[0] == (byte) 37 && bArr[1] == (byte) 80 && bArr[2] == (byte) 68 && bArr[3] == (byte) 70) {
                return true;
            }
            return false;
        } catch (IOException unused) {
            return false;
        }
    }

    public Uri getDocumentUri() {
        return this.pdfDocument;
    }
}
