package net.officefloor.hq.app.locale;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The locale each user has chosen, kept per sheet; {@link #DEFAULT} for a user who has chosen none. */
@Repository
public class LocaleStore {

    public static final String DEFAULT = "en";

    /** The locales a user may choose. */
    public static final List<String> LOCALES = List.of("en", "de");

    private final JdbcTemplate jdbc;

    public LocaleStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public String localeOf(String sheetId, String user) {
        List<String> found = jdbc.queryForList("SELECT locale FROM user_locale WHERE sheet_id = ? AND username = ?",
                String.class, sheetId, user);
        return found.isEmpty() ? DEFAULT : found.get(0);
    }

    public void choose(String sheetId, String user, String locale) {
        jdbc.update("MERGE INTO user_locale (sheet_id, username, locale) KEY (sheet_id, username) VALUES (?, ?, ?)",
                sheetId, user, locale);
    }
}
