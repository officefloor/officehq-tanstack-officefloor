import { useState } from 'react';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { useClaims, type Claim } from '../claims/claims';
import { usePeople, type Person } from '../people/people';

/** The duplicate claims: for each person, date and amount with two or more claims (VOID and DELETED ones left out),
 *  one line `<name> <date> <amount> x<count>`, people in list order, then by date, then by amount. */
export function duplicateLines(people: Person[], claims: Claim[]): string[] {
  const lines: string[] = [];
  for (const p of people) {
    const groups = new Map<string, { date: string; amount: string; count: number }>();
    for (const c of claims) {
      if (c.person !== p.id || c.status === 'VOID' || c.status === 'DELETED') continue;
      const key = `${c.date} ${c.amount}`;
      const g = groups.get(key);
      if (g) g.count++;
      else groups.set(key, { date: c.date, amount: c.amount, count: 1 });
    }
    [...groups.values()]
      .filter((g) => g.count >= 2)
      .sort((a, b) => a.date.localeCompare(b.date) || Number(a.amount) - Number(b.amount))
      .forEach((g) => lines.push(`${p.name} ${g.date} ${g.amount} x${g.count}`));
  }
  return lines;
}

// The sheet's duplicate claims, figured from the people and claims as the screen holds them at each press.
function Duplicates({ sheet }: { sheet: string; user: string }) {
  const [shown, setShown] = useState(false);
  const { data: people = [] } = usePeople(sheet);
  const { data: claims = [] } = useClaims(sheet);
  const lines = duplicateLines(people, claims);

  return (
    <section>
      <h2>Duplicate claims</h2>
      <button type="button" data-testid="dups-show" onClick={() => setShown(true)}>
        Show
      </button>
      {shown && (
        <ul>
          {lines.map((line) => (
            <li key={line} data-testid="dup-line">
              <code>{line}</code>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 102, Component: Duplicates });
