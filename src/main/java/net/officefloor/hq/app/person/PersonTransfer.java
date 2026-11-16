package net.officefloor.hq.app.person;

import net.officefloor.hq.app.rollup.RollupBefore;

/** A transfer once checked: the person transferred (who leaves their sheet), the sheet they go to, who transfers them,
 *  the other sheet's figures before it (the sheet they leave takes its own, see TakeRollup), and the person there they
 *  merge into (null when no one there has one of their names, so they arrive as themselves). */
public record PersonTransfer(Person person, String toSheet, String by, RollupBefore toBefore, Person into) {
}
