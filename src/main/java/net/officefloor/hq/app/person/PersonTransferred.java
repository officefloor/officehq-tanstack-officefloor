package net.officefloor.hq.app.person;

import net.officefloor.hq.app.rollup.Rollup;

/** A transfer done: the person's name, the sheet they left, the sheet they arrived on, and that sheet's figures
 *  before they arrived. */
public record PersonTransferred(String name, String fromSheet, String toSheet, Rollup toBefore) {
}
