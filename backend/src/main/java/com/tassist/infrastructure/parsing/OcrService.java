package com.tassist.infrastructure.parsing;

import net.sourceforge.tess4j.Tesseract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Optional OCR over rendered PDF pages via Tesseract (tess4j). Reads text that lives inside
 * images (screenshots of tables/code), which the PDF text layer can't provide.
 * <p>Degrades gracefully: if OCR is disabled or the native engine/tessdata is missing, methods
 * return empty and the caller keeps the normal text-layer extraction. OCR failures never break
 * an upload.
 */
@Component
public class OcrService {
    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    private final boolean enabled;
    private final String tessdataPath;
    private final String language;
    private volatile Boolean available; // lazily probed once

    private final String nativeLibPath;

    public OcrService(
            @Value("${tassist.ocr.enabled:true}") boolean enabled,
            @Value("${tassist.ocr.tessdata-path:/opt/homebrew/share/tessdata}") String tessdataPath,
            @Value("${tassist.ocr.language:eng}") String language,
            @Value("${tassist.ocr.native-lib-path:/opt/homebrew/lib}") String nativeLibPath) {
        this.enabled = enabled;
        this.tessdataPath = tessdataPath;
        this.language = language;
        this.nativeLibPath = nativeLibPath;
    }

    public boolean isEnabled() { return enabled && probe(); }

    /** True only if OCR is on AND tessdata is present. Probed once, cached. */
    private boolean probe() {
        if (!enabled) return false;
        if (available != null) return available;
        synchronized (this) {
            if (available != null) return available;
            // Point JNA at the native tesseract/leptonica libs (Homebrew: /opt/homebrew/lib).
            if (nativeLibPath != null && !nativeLibPath.isBlank()) {
                String existing = System.getProperty("jna.library.path", "");
                if (!existing.contains(nativeLibPath)) {
                    System.setProperty("jna.library.path",
                        existing.isBlank() ? nativeLibPath : existing + File.pathSeparator + nativeLibPath);
                }
            }
            File data = new File(tessdataPath, language + ".traineddata");
            boolean ok = data.isFile();
            if (!ok) log.warn("OCR enabled but tessdata not found at {} — OCR will be skipped. "
                + "Install tesseract + set tassist.ocr.tessdata-path.", data.getAbsolutePath());
            available = ok;
            return ok;
        }
    }

    /** OCR a rendered page image. Returns "" on any failure (never throws). */
    public String ocr(BufferedImage pageImage) {
        if (!isEnabled() || pageImage == null) return "";
        try {
            Tesseract t = new Tesseract();
            t.setDatapath(tessdataPath);
            t.setLanguage(language);
            t.setPageSegMode(3);   // fully automatic page segmentation
            t.setOcrEngineMode(1); // LSTM engine
            String out = t.doOCR(pageImage);
            return out == null ? "" : out.strip();
        } catch (Throwable e) { // Throwable: native lib issues can be Errors, not Exceptions
            log.warn("OCR failed on a page: {}", e.getMessage());
            return "";
        }
    }
}
