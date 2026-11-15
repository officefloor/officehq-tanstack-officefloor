package net.officefloor.hq.app.person;

import net.officefloor.hq.app.rollup.Rollup;

/** A transfer just made: the person as they stood on the sheet they left, the sheet they went to,
 *  and that sheet's figures before they arrived, for its audit lines. */
public record Transfer(Person person, String to, Rollup toBefore) {
}
