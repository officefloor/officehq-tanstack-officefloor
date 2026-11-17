package net.officefloor.hq.app.person;

import java.util.List;

/** The person's own terms as they were before an edit: those in force on the sheet's today (null: none), and all those
 *  recorded (see Person#ownTerms); {@code given} is whether the edit recorded terms (gave a from-date, or terms other
 *  than those in force), even ones equal to those it replaced. */
public record TermsBefore(Integer days, List<PersonTerms> recorded, boolean given) {

    public TermsBefore(Integer days, List<PersonTerms> recorded) {
        this(days, recorded, false);
    }
}
