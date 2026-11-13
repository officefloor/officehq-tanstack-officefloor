import { counts, useClaims } from '../claims/claims';
import { liveHolds, usePeople } from '../people/people';
import { claimDue, total, useRates } from '../rates/rates';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { useDueRules } from '../settings/settings';

// For every calendar month (yyyy-mm) any claim on the sheet is dated in, the due-total of the claims dated in it that
// count: their dues added up, skipping n/a — not capped, not trimmed.
export function MonthTotals() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople().data ?? [];
  const claims = useClaims().data ?? [];
  const rates = useRates().data ?? [];
  const rules = useDueRules();

  const months = [...new Set(claims.map((c) => c.date.slice(0, 7)))].sort();
  if (!user || !sheet || months.length === 0) return null;

  const dues = new Map<string, string[]>(months.map((m) => [m, []]));
  for (const p of people) {
    const holds = liveHolds(p, rules.today);
    for (const c of claims) {
      if (c.personId !== p.id || !counts(c, holds, rules.today)) continue;
      dues.get(c.date.slice(0, 7))?.push(claimDue(c, rates, p.grades, rules));
    }
  }
  return (
    <ul>
      {months.map((m) => (
        <li key={m}>
          Month {m} due total: <strong data-testid={`month-${m}-due-total`}>{total(dues.get(m) ?? [])}</strong>
        </li>
      ))}
    </ul>
  );
}
