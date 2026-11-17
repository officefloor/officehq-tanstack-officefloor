import { useState } from 'react';
import { SheetPage } from '../../slots/defs/sheetPage';
import { useClaims, type Claim } from '../claims/claims';
import { usePeople, type Person } from '../people/people';

/** The duplicate claims: for each person, date and amount with two or more claims (VOID and DELETED claims left out),
 *  one line <name> <date> <amount> x<count>; people in list order, then by date, then by amount. */
export function duplicates(people: Person[], claims: Claim[]): string[] {
  const lines: string[] = [];
  for (const p of people) {
    const groups = new Map<string, { date: string; amount: string; count: number }>();
    for (const c of claims) {
      if (c.personId !== p.id || c.status === 'VOID' || c.status === 'DELETED') continue;
      const key = `${c.date} ${c.amount}`;
      const g = groups.get(key);
      if (g) g.count++;
      else groups.set(key, { date: c.date, amount: c.amount, count: 1 });
    }
    [...groups.values()]
      .filter((g) => g.count >= 2)
      .sort((a, b) => (a.date !== b.date ? (a.date < b.date ? -1 : 1) : Number(a.amount) - Number(b.amount)))
      .forEach((g) => lines.push(`${p.name} ${g.date} ${g.amount} x${g.count}`));
  }
  return lines;
}

/** The sheet's duplicate claims (see duplicates), shown once Show is pressed and kept current from then on. */
function Dups({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const [shown, setShown] = useState(false);
  const lines = shown ? duplicates(people.data ?? [], claims.data ?? []) : [];
  return (
    <section data-testid="dups-view">
      <h3>Duplicate claims</h3>
      <button type="button" data-testid="dups-show" onClick={() => setShown(true)}>
        Show
      </button>
      <ul>
        {lines.map((line, i) => (
          <li key={i} data-testid="dup-line">
            {line}
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetPage.fill({ order: 41, Component: Dups });
