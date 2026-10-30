package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Cancels the person's last statement; only a steward cancels, and only a person with a statement.
 * The claims it bound become live again (a figure an earlier statement bound is not restored), the
 * statement count goes down by one and the person shows the statement before it, or none.
 */
public class CancelStatement {
    public void cancel(@Val StatementRequest request, @Val Person person, Claims claims, People people,
            Settings settings, Out<CancelledStatement> cancelled) throws ViewerRefusedException, InvalidPersonException {
        String sheetId = person.getSheetId();
        if (!Settings.STEWARD.equals(settings.role(sheetId, request.user()))) {
            throw new ViewerRefusedException("Only a steward can cancel a statement");
        }
        if (person.getStatementCount() <= 0) {
            throw new InvalidPersonException("The person has no statement to cancel");
        }
        BigDecimal dueTotal = person.getStatedTotal();
        for (Claim claim : claims.ofPerson(sheetId, person.getId())) {
            if (claim.isStated() || claim.getStatedDue() != null) {
                claim.setStated(false);
                claim.setStatedDue(null);
                claims.bind(claim);
            }
        }
        people.cancelStatement(person);
        cancelled.set(new CancelledStatement(dueTotal));
    }
}
