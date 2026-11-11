package net.officefloor.hq.app.locale;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.web.ObjectResponse;

/** The signed-in user's locale. */
public class RespondWithLocale {
    public void respond(LocaleRepository locales, HttpServletRequest request, ObjectResponse<LocaleResponse> response) {
        response.send(new LocaleResponse(locales.localeOf(Audit.user(request))));
    }
}
