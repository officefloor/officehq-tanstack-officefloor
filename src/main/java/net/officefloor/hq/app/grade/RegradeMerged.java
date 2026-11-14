package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;

/**
 * Makes the merge retroactive: every grade record of each person it moved that is on the merged grade becomes the
 * other grade, so all their claims, whatever their date, use the other grade from then on.
 */
public class RegradeMerged {
    public void regrade(@Val GradeMerge merge, @Val GradeMergeMoved moved, PersonRepository people) {
        for (Long id : moved.people()) {
            people.regrade(merge.sheetId(), id, merge.from(), merge.into());
        }
    }
}
