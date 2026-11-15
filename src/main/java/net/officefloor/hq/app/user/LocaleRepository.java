package net.officefloor.hq.app.user;

import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Each user's locale: en unless they chose another. */
@Repository
public class LocaleRepository {

    /** The locales a user may choose. */
    public static final Set<String> LOCALES = Set.of("en", "de");

    private final JdbcTemplate jdbc;

    public LocaleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The user's locale; en when never chosen. */
    public String localeOf(String user) {
        List<String> found = jdbc.queryForList("SELECT locale FROM user_locale WHERE user_name = ?", String.class, user);
        return found.isEmpty() ? "en" : found.get(0);
    }

    public void set(String user, String locale) {
        jdbc.update("MERGE INTO user_locale (user_name, locale) KEY (user_name) VALUES (?, ?)", user, locale);
    }
}
