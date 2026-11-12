package net.officefloor.hq.app.history;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.h2.api.Trigger;

/** Keeps every version of a row of the table it is set on in {@code <table>_hist}: after each insert or
 *  update the new row, after a delete the old one marked gone, each with the sheet's today when the change
 *  was made (the real date when the sheet has none set) and the next {@code hist_seq}. See {@link History}. */
public class HistoryTrigger implements Trigger {

    private String insert;

    private int columns;

    private int sheetColumn;

    @Override
    public void init(Connection conn, String schemaName, String triggerName, String tableName, boolean before,
            int type) throws SQLException {
        List<String> names = new ArrayList<>();
        try (ResultSet rs = conn.getMetaData().getColumns(null, schemaName, tableName, null)) {
            while (rs.next()) {
                names.add(rs.getString("COLUMN_NAME"));
            }
        }
        columns = names.size();
        sheetColumn = names.indexOf("SHEET_ID");
        StringBuilder sql = new StringBuilder("INSERT INTO \"" + schemaName + "\".\"" + tableName
                + "_HIST\" (HIST_SEQ, HIST_MADE_ON, HIST_GONE");
        for (String name : names) {
            sql.append(", \"").append(name).append('"');
        }
        sql.append(") VALUES (NEXT VALUE FOR \"").append(schemaName).append("\".HIST_SEQ, ?, ?");
        sql.append(", ?".repeat(columns)).append(')');
        insert = sql.toString();
    }

    @Override
    public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
        Object[] row = newRow != null ? newRow : oldRow;
        try (PreparedStatement ps = conn.prepareStatement(insert)) {
            ps.setDate(1, Date.valueOf(today(conn, row[sheetColumn])));
            ps.setBoolean(2, newRow == null);
            for (int i = 0; i < columns; i++) {
                ps.setObject(i + 3, row[i]);
            }
            ps.executeUpdate();
        }
    }

    /** The sheet's today, as {@code SheetStore} reads it. */
    private static LocalDate today(Connection conn, Object sheetId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT today FROM sheet WHERE id = ?")) {
            ps.setObject(1, sheetId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getObject(1, LocalDate.class) : LocalDate.now();
            }
        }
    }
}
