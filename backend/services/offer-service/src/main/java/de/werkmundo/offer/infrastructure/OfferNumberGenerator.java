package de.werkmundo.offer.infrastructure;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@ApplicationScoped
public class OfferNumberGenerator {

    @Inject
    EntityManager entityManager;

    @Transactional(Transactional.TxType.MANDATORY)
    public String nextNumber() {
        String month = YearMonth.now(ZoneId.of("Europe/Berlin"))
                .format(DateTimeFormatter.ofPattern("yyyyMM"));

        int inserted = entityManager.createNativeQuery(
                "INSERT INTO offer_number_counter (year_month, last_number) " +
                "VALUES (?1, 1) ON CONFLICT (year_month) DO NOTHING")
                .setParameter(1, month)
                .executeUpdate();

        int number;

        if (inserted == 1) {
            number = 1;
        } else {
            Number last = (Number) entityManager.createNativeQuery(
                    "SELECT last_number FROM offer_number_counter " +
                    "WHERE year_month = ?1 FOR UPDATE")
                    .setParameter(1, month)
                    .getSingleResult();

            number = Math.addExact(last.intValue(), 1);

            entityManager.createNativeQuery(
                    "UPDATE offer_number_counter SET last_number = ?1 " +
                    "WHERE year_month = ?2")
                    .setParameter(1, number)
                    .setParameter(2, month)
                    .executeUpdate();
        }

        return String.format(Locale.ROOT, "ANG-%s-%04d", month, number);
    }
}
