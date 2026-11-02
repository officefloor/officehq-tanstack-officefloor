import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import { peoplePath, usePeople } from '../people/people';
import type { Person } from '../people/people';
import { recentSettledCents, useClaims } from '../claims/claims';
import { twoDecimals } from '../rates/rates';
import { useSheetToday } from '../sheet/sheet';
import { patronsKey, shareCents, usePatrons } from './patrons';
import type { Patron } from './patrons';
import { Fig } from '../locale/Fig';

/** A figure in ten-thousandths of a hundredth (hundredths times a share in hundredths of a percent) as hundredths,
 *  rounded half to even. */
function roundHundredths(product: bigint): bigint {
  const abs = product < 0n ? -product : product;
  let quotient = abs / 10000n;
  const remainder = abs % 10000n;
  if (remainder > 5000n || (remainder === 5000n && quotient % 2n === 1n)) quotient += 1n;
  return product < 0n ? -quotient : quotient;
}

function PatronsHeader() {
  return (
    <>
      <th>Patrons</th>
      <th>Backing</th>
      <th />
    </>
  );
}

// A person's patrons, each with their share (see shareCents; shown "<name> <share>", sorted by name, comma separated),
// and their backing: over the patrons, each patron's settled figures of their SETTLED claims dated within the 90 days up
// to and including the sheet's today, times their share divided by 100, added up and rounded half to even (0.00 with
// none). A patron is picked from the others on the sheet and named one at a time; the server refuses a patron already
// named.
function PersonPatrons({ sheet, user, person }: { sheet: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const { data: everyone = [] } = usePeople(sheet);
  const { data: patrons = [] } = usePatrons(sheet);
  const { data: claims = [] } = useClaims(sheet);
  const today = useSheetToday(sheet);
  const [picked, setPicked] = useState('');

  const mine = patrons
    .filter((p) => p.person === person.id)
    .sort((a, b) => a.named - b.named)
    .map((p) => everyone.find((e) => e.id === p.patron))
    .filter((p): p is Person => p !== undefined);
  const shares = shareCents(mine.length);
  const named = mine.map((p, i) => ({ patron: p, share: shares[i] }));
  const names = [...named]
    .sort((a, b) => a.patron.name.localeCompare(b.patron.name))
    .map((n) => `${n.patron.name} ${twoDecimals(n.share)}`);
  const backing = roundHundredths(
    named.reduce((sum, n) => sum + recentSettledCents(claims, n.patron, today) * n.share, 0n),
  );
  const others = everyone.filter((p) => p.id !== person.id);

  const add = useMutation({
    mutationFn: (patron: number) =>
      postJson<Patron[]>(byUser(`${peoplePath(sheet)}/${person.id}/patrons`, user), { patron }),
    onSuccess: (updated) => {
      queryClient.setQueryData(patronsKey(sheet), updated);
      setPicked('');
    },
  });
  const send = useSubmitOnce(add);

  return (
    <>
      <td data-testid="person-patrons">{names.join(', ')}</td>
      <td data-testid="person-backing"><Fig>{twoDecimals(backing)}</Fig></td>
      <td>
        <select
          data-testid="person-patron-select"
          value={picked}
          onChange={(e) => {
            setPicked(e.target.value);
            add.reset();
          }}
        >
          <option value="">Patron…</option>
          {others.map((p) => (
            <option key={p.id} value={String(p.id)}>
              {p.name}
            </option>
          ))}
        </select>{' '}
        <button
          type="button"
          data-testid="person-patron-add"
          disabled={add.isPending}
          onClick={() => send(picked === '' ? -1 : Number(picked))}
        >
          Add patron
        </button>
        {add.isError && (
          <span data-testid="person-patron-error" role="alert">
            {' '}A patron must be someone else on the sheet, not already named.
          </span>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonColumnHeaders.fill({ order: 50, Component: PatronsHeader }),
  PersonColumns.fill({ order: 50, Component: PersonPatrons }),
];
