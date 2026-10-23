package net.officefloor.hq.app.locale;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Each user's chosen locale, per user (not per sheet). */
@Repository
public class LocaleRepository {

    /** The locale of a user who never chose one. */
    public static final String DEFAULT = "en";

    /** The locales a user may choose. */
    public static final List<String> LOCALES = List.of("en", "de");

    private final JdbcTemplate jdbc;

    public LocaleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public String localeOf(String user) {
        List<String> found = jdbc.queryForList("SELECT locale FROM user_locale WHERE user_name = ?", String.class, user);
        return found.isEmpty() ? DEFAULT : found.get(0);
    }

    public void save(String user, String locale) {
        jdbc.update("MERGE INTO user_locale (user_name, locale) KEY (user_name) VALUES (?, ?)", user, locale);
    }
}
