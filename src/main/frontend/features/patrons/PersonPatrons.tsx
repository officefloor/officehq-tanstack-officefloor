import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useOneAtATime } from '../../query/useOneAtATime';
import { useClaims } from '../claims/claims';
import { usePeople } from '../people/people';
import type { Person } from '../people/people';
import { usePersonError } from '../people/personError';
import { NA, total } from '../rates/rates';
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

/** How many days back from the sheet's today, today included, a patron's settled claim counts toward backing. */
const BACKING_DAYS = 90;

/** The first date (yyyy-MM-dd) of the backing window ending on the sheet's today. */
const backingStart = (today: string) =>
  new Date(Date.parse(`${today}T00:00:00Z`) - (BACKING_DAYS - 1) * 86_400_000).toISOString().slice(0, 10);

// A person's backing: their patrons' SETTLED claims dated within the 90 days up to and including the sheet's
// today, each at the figure frozen when it was settled, added up; 0.00 with none.
export function PersonBacking({ person }: { person: Person }) {
  const ids = patronIdsOf(usePatrons().data ?? [], person.id);
  const today = useDueRules().today;
  const claims = useClaims().data ?? [];
  const from = today === '' ? '' : backingStart(today);
  const dues = claims
    .filter((c) => ids.includes(c.personId) && c.status === 'SETTLED' && today !== '' && c.date >= from && c.date <= today)
    .map((c) => c.due ?? NA);
  return <td data-testid="person-backing">{total(dues)}</td>;
}
