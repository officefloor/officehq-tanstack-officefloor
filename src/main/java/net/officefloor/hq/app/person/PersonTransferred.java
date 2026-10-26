package net.officefloor.hq.app.person;

import net.officefloor.hq.app.rollup.RollupSnapshot;

/**
 * The transfer just made: the person's name, the sheet they went to, that sheet's figures before they arrived and the
 * name of the person there they were merged into (null when none).
 */
public record PersonTransferred(String name, String to, RollupSnapshot toBefore, String mergedInto) {
}
