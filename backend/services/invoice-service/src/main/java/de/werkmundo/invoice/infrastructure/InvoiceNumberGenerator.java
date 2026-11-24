package de.werkmundo.invoice.infrastructure;

import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

@ApplicationScoped
public class InvoiceNumberGenerator {

    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

    private static final String SQL = """
            INSERT INTO invoice_number_counter (year_month, last_number)
            VALUES (?, 1)
            ON CONFLICT (year_month)
            DO UPDATE SET last_number = invoice_number_counter.last_number + 1
            RETURNING last_number
            """;

    // Must use the same database transaction as final invoice issuance.
    public String nextNumber(Connection connection) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String month = YearMonth.now(ZONE)
                .format(DateTimeFormatter.ofPattern("yyyyMM"));

        try (PreparedStatement statement = connection.prepareStatement(SQL)) {
            statement.setString(1, month);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Invoice counter did not return a number");
                }
                int number = result.getInt(1);
                if (number > 9999) {
                    throw new IllegalStateException("Monthly invoice number limit exceeded");
                }
                return String.format(Locale.ROOT, "RE-%s-%04d", month, number);
            }
        }
    }
}
