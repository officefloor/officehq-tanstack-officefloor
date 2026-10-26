package db.migration;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * A rate is a percent from 0.00 to 99999.99 (it was capped at 100.00). The rate columns are widened in place, so
 * their position (which the snapshots copy by) is kept, and the rate table's check is replaced by one on the new
 * range. In Java, as the old check was created unnamed and is found by the column it checks.
 */
public class V72__rate_widen extends BaseJavaMigration {

    private static final String[] TABLES = {
        "PUBLIC.RATE", "HIST.RATE", "ASOF.RATE", "PUBLIC.RATE_VERSION", "HIST.RATE_VERSION", "ASOF.RATE_VERSION" };

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement st = context.getConnection().createStatement()) {
            List<String> checks = new ArrayList<>();
            try (ResultSet rs = st.executeQuery("SELECT tc.CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc"
                    + " JOIN INFORMATION_SCHEMA.CHECK_CONSTRAINTS cc ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA"
                    + " AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME"
                    + " WHERE tc.TABLE_SCHEMA = 'PUBLIC' AND tc.TABLE_NAME = 'RATE' AND tc.CONSTRAINT_TYPE = 'CHECK'"
                    + " AND UPPER(cc.CHECK_CLAUSE) LIKE '%100%'")) {
                while (rs.next()) {
                    checks.add(rs.getString(1));
                }
            }
            for (String check : checks) {
                st.execute("ALTER TABLE PUBLIC.RATE DROP CONSTRAINT \"" + check + "\"");
            }
            for (String table : TABLES) {
                st.execute("ALTER TABLE " + table + " ALTER COLUMN RATE SET DATA TYPE DECIMAL(7, 2)");
            }
            st.execute("ALTER TABLE PUBLIC.RATE ADD CONSTRAINT RATE_RANGE CHECK (RATE >= 0 AND RATE <= 99999.99)");
        }
    }
}
