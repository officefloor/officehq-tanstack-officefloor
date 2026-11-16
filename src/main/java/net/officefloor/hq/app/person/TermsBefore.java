package net.officefloor.hq.app.person;

import java.util.List;

/** The person's own terms as they were before an edit: those in force on the sheet's today (null: none), and all those
 *  recorded (see Person#ownTerms). */
public record TermsBefore(Integer days, List<PersonTerms> recorded) {
}
