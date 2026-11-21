package de.winfprojekt.werkmundo.userservice.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CompanyTest {

    private static final String IBAN_1 = "DE89370400440532013000";
    private static final String IBAN_2 = "DE02120300000000202051";

    @Test
    void newCompanyNeedsOnlyNameAndGetsDefaultAiPreferences() {
        Company company = new Company("  Elektro Müller  ");

        assertEquals("Elektro Müller", company.getName());
        assertEquals("DE", company.getCountry());
        assertEquals(ToneOfVoice.FORMAL, company.getAiPreferences().getToneOfVoice());
        assertEquals(DetailLevel.NORMAL, company.getAiPreferences().getDetailLevel());
        assertThrows(IllegalArgumentException.class, () -> new Company(" "));
    }

    @Test
    void firstBankAccountBecomesDefaultAndDefaultMovesOnRemoval() {
        Company company = new Company("Elektro Müller");
        BankAccount first = company.addBankAccount("Max Müller", IBAN_1, "COBADEFFXXX", "Commerzbank");
        BankAccount second = company.addBankAccount("Max Müller", IBAN_2, null, null);

        assertTrue(first.isDefaultAccount());
        assertFalse(second.isDefaultAccount());

        company.markDefaultBankAccount(second.getId());
        assertTrue(second.isDefaultAccount());
        assertFalse(first.isDefaultAccount());

        company.removeBankAccount(second.getId());
        assertTrue(first.isDefaultAccount());
        assertEquals(1, company.getBankAccounts().size());
    }

    @Test
    void rejectsInvalidAndDuplicateIban() {
        Company company = new Company("Elektro Müller");
        assertThrows(IllegalArgumentException.class, () -> company.addBankAccount("Max", "DE00123", null, null));
        company.addBankAccount("Max", IBAN_1, null, null);
        assertThrows(IllegalArgumentException.class, () -> company.addBankAccount("Max", "de89 3704 0044 0532 0130 00", null, null));
    }

    @Test
    void employeeGroupNamesAreUniqueAndFirstIsDefault() {
        Company company = new Company("Elektro Müller");
        EmployeeGroup geselle = company.addEmployeeGroup("Geselle", new BigDecimal("75"));
        EmployeeGroup meister = company.addEmployeeGroup("Meister", new BigDecimal("115.00"));

        assertTrue(geselle.isDefaultGroup());
        assertEquals(new BigDecimal("75.00"), geselle.getHourlyRate());
        assertThrows(IllegalArgumentException.class, () -> company.addEmployeeGroup("geselle", BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class, () -> company.renameEmployeeGroup(meister.getId(), "GESELLE"));
        assertThrows(IllegalArgumentException.class, () -> company.addEmployeeGroup("Azubi", new BigDecimal("-1")));

        company.removeEmployeeGroup(geselle.getId());
        assertSame(meister, company.defaultEmployeeGroup().orElseThrow());
    }

    @Test
    void taxSettingsNeedTaxNumberOrVatId() {
        Company company = new Company("Elektro Müller");
        TaxInvoiceData withoutIdentifier = new TaxInvoiceData(
                VatTreatment.REGULAR, new BigDecimal("19"), null, " ", "RE-2026-", null, 14, null);
        assertThrows(IllegalArgumentException.class, () -> company.configureTaxInvoice(withoutIdentifier));

        TaxInvoiceSettings settings = company.configureTaxInvoice(new TaxInvoiceData(
                VatTreatment.REGULAR, new BigDecimal("19"), "123/456/78901", null, "RE-2026-", "AN-2026-", 14,
                "Rechnung@Mueller.de"));
        assertEquals(new BigDecimal("19.00"), settings.getDefaultVatRate());
        assertEquals("rechnung@mueller.de", settings.getInvoiceEmail());

        // Ein zweiter Aufruf ändert dieselben Einstellungen statt neue anzulegen.
        TaxInvoiceSettings updated = company.configureTaxInvoice(new TaxInvoiceData(
                VatTreatment.REGULAR, new BigDecimal("19"), null, "de 123456789", "RE-", null, 30, null));
        assertSame(settings, updated);
        assertEquals("DE123456789", updated.getVatId());
        assertNull(updated.getTaxNumber());
    }

    @Test
    void smallBusinessMustNotChargeVat() {
        Company company = new Company("Maler Schmidt");
        assertThrows(IllegalArgumentException.class, () -> company.configureTaxInvoice(new TaxInvoiceData(
                VatTreatment.SMALL_BUSINESS, new BigDecimal("19"), "123/456/78901", null, "RE-", null, 14, null)));
        TaxInvoiceSettings settings = company.configureTaxInvoice(new TaxInvoiceData(
                VatTreatment.SMALL_BUSINESS, BigDecimal.ZERO, "123/456/78901", null, "RE-", null, 14, null));
        assertEquals(VatTreatment.SMALL_BUSINESS, settings.getVatTreatment());
    }

    @Test
    void travelCostModelRequiresMatchingRates() {
        Company company = new Company("Elektro Müller");
        assertThrows(IllegalArgumentException.class,
                () -> company.configureTravelCosts(new TravelCostData(TravelCostModel.FLAT, null, null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> company.configureTravelCosts(new TravelCostData(TravelCostModel.KM_ONLY, null, null, null)));

        TravelCostSettings settings = company.configureTravelCosts(
                new TravelCostData(TravelCostModel.FLAT_PLUS_KM, new BigDecimal("25"), new BigDecimal("0.5"), 10));
        assertEquals(new BigDecimal("0.50"), settings.getKmRate());
        assertEquals(10, settings.getFreeKm());
    }

    @Test
    void textTemplateIsReplacedPerType() {
        Company company = new Company("Elektro Müller");
        company.putTextTemplate(TextTemplateType.PAYMENT_TERMS, "Zahlbar in 14 Tagen.");
        company.putTextTemplate(TextTemplateType.PAYMENT_TERMS, "Zahlbar in 30 Tagen.");

        assertEquals(1, company.getTextTemplates().size());
        assertEquals("Zahlbar in 30 Tagen.",
                company.findTextTemplate(TextTemplateType.PAYMENT_TERMS).orElseThrow().getContent());
    }

    @Test
    void reportsMissingInvoiceRequirements() {
        Company company = new Company("Elektro Müller");
        assertTrue(company.missingInvoiceRequirements().contains("Bankverbindung"));

        CompanyFixtures.completeForInvoices(company);
        assertTrue(company.missingInvoiceRequirements().isEmpty());
    }
}
