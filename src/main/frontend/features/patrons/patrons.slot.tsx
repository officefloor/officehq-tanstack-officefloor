import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { useClaims } from '../claims/claims';
import { plusDays } from '../overdue/late';
import { usePeople, type Person } from '../people/people';
import { useSheetToday } from '../session/session';
import { sum } from '../rates/rates';
import { patronsKey, usePatrons, type Patron } from './patrons';

function PatronsHead() {
  return (
    <>
      <th>Patrons</th>
      <th>Backing</th>
      <th />
    </>
  );
}

/** A person's patrons (names, sorted), their backing (the dues fixed when settled of their patrons' SETTLED claims dated
 *  within the 90 days up to and including the sheet's today, added up; 0.00 with none), and the picker that names another person on the sheet as a patron. A refused add shows the row's
 *  patron error. */
function PersonPatrons({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
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
  const names = mine.map((p) => p.name).sort((a, b) => a.localeCompare(b));
  const since = today === '' ? '' : plusDays(today, -89);
  const patronIds = new Set(mine.map((p) => p.id));
  const backing = sum(
    (claims.data ?? [])
      .filter((c) => patronIds.has(c.personId) && c.status === 'SETTLED' && c.date >= since && c.date <= today)
      .map((c) => c.due ?? 'n/a'),
  );
  const others = (people.data ?? []).filter((p) => p.id !== person.id);

  return (
    <>
      <td data-testid="person-patrons">{names.join(', ')}</td>
      <td data-testid="person-backing">{backing}</td>
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
