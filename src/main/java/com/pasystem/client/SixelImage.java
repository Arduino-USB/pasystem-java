package com.pasystem.client;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.sshtools.jsixel.awt.AWTImageBitmap;
import com.sshtools.jsixel.lib.bitmap.Bitmap2Sixel;
import com.sshtools.jsixel.lib.bitmap.Sixel2Bitmap;

public class SixelImage {

    private BufferedImage originalImage;    // Unscaled master copy
    private BufferedImage internalImage;    // Currently active (potentially scaled) image
    private String cachedSixelString;

    public static class Size {
        public int width;
        public int height;

        public Size(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

    /**
     * Constructor from an existing Sixel file path.
     */
    public SixelImage(Path sixelPath) {
        try {
            this.cachedSixelString = Files.readString(sixelPath);
            this.originalImage = null;
            this.internalImage = null;
        } catch (IOException e) {
            throw new RuntimeException(
                "Failed to read sixel file from path: " + sixelPath,
                e
            );
        }
    }

    /**
     * Internal constructor from a BufferedImage.
     */
    private SixelImage(BufferedImage image) {
        this.originalImage = image;
        this.internalImage = image;
        this.cachedSixelString = null;
    }

    /**
     * Constructor generating an image with text, a background color, and explicit dimensions.
     */
    public SixelImage(
        String text,
        String bgHexCode,
        String textHexCode,
        Path fontPath,
        int padding,
        int targetWidth,
        int targetHeight
    ) {
        BufferedImage img = renderTextToImage(
            text,
            parseHexColor(bgHexCode),
            parseHexColor(textHexCode),
            fontPath,
            padding,
            null,
            targetWidth,
            targetHeight
        );
        this.originalImage = img;
        this.internalImage = img;
        this.cachedSixelString = null;
    }

    /**
     * Constructor generating an image with text and a background color (auto-sized).
     */
    public SixelImage(
        String text,
        String bgHexCode,
        String textHexCode,
        Path fontPath,
        int padding
    ) {
        this(text, bgHexCode, textHexCode, fontPath, padding, 0, 0);
    }

    /**
     * Constructor generating text over another SixelImage.
     */
    public SixelImage(
        String text,
        SixelImage imageBG,
        String textHexCode,
        Path fontPath,
        int padding
    ) {
        BufferedImage bgBuffer = null;

        if (imageBG != null) {
            bgBuffer = imageBG.getBufferedImage();
        }

        Color textColor = parseHexColor(textHexCode);

        BufferedImage img = renderTextToImage(
            text,
            null,
            textColor,
            fontPath,
            padding,
            bgBuffer,
            0,
            0
        );

        this.originalImage = img;
        this.internalImage = img;
        this.cachedSixelString = null;
    }

    /**
     * Returns the Sixel string.
     */
    public String getSixelString() {
        if (this.cachedSixelString == null) {
            ensureInternalImage();

            this.cachedSixelString = encodeImageToSixel(
                this.internalImage
            );
        }

        return this.cachedSixelString;
    }

    /**
     * Returns the current image dimensions.
     */
    public Size getSize() {
        ensureInternalImage();

        return new Size(
            this.internalImage.getWidth(),
            this.internalImage.getHeight()
        );
    }

    /**
     * Returns the underlying BufferedImage (current state).
     */
    private BufferedImage getBufferedImage() {
        ensureInternalImage();
        return this.internalImage;
    }

    /**
     * Ensures that internalImage and originalImage exist.
     */
    private void ensureInternalImage() {
        if (this.internalImage != null && this.originalImage != null) {
            return;
        }

        if (this.cachedSixelString == null) {
            throw new IllegalStateException(
                "SixelImage contains neither an image nor Sixel data."
            );
        }

        BufferedImage decoded = decodeSixelToImage(this.cachedSixelString);
        this.originalImage = decoded;
        this.internalImage = decoded;
    }

    /**
     * Resizes the image consistently from the untouched master copy.
     */
    public void setSize(int width, int height) {

		System.out.println("[SixelImage] Debug: setSize received width: " + width + ", height: " + height);
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                "Width and height must be greater than zero."
            );
        }

        ensureInternalImage();

        BufferedImage resized = new BufferedImage(
            width,
            height,
            BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2d = resized.createGraphics();

        try {
            g2d.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR
            );

            g2d.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY
            );

            g2d.drawImage(
                this.originalImage,
                0,
                0,
                width,
                height,
                null
            );
        } finally {
            g2d.dispose();
        }

        this.internalImage = resized;
        this.cachedSixelString = null;
    }

    /**
     * Permanently bakes another image onto the unscaled master copy.
     */
    public void addImageOverlay(
        SixelImage image,
        int xPercent,
        int yPercent
    ) {
        if (image == null) {
            return;
        }

        ensureInternalImage();
        image.ensureInternalImage();

        int targetWidth = this.originalImage.getWidth();
        int targetHeight = this.originalImage.getHeight();

        int overlayWidth = image.originalImage.getWidth();
        int overlayHeight = image.originalImage.getHeight();

        int xCoord = (int) (
            targetWidth * (xPercent / 100.0)
        );

        int yCoord = (int) (
            targetHeight * (yPercent / 100.0)
        );

        BufferedImage combined = new BufferedImage(
            targetWidth,
            targetHeight,
            BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2d = combined.createGraphics();

        try {
            g2d.drawImage(
                this.originalImage,
                0,
                0,
                null
            );

            g2d.drawImage(
                image.originalImage,
                xCoord,
                yCoord,
                overlayWidth,
                overlayHeight,
                null
            );
        } finally {
            g2d.dispose();
        }

        this.originalImage = combined;
        this.internalImage = combined;
        this.cachedSixelString = null;
    }

    // ============================================================
    // Rendering & Font Helpers
    // ============================================================

    private Color parseHexColor(String hexCode) {
        if (hexCode == null) {
            return Color.BLACK;
        }

        String cleanHex = hexCode.startsWith("#")
            ? hexCode.substring(1)
            : hexCode;

        return new Color(
            Integer.parseInt(cleanHex, 16)
        );
    }

    private Font loadFont(Path fontPath) {
        try {
            if (fontPath != null && Files.exists(fontPath)) {
                try (InputStream is = Files.newInputStream(fontPath)) {
                    return Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(16f);
                }
            }
        } catch (Exception e) {
            // Fallback handled below
        }
        return new Font("SansSerif", Font.PLAIN, 16);
    }

    private Font deriveBestFitFont(Font baseFont, String text, int maxWidth, int maxHeight) {
        float low = 1f;
        float high = 300f;
        float optimalSize = 16f;

        BufferedImage dummy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = dummy.createGraphics();

        while (high - low > 0.5f) {
            float mid = (low + high) / 2f;
            Font testFont = baseFont.deriveFont(mid);
            g2d.setFont(testFont);
            FontMetrics fm = g2d.getFontMetrics();

            if (fm.stringWidth(text) <= maxWidth && fm.getHeight() <= maxHeight) {
                optimalSize = mid;
                low = mid;
            } else {
                high = mid;
            }
        }
        g2d.dispose();
        return baseFont.deriveFont(optimalSize);
    }

    private BufferedImage renderTextToImage(
        String text,
        Color bgColor,
        Color textColor,
        Path fontPath,
        int padding,
        BufferedImage bgImage,
        int targetWidth,
        int targetHeight
    ) {
        Font font = loadFont(fontPath);

        if (targetWidth > 0 && targetHeight > 0 && text != null && !text.isEmpty()) {
            font = deriveBestFitFont(font, text, targetWidth - (padding * 2), targetHeight - (padding * 2));
        }

        BufferedImage tempImage = new BufferedImage(
            1,
            1,
            BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2dTemp = tempImage.createGraphics();
        g2dTemp.setFont(font);
        FontMetrics metrics = g2dTemp.getFontMetrics();

        int textWidth = metrics.stringWidth(text);
        int textHeight = metrics.getHeight();

        g2dTemp.dispose();

        int totalWidth = (targetWidth > 0) ? targetWidth : textWidth + (padding * 2);
        int totalHeight = (targetHeight > 0) ? targetHeight : textHeight + (padding * 2);

        BufferedImage result;

        if (bgImage != null) {
            result = new BufferedImage(
                Math.max(totalWidth, bgImage.getWidth()),
                Math.max(totalHeight, bgImage.getHeight()),
                BufferedImage.TYPE_INT_ARGB
            );

            Graphics2D g2d = result.createGraphics();

            try {
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                g2d.drawImage(
                    bgImage,
                    0,
                    0,
                    null
                );

                g2d.setFont(font);
                g2d.setColor(textColor);

                int x = padding;
                int y = padding + metrics.getAscent();
                if (targetWidth > 0) {
                    x = Math.max(padding, (targetWidth - textWidth) / 2);
                }
                if (targetHeight > 0) {
                    y = (targetHeight - metrics.getHeight()) / 2 + metrics.getAscent();
                }

                g2d.drawString(text, x, y);
            } finally {
                g2d.dispose();
            }
        } else {
            result = new BufferedImage(
                totalWidth,
                totalHeight,
                BufferedImage.TYPE_INT_ARGB
            );

            Graphics2D g2d = result.createGraphics();

            try {
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                g2d.setColor(
                    bgColor != null
                        ? bgColor
                        : Color.WHITE
                );

                g2d.fillRect(
                    0,
                    0,
                    totalWidth,
                    totalHeight
                );

                g2d.setFont(font);
                g2d.setColor(textColor);

                int x = padding;
                int y = padding + metrics.getAscent();
                if (targetWidth > 0) {
                    x = Math.max(padding, (targetWidth - textWidth) / 2);
                }
                if (targetHeight > 0) {
                    y = (targetHeight - metrics.getHeight()) / 2 + metrics.getAscent();
                }

                g2d.drawString(text, x, y);
            } finally {
                g2d.dispose();
            }
        }

        return result;
    }

    // ============================================================
    // JSixel 0.3.0
    // ============================================================

    private String encodeImageToSixel(BufferedImage image) {
        int maxRetries = 5;
        long retryDelayMs = 500;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                AWTImageBitmap bitmap =
                    new AWTImageBitmap
                        .BufferedImageBitmapBuilder()
                        .fromImage(image)
                        .build();

                Bitmap2Sixel sixel =
                    new Bitmap2Sixel
                        .Bitmap2SixelBuilder()
                        .fromBitmap(bitmap)
                        .build();

                StringBuilder output = new StringBuilder();
                sixel.write(output);

                if (output.isEmpty()) {
                    throw new IOException("JSixel produced no output.");
                }

                return output.toString();

            } catch (Exception e) {
                if (attempt == maxRetries) {
                    throw new RuntimeException(
                        "Failed to encode image as Sixel after " + maxRetries + " attempts.",
                        e
                    );
                }
                
                System.err.println("[SixelImage] Warning: Sixel encoding failed (attempt " + attempt + "/" + maxRetries + "). Retrying in " + retryDelayMs + "ms...");
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Sixel encoding retry interrupted", ie);
                }
            }
        }
        
        throw new RuntimeException("Failed to encode image as Sixel: Max retries exceeded.");
    }

    private BufferedImage decodeSixelToImage(String sixelContent) {
        try {
            Sixel2Bitmap decoded =
                new Sixel2Bitmap
                    .Sixel2BitmapBuilder()
                    .fromString(sixelContent)
                    .build();

            AWTImageBitmap bitmap =
                new AWTImageBitmap
                    .BufferedImageBitmapBuilder()
                    .fromBitmap(decoded.bitmap())
                    .build();

            java.lang.reflect.Method imageMethod =
                AWTImageBitmap.class.getDeclaredMethod("image");

            imageMethod.setAccessible(true);

            java.awt.image.RenderedImage renderedImage =
                (java.awt.image.RenderedImage) imageMethod.invoke(bitmap);

            if (!(renderedImage instanceof BufferedImage)) {
                throw new IOException(
                    "JSixel returned a RenderedImage that is not a BufferedImage: "
                        + renderedImage.getClass().getName()
                );
            }

            return (BufferedImage) renderedImage;

        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to decode Sixel.",
                e
            );
        }
    }
        
    public BufferedImage getInternalImage() {
        ensureInternalImage();
        return internalImage;
    }
}
