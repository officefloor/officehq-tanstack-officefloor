import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonHeads } from '../../slots/defs/personRow';
import { peopleKey, usePeople, type Person } from '../people/people.slot';
import { personActionKey, useLastPersonAction } from '../people/personAction';
import { showHundredths } from '../rates/useRates';
import { useDueTotals } from '../rates/rates.slot';
import { useUser } from '../session/useSession';

function PatronHeads() {
  return (
    <>
      <th>Patrons</th>
      <th>Backing</th>
      <th />
    </>
  );
}

// The person's patrons (names, sorted), their backing — the patrons' due-totals added up, skipping those that are
// n/a — and the picker that names one more. Only other people not yet their patrons are offered. A refusal shows on
// the person until the picker is changed or the next action on their row.
function PatronCells({ sheetId, personId }: { sheetId: string; user: string; personId: number }) {
  const people = usePeople(sheetId).data ?? [];
  const person = people.find((p) => p.id === personId);
  const totals = useDueTotals(sheetId);
  const [chosen, setChosen] = useState('');
  const [user] = useUser();
  const queryClient = useQueryClient();
  const add = useMutation({
    mutationKey: personActionKey(sheetId, personId),
    mutationFn: async (patron: string) => {
      if (patron === '') throw new Error('Choose a patron');
      return postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/patrons`, {
        patron: Number(patron),
        by: user ?? '',
      });
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
      setChosen('');
    },
  });
  const last = useLastPersonAction(sheetId, personId, add.submittedAt);
  const patrons = person?.patrons ?? [];
  const offered = people.filter((p) => p.id !== personId && !patrons.includes(p.name));
  let backing = 0n;
  for (const p of people) {
    if (!patrons.includes(p.name)) continue;
    const t = totals.get(p.id);
    if (t != null) backing += t;
  }

  return (
    <>
      <td data-testid="person-patrons">{patrons.join(', ')}</td>
      <td data-testid="person-backing">{showHundredths(backing)}</td>
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
          {offered.map((p) => (
            <option key={p.id} value={String(p.id)}>
              {p.name}
            </option>
          ))}
        </select>{' '}
        <button type="button" data-testid="person-patron-add" disabled={add.isPending} onClick={() => add.mutate(chosen)}>
          Add patron
        </button>
        {add.isError && last && (
          <span data-testid="person-patron-error" role="alert">
            {add.error.message}
          </span>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonHeads.fill({ order: 50, Component: PatronHeads }),
  PersonCells.fill({ order: 50, Component: PatronCells }),
];
