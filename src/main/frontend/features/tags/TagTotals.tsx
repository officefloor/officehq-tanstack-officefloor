import { counts, useClaims } from '../claims/claims';
import { liveHolds, usePeople } from '../people/people';
import { claimDue, total, useRates } from '../rates/rates';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { useDueRules } from '../settings/settings';
import { useTags } from './tags';

// For every tag ever used on the sheet, the due-total of the claims that count which carry it, or whose person
// carries it (once, when both do; a claim with two tags counts in both): their dues added up, skipping n/a — not
// capped, not trimmed.
export function TagTotals() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const tags = useTags().data ?? [];
  const people = usePeople().data ?? [];
  const claims = useClaims().data ?? [];
  const rates = useRates().data ?? [];
  const rules = useDueRules();

  if (!user || !sheet || tags.length === 0) return null;

  const dues = new Map<string, string[]>(tags.map((t) => [t, []]));
  for (const p of people) {
    const holds = liveHolds(p, rules.today);
    for (const c of claims) {
      if (c.personId !== p.id) continue;
      const carried = new Set([...c.tags, ...p.tags]);
      if (carried.size === 0 || !counts(c, holds, rules.today)) continue;
      const due = claimDue(c, rates, p.grades, rules);
      for (const t of carried) dues.get(t)?.push(due);
    }
  }
  return (
    <ul>
      {tags.map((t) => (
        <li key={t}>
          Tag {t} due total: <strong data-testid={`tag-${t}-due-total`}>{total(dues.get(t) ?? [])}</strong>
        </li>
      ))}
    </ul>
  );
}
