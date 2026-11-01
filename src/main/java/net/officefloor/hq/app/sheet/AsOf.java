package net.officefloor.hq.app.sheet;

import java.sql.Connection;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.officefloor.hq.app.rate.Rates;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** A sheet as it stood at the end of a date: every change made while the sheet's today was on or
 *  before that date counts, every later change does not, and that date stands in for the sheet's
 *  today. The sheet's rows are rebuilt from the change history (see {@link History}) into the asof
 *  schema, and the work runs against it in place of the live tables, so every rule reads the sheet
 *  as it was. The work must only read: what it writes is thrown away. */
@Service
public class AsOf {

    /** The tables of a sheet's data (keyed by sheet_id) kept in asof_hist and rebuilt into asof. */
    static final List<String> TABLES = List.of("CLAIM", "PERSON", "PERSON_GRADE", "PERSON_HOLD", "PERSON_PATRON",
            "PERSON_WATCH", "PERSON_STATEMENT", "PERIOD", "PERIOD_PERSON_TOTAL", "PERIOD_NUMBER", "RATE", "RATE_VERSION",
            "SHEET_SETTING", "SHEET_SETTING_DATED", "SHEET_ROLE", "SHEET_PERMISSION", "SHEET_DELEGATION",
            "CLAIM_LOG", "SHEET_GRADE_TERMS", "SHEET_HOLIDAY", "SHEET_VOID_REASON", "CLAIM_COMMENT", "PERSON_ALIAS",
            "POOL", "POOL_MEMBER", "CREW", "CREW_MEMBER", "PLAN", "PLAN_CLAIM", "PLAN_PAUSE", "CLAIM_LINE", "PERSON_SNAPSHOT", "SHEET_NOTE");

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final Map<String, List<String>> columns = new HashMap<>();
    private final Rates rates;

    public AsOf(JdbcTemplate jdbc, PlatformTransactionManager transactionManager, Rates rates) {
        this.jdbc = jdbc;
        this.rates = rates;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    /** Run `work` against the sheet as it stood at the end of `date`. It runs in a transaction of its
     *  own, on one connection, which is rolled back: the rebuilt rows are never kept. */
    public <T> T at(String sheetId, LocalDate date, Supplier<T> work) {
        return transactions.execute(status -> {
            status.setRollbackOnly();
            // One rebuild of the sheet at a time.
            jdbc.query("SELECT id FROM PUBLIC.sheet WHERE id = ? FOR UPDATE", (RowCallbackHandler) rs -> {}, sheetId);
            rebuild(sheetId, date);
            schema("ASOF");
            try {
                return work.get();
            } finally {
                schema("PUBLIC");
            }
        });
    }

    /** What a screen reads: `work` on the sheet as it stood at the end of the as-of date given (see
     *  {@link #at}), or on the live sheet when none is given. Refused: an as-of that is not a date. */
    public <T> T view(String sheetId, String asOfText, Supplier<T> work) throws InvalidSheetRequestException {
        if (asOfText == null || asOfText.isBlank()) return work.get();
        LocalDate date;
        try {
            date = LocalDate.parse(asOfText.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidSheetRequestException("Not a date: " + asOfText);
        }
        return at(sheetId, date, work);
    }

    private void rebuild(String sheetId, LocalDate date) {
        jdbc.update("DELETE FROM ASOF.sheet WHERE id = ?", sheetId);
        jdbc.update("INSERT INTO ASOF.sheet SELECT * FROM PUBLIC.sheet WHERE id = ?", sheetId);
        // Archived only by an archive made by then.
        jdbc.update("UPDATE ASOF.sheet SET today = ?, archived = archived AND (archived_on IS NULL OR archived_on <= ?) "
                + "WHERE id = ?", Date.valueOf(date), Date.valueOf(date), sheetId);
        for (String table : TABLES) {
            List<String> cols = columns(table);
            String names = '"' + String.join("\", \"", cols) + '"';
            // The rows as they stood: each change made by then removes or adds one, in the order made.
            Map<List<Object>, Integer> rows = new LinkedHashMap<>();
            jdbc.query("SELECT " + names + ", H_OP FROM ASOF_HIST.\"" + table + "\" WHERE sheet_id = ? "
                    + "AND (H_MADE_ON IS NULL OR H_MADE_ON <= ?) ORDER BY H_SEQ", (RowCallbackHandler) rs -> {
                        List<Object> row = new ArrayList<>(cols.size());
                        for (int i = 1; i <= cols.size(); i++) row.add(rs.getObject(i));
                        rows.merge(row, "I".equals(rs.getString(cols.size() + 1)) ? 1 : -1, Integer::sum);
                    }, sheetId, Date.valueOf(date));
            // A snapshot of a day is written as the day is left: it is not part of the sheet at the end of it.
            if (table.equals("PERSON_SNAPSHOT")) {
                int leftDay = cols.indexOf("LEFT_DAY");
                rows.keySet().removeIf(row -> {
                    Object left = row.get(leftDay);
                    LocalDate day = left instanceof Date d ? d.toLocalDate() : (LocalDate) left;
                    return !day.isBefore(date);
                });
            }
            jdbc.update("DELETE FROM ASOF.\"" + table + "\" WHERE sheet_id = ?", sheetId);
            String insert = "INSERT INTO ASOF.\"" + table + "\" (" + names + ") VALUES ("
                    + "?, ".repeat(cols.size() - 1) + "?)";
            List<Object[]> batch = new ArrayList<>();
            rows.forEach((row, n) -> {
                for (int i = 0; i < n; i++) batch.add(row.toArray());
            });
            if (!batch.isEmpty()) jdbc.batchUpdate(insert, batch);
        }
        ancestors(sheetId);
    }

    /** The tables an ancestor sheet lends the sheet: its rates (a grade with none on the sheet takes
     *  theirs) and its settings (naming its own parent). */
    private static final List<String> ANCESTOR_TABLES = List.of("RATE", "RATE_VERSION", "SHEET_SETTING");

    /** The sheets of the sheet's rate lineage (see {@link Rates#lineage}): its parent sheets and the sheet
     *  it was cloned from, as they stand now: each keeps a today of its own, so only the sheet itself is
     *  rebuilt as of the date. */
    private void ancestors(String sheetId) {
        for (String sheet : rates.lineage(sheetId, parentIn("ASOF", sheetId))) {
            for (String table : ANCESTOR_TABLES) {
                String names = '"' + String.join("\", \"", columns(table)) + '"';
                jdbc.update("DELETE FROM ASOF.\"" + table + "\" WHERE sheet_id = ?", sheet);
                jdbc.update("INSERT INTO ASOF.\"" + table + "\" (" + names + ") SELECT " + names
                        + " FROM PUBLIC.\"" + table + "\" WHERE sheet_id = ?", sheet);
            }
        }
    }

    private String parentIn(String schema, String sheetId) {
        return jdbc.query("SELECT parent_id FROM " + schema + ".sheet_setting WHERE sheet_id = ?",
                (rs, i) -> rs.getString(1), sheetId).stream().filter(p -> p != null).findFirst().orElse(null);
    }

    private List<String> columns(String table) {
        synchronized (columns) {
            return columns.computeIfAbsent(table, t -> jdbc.queryForList("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS "
                    + "WHERE TABLE_SCHEMA = 'ASOF' AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION", String.class, t));
        }
    }

    private void schema(String schema) {
        jdbc.execute((ConnectionCallback<Void>) (Connection c) -> {
            c.setSchema(schema);
            return null;
        });
    }
}
