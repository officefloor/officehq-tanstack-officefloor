package net.officefloor.hq.app.locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A user name's locale: en or de. A name with none is en. */
@Entity
@Table(name = "user_locale")
public class UserLocale {

    public static final String EN = "en";

    public static final String DE = "de";

    @Id
    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(nullable = false)
    private String locale;

    protected UserLocale() {
    }

    public UserLocale(String userName, String locale) {
        this.userName = userName;
        this.locale = locale;
    }

    public String getUserName() {
        return userName;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }
}
