package net.officefloor.hq.app.opening;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Add each pasted opening balance, in order, to the person's opening balance for the period (OPENING_SET).
 *  Every line is checked first — the person on the sheet by name or alias (ignoring case), the period on the
 *  sheet by its name — so one refused line refuses the whole paste and nothing is written. */
public class ImportOpenings {
    private record Checked(Person person, Period period, OpeningLine line) {
    }

    public void save(@PathVariable("sheetId") String sheetId, @Val OpeningInput input,
            ServerHttpConnection connection, PersonRepository people, PeriodRepository periods, Audit audit,
            Out<OpeningResult> result) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!people.sheetExists(id)) {
            throw new BadRequestException("no such sheet");
        }
        List<Checked> checked = new ArrayList<>();
        for (OpeningLine line : input.lines()) {
            Person person = people.findByName(id, line.name());
            if (person == null) {
                throw new BadRequestException("line " + line.line() + ": no one on the sheet is named " + line.name());
            }
            Period period = periods.find(id, line.period());
            if (period == null) {
                throw new BadRequestException("line " + line.line() + ": there is no period " + line.period());
            }
            checked.add(new Checked(person, period, line));
        }
        for (Checked c : checked) {
            periods.addOpening(id, c.period().id(), c.person().id(), c.line().amount());
            audit.record("OPENING_SET sheet=" + id + " by=" + by + " name=" + c.person().name() + " period="
                    + c.period().name() + " amount=" + c.line().amount().setScale(2).toPlainString());
        }
        result.set(new OpeningResult(checked.size()));
    }
}
