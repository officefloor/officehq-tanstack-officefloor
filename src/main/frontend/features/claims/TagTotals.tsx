import { useQuery } from '@tanstack/react-query';
import { gradeOn, listPeople, peopleKey } from '../people/api';
import { claimDue, listRates, ratesKey, sumDues } from '../rates/api';
import { useFloor, useLate } from '../settings/api';
import { useSheetToday } from '../sheet/api';
import { claimsKey, counts, listClaims, listTags, liveClaims, tagsKey, tagsOf, type Claim } from './api';
import { Fig } from '../session/Fig';

/** For every tag ever used on the sheet, its due-total: the due figures of the claims that count (the
 *  live ones, OPEN, PENDING or SETTLED) carrying it, added up (n/a ones skipped); a claim with two tags
 *  counts in both, and a claim counts under its person's tags too (once for a tag both carry). */
export function TagTotals({ sheetId, user }: { sheetId: string; user: string }) {
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  const used = useQuery({ queryKey: tagsKey(sheetId), queryFn: () => listTags(sheetId) });
  const rates = useQuery({ queryKey: ratesKey(sheetId), queryFn: () => listRates(sheetId) });
  const floor = useFloor(sheetId);
  const today = useSheetToday(sheetId, user);
  const charges = useLate(sheetId, today);
  const byId = new Map((people.data ?? []).map((p) => [p.id, p]));
  const live = liveClaims(claims.data ?? [], people.data).filter((c) => counts(c) && byId.has(c.personId));
  const carried = (c: Claim) => [...tagsOf(c), ...tagsOf(byId.get(c.personId) ?? { tags: null })];
  const tags = [...new Set([...(used.data ?? []), ...(claims.data ?? []).flatMap(tagsOf),
    ...(people.data ?? []).flatMap(tagsOf)])].sort();
  if (tags.length === 0) return null;
  const dueOf = (tag: string) => sumDues(live.filter((c) => carried(c).includes(tag)).map((c) => {
    const p = byId.get(c.personId)!;
    return claimDue(c, rates.data, (d) => gradeOn(p, d), floor, charges);
  }));
  return (
    <p>
      Due by tag{' '}
      {tags.map((t) => (
        <span key={t}>
          {t} <span data-testid={`tag-${t}-due-total`}><Fig>{dueOf(t)}</Fig></span>{' '}
        </span>
      ))}
    </p>
  );
}
