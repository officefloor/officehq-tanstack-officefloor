package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the alias just given (PERSON_ALIAS_ADDED). */
public class RecordAliasAdded {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Person person, @Val Alias alias, Audit audit) {
        audit.record("PERSON_ALIAS_ADDED sheet=" + person.getSheetId() + " by=" + (by == null ? "" : by)
                + " name=" + person.getName() + " alias=" + alias.alias());
    }
}
