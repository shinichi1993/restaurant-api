package com.restaurant.api.util;

import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.Image;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;


/**
 * QrCodeUtil
 * =========================================================
 * Utility tạo QR Code dùng chung trong hệ thống
 *
 * Dùng cho:
 *  - Order Slip
 *  - Invoice (sau này)
 *  - Export PDF / in bill
 *
 * Lưu ý:
 *  - Trả về Image (OpenPDF)
 *  - KHÔNG chứa logic nghiệp vụ
 */
public class QrCodeUtil {

    /**
     * Tạo QR code dạng PNG byte[]
     * @param text nội dung encode (orderCode)
     */
    public static byte[] generateQrPng(String text, int size) {
        try {
            BitMatrix matrix = new MultiFormatWriter()
                    .encode(text, BarcodeFormat.QR_CODE, size, size);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo QR Code", e);
        }
    }

    /**
     * Tạo QR Code Image từ text
     *
     * @param text nội dung QR (vd: orderCode)
     * @param size kích thước QR (px)
     * @return Image (OpenPDF)
     */
    public static Image generateQrImage(String text, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            var bitMatrix = writer.encode(
                    text,
                    BarcodeFormat.QR_CODE,
                    size,
                    size
            );

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();

            BufferedImage image = new BufferedImage(
                    width,
                    height,
                    BufferedImage.TYPE_INT_RGB
            );

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    image.setRGB(
                            x,
                            y,
                            bitMatrix.get(x, y)
                                    ? 0xFF000000
                                    : 0xFFFFFFFF
                    );
                }
            }

            return Image.getInstance(image, null);

        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo QR Code", e);
        }
    }
}
