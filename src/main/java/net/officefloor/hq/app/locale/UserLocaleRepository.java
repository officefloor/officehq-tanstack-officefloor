package net.officefloor.hq.app.locale;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLocaleRepository extends JpaRepository<UserLocale, String> {

    /** The user name's locale: en unless one is saved. */
    default String localeOf(String userName) {
        return findById(userName).map(UserLocale::getLocale).orElse(UserLocale.EN);
    }
}
