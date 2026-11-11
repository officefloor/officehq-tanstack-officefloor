package net.officefloor.hq.app.locale;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Each user's locale, kept per user (not per sheet); en for a user who never chose one. */
@Repository
public class LocaleRepository {

    public static final String DEFAULT = "en";

    private final JdbcTemplate jdbc;

    public LocaleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The locale {@code user} chose, or en. */
    public String localeOf(String user) {
        List<String> found = jdbc.queryForList("SELECT locale FROM user_locale WHERE user_name = ?", String.class, user);
        return found.isEmpty() ? DEFAULT : found.get(0);
    }

    /** {@code user} chooses {@code locale}. */
    public void save(String user, String locale) {
        jdbc.update("MERGE INTO user_locale (user_name, locale) KEY (user_name) VALUES (?, ?)", user, locale);
    }
}
