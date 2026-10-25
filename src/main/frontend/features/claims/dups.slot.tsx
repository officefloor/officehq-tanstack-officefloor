import { useState } from 'react';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { usePeople } from '../people/people';
import { useClaims } from './claims';

// The duplicate claims, shown once the button is pressed: for each person, date and amount with two or more claims
// (VOID and DELETED claims left out), one line <name> <date> <amount> x<count> — people in list order, then by date,
// then by amount.
function DuplicateClaims() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople();
  const claims = useClaims();
  const [shown, setShown] = useState(false);
  if (!user || !sheet) return null;
  const counted = (claims.data ?? []).filter((c) => c.status !== 'VOID' && c.status !== 'DELETED');
  const lines = (people.data ?? []).flatMap((p) => {
    const groups = new Map<string, { date: string; amount: string; count: number }>();
    for (const c of counted.filter((c) => c.personId === p.id)) {
      const key = `${c.date} ${c.amount}`;
      const g = groups.get(key);
      if (g) g.count += 1;
      else groups.set(key, { date: c.date, amount: c.amount, count: 1 });
    }
    return [...groups.values()]
      .filter((g) => g.count >= 2)
      .sort((a, b) => a.date.localeCompare(b.date) || Number(a.amount) - Number(b.amount))
      .map((g) => `${p.name} ${g.date} ${g.amount} x${g.count}`);
  });
  return (
    <section data-testid="dups">
      <button type="button" data-testid="dups-show" onClick={() => setShown(true)}>
        Show duplicate claims
      </button>
      {shown && (
        <ul>
          {lines.map((l) => <li key={l} data-testid="dup-line">{l}</li>)}
        </ul>
      )}
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 34.6, Component: DuplicateClaims });
