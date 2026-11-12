package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateGenerate {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody GenerateRequest request,
            SheetStore sheets, Out<GenerateInput> body, RoleStore roles) throws InvalidSheetException {
        roles.refuseChange(sheetId.trim(), request == null ? null : request.user());
        if (request == null) {
            throw new InvalidSheetException("A from and to date are required");
        }
        LocalDate from = date(sheetId, request.from(), sheets);
        LocalDate to = date(sheetId, request.to(), sheets);
        if (to.isBefore(from)) {
            throw new InvalidSheetException("The to date must not be before the from date");
        }
        body.set(new GenerateInput(YearMonth.from(from), YearMonth.from(to), request.user()));
    }

    private static LocalDate date(String sheetId, String value, SheetStore sheets) throws InvalidSheetException {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            return sheets.today(sheetId.trim());
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidSheetException("Not a date: " + text);
        }
    }
}
