import { SheetPanels } from '../../slots/defs/sheetPanels';
import { claimDueCents, live, useClaims } from '../claims/useClaims';
import { gradeOn, heldOnBy, usePeople } from '../people/usePeople';
import { rateOn, totalDue, useRates } from '../rates/useRates';
import { useFloor, useLate } from '../settings/useSettings';
import { useSheetToday } from '../sheet/useSheet';

/**
 * A due-total per tag, for every tag ever used on the sheet: the dues of the claims that count
 * (as the sheet's totals count them) which carry it, each as its claim shows it, added up (n/a
 * ones skipped). A claim with two tags counts in both.
 */
function TagsPanel({ sheet }: { user: string; sheet: string }) {
  const { data: all = [] } = useClaims(sheet);
  const { data: people = [] } = usePeople(sheet);
  const { data: rates = [] } = useRates(sheet);
  const today = useSheetToday(sheet);
  const floor = useFloor(sheet);
  const late = useLate(sheet);
  const byId = new Map(people.map((p) => [p.id, p]));
  const claims = live(all, heldOnBy(people, today));
  const tags = [...new Set(all.flatMap((c) => c.tagsUsed))].sort();
  const dueOf = (c: (typeof claims)[number]) => {
    const p = byId.get(c.personId);
    return claimDueCents(c, floor, p ? rateOn(rates, gradeOn(p, c.date), c.date) : undefined, late);
  };

  return (
    <section data-testid="tags-panel">
      <h2>Tags</h2>
      <table>
        <thead>
          <tr>
            <th>Tag</th>
            <th>Due</th>
          </tr>
        </thead>
        <tbody>
          {tags.map((t) => (
            <tr key={t}>
              <td>{t}</td>
              <td data-testid={`tag-${t}-due-total`}>{totalDue(claims.filter((c) => c.tags.includes(t)).map(dueOf))}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 26, Component: TagsPanel });
