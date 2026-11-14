import { useState } from 'react';
import { useClaims } from '../claims/claims';
import { usePeople } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { duplicates } from './dups';

// The duplicate claims, shown once the button is pressed: one line per person, date and amount with two or more claims.
export function Duplicates() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople().data ?? [];
  const claims = useClaims().data ?? [];
  const [shown, setShown] = useState(false);

  if (!user || !sheet) return null;

  return (
    <section>
      <button type="button" data-testid="dups-show" onClick={() => setShown(true)}>
        Show duplicate claims
      </button>
      {shown && (
        <ul data-testid="dups">
          {duplicates(people, claims).map((d, i) => (
            <li key={i} data-testid="dup-line">
              {d.name} {d.date} {d.amount} x{d.count}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
