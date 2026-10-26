package net.officefloor.hq.app.quickadd;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.claim.ClaimAmount;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.person.PersonName;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * The quick-add bar's fields, under the person and claim forms' rules. Publishes the person it names (grade A, should
 * they be new) and a claim of the amount dated the sheet's today with an empty note, with the user making the change.
 */
public class ValidateQuickAdd {

    static final String GRADE = "A";

    public void validate(@RequestBody QuickAddRequest request, Out<PersonRequest> person, Out<ClaimInput> claim,
            Out<ChangedBy> changedBy) throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String name = PersonName.of(request == null ? null : request.name());
        claim.set(new ClaimInput(ClaimAmount.of(request == null ? null : request.amount()), null, ""));
        person.set(new PersonRequest(name, GRADE, by.name(), null, null, null, null));
        changedBy.set(by);
    }
}
