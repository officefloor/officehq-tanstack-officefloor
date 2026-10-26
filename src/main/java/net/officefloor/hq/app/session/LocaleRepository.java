package net.officefloor.hq.app.session;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Each user's locale: en unless the user chose another. */
@Repository
public class LocaleRepository {

    public static final String DEFAULT = "en";

    private final JdbcTemplate jdbc;

    public LocaleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public String locale(String user) {
        return jdbc.query("SELECT locale FROM user_locale WHERE user_name = ?",
                rs -> rs.next() ? rs.getString(1) : DEFAULT, user);
    }

    public void setLocale(String user, String locale) {
        jdbc.update("MERGE INTO user_locale (user_name, locale) KEY (user_name) VALUES (?, ?)", user, locale);
    }
}
