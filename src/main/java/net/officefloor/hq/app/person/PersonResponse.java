package net.officefloor.hq.app.person;

/** A person as the screen shows them, with their statement count and the last statement's due-total (two
 *  decimals; null when n/a, or none taken) and date (yyyy-mm-dd; null when none taken). */
public record PersonResponse(long id, String name, String grade, long version, int statementCount,
        String statedTotal, String statementDate) {
    public static PersonResponse from(Person p) {
        return new PersonResponse(p.id(), p.name(), p.grade(), p.version(), p.statementCount(),
                p.statedTotal() != null ? p.statedTotal().toPlainString() : null,
                p.statementDate() != null ? p.statementDate().toString() : null);
    }
}
