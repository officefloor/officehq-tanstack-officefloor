package net.officefloor.hq.app.locale;

import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The locale each user chose per sheet: en (any name that chose none) or de. It only changes how the screen shows
 *  figures to that user; exports and audit lines are the same whichever it is. */
@Repository
public class Locales {

    public static final String EN = "en";
    public static final String DE = "de";
    public static final Set<String> ALL = Set.of(EN, DE);

    private final JdbcTemplate jdbc;

    public Locales(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The user's locale on the sheet: en unless chosen. */
    public String of(String sheet, String user) {
        List<String> found = jdbc.queryForList("SELECT locale FROM user_locale WHERE sheet = ? AND user_name = ?",
                String.class, sheet, user);
        return found.isEmpty() ? EN : found.get(0);
    }

    public void set(String sheet, String user, String locale) {
        jdbc.update("MERGE INTO user_locale (sheet, user_name, locale) KEY (sheet, user_name) VALUES (?, ?, ?)", sheet,
                user, locale);
    }
}
