package net.officefloor.hq.app.history;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.h2.api.Trigger;

/** The trigger on each of a sheet's tables (installed by {@link RowCaptureInstaller}): keeps every insert,
 *  update and delete of a row in row_change (see V49__row_change.sql), with the sheet's today when it was
 *  made, in the same transaction. */
public class RowCapture implements Trigger {

    /** The on_date of a row kept before its sheet had a today: the beginning of time. */
    static final Date BEGINNING = Date.valueOf(LocalDate.of(1, 1, 1));

    private String table;
    private int sheetColumn;
    private int todayColumn = -1;

    @Override
    public void init(Connection conn, String schemaName, String triggerName, String tableName, boolean before, int type)
            throws SQLException {
        this.table = tableName;
        List<String> columns = columns(conn, schemaName, tableName);
        boolean sheet = tableName.equalsIgnoreCase("SHEET");
        this.sheetColumn = columns.indexOf(sheet ? "ID" : "SHEET_ID");
        if (sheet) this.todayColumn = columns.indexOf("TODAY");
    }

    @Override
    public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
        Object[] row = newRow != null ? newRow : oldRow;
        String sheetId = String.valueOf(row[sheetColumn]);
        Date onDate = todayColumn >= 0 && newRow != null ? date(newRow[todayColumn]) : today(conn, sheetId);
        record(conn, sheetId, onDate, table, oldRow, newRow);
    }

    /** Keep one row change. */
    static void record(Connection conn, String sheetId, Date onDate, String table, Object[] oldRow, Object[] newRow)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO PUBLIC.ROW_CHANGE"
                + " (sheet_id, on_date, table_name, old_row, new_row) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, sheetId);
            ps.setDate(2, onDate);
            ps.setString(3, table);
            ps.setBytes(4, oldRow == null ? null : write(oldRow));
            ps.setBytes(5, newRow == null ? null : write(newRow));
            ps.executeUpdate();
        }
    }

    /** The sheet's today as the change is made; the beginning of time when the sheet has none. */
    private static Date today(Connection conn, String sheetId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT today FROM PUBLIC.SHEET WHERE id = ?")) {
            ps.setString(1, sheetId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getDate(1) != null ? rs.getDate(1) : BEGINNING;
            }
        }
    }

    private static Date date(Object value) {
        if (value instanceof Date d) return d;
        if (value instanceof LocalDate d) return Date.valueOf(d);
        return value == null ? BEGINNING : Date.valueOf(value.toString());
    }

    /** A table's column names, in column order. */
    static List<String> columns(Connection conn, String schema, String table) throws SQLException {
        List<String> columns = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS"
                + " WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION")) {
            ps.setString(1, schema);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) columns.add(rs.getString(1));
            }
        }
        return columns;
    }

    /** Only the value types a row holds may be read back. */
    private static final ObjectInputFilter VALUES = ObjectInputFilter.Config.createFilter(
            "java.lang.*;java.math.*;java.sql.*;java.time.*;java.util.UUID;[Ljava.lang.Object;;[B;!*");

    static byte[] write(Object[] row) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(row);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    static Object[] read(byte[] bytes) {
        if (bytes == null) return null;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            in.setObjectInputFilter(VALUES);
            return (Object[]) in.readObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}
