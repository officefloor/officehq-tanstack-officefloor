package net.officefloor.hq.app.sheet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.h2.api.Trigger;

/** The H2 trigger that keeps a table's change history in asof_hist: each change to a row writes the
 *  row it removed ('D') and the row it added ('I'), with the sheet's today when it was made. An
 *  update that changes nothing writes nothing. See {@link AsOf}. */
public class History implements Trigger {

    private int sheetColumn;
    private String insert;

    @Override
    public void init(Connection conn, String schemaName, String triggerName, String tableName, boolean before, int type)
            throws SQLException {
        List<String> columns = columns(conn, schemaName, tableName);
        sheetColumn = columns.indexOf("SHEET_ID");
        StringBuilder names = new StringBuilder();
        StringBuilder marks = new StringBuilder();
        for (String column : columns) {
            names.append('"').append(column).append("\", ");
            marks.append("?, ");
        }
        insert = "INSERT INTO ASOF_HIST.\"" + tableName + "\" (" + names + "H_SEQ, H_MADE_ON, H_OP) VALUES ("
                + marks + "NEXT VALUE FOR ASOF_HIST.CHANGE_SEQ, "
                + "(SELECT today FROM PUBLIC.sheet WHERE id = ?), ?)";
    }

    @Override
    public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
        if (oldRow != null && newRow != null && Arrays.equals(oldRow, newRow)) return;
        if (oldRow != null) write(conn, oldRow, "D");
        if (newRow != null) write(conn, newRow, "I");
    }

    private void write(Connection conn, Object[] row, String op) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(insert)) {
            int i = 1;
            for (Object value : row) ps.setObject(i++, value);
            ps.setObject(i++, row[sheetColumn]);
            ps.setString(i, op);
            ps.executeUpdate();
        }
    }

    /** The table's column names, in order. */
    static List<String> columns(Connection conn, String schema, String table) throws SQLException {
        List<String> columns = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION")) {
            ps.setString(1, schema);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) columns.add(rs.getString(1));
            }
        }
        return columns;
    }
}
