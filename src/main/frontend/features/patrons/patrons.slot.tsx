import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import { peoplePath, usePeople } from '../people/people';
import type { Person } from '../people/people';
import { personDueCents, useClaims, useLate } from '../claims/claims';
import { twoDecimals, useRates } from '../rates/rates';
import { useCapCents, useFloorCents } from '../settings/settings';
import { useSheetToday } from '../sheet/sheet';
import { patronsKey, usePatrons } from './patrons';
import type { Patron } from './patrons';

function PatronsHeader() {
  return (
    <>
      <th>Patrons</th>
      <th>Backing</th>
      <th />
    </>
  );
}

// A person's patrons (names sorted, comma separated) and their backing: the patrons' due-totals added up,
// skipping those that are n/a. A patron is picked from the others on the sheet and named one at a time; the
// server refuses a patron already named.
function PersonPatrons({ sheet, user, person }: { sheet: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const { data: everyone = [] } = usePeople(sheet);
  const { data: patrons = [] } = usePatrons(sheet);
  const { data: claims = [] } = useClaims(sheet);
  const { data: rates = [] } = useRates(sheet);
  const today = useSheetToday(sheet);
  const floor = useFloorCents(sheet);
  const cap = useCapCents(sheet);
  const late = useLate(sheet);
  const [picked, setPicked] = useState('');

  const mine = patrons
    .filter((p) => p.person === person.id)
    .map((p) => everyone.find((e) => e.id === p.patron))
    .filter((p): p is Person => p !== undefined);
  const names = mine.map((p) => p.name).sort((a, b) => a.localeCompare(b));
  const backing = mine.reduce((sum, p) => sum + (personDueCents(claims, rates, p, today, floor, late, cap) ?? 0n), 0n);
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
      <td data-testid="person-backing">{twoDecimals(backing)}</td>
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
