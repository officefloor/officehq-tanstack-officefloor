package net.officefloor.hq.app.person;

import net.officefloor.hq.app.rollup.Rollup;

/** A transfer just made: the person as they stood on the sheet they left, the sheet they went to,
 *  that sheet's figures before they arrived, and the person there they were merged into (null: none),
 *  for its audit lines. */
public record Transfer(Person person, String to, Rollup toBefore, Person into) {
}
