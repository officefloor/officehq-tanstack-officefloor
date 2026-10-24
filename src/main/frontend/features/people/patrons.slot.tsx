import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { useRowAction } from '../../api/rowAction';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { useClaims, type Claim } from '../claims/claims';
import { sumDue } from '../rates/rates';
import { peoplePath, peopleQueryKey, usePeople, type Person } from './people';

/** How many days back from the sheet's today (today included) a patron's settled claim counts toward backing. */
const BACKING_DAYS = 90;

/** The day a number of days before a date (YYYY-MM-DD). */
const daysBefore = (day: string, n: number) =>
  new Date(Date.parse(`${day}T00:00:00Z`) - n * 86_400_000).toISOString().slice(0, 10);

/** A person's backing: their patrons' frozen figures over SETTLED claims dated within the window ending today. */
export function backingOf(patrons: Person[], claims: Claim[], today: string | undefined): string {
  if (today === undefined) return sumDue([]);
  const since = daysBefore(today, BACKING_DAYS - 1);
  const ids = new Set(patrons.map((p) => p.id));
  return sumDue(claims
    .filter((c) => ids.has(c.personId) && c.status === 'SETTLED' && c.due !== null && since <= c.date && c.date <= today)
    .map((c) => c.due as string));
}

// A person's patrons — other people on the sheet they have named, one at a time, each once; shown by name,
// sorted — and the button that names another. Their backing is the sum of their patrons' settled figures over
// their SETTLED claims dated within the 90 days up to and including the sheet's today, each at its frozen figure
// (0.00 with none).
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
  const today = useSheetToday();
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
  const backing = backingOf(patrons, claims.data ?? [], today);
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
