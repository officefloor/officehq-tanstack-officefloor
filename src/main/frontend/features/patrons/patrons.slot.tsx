import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { live, useClaims } from '../claims/claims';
import { usePeople, type Person } from '../people/people';
import { personDue, sum, useRates } from '../rates/rates';
import { useFloor, useLate } from '../settings/settings';
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

/** A person's patrons (names, sorted), their backing (their patrons' due-totals added up, skipping n/a; 0.00 with no
 *  patrons), and the picker that names another person on the sheet as a patron. A refused add shows the row's
 *  patron error. */
function PersonPatrons({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const people = usePeople(sheetId);
  const patrons = usePatrons(sheetId);
  const claims = useClaims(sheetId);
  const rates = useRates(sheetId);
  const late = useLate(sheetId);
  const floor = useFloor(sheetId);
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
  const backing = sum(
    mine.map((p) =>
      personDue(live(claims.data?.filter((c) => c.personId === p.id) ?? [], [p]), rates.data, p.grade, late, floor),
    ),
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
