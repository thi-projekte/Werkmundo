package de.werkmundo.document.domain;

public record DocumentMetadata(String documentId, String referenceId, DocumentType type, String fileName) {
    public static DocumentMetadata from(String documentId, Document document) {
        return new DocumentMetadata(documentId, document.referenceId(), document.type(), document.fileName());
    }
}
