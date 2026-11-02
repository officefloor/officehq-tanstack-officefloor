import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonColumnHead } from '../../slots/defs/personRow';
import { patronsKey, usePatrons, usePeople, type Person } from '../people/usePeople';
import { useDueTotals } from '../rates/useDueTotals';
import { sumDues } from '../rates/useRates';

/** A person's patrons: the other people on the sheet they have named (names sorted, comma and space
 *  separated), their backing (their patrons' due-totals added up, skipping 'n/a'; 0.00 with none),
 *  and a picker that names one more. Naming themselves, or a patron twice, is refused. */
function PersonPatrons({ sheetId, personId }: { sheetId: string; personId: number }) {
  const people = usePeople(sheetId);
  const patrons = usePatrons(sheetId);
  const totals = useDueTotals(sheetId);
  const queryClient = useQueryClient();
  const [chosen, setChosen] = useState('');

  const named = (patrons.data ?? [])
    .filter((x) => x.personId === personId)
    .map((x) => people.data?.find((p) => p.id === x.patronId))
    .filter((p): p is Person => p !== undefined);
  const names = named.map((p) => p.name).sort((a, b) => (a < b ? -1 : a > b ? 1 : 0));
  const backing = sumDues(named.map((p) => totals.get(p.id) ?? 'n/a'));

  const add = useMutation({
    mutationFn: (patronId: number) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/patrons`, { patronId }),
    onSuccess: async () => {
      setChosen('');
      await queryClient.invalidateQueries({ queryKey: patronsKey(sheetId) });
    },
  });

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
          {(people.data ?? []).map((p) => (
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
            add.reset();
            add.mutate(Number(chosen));
          }}
        >
          Add patron
        </button>
        {add.isError && <span data-testid="person-patron-error">{add.error.message}</span>}
      </td>
    </>
  );
}

export const contributions = [
  PersonColumnHead.fill({
    order: 50,
    Component: () => (
      <>
        <th>Patrons</th>
        <th>Backing</th>
        <th />
      </>
    ),
  }),
  PersonCells.fill({ order: 50, Component: PersonPatrons }),
];
