package net.officefloor.hq.app.user;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The store of each signed-in user's locale, kept per user across every sheet. */
@Repository
public class Locales {

    /** The locales a user may choose; the first is every user's until they choose. */
    public static final List<String> KNOWN = List.of("en", "de");

    private final JdbcTemplate jdbc;

    public Locales(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public String of(String user) {
        List<String> found = jdbc.queryForList("SELECT locale FROM user_locale WHERE user_name = ?", String.class, user);
        return found.isEmpty() ? KNOWN.get(0) : found.get(0);
    }

    public void set(String user, String locale) {
        jdbc.update("MERGE INTO user_locale (user_name, locale) KEY (user_name) VALUES (?, ?)", user, locale);
    }
}
