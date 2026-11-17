import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { useClaims } from '../claims/claims';
import { plusDays } from '../overdue/late';
import { shownName, usePeople, type Person } from '../people/people';
import { useSheetToday } from '../session/session';
import { equalShares, sharedSum, sum } from '../rates/rates';
import { patronsKey, usePatrons, type Patron } from './patrons';
import { useShownColumns } from '../columns/columns';

function PatronsHead({ user }: { user: string }) {
  const shown = useShownColumns(user);
  return (
    <>
      {shown('patrons') && <th>Patrons</th>}
      {shown('backing') && <th>Backing</th>}
      <th />
    </>
  );
}

/** A person's patrons with their shares (100.00 split equally in the order they were named, the remainder to the
 *  first; shown as "<name> <share>", sorted by name), their backing (over the patrons, each patron's figure — the dues
 *  fixed when settled of their SETTLED claims dated within the 90 days up to and including the sheet's today, added
 *  up — times their share / 100, added up and rounded half to even; 0.00 with none), and the picker that names another person on the sheet as a patron. A refused add shows the row's
 *  patron error. */
function PersonPatrons({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const shown = useShownColumns(user);
  const people = usePeople(sheetId);
  const patrons = usePatrons(sheetId);
  const claims = useClaims(sheetId);
  const today = useSheetToday(sheetId);
  const queryClient = useQueryClient();
  const [chosen, setChosen] = useState('');

  const submit = useSubmitOnce();
  const add = useMutation({
    mutationFn: (patronId: number) =>
      postJson<Patron>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/patrons`, { patronId, by: user }),
    onSuccess: () => {
      setChosen('');
      return queryClient.invalidateQueries({ queryKey: patronsKey(sheetId) });
    },
    onSettled: submit.end,
  });

  const mine = (patrons.data ?? [])
    .filter((p) => p.personId === person.id)
    .map((p) => people.data?.find((q) => q.id === p.patronId))
    .filter((p): p is Person => p !== undefined);
  const shares = equalShares(mine.length);
  const names = mine
    .map((p, i) => ({ name: p.name, shown: shownName(p), share: shares[i] }))
    .sort((a, b) => a.name.localeCompare(b.name))
    .map((p) => `${p.shown} ${p.share}`);
  const since = today === '' ? '' : plusDays(today, -89);
  const settled = (claims.data ?? []).filter((c) => c.status === 'SETTLED' && c.date >= since && c.date <= today);
  const backing = sharedSum(
    mine.map((p, i) => ({
      value: sum(settled.filter((c) => c.personId === p.id).map((c) => c.due ?? 'n/a')),
      pct: shares[i],
    })),
  );
  const others = (people.data ?? []).filter((p) => p.id !== person.id);

  return (
    <>
      {shown('patrons') && <td data-testid="person-patrons">{names.join(', ')}</td>}
      {shown('backing') && <td data-testid="person-backing">{backing}</td>}
      <td>
        <select
          data-testid="person-patron-select"
          value={chosen}
          onChange={(e) => {
            setChosen(e.target.value);
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
          onClick={() => {
            if (!submit.begin()) return;
            add.reset();
            add.mutate(Number(chosen)); // nothing chosen: no person 0, so the server refuses it
          }}
        >
          Add patron
        </button>
        {add.isError && <span data-testid="person-patron-error"> Could not add this patron.</span>}
      </td>
    </>
  );
}

export const contributions = [
  PeopleHead.fill({ order: 60, Component: PatronsHead }),
  PersonRow.fill({ order: 60, Component: PersonPatrons }),
];
