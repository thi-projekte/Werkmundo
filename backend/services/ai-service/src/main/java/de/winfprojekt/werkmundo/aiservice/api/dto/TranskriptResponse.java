package de.winfprojekt.werkmundo.aiservice.api.dto;

/** Antwort von {@code POST /ai/transcribe}. Feldname wie im Vorsemester ({@code transkript}). */
public record TranskriptResponse(String transkript) {}
