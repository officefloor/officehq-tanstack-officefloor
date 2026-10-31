import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { live, personDueCents, useClaims } from '../claims/useClaims';
import { gradeOn, heldOnBy, peopleKey, usePeople, type Person } from '../people/usePeople';
import { rateOn, totalDue, useRates } from '../rates/useRates';
import { useFloor, useLate } from '../settings/useSettings';
import { useSheetToday } from '../sheet/useSheet';

/**
 * A person's patrons: the other people on the sheet they have named (names sorted, comma and space
 * separated), and their backing, the patrons' due-totals added up (an n/a one is skipped; 0.00 with
 * no patrons). One patron is named at a time; the server refuses the person themselves and someone
 * already their patron.
 */
function PersonPatrons({ user, sheet, person }: { user: string; sheet: string; person: Person }) {
  const queryClient = useQueryClient();
  const { data: people = [] } = usePeople(sheet);
  const { data: all = [] } = useClaims(sheet);
  const { data: rates = [] } = useRates(sheet);
  const today = useSheetToday(sheet);
  const floor = useFloor(sheet);
  const late = useLate(sheet);
  const [chosen, setChosen] = useState('');

  const patrons = people.filter((p) => person.patrons.includes(p.id));
  const names = patrons.map((p) => p.name).sort();
  const claims = live(all, heldOnBy(people));
  const backing = patrons.map((p) =>
    personDueCents(claims.filter((c) => c.personId === p.id), floor, rateOn(rates, gradeOn(p, today), today), (d) => rateOn(rates, gradeOn(p, d), d), late),
  );

  const add = useMutation({
    mutationFn: (patronId: number) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/patrons`, { patronId, user }),
    onSuccess: () => setChosen(''),
    onSettled: () => queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
  });

  return (
    <>
      <td data-testid="person-patrons">{names.join(', ')}</td>
      <td data-testid="person-backing">{totalDue(backing)}</td>
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
