package com.benzveen.pdfdigitalsignature.Document;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import com.benzveen.pdfdigitalsignature.DigitalSignatureActivity;
import com.benzveen.pdfdigitalsignature.PDF.PDSPDFDocument;
import com.benzveen.pdfdigitalsignature.PDF.PDSPDFPage;
import com.benzveen.pdfdigitalsignature.PDSModel.PDSElement;
import com.benzveen.pdfdigitalsignature.utils.ViewUtils;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfSignatureAppearance;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.security.BouncyCastleDigest;
import com.itextpdf.text.pdf.security.DigestAlgorithms;
import com.itextpdf.text.pdf.security.ExternalDigest;
import com.itextpdf.text.pdf.security.ExternalSignature;
import com.itextpdf.text.pdf.security.MakeSignature;
import com.itextpdf.text.pdf.security.PrivateKeySignature;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;

public class PDSSaveAsPDFAsyncTask extends AsyncTask<Void, Void, Boolean> {

    private String mfileName;
    DigitalSignatureActivity mCtx;


    public PDSSaveAsPDFAsyncTask(DigitalSignatureActivity context, String str) {
        this.mCtx = context;
        this.mfileName = str;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mCtx.savingProgress.setVisibility(View.VISIBLE);
    }

    @Override
    public Boolean doInBackground(Void... voidArr) {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        PDSPDFDocument document = mCtx.getDocument();
        File root = mCtx.getFilesDir();

        File myDir = new File(root + "/DigitalSignature");
        if (!myDir.exists()) {
            myDir.mkdirs();
        }
        File file = new File(myDir.getAbsolutePath(), mfileName);
        if (file.exists())
            file.delete();
        try {
            InputStream stream = document.stream;
            FileOutputStream os = new FileOutputStream(file);
            PdfReader reader = new PdfReader(stream);
            PdfStamper signer = null;
            Bitmap createBitmap = null;
            for (int i = 0; i < document.getNumPages(); i++) {
                Rectangle mediabox = reader.getPageSize(i + 1);
                for (int j = 0; j < document.getPage(i).getNumElements(); j++) {
                    PDSPDFPage page = document.getPage(i);
                    PDSElement element = page.getElement(j);
                    RectF bounds = element.getRect();
                    if (element.getType() == PDSElement.PDSElementType.PDSElementTypeSignature) {
                        PDSElementViewer viewer = element.mElementViewer;
                        View dummy = viewer.getElementView();
                        View view = ViewUtils.createSignatureView(mCtx, element, viewer.mPageViewer.getToViewCoordinatesMatrix());
                        createBitmap = Bitmap.createBitmap(dummy.getWidth(), dummy.getHeight(), Bitmap.Config.ARGB_8888);
                        view.draw(new Canvas(createBitmap));
                    } else {
                        createBitmap = element.getBitmap();
                    }
                    ByteArrayOutputStream saveBitmap = new ByteArrayOutputStream();
                    createBitmap.compress(Bitmap.CompressFormat.PNG, 100, saveBitmap);
                    byte[] byteArray = saveBitmap.toByteArray();
                    createBitmap.recycle();

                    Image sigimage = Image.getInstance(byteArray);
                    if (mCtx.alises != null && mCtx.keyStore != null && mCtx.mdigitalIDPassword != null) {
                        KeyStore ks = mCtx.keyStore;
                        String alias = mCtx.alises;
                        PrivateKey pk = (PrivateKey) ks.getKey(alias, mCtx.mdigitalIDPassword.toCharArray());
                        Certificate[] chain = ks.getCertificateChain(alias);
                        if (signer == null)
                            signer = PdfStamper.createSignature(reader, os, '\0');

                        PdfSignatureAppearance appearance = signer.getSignatureAppearance();

                        float top = mediabox.getHeight() - (bounds.top + bounds.height());
                        appearance.setVisibleSignature(new Rectangle(bounds.left, top, bounds.left + bounds.width(), top + bounds.height()), i + 1, "sig" + j);
                        appearance.setRenderingMode(PdfSignatureAppearance.RenderingMode.GRAPHIC);
                        appearance.setSignatureGraphic(sigimage);
                        ExternalDigest digest = new BouncyCastleDigest();
                        ExternalSignature signature = new PrivateKeySignature(pk, DigestAlgorithms.SHA256, null);
                        MakeSignature.signDetached(appearance, digest, signature, chain, null, null, null, 0, MakeSignature.CryptoStandard.CADES);


                    } else {
                        if (signer == null)
                            signer = new PdfStamper(reader, os, '\0');
                        PdfContentByte contentByte = signer.getOverContent(i + 1);
                        sigimage.setAlignment(Image.ALIGN_UNDEFINED);
                        sigimage.scaleToFit(bounds.width(), bounds.height());
                        sigimage.setAbsolutePosition(bounds.left - (sigimage.getScaledWidth() - bounds.width()) / 2, mediabox.getHeight() - (bounds.top + bounds.height()));
                        contentByte.addImage(sigimage);
                    }
                }
            }
            if (signer != null)
                signer.close();
            if (reader != null)
                reader.close();
            if (os != null)
                os.close();
        } catch (Exception e) {
            e.printStackTrace();
            if (file.exists()) {
                file.delete();
            }
            return false;
        }
        return true;
    }

    @Override
    public void onPostExecute(Boolean result) {
        mCtx.runPostExecution();
        if (!result)
            Toast.makeText(mCtx, "Something went wrong while Signing PDF document, Please try again", Toast.LENGTH_LONG).show();
        else
            Toast.makeText(mCtx, "PDF document saved successfully", Toast.LENGTH_LONG).show();

    }
}

