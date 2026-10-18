import { useState } from 'react';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { show } from '../rates/rates';
import { useDueTotal } from '../rates/rates.slot';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { addPatron, peopleKey, usePeople } from './people';

function PatronColumns() {
  return (
    <>
      <th>Patrons</th>
      <th>Backing</th>
      <th>Add patron</th>
    </>
  );
}

// A person's patrons (names, sorted), their backing (their patrons' due-totals added up, skipping n/a),
// and a picker that names another person on the sheet as a patron. A refusal shows on the row.
function PersonPatrons({ personId }: { personId: number }) {
  const [sheet] = useCurrentSheet();
  const [user] = useCurrentUser();
  const people = usePeople().data ?? [];
  const person = people.find((p) => p.id === personId);
  const dueOf = useDueTotal();
  const [choice, setChoice] = useState('');
  const [error, setError] = useState('');
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const add = useMutation({
    mutationFn: (patronId: number) => addPatron(sheet!, user, personId, patronId),
    onSuccess: async () => {
      setChoice('');
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet!) });
    },
    onError: async () => {
      setError('The patron could not be added: not the person themselves, nor one already named.');
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet!) });
    },
  });

  const patronIds = person?.patronIds ?? [];
  const patrons = people.filter((p) => patronIds.includes(p.id));
  const names = patrons.map((p) => p.name).sort((a, b) => (a < b ? -1 : a > b ? 1 : 0));
  const backing = patrons.reduce((n, p) => n + (dueOf(p.id) ?? 0n), 0n);

  const submit = () => {
    setError('');
    if (!choice) {
      setError('Choose a patron.');
      return;
    }
    once(() => add.mutateAsync(Number(choice)));
  };

  return (
    <>
      <td data-testid="person-patrons">{names.join(', ')}</td>
      <td data-testid="person-backing">{show(backing)}</td>
      <td>
        <select
          data-testid="person-patron-select"
          value={choice}
          onChange={(e) => {
            setChoice(e.target.value);
            setError('');
          }}
        >
          <option value="">Choose…</option>
          {people.map((p) => (
            <option key={p.id} value={String(p.id)}>
              {p.name}
            </option>
          ))}
        </select>
        <button type="button" data-testid="person-patron-add" onClick={submit} disabled={add.isPending}>
          Add
        </button>
        {error && <span data-testid="person-patron-error">{error}</span>}
      </td>
    </>
  );
}

export const contributions = [
  PersonColumns.fill({ order: 40, Component: PatronColumns }),
  PersonCells.fill({ order: 40, Component: PersonPatrons }),
];
