package de.werkmundo.document.domain;

public enum DocumentType {
    OFFER("angebot"),
    INVOICE("rechnung");

    private final String filePrefix;

    DocumentType(String filePrefix) {
        this.filePrefix = filePrefix;
    }

    public String fileName(String referenceId) {
        return filePrefix + "-" + referenceId.replaceAll("[^A-Za-z0-9_-]", "_") + ".pdf";
    }
}
