import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { peopleKey, peopleQuery } from '../people/peopleQuery';
import type { Person } from '../people/peopleQuery';
import { claimsKey, claimsQuery } from './claimsQuery';
import type { Claim } from './claimsQuery';

/**
 * The duplicate claims: one line "<name> <date> <amount> x<count>" for each person, date and amount with two or more
 * claims (VOID and DELETED claims left out), people in list order, then by date, then by amount.
 */
export function duplicateLines(people: Person[], claims: Claim[]): string[] {
  const lines: string[] = [];
  for (const p of people) {
    const groups = new Map<string, { date: string; cents: number; count: number }>();
    for (const c of claims) {
      if (c.personId !== p.id || c.status === 'VOID' || c.status === 'DELETED') continue;
      const cents = Math.round(Number(c.amount) * 100);
      const key = `${c.date} ${cents}`;
      const g = groups.get(key) ?? { date: c.date, cents, count: 0 };
      g.count += 1;
      groups.set(key, g);
    }
    [...groups.values()]
      .filter((g) => g.count >= 2)
      .sort((a, b) => (a.date !== b.date ? (a.date < b.date ? -1 : 1) : a.cents - b.cents))
      .forEach((g) => lines.push(`${p.name} ${g.date} ${(g.cents / 100).toFixed(2)} x${g.count}`));
  }
  return lines;
}

/** Shows the duplicate claims, re-read from the store each time the button is pressed. */
function DuplicateClaims({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  const claims = useQuery(claimsQuery(sheetId)).data ?? [];
  const [shown, setShown] = useState(false);

  const show = async () => {
    setShown(false);
    await Promise.all([
      queryClient.refetchQueries({ queryKey: peopleKey(sheetId), exact: true }),
      queryClient.refetchQueries({ queryKey: claimsKey(sheetId), exact: true }),
    ]);
    setShown(true);
  };

  return (
    <section>
      <h3>Duplicate claims</h3>
      <button type="button" data-testid="dups-show" onClick={show}>
        Show duplicates
      </button>
      {shown &&
        duplicateLines(people, claims).map((line) => (
          <div key={line} data-testid="dup-line">
            {line}
          </div>
        ))}
    </section>
  );
}

export const contribution = SheetPanel.fill({ order: 12.5, Component: DuplicateClaims });
