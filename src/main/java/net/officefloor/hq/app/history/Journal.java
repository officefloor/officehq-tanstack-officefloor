package net.officefloor.hq.app.history;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.h2.api.Trigger;

/**
 * Copies every row a change writes to its table's history ({@code <table>_history}), with the sheet's today when the
 * change was made; a deleted row is copied as it was, marked gone. See {@link AsOf} for how the history is read back.
 */
public class Journal implements Trigger {

    private List<String> columns;
    private String insert;
    private int sheetColumn;
    private int personColumn;

    @Override
    public void init(Connection conn, String schemaName, String triggerName, String tableName, boolean before, int type)
            throws SQLException {
        columns = new ArrayList<>();
        try (ResultSet rs = conn.getMetaData().getColumns(null, schemaName, tableName, null)) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME"));
            }
        }
        // The sheet's own row names its sheet by its id.
        sheetColumn = columns.indexOf("SHEET".equals(tableName) ? "ID" : "SHEET_ID");
        personColumn = columns.indexOf("PERSON_ID");
        insert = "INSERT INTO " + tableName + "_HISTORY (" + String.join(", ", columns)
                + ", HISTORY_ON, HISTORY_GONE) VALUES (" + "?, ".repeat(columns.size()) + "?, ?)";
    }

    @Override
    public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
        Object[] row = newRow != null ? newRow : oldRow;
        try (PreparedStatement ps = conn.prepareStatement(insert)) {
            for (int i = 0; i < columns.size(); i++) {
                ps.setObject(i + 1, row[i]);
            }
            ps.setObject(columns.size() + 1, today(conn, sheetOf(conn, row)));
            ps.setBoolean(columns.size() + 2, newRow == null);
            ps.executeUpdate();
        }
    }

    /** The row's sheet: its own, else its person's. */
    private String sheetOf(Connection conn, Object[] row) throws SQLException {
        if (sheetColumn >= 0) {
            return (String) row[sheetColumn];
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT sheet_id FROM person WHERE id = ?")) {
            ps.setObject(1, row[personColumn]);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /** The sheet's today: the real date for a sheet never stored. */
    private static LocalDate today(Connection conn, String sheetId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT today_date FROM sheet WHERE id = ?")) {
            ps.setString(1, sheetId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getObject(1, LocalDate.class) : LocalDate.now();
            }
        }
    }
}
