package db.migration;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * A claim may be PENDING: a clerk settled it above the clerk limit and it waits for a steward to approve
 * (SETTLED) or reject (back to OPEN) it. The status's check is widened to allow it. H2 named that unnamed
 * check itself, so it is looked up here rather than in SQL. A PENDING claim keeps the statement binding it
 * had (stated, stated_due) but is not bound by it until it is rejected.
 */
public class V22__claim_pending extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        try (Statement st = context.getConnection().createStatement()) {
            List<String> checks = new ArrayList<>();
            try (ResultSet rs = st.executeQuery("SELECT t.CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS t"
                    + " JOIN INFORMATION_SCHEMA.CHECK_CONSTRAINTS c ON c.CONSTRAINT_SCHEMA = t.CONSTRAINT_SCHEMA"
                    + " AND c.CONSTRAINT_NAME = t.CONSTRAINT_NAME"
                    + " WHERE t.TABLE_NAME = 'CLAIM' AND t.CONSTRAINT_TYPE = 'CHECK' AND c.CHECK_CLAUSE LIKE '%STATUS%'")) {
                while (rs.next()) checks.add(rs.getString(1));
            }
            for (String check : checks) {
                st.execute("ALTER TABLE claim DROP CONSTRAINT \"" + check + "\"");
            }
            st.execute("ALTER TABLE claim ADD CONSTRAINT claim_status_check"
                    + " CHECK (status IN ('DRAFT', 'OPEN', 'PENDING', 'SETTLED', 'VOID'))");
        }
    }
}
