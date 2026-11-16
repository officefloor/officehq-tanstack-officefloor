package net.officefloor.hq.app.person;

import net.officefloor.hq.app.rollup.RollupBefore;

/** A transfer once checked: the person transferred (who leaves their sheet), the sheet they go to, who transfers them,
 *  and the other sheet's figures before it (the sheet they leave takes its own, see TakeRollup). */
public record PersonTransfer(Person person, String toSheet, String by, RollupBefore toBefore) {
}
