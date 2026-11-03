import { SheetBody } from '../../slots/defs/sheetBody';
import { counts, useClaims, useTagsUsed, type Claim } from '../claims/useClaims';
import { claimDueNow } from '../overdue/useOverdue';
import { gradeOn, holdsById, usePeople, type Person } from '../people/usePeople';
import { rateFor, sumDues, useRates } from '../rates/useRates';
import { useSettings } from '../settings/useSettings';
import { useSheetToday } from '../today/useSheetToday';

/** Whether a claim or its person carries a tag. */
function carries(c: Claim, people: Person[] | undefined, tag: string): boolean {
  const person = people?.find((p) => p.id === c.personId);
  return (c.tags ?? '').split(',').includes(tag) || (person?.tags ?? '').split(',').includes(tag);
}

/** Per tag ever used on the sheet, its due-total: the dues of the claims that count (as in a person's
 *  due-total) which carry it, or whose person carries it, added up once each, skipping 'n/a' (a claim
 *  with two tags counts in both). */
function TagTotals({ sheetId }: { sheetId: string }) {
  const tags = useTagsUsed(sheetId);
  const claims = useClaims(sheetId);
  const people = usePeople(sheetId);
  const rates = useRates(sheetId);
  const settings = useSettings(sheetId).data;
  const today = useSheetToday(sheetId).data?.today;
  const holds = holdsById(people.data, today);
  const dues = new Map<number, string>();
  for (const c of claims.data ?? []) {
    const person = people.data?.find((p) => p.id === c.personId);
    if ((c.tags === null && !person?.tags) || !counts(c, holds, today)) continue;
    const grade = gradeOn(person, c.date);
    dues.set(c.id, claimDueNow(c, claims.data, rateFor(rates.data, grade, c.date, c.added), settings, today, grade));
  }
  return (
    <section data-testid="tag-totals">
      <table>
        <thead>
          <tr>
            <th>Tag</th>
            <th>Due total</th>
          </tr>
        </thead>
        <tbody>
          {(tags.data ?? []).map((t) => (
            <tr key={t}>
              <td>{t}</td>
              <td data-testid={`tag-${t}-due-total`}>
                {sumDues((claims.data ?? []).filter((c) => dues.has(c.id) && carries(c, people.data, t)).map((c) => dues.get(c.id)!))}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 41, Component: ({ sheetId }) => <TagTotals sheetId={sheetId} /> });
