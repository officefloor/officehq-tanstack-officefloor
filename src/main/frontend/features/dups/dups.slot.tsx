import { useState } from 'react';
import { SheetBody } from '../../slots/defs/sheetBody';
import { usePeople } from '../people/people';
import { useClaims } from '../claims/claims';
import { duplicates } from './dups';

// The duplicate claims: a button shows, one line each as <name> <date> <amount> x<count>, every
// person, date and amount with two or more claims (VOID and DELETED left out), people in list
// order, then by date, then by amount. Kept current with the sheet once shown.
function Duplicates({ sheetId }: { sheetId: string }) {
  const [shown, setShown] = useState(false);
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const dups = shown ? duplicates(people, claims) : [];

  return (
    <section data-testid="dups">
      <button type="button" data-testid="dups-show" onClick={() => setShown(true)}>
        Show duplicate claims
      </button>
      {shown && (
        <ul>
          {dups.map((d, i) => (
            <li key={i} data-testid="dup-line">
              {d.name} {d.date} {d.amount} x{d.count}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 2.6, Component: Duplicates });
