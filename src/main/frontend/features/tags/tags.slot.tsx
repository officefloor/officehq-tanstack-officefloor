import { useQuery } from '@tanstack/react-query';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { claimsQuery, counts, tagsQuery } from '../claims/claimsQuery';
import { peopleQuery } from '../people/peopleQuery';
import { figure, ratesQuery, sumDue } from '../rates/ratesQuery';
import { useFloor, useLate } from '../settings/settingsQuery';
import { useSheetToday } from '../sheet/useSheetToday';

/**
 * A due-total per tag, for every tag ever used on the sheet: the figures of the claims that count which carry it, added
 * up (n/a skipped; not capped). A claim with two tags counts in both.
 */
function TagTotals({ sheetId }: { sheetId: string }) {
  const tags = useQuery(tagsQuery(sheetId)).data ?? [];
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  const claims = useQuery(claimsQuery(sheetId)).data ?? [];
  const rates = useQuery(ratesQuery(sheetId)).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const counting = claims.filter((c) => counts(c, people, today));

  return (
    <table data-testid="tag-totals">
      <thead>
        <tr>
          <th>Tag</th>
          <th>Due</th>
        </tr>
      </thead>
      <tbody>
        {tags.map((t) => (
          <tr key={t} data-testid={`tag-${t}`}>
            <td>{t}</td>
            <td data-testid={`tag-${t}-due-total`}>
              {sumDue(counting
                .filter((c) => c.tags.includes(t))
                .map((c) => figure(c, rates, people.find((p) => p.id === c.personId), floor, late)))}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

export const contribution = SheetPanel.fill({ order: 26, Component: TagTotals });
