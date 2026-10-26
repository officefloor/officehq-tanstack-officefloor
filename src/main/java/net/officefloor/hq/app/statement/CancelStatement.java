package net.officefloor.hq.app.statement;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class CancelStatement {
    // Only a steward cancels a person's last statement. Every claim it bound is live again: a statement
    // rebinds all of the person's counting claims, so the figures an earlier statement bound are gone and
    // are not restored.
    public void cancel(@Val Person person, Claims claims, Settings settings, Statements statements,
            ServerHttpConnection connection, Out<CancelledStatement> cancelledOut) throws StatementRefusedException {
        if (!Settings.STEWARD.equals(settings.role(person.sheetId(), Audit.user(connection).trim()))) {
            throw new StatementRefusedException("Only a steward may cancel a statement.");
        }
        statements.lock(person.id());
        CancelledStatement cancelled = statements.removeLast(person.sheetId(), person.id());
        if (cancelled == null) {
            throw new StatementRefusedException(person.name() + " has no statement to cancel.");
        }
        claims.bind(person.sheetId(), person.id(), java.util.Map.of());
        cancelledOut.set(cancelled);
    }
}
