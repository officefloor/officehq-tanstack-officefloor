package net.officefloor.hq.app.person;

import net.officefloor.hq.app.rollup.Rollup;

/** A person transferred: the sheet they went to, their copy there (or the person they merged into, when
 *  {@code merged}), and that sheet's figures before they arrived. */
public record Transferred(String to, Person person, Rollup toBefore, boolean merged) {
}
