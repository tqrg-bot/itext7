/*

    This file is part of the iText (R) project.
    Copyright (c) 1998-2019 iText Group NV
    Authors: Bruno Lowagie, Paulo Soares, et al.

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation with the addition of the
    following permission added to Section 15 as permitted in Section 7(a):
    FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED BY
    ITEXT GROUP. ITEXT GROUP DISCLAIMS THE WARRANTY OF NON INFRINGEMENT
    OF THIRD PARTY RIGHTS

    This program is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
    or FITNESS FOR A PARTICULAR PURPOSE.
    See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License
    along with this program; if not, see http://www.gnu.org/licenses or write to
    the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
    Boston, MA, 02110-1301 USA, or download the license from the following URL:
    http://itextpdf.com/terms-of-use/

    The interactive user interfaces in modified source and object code versions
    of this program must display Appropriate Legal Notices, as required under
    Section 5 of the GNU Affero General Public License.

    In accordance with Section 7(b) of the GNU Affero General Public License,
    a covered work must retain the producer line in every PDF that is created
    or manipulated using iText.

    You can be released from the requirements of the license by purchasing
    a commercial license. Buying such a license is mandatory as soon as you
    develop commercial activities involving the iText software without
    disclosing the source code of your own applications.
    These activities include: offering paid services to customers as an ASP,
    serving PDFs on the fly in a web application, shipping iText with a closed
    source product.

    For more information, please contact iText Software Corp. at this
    address: sales@itextpdf.com
 */
package com.itextpdf.kernel.pdf.canvas.wmf;

import com.itextpdf.io.image.ImageType;
import com.itextpdf.kernel.PdfException;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.kernel.pdf.xobject.PdfXObject;

import java.io.IOException;
import java.io.InputStream;

/**
 * Helper class for the WmfImage implementation. Assists in the creation of a {@link com.itextpdf.kernel.pdf.xobject.PdfFormXObject}.
 */
public class WmfImageHelper {

    /** Scales the WMF font size. The default value is 0.86. */
    public static float wmfFontCorrection = 0.86f;


    private WmfImageData wmf;

    private float plainWidth;
    private float plainHeight;

    /**
     * Creates a helper instance.
     *
     * @param wmf the {@link WmfImageData} object
     */
    public WmfImageHelper(ImageData wmf) {
        if (wmf.getOriginalType() != ImageType.WMF)
            throw new IllegalArgumentException("WMF image expected");
        this.wmf = (WmfImageData)wmf;
        processParameters();
    }

    /**
     * This method checks if the image is a valid WMF and processes some parameters.
     */
    private void processParameters() {
        InputStream is = null;
        try {
            String errorID;
            if (wmf.getData() == null){
                is = wmf.getUrl().openStream();
                errorID = wmf.getUrl().toString();
            }
            else{
                is = new java.io.ByteArrayInputStream(wmf.getData());
                errorID = "Byte array";
            }
            InputMeta in = new InputMeta(is);
            if (in.readInt() != 0x9AC6CDD7)	{
                throw new PdfException(PdfException._1IsNotAValidPlaceableWindowsMetafile, errorID);
            }
            in.readWord();
            int left = in.readShort();
            int top = in.readShort();
            int right = in.readShort();
            int bottom = in.readShort();
            int inch = in.readWord();
            wmf.setDpi(72, 72);
            wmf.setHeight((float) (bottom - top) / inch * 72f);
            wmf.setWidth((float) (right - left) / inch * 72f);
        } catch (IOException e) {
            throw new PdfException(PdfException.WmfImageException);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) { }
            }
        }
    }

    /**
     * Create a PdfXObject based on the WMF image. The PdfXObject will have the dimensions of the
     * WMF image.
     *
     * @param document PdfDocument to add the PdfXObject to
     * @return PdfXObject based on the WMF image
     */
    public PdfXObject createFormXObject(PdfDocument document) {
        PdfFormXObject pdfForm = new PdfFormXObject(new Rectangle(0, 0, wmf.getWidth(), wmf.getHeight()));
        PdfCanvas canvas = new PdfCanvas(pdfForm, document);

        InputStream is = null;
        try {
            if (wmf.getData() == null){
                is = wmf.getUrl().openStream();
            }
            else{
                is = new java.io.ByteArrayInputStream(wmf.getData());
            }
            MetaDo meta = new MetaDo(is, canvas);
            meta.readAll();
        } catch (IOException e) {
            throw new PdfException(PdfException.WmfImageException, e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) { }
            }
        }
        return pdfForm;
    }
}
