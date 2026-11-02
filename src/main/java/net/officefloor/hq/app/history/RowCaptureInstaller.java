package net.officefloor.hq.app.history;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/** At start-up, once the schema is migrated: puts the {@link RowCapture} trigger on each of a sheet's tables
 *  (every table with a sheet_id, and the sheet itself), and makes the table each is replayed into in the asof
 *  schema (see {@link AsOfView}). A table added later is captured the same way, with no change here. */
@Component
@DependsOn("flywayInitializer")
public class RowCaptureInstaller implements InitializingBean {

    /** The tables that are not a sheet's state: the change log itself, and the history a statement as of a
     *  past date reads (V48__history.sql). */
    private static final Set<String> SKIPPED = Set.of("ROW_CHANGE", "CLAIM_HISTORY", "RATE_HISTORY", "GRADE_HISTORY",
            "SETTING_HISTORY", "POLICY_HISTORY", "RETIRE_HISTORY");

    /** A captured table: its name, its columns in order, the positions of its primary key's columns (empty
     *  when it has none) and of the column holding the sheet's id. */
    public record Table(String name, List<String> columns, List<Integer> key, int sheetColumn) {
    }

    private final DataSource dataSource;
    private final Map<String, Table> tables = new LinkedHashMap<>();

    public RowCaptureInstaller(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** The captured tables, by name. */
    public Map<String, Table> tables() {
        return tables;
    }

    @Override
    public void afterPropertiesSet() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            List<String> names = new ArrayList<>();
            try (ResultSet rs = conn.createStatement().executeQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES"
                    + " WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE = 'BASE TABLE' ORDER BY TABLE_NAME")) {
                while (rs.next()) names.add(rs.getString(1));
            }
            boolean baseline = empty(conn);
            for (String name : names) {
                if (SKIPPED.contains(name)) continue;
                List<String> columns = RowCapture.columns(conn, "PUBLIC", name);
                int sheetColumn = columns.indexOf(name.equals("SHEET") ? "ID" : "SHEET_ID");
                if (sheetColumn < 0) continue;
                Table table = new Table(name, columns, key(conn, name, columns), sheetColumn);
                tables.put(name, table);
                try (Statement st = conn.createStatement()) {
                    st.execute("DROP TABLE IF EXISTS ASOF.\"" + name + "\"");
                    st.execute("CREATE TABLE ASOF.\"" + name + "\" AS SELECT * FROM PUBLIC.\"" + name + "\" WITH NO DATA");
                    st.execute("DROP TRIGGER IF EXISTS PUBLIC.\"CAPTURE_" + name + "\"");
                    st.execute("CREATE TRIGGER PUBLIC.\"CAPTURE_" + name + "\" AFTER INSERT, UPDATE, DELETE ON PUBLIC.\""
                            + name + "\" FOR EACH ROW CALL '" + RowCapture.class.getName() + "'");
                }
                if (baseline) baseline(conn, table);
            }
        }
    }

    private static boolean empty(Connection conn) throws SQLException {
        try (ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM PUBLIC.ROW_CHANGE")) {
            return rs.next() && rs.getLong(1) == 0;
        }
    }

    /** The rows there before any change was kept count from the beginning of time. */
    private static void baseline(Connection conn, Table table) throws SQLException {
        try (ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM PUBLIC.\"" + table.name() + "\"")) {
            while (rs.next()) {
                Object[] row = new Object[table.columns().size()];
                for (int i = 0; i < row.length; i++) row[i] = rs.getObject(i + 1);
                RowCapture.record(conn, String.valueOf(row[table.sheetColumn()]), RowCapture.BEGINNING, table.name(), null, row);
            }
        }
    }

    private static List<Integer> key(Connection conn, String table, List<String> columns) throws SQLException {
        List<Integer> key = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT k.COLUMN_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS c"
                + " JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE k ON k.CONSTRAINT_SCHEMA = c.CONSTRAINT_SCHEMA"
                + " AND k.CONSTRAINT_NAME = c.CONSTRAINT_NAME WHERE c.TABLE_SCHEMA = 'PUBLIC' AND c.TABLE_NAME = ?"
                + " AND c.CONSTRAINT_TYPE = 'PRIMARY KEY' ORDER BY k.ORDINAL_POSITION")) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) key.add(columns.indexOf(rs.getString(1)));
            }
        }
        return key;
    }
}
