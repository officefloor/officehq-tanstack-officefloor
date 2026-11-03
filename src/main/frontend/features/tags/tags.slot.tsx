import { SheetBody } from '../../slots/defs/sheetBody';
import { counts, useClaims, useTagsUsed } from '../claims/useClaims';
import { claimDueNow } from '../overdue/useOverdue';
import { gradeOn, holdsById, usePeople } from '../people/usePeople';
import { rateFor, sumDues, useRates } from '../rates/useRates';
import { useSettings } from '../settings/useSettings';
import { useSheetToday } from '../today/useSheetToday';

/** Per tag ever used on the sheet, its due-total: the dues of the claims that count (as in a person's
 *  due-total) which carry it added up, skipping 'n/a' (a claim with two tags counts in both). */
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
    if (c.tags === null || !counts(c, holds, today)) continue;
    const person = people.data?.find((p) => p.id === c.personId);
    const grade = gradeOn(person, c.date);
    dues.set(c.id, claimDueNow(c, claims.data, rateFor(rates.data, grade, c.date), settings, today, grade));
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
                {sumDues((claims.data ?? []).filter((c) => dues.has(c.id) && c.tags!.split(',').includes(t)).map((c) => dues.get(c.id)!))}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 41, Component: ({ sheetId }) => <TagTotals sheetId={sheetId} /> });
