package db.migration;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Drop the checks that held a rate's and a person's grade to A, B or C (H2 named them itself): a sheet's grades are
 *  now A, B and C and those added to it, which the app checks. */
public class V123__grade_checks extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        List<String[]> checks = new ArrayList<>();
        try (Statement st = context.getConnection().createStatement();
                ResultSet rs = st.executeQuery("SELECT tc.TABLE_NAME, tc.CONSTRAINT_NAME "
                        + "FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc JOIN INFORMATION_SCHEMA.CHECK_CONSTRAINTS cc "
                        + "ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME "
                        + "WHERE tc.CONSTRAINT_TYPE = 'CHECK' AND tc.TABLE_NAME IN ('RATE', 'PERSON_GRADE') "
                        + "AND UPPER(cc.CHECK_CLAUSE) LIKE '%GRADE%'")) {
            while (rs.next()) {
                checks.add(new String[] { rs.getString(1), rs.getString(2) });
            }
        }
        try (Statement st = context.getConnection().createStatement()) {
            for (String[] check : checks) {
                st.execute("ALTER TABLE \"" + check[0] + "\" DROP CONSTRAINT \"" + check[1] + "\"");
            }
        }
    }
}
