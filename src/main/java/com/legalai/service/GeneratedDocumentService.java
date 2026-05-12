package com.legalai.service;

import com.legalai.domain.DocumentTemplate;
import com.legalai.domain.GeneratedDocument;
import com.legalai.dto.request.GenerateDocumentRequest;
import com.legalai.dto.request.UpdateGeneratedDocumentRequest;
import com.legalai.dto.response.DocumentTemplateResponse;
import com.legalai.dto.response.GeneratedDocumentResponse;
import com.legalai.exception.CustomException;
import com.legalai.exception.ErrorCode;
import com.legalai.repository.DocumentTemplateRepository;
import com.legalai.repository.GeneratedDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeneratedDocumentService {

    private final DocumentTemplateRepository templateRepository;
    private final GeneratedDocumentRepository generatedDocumentRepository;

    @Transactional(readOnly = true)
    public List<DocumentTemplateResponse> getTemplates() {
        return templateRepository.findByActiveTrue()
                .stream()
                .map(DocumentTemplateResponse::from)
                .toList();
    }

    @Transactional
    public GeneratedDocumentResponse generate(GenerateDocumentRequest request, Long userId) {
        DocumentTemplate template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new CustomException(ErrorCode.TEMPLATE_NOT_FOUND));

        String content = fillTemplate(template.getTemplateContent(), request.getFields());

        GeneratedDocument doc = GeneratedDocument.builder()
                .userId(userId)
                .templateId(template.getId())
                .title(request.getTitle())
                .content(content)
                .build();
        generatedDocumentRepository.save(doc);

        return GeneratedDocumentResponse.from(doc);
    }

    @Transactional(readOnly = true)
    public List<GeneratedDocumentResponse> getGeneratedDocuments(Long userId) {
        return generatedDocumentRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(GeneratedDocumentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public GeneratedDocumentResponse getGeneratedDocument(Long id, Long userId) {
        GeneratedDocument doc = findDocument(id, userId);
        return GeneratedDocumentResponse.from(doc);
    }

    @Transactional
    public GeneratedDocumentResponse update(Long id, UpdateGeneratedDocumentRequest request, Long userId) {
        GeneratedDocument doc = findDocument(id, userId);
        doc.update(request.getTitle(), request.getContent());
        return GeneratedDocumentResponse.from(doc);
    }

    public byte[] generatePdf(Long id, Long userId) {
        GeneratedDocument doc = findDocument(id, userId);

        try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            pdf.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(pdf, page)) {
                // 윈도우 맑은고딕, 없으면 기본 폰트로 폴백
                File malgunFont = new File("C:/Windows/Fonts/malgun.ttf");
                float fontSize = 11;
                float margin = 50;
                float yStart = PDRectangle.A4.getHeight() - margin;
                float width = PDRectangle.A4.getWidth() - 2 * margin;

                if (malgunFont.exists()) {
                    PDType0Font font = PDType0Font.load(pdf, malgunFont);
                    writeKoreanText(stream, font, doc.getTitle(), doc.getContent(),
                            fontSize, margin, yStart, width);
                } else {
                    PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                    writeFallbackText(stream, font, doc.getTitle(), doc.getContent(),
                            fontSize, margin, yStart, width);
                }
            }

            pdf.save(out);
            return out.toByteArray();

        } catch (IOException e) {
            log.error("PDF 생성 실패 - id: {}", id, e);
            throw new CustomException(ErrorCode.PDF_GENERATION_FAILED);
        }
    }

    private void writeKoreanText(PDPageContentStream stream, PDType0Font font,
                                  String title, String content,
                                  float fontSize, float margin, float yStart, float width) throws IOException {
        float leading = fontSize * 1.5f;
        float y = yStart;

        // 제목
        stream.beginText();
        stream.setFont(font, fontSize + 4);
        stream.newLineAtOffset(margin, y);
        stream.showText(title);
        stream.endText();
        y -= leading * 2;

        // 구분선
        stream.moveTo(margin, y);
        stream.lineTo(margin + width, y);
        stream.stroke();
        y -= leading;

        // 본문 (줄바꿈 처리)
        stream.setFont(font, fontSize);
        for (String line : content.split("\n")) {
            if (y < margin) break;
            stream.beginText();
            stream.newLineAtOffset(margin, y);
            stream.showText(line.isEmpty() ? " " : line);
            stream.endText();
            y -= leading;
        }
    }

    private void writeFallbackText(PDPageContentStream stream, PDType1Font font,
                                    String title, String content,
                                    float fontSize, float margin, float yStart, float width) throws IOException {
        float leading = fontSize * 1.5f;
        float y = yStart;

        stream.beginText();
        stream.setFont(font, fontSize + 4);
        stream.newLineAtOffset(margin, y);
        stream.showText(title.replaceAll("[^\\x00-\\x7F]", "?"));
        stream.endText();
        y -= leading * 2;

        stream.setFont(font, fontSize);
        for (String line : content.split("\n")) {
            if (y < margin) break;
            stream.beginText();
            stream.newLineAtOffset(margin, y);
            stream.showText(line.replaceAll("[^\\x00-\\x7F]", "?").isEmpty() ? " " : line.replaceAll("[^\\x00-\\x7F]", "?"));
            stream.endText();
            y -= leading;
        }
    }

    private String fillTemplate(String templateContent, Map<String, String> fields) {
        if (fields == null || fields.isEmpty()) return templateContent;
        String result = templateContent;
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    private GeneratedDocument findDocument(Long id, Long userId) {
        return generatedDocumentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.GENERATED_DOCUMENT_NOT_FOUND));
    }
}
