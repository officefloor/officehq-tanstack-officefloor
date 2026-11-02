package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Sets the person's approver, replacing any they had: that user may approve or reject their PENDING claims. */
public class SetApprover {
    public void apply(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Person person,
            @Val Approver approver, People people, Audit audit) {
        people.setApprover(person.id(), approver.user().isEmpty() ? null : approver.user());
        audit.record("APPROVER_SET sheet=" + sheet + " by=" + user + " name=" + person.name() + " user="
                + approver.user());
    }
}
