import { SheetPage } from '../../slots/defs/sheetPage';
import { live, useClaims } from '../claims/claims';
import { gradeOn, usePeople } from '../people/people';
import { claimDue, rateFor, sum, useRates } from '../rates/rates';
import { useFloor, useLate } from '../settings/settings';
import { useTags } from './tags';

/** For every tag ever used on the sheet, its due-total: the figures (see claimDue) of the claims that count in the
 *  live totals (see live) which carry it, added up, skipping n/a; a claim with two tags counts in both. */
function TagTotals({ sheetId }: { sheetId: string }) {
  const tags = useTags(sheetId);
  const claims = useClaims(sheetId);
  const people = usePeople(sheetId);
  const rates = useRates(sheetId);
  const late = useLate(sheetId);
  const floor = useFloor(sheetId);
  const byId = new Map((people.data ?? []).map((p) => [p.id, p]));
  const figures = live(claims.data ?? [], people.data, late.today).flatMap((c) => {
    const person = byId.get(c.personId);
    if (person === undefined) return [];
    return [{ tags: c.tags, due: claimDue(c, rateFor(rates.data, gradeOn(person, c.date), c.date), floor, late) }];
  });
  return (
    <section data-testid="tag-totals">
      <h3>Due by tag</h3>
      <table>
        <tbody>
          {(tags.data ?? []).map((t) => (
            <tr key={t}>
              <td>{t}</td>
              <td data-testid={`tag-${t}-due-total`}>{sum(figures.filter((f) => f.tags.includes(t)).map((f) => f.due))}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {tags.isError && <p data-testid="tags-load-error">Could not load tags.</p>}
    </section>
  );
}

export const contribution = SheetPage.fill({ order: 31, Component: TagTotals });
