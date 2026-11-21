package de.winfprojekt.werkmundo.userservice.domain;

import java.math.BigDecimal;

/** Testdaten: ein Betrieb mit allen Angaben, die für Rechnungen nötig sind. Alle Werte sind erfunden. */
public final class CompanyFixtures {

    private CompanyFixtures() {
    }

    public static Company completeForInvoices(Company company) {
        company.changeLegalDetails("Einzelunternehmen", "Elektro", null, null, "Handwerksrolle Nr. 12345");
        company.changeAddress("Hauptstraße", "12", "85049", "Ingolstadt", "de");
        company.changeContact("info@elektro-mueller.example", "0841 123456", "Max Müller", null);
        company.addBankAccount("Max Müller", "DE89370400440532013000", "COBADEFFXXX", "Beispielbank");
        company.addEmployeeGroup("Geselle", new BigDecimal("75.00"));
        company.configureTaxInvoice(new TaxInvoiceData(
                VatTreatment.REGULAR, new BigDecimal("19.00"), "124/567/89012", null, "RE-2026-", "AN-2026-", 14, null));
        return company;
    }
}
