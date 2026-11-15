import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { listPeople, peopleKey } from '../people/api';
import { claimsKey, listClaims } from './api';

/** The duplicate claims: for each person, date and amount with two or more claims (VOID and DELETED ones
 *  left out), one line `<name> <date> <amount> x<count>`, people in list order, then by date, then by
 *  amount. Shown once asked for, re-read from the store each time. */
export function Duplicates({ sheetId }: { sheetId: string; user: string }) {
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  const [shown, setShown] = useState(false);

  const lines = (people.data ?? []).flatMap((p) => {
    const groups = new Map<string, { date: string; amount: string; count: number }>();
    for (const c of claims.data ?? []) {
      if (c.personId !== p.id || c.status === 'VOID' || c.status === 'DELETED') continue;
      const key = `${c.date} ${Number(c.amount)}`;
      const g = groups.get(key);
      if (g) g.count++;
      else groups.set(key, { date: c.date, amount: c.amount, count: 1 });
    }
    return [...groups.values()]
      .filter((g) => g.count >= 2)
      .sort((x, y) => x.date.localeCompare(y.date) || Number(x.amount) - Number(y.amount))
      .map((g) => `${p.name} ${g.date} ${Number(g.amount).toFixed(2)} x${g.count}`);
  });

  return (
    <section>
      <h3>Duplicate claims</h3>
      <button
        type="button"
        data-testid="dups-show"
        onClick={() => {
          setShown(true);
          void people.refetch();
          void claims.refetch();
        }}
      >
        Show duplicates
      </button>
      {shown && (
        <ul>
          {lines.map((l) => <li key={l} data-testid="dup-line">{l}</li>)}
        </ul>
      )}
    </section>
  );
}
