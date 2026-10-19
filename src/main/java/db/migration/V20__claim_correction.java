package db.migration;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * A claim with a negative amount is a correction: corrects is the number of the SETTLED claim of the
 * same person it corrects (NULL for any other claim). The amount's check (0.00 or more) is dropped so a
 * correction can be stored. H2 named that unnamed check itself, so it is looked up here rather than in SQL.
 */
public class V20__claim_correction extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        try (Statement st = context.getConnection().createStatement()) {
            List<String> checks = new ArrayList<>();
            try (ResultSet rs = st.executeQuery("SELECT t.CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS t"
                    + " JOIN INFORMATION_SCHEMA.CHECK_CONSTRAINTS c ON c.CONSTRAINT_SCHEMA = t.CONSTRAINT_SCHEMA"
                    + " AND c.CONSTRAINT_NAME = t.CONSTRAINT_NAME"
                    + " WHERE t.TABLE_NAME = 'CLAIM' AND t.CONSTRAINT_TYPE = 'CHECK' AND c.CHECK_CLAUSE LIKE '%AMOUNT%'")) {
                while (rs.next()) checks.add(rs.getString(1));
            }
            for (String check : checks) {
                st.execute("ALTER TABLE claim DROP CONSTRAINT \"" + check + "\"");
            }
            st.execute("ALTER TABLE claim ADD COLUMN corrects INT");
        }
    }
}
