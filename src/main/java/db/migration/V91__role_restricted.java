package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * A fourth role, restricted: sees on the people list only the people of the crews they lead or belong to, and
 * themselves. The role check of sheet_role is replaced in place (its constraint was not named), so the table and its
 * journal trigger stay as they are.
 */
public class V91__role_restricted extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        List<String> checks = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT constraint_name FROM information_schema.table_constraints "
                + "WHERE table_name = 'SHEET_ROLE' AND constraint_type = 'CHECK'"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                checks.add(rs.getString(1));
            }
        }
        try (Statement st = conn.createStatement()) {
            for (String name : checks) {
                st.execute("ALTER TABLE sheet_role DROP CONSTRAINT \"" + name + "\"");
            }
            st.execute("ALTER TABLE sheet_role ADD CONSTRAINT sheet_role_role "
                    + "CHECK (role IN ('clerk', 'steward', 'viewer', 'restricted'))");
        }
    }
}
