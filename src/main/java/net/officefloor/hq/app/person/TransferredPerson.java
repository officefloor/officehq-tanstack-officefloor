package net.officefloor.hq.app.person;

import net.officefloor.hq.app.rollup.Figures;

/**
 * A transfer done: the person transferred ({@code from}, now gone from their sheet), the sheet they went to, and that
 * sheet's figures before they arrived (for its ROLLUP and TOTALS lines), and the person there they were merged into
 * ({@code null} when they arrived as themselves).
 */
public record TransferredPerson(Person from, String toSheetId, Figures toBefore, Person into) {
}
