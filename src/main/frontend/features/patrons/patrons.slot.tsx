import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonHeads } from '../../slots/defs/personRow';
import { peopleKey, usePeople, type Person } from '../people/people.slot';
import { personActionKey, useLastPersonAction } from '../people/personAction';
import { addDays, showHundredths, toHundredths } from '../rates/useRates';
import { useClaims } from '../claims/useClaims';
import { useSheetToday } from '../today/today.slot';
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

// The person's patrons (names, sorted), their backing — the patrons' SETTLED claims dated within the 90 days up to and
// including the sheet's today, each at its frozen figure, added up (0.00 with none) — and the picker that names one more. Only other people not yet their patrons are offered. A refusal shows on
// the person until the picker is changed or the next action on their row.
function PatronCells({ sheetId, personId }: { sheetId: string; user: string; personId: number }) {
  const people = usePeople(sheetId).data ?? [];
  const person = people.find((p) => p.id === personId);
  const claims = useClaims(sheetId).data ?? [];
  const today = useSheetToday(sheetId).data?.today;
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
  if (today !== undefined) {
    const since = addDays(today, -89);
    const patronIds = new Set(people.filter((p) => patrons.includes(p.name)).map((p) => p.id));
    for (const c of claims) {
      if (!patronIds.has(c.personId) || c.status !== 'SETTLED' || c.settledDue == null) continue;
      if (c.date < since || c.date > today) continue;
      backing += toHundredths(c.settledDue);
    }
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
