package com.passwordmanager.util;

import java.awt.image.BufferedImage;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public final class QrCodeUtil {

    private QrCodeUtil() {}

    public static BufferedImage generateQr(String data, int size) throws WriterException {
        QRCodeWriter qrWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrWriter.encode(data, BarcodeFormat.QR_CODE, size, size);

        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                int color = bitMatrix.get(x, y) ? 0x000000 : 0xFFFFFF;
                image.setRGB(x, y, color);
            }
        }
        return image;
    }
}
