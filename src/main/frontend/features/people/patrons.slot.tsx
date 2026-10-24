import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { useRowAction } from '../../api/rowAction';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { useClaims } from '../claims/claims';
import { useSettings } from '../roles/roles';
import { useRates, sumDue } from '../rates/rates';
import { personDue } from '../rates/due.slot';
import { peoplePath, peopleQueryKey, usePeople, type Person } from './people';

// A person's patrons — other people on the sheet they have named, one at a time, each once; shown by name,
// sorted — and the button that names another. Their backing is their patrons' due-totals added up, skipping
// n/a (0.00 with no patrons).
function PatronsHeader() {
  return (
    <>
      <th>Patrons</th>
      <th>Backing</th>
    </>
  );
}

function PersonPatrons({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople();
  const claims = useClaims();
  const rates = useRates();
  const today = useSheetToday();
  const settings = useSettings();
  const [chosen, setChosen] = useState('');
  const queryClient = useQueryClient();
  const add = useMutation({
    mutationFn: (body: { patronId: number }) =>
      postJson<Person>(`${peoplePath(sheet ?? '')}/${person.id}/patrons`, body, user),
    onSuccess: () => {
      setChosen('');
      return queryClient.invalidateQueries({ queryKey: peopleQueryKey(sheet ?? '') });
    },
  });
  const submit = useSubmitOnce(add);
  const rowAction = useRowAction(`person-${person.id}`);
  const everyone = people.data ?? [];
  const patrons = person.patronIds
    .map((id) => everyone.find((p) => p.id === id))
    .filter((p): p is Person => p !== undefined);
  const names = patrons.map((p) => p.name).sort((a, b) => (a < b ? -1 : a > b ? 1 : 0));
  const backing = sumDue(patrons.map((p) => personDue(p, claims.data ?? [], rates.data, today, settings.data)));
  return (
    <>
      <td>
        <span data-testid="person-patrons">{names.join(', ')}</span>{' '}
        <select
          data-testid="person-patron-select"
          value={chosen}
          onChange={(e) => {
            setChosen(e.target.value);
            add.reset();
          }}
        >
          <option value="">Choose a patron</option>
          {everyone.filter((p) => p.id !== person.id).map((p) => (
            <option key={p.id} value={String(p.id)}>{p.name}</option>
          ))}
        </select>
        <button
          type="button"
          data-testid="person-patron-add"
          disabled={add.isPending}
          onClick={() => { rowAction.take(); submit({ patronId: Number(chosen) }); }}
        >
          Add patron
        </button>
        {add.isError && rowAction.isLatest && <span data-testid="person-patron-error">The patron could not be added.</span>}
      </td>
      <td data-testid="person-backing">{backing}</td>
    </>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 47, Component: PatronsHeader }),
  PersonCells.fill({ order: 47, Component: PersonPatrons }),
];
