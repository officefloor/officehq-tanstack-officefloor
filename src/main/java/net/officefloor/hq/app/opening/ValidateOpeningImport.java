package net.officefloor.hq.app.opening;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.period.PeriodResponse;
import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Checks every pasted line before anything is stored. Each line is name,period,amount: the name (or an alias)
 * of a person on the sheet (ignoring case and the spaces around it), the name of a period on the sheet
 * (ignoring case) and an amount of two decimals, 0.00 or more. Blank lines are skipped; a paste with no line
 * is refused.
 */
public class ValidateOpeningImport {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody OpeningImportRequest request,
            PersonStore people, PeriodStore periods, RoleStore roles, Out<OpeningImportInput> imported)
            throws InvalidSheetException {
        String sheet = sheetId.trim();
        String user = request == null ? null : request.user();
        roles.refuseChange(sheet, user);
        String text = request == null || request.text() == null ? "" : request.text();
        List<PeriodResponse> onSheet = periods.list(sheet);
        String[] lines = text.split("\\r?\\n");
        List<OpeningLine> openings = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] fields = line.split(",", -1);
            if (fields.length != 3) {
                throw new InvalidSheetException("Line " + (i + 1) + " must be name,period,amount");
            }
            Person person = people.findByName(sheet, fields[0]);
            if (person == null) {
                throw new InvalidSheetException("Line " + (i + 1) + ": No such person: " + fields[0].trim());
            }
            String periodName = fields[1].trim();
            PeriodResponse period = onSheet.stream().filter(p -> p.name().equalsIgnoreCase(periodName)).findFirst()
                    .orElse(null);
            if (period == null) {
                throw new InvalidSheetException("Line " + (i + 1) + ": No such period: " + periodName);
            }
            String amount = fields[2].trim();
            if (!amount.matches("\\d+\\.\\d{2}")) {
                throw new InvalidSheetException("Line " + (i + 1) + ": The amount must be 0.00 or more, with two decimals");
            }
            openings.add(new OpeningLine(person, period.name(), new BigDecimal(amount)));
        }
        if (openings.isEmpty()) {
            throw new InvalidSheetException("Paste at least one line of name,period,amount");
        }
        imported.set(new OpeningImportInput(openings, user));
    }
}
