package net.officefloor.hq.app.locale;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LocaleRepository {

    /** The locales a user may choose; the first is the one a user has until they choose. */
    public static final List<String> LOCALES = List.of("en", "de");

    private final JdbcTemplate jdbc;

    public LocaleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The user's locale; 'en' when they never chose one. */
    public String locale(String user) {
        List<String> found = jdbc.queryForList("SELECT locale FROM user_locale WHERE user_name = ?", String.class, user);
        return found.isEmpty() ? LOCALES.get(0) : found.get(0);
    }

    /** Keep the user's locale. */
    public void setLocale(String user, String locale) {
        jdbc.update("MERGE INTO user_locale (user_name, locale) KEY (user_name) VALUES (?, ?)", user, locale);
    }
}
