import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { useClaims } from '../claims/useClaims';
import { peopleKey, usePeople, type Person } from '../people/usePeople';
import { hundredths, totalDue } from '../rates/useRates';
import { useSheetToday } from '../sheet/useSheet';

/**
 * A person's patrons: the other people on the sheet they have named (names sorted, comma and space
 * separated), and their backing, the patrons' SETTLED claims dated within the 90 days up to and
 * including the sheet's today added up, each at the due fixed when it was settled (0.00 with none). One patron is named at a time; the server refuses the person themselves and someone
 * already their patron.
 */
/** The first day (yyyy-mm-dd) of the 90 days up to and including `today`. */
function backingSince(today: string): string {
  if (today === '') return '';
  return new Date(Date.parse(`${today}T00:00:00Z`) - 89 * 86_400_000).toISOString().slice(0, 10);
}

function PersonPatrons({ user, sheet, person }: { user: string; sheet: string; person: Person }) {
  const queryClient = useQueryClient();
  const { data: people = [] } = usePeople(sheet);
  const { data: all = [] } = useClaims(sheet);
  const today = useSheetToday(sheet);
  const [chosen, setChosen] = useState('');

  const patrons = people.filter((p) => person.patrons.includes(p.id));
  const names = patrons.map((p) => p.name).sort();
  const since = backingSince(today);
  const backing = all
    .filter((c) => person.patrons.includes(c.personId) && c.status === 'SETTLED' && c.due !== null)
    .filter((c) => c.date >= since && c.date <= today)
    .reduce((sum, c) => sum + hundredths(c.due!), 0n);

  const add = useMutation({
    mutationFn: (patronId: number) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/patrons`, { patronId, user }),
    onSuccess: () => setChosen(''),
    onSettled: () => queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
  });

  return (
    <>
      <td data-testid="person-patrons">{names.join(', ')}</td>
      <td data-testid="person-backing">{totalDue([backing])}</td>
      <td>
        <select
          data-testid="person-patron-select"
          value={chosen}
          onChange={(e) => {
            add.reset();
            setChosen(e.target.value);
          }}
        >
          <option value="">Choose a patron…</option>
          {people
            .filter((p) => p.id !== person.id)
            .map((p) => (
              <option key={p.id} value={String(p.id)}>
                {p.name}
              </option>
            ))}
        </select>
        <button
          type="button"
          data-testid="person-patron-add"
          disabled={add.isPending || chosen === ''}
          onClick={() => {
            add.reset();
            add.mutate(Number(chosen));
          }}
        >
          Add patron
        </button>
        {add.isError && (
          <span data-testid="person-patron-error" role="alert">
            {' '}
            Could not name this patron (not themselves, and not someone already their patron).
          </span>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonColumns.fill({
    order: 40,
    Component: () => (
      <>
        <th>Patrons</th>
        <th>Backing</th>
        <th />
      </>
    ),
  }),
  PersonCells.fill({ order: 40, Component: PersonPatrons }),
];
