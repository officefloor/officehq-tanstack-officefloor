package net.officefloor.hq.app.statement;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Val;

/** Writes STATEMENT_CANCELLED to the audit file once the statement is removed. Then writes the figures the change
 *  moved (see Rollup). */
public class RecordStatementCancelled {
    public void record(@Val Statement statement, @Val Person person, @Val RollupBefore before, Rollup rollup, Audit audit) {
        audit.record("STATEMENT_CANCELLED sheet=" + statement.sheetId() + " by=" + statement.by() + " name=" + person.name()
                + " due-total=" + statement.dueTotal());
        rollup.record(statement.sheetId(), statement.by(), before);
    }
}
