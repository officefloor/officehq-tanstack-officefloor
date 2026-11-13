import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useOneAtATime } from '../../query/useOneAtATime';
import { useClaims } from '../claims/claims';
import { usePeople } from '../people/people';
import type { Person } from '../people/people';
import { usePersonError } from '../people/personError';
import { personDue, total, useRates } from '../rates/rates';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { useDueRules } from '../settings/settings';
import { patronIdsOf, patronsKey, usePatrons } from './patrons';
import type { Patron } from './patrons';

// A person's patrons (their names, sorted) and the select and button that name one more: anyone else on
// the sheet who is not already their patron.
export function PersonPatrons({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople().data ?? [];
  const ids = patronIdsOf(usePatrons().data ?? [], person.id);
  const [chosen, setChosen] = useState('');
  const [error, setError] = usePersonError(person.id, 'patron');
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const names = people
    .filter((p) => ids.includes(p.id))
    .map((p) => p.name)
    .sort((a, b) => a.localeCompare(b));
  const offered = people.filter((p) => p.id !== person.id && !ids.includes(p.id));

  const add = useMutation({
    mutationFn: (patronId: number) =>
      postJson<Patron>(
        `/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/patrons?user=${encodeURIComponent(user)}`,
        { patronId },
      ),
    onSuccess: async () => {
      setChosen('');
      await queryClient.invalidateQueries({ queryKey: patronsKey(sheet) });
    },
    onError: () => setError('The patron could not be added.'),
  });

  const submit = () => {
    setError('');
    if (!chosen) {
      setError('Choose a patron.');
      return;
    }
    once(() => add.mutateAsync(Number(chosen)));
  };

  return (
    <td>
      <span data-testid="person-patrons">{names.join(', ')}</span>{' '}
      <select
        data-testid="person-patron-select"
        value={chosen}
        onChange={(e) => {
          setChosen(e.target.value);
          setError('');
        }}
      >
        <option value="" />
        {offered.map((p) => (
          <option key={p.id} value={p.id}>
            {p.name}
          </option>
        ))}
      </select>
      <button type="button" data-testid="person-patron-add" disabled={add.isPending} onClick={submit}>
        Add patron
      </button>
      {error && <span data-testid="person-patron-error">{error}</span>}
    </td>
  );
}

// A person's backing: their patrons' due-totals added up, skipping n/a; 0.00 with no patrons.
export function PersonBacking({ person }: { person: Person }) {
  const people = usePeople().data ?? [];
  const ids = patronIdsOf(usePatrons().data ?? [], person.id);
  const rates = useRates().data ?? [];
  const rules = useDueRules();
  const claims = useClaims().data ?? [];
  const dues = people
    .filter((p) => ids.includes(p.id))
    .map((p) => personDue(claims.filter((c) => c.personId === p.id), rates, p.grades, p.held, rules));
  return <td data-testid="person-backing">{total(dues)}</td>;
}
