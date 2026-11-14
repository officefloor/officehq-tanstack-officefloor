package net.officefloor.hq.app.locale;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LocaleRepository {

    /** The locale of a user who never chose one. */
    public static final String DEFAULT = "en";

    private final JdbcTemplate jdbc;

    public LocaleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The user's locale on the sheet, {@link #DEFAULT} unless chosen. */
    public String find(String sheetId, String user) {
        List<String> found = jdbc.queryForList("SELECT locale FROM user_locale WHERE sheet_id = ? AND user_name = ?",
                String.class, sheetId, user);
        return found.isEmpty() ? DEFAULT : found.get(0);
    }

    public void save(String sheetId, String user, String locale) {
        jdbc.update("MERGE INTO user_locale (sheet_id, user_name, locale) KEY (sheet_id, user_name) VALUES (?, ?, ?)",
                sheetId, user, locale);
    }
}
