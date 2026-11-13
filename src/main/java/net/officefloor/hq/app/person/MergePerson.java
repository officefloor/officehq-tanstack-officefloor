package net.officefloor.hq.app.person;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.plugin.variable.Val;

/**
 * Merges the person into the other: their claims move to the other (numbers and dates kept), listed after the
 * other's in the order they were added, each keeping any figure a statement bound; then their name, aliases, patrons,
 * pool memberships and statements pass to the other (see {@link PersonRepository#merge}) and they leave the sheet.
 */
public class MergePerson {
    public void merge(@Val Merge merge, ClaimRepository claims, PersonRepository people) {
        List<Claim> moving = new ArrayList<>(claims.findForPerson(merge.from().sheetId(), merge.from().id()));
        Collections.reverse(moving);
        for (Claim claim : moving) {
            claims.moveMerged(claim, merge.into().id());
        }
        people.merge(merge.from(), merge.into());
    }
}
