package net.officefloor.hq.app.person;

import net.officefloor.hq.app.rollup.Snapshot;

/** A person just transferred from one sheet to another, by whom, and the other sheet's figures as
 *  they were before the person arrived. */
public record Transfer(String name, String from, String to, String user, Snapshot toBefore) {
}
