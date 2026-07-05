package com.resumescreening.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Extracts plain text from uploaded resume files so it can be fed into the LLM prompt.
 * Supports PDF, DOCX and plain TXT resumes.
 */
@Service
public class ResumeTextExtractorService {

    public String extractText(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();

        if (fileName.endsWith(".pdf")) {
            return extractFromPdf(file.getInputStream());
        } else if (fileName.endsWith(".docx")) {
            return extractFromDocx(file.getInputStream());
        } else {
            // Treat as plain text (.txt or unknown -> best effort)
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        }
    }

    private String extractFromPdf(InputStream inputStream) throws IOException {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractFromDocx(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    /** Best-effort guess of candidate name from file name, used as a fallback if the LLM doesn't extract one. */
    public String guessNameFromFileName(String fileName) {
        if (fileName == null) return "Unknown Candidate";
        String base = fileName.replaceAll("(?i)\\.(pdf|docx|txt)$", "");
        base = base.replaceAll("[_\\-]+", " ").trim();
        return base.isBlank() ? "Unknown Candidate" : base;
    }
}
