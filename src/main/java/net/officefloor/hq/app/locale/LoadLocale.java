package net.officefloor.hq.app.locale;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

public class LoadLocale {
    public void load(@Val Sheet sheet, @RequestParam("user") String user, LocaleRepository repository,
            Out<UserLocale> locale) {
        locale.set(new UserLocale(user, repository.find(sheet.id(), user)));
    }
}
