package net.officefloor.hq.app.history;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestParam;

/** The date a read shows the sheet as of (the {@code asof} parameter; none: as it stands now). Publishes it. */
public class ValidateViewAsOf {
    public void validate(@RequestParam(value = "asof", required = false) String asof, Out<ViewAsOf> view)
            throws BadRequestException {
        String text = asof == null ? "" : asof.trim();
        try {
            view.set(new ViewAsOf(text.isEmpty() ? null : LocalDate.parse(text)));
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Enter an as-of date as yyyy-mm-dd");
        }
    }
}
