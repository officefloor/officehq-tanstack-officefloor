package net.officefloor.hq.app.person;

import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.Sheet;

/**
 * A person just transferred: the person as they were on this sheet, the sheet they went to, and that
 * sheet's figures before they arrived (so its rollup lines tell which moved), and the person there they
 * were merged into (null: none).
 */
public record Transfer(Person person, Sheet to, Rollup toBefore, Person mergedInto) {
}
