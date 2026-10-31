package net.officefloor.hq.app.sheet;

import java.sql.Connection;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
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
            "PERSON_WATCH", "PERSON_STATEMENT", "PERIOD", "PERIOD_PERSON_TOTAL", "PERIOD_NUMBER", "RATE",
            "SHEET_SETTING", "SHEET_SETTING_DATED", "SHEET_ROLE", "SHEET_PERMISSION", "SHEET_DELEGATION",
            "CLAIM_LOG", "SHEET_GRADE_TERMS");

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final Map<String, List<String>> columns = new HashMap<>();

    public AsOf(JdbcTemplate jdbc, PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
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

    private void rebuild(String sheetId, LocalDate date) {
        jdbc.update("DELETE FROM ASOF.sheet WHERE id = ?", sheetId);
        jdbc.update("INSERT INTO ASOF.sheet SELECT * FROM PUBLIC.sheet WHERE id = ?", sheetId);
        jdbc.update("UPDATE ASOF.sheet SET today = ? WHERE id = ?", Date.valueOf(date), sheetId);
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
            jdbc.update("DELETE FROM ASOF.\"" + table + "\" WHERE sheet_id = ?", sheetId);
            String insert = "INSERT INTO ASOF.\"" + table + "\" (" + names + ") VALUES ("
                    + "?, ".repeat(cols.size() - 1) + "?)";
            List<Object[]> batch = new ArrayList<>();
            rows.forEach((row, n) -> {
                for (int i = 0; i < n; i++) batch.add(row.toArray());
            });
            if (!batch.isEmpty()) jdbc.batchUpdate(insert, batch);
        }
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
