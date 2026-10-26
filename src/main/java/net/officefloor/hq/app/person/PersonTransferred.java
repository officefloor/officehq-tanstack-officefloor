package net.officefloor.hq.app.person;

import net.officefloor.hq.app.rollup.RollupSnapshot;

/** The transfer just made: the person's name, the sheet they went to and that sheet's figures before they arrived. */
public record PersonTransferred(String name, String to, RollupSnapshot toBefore) {
}
