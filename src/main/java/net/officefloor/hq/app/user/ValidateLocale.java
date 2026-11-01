package net.officefloor.hq.app.user;

import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateLocale {
    public void validate(@PathVariable("user") String user, @RequestBody LocaleRequest request, Out<LocaleResponse> body)
            throws InvalidSheetRequestException {
        if (user == null || user.isBlank())
            throw new InvalidSheetRequestException("A locale needs the signed-in user");
        String locale = request == null || request.locale() == null ? "" : request.locale().trim();
        if (!Locales.KNOWN.contains(locale))
            throw new InvalidSheetRequestException("A locale is one of " + String.join(", ", Locales.KNOWN));
        body.set(new LocaleResponse(user.trim(), locale));
    }
}
