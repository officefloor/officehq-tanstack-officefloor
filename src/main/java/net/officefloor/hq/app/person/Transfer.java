package net.officefloor.hq.app.person;

import net.officefloor.hq.app.rollup.Rollup;

/** A transfer that went through: the sheet left, the sheet arrived on, the person's name, the other sheet's figures
 *  before the person arrived, and the name of the person on the other sheet they were merged into (null when none). */
public record Transfer(String fromSheet, String toSheet, String name, Rollup toBefore, String mergedInto) {
}
