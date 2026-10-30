package net.officefloor.hq.app.user;

import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Each user's locale, the way figures are shown to them; per user, not per sheet. */
@Repository
public class UserLocales {

    /** The locales a user may choose; the first is the one a user has until they choose. */
    public static final List<String> LOCALES = List.of("en", "de");

    private final JdbcTemplate jdbc;

    public UserLocales(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public static boolean isKnown(String locale) {
        return Set.copyOf(LOCALES).contains(locale);
    }

    public String localeOf(String user) {
        List<String> found = jdbc.queryForList("SELECT locale FROM user_locale WHERE user_name = ?", String.class, user);
        return found.isEmpty() ? LOCALES.get(0) : found.get(0);
    }

    public void setLocale(String user, String locale) {
        jdbc.update("MERGE INTO user_locale (user_name, locale) KEY (user_name) VALUES (?, ?)", user, locale);
    }
}
