import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonRow } from '../../slots/defs/personRow';
import { live, useClaims } from '../claims/claims';
import { usePeople } from '../people/people';
import type { Person } from '../people/people';
import { recentSettledSum, useRates } from '../rates/rates';
import { useFloor, useLate } from '../settings/settings';
import { useSheetToday } from '../sheet/sheet';
import { patronsKey, usePatrons } from './patrons';
import type { Patron } from './patrons';

/** A person's patrons (their names, sorted), their backing — the patrons' settled figures over their SETTLED claims
 *  dated within the 90 days up to and including the sheet's today (see recentSettledSum); 0.00 with none — and the
 *  picker that names one more. */
function PersonPatrons({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const people = usePeople(sheetId).data ?? [];
  const claims = live(useClaims(sheetId).data ?? [], people);
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const patrons = (usePatrons(sheetId).data ?? [])
    .filter((p) => p.personId === person.id)
    .map((p) => people.find((q) => q.id === p.patronId))
    .filter((p): p is Person => p !== undefined)
    .sort((a, b) => (a.name < b.name ? -1 : a.name > b.name ? 1 : 0));
  const backing = recentSettledSum(patrons, claims, rates, today, floor, late);
  const [chosen, setChosen] = useState('');
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const add = useMutation({
    mutationFn: (patronId: number) =>
      postJson<Patron>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/patrons`, { user, patronId }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: patronsKey(sheetId) });
      setChosen('');
    },
    onError: (e) =>
      setError(e instanceof HttpError && e.body ? e.body : `Could not name this patron for ${person.name}.`),
    onSettled: once.done,
  });

  return (
    <td>
      Patrons <span data-testid="person-patrons">{patrons.map((p) => p.name).join(', ')}</span>{' '}
      Backing <span data-testid="person-backing">{backing}</span>{' '}
      <select
        data-testid="person-patron-select"
        value={chosen}
        onChange={(e) => {
          setChosen(e.target.value);
          setError('');
        }}
      >
        <option value="">Patron…</option>
        {people.map((p) => (
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
          setError('');
          if (chosen === '') {
            setError('Choose a person to name as a patron.');
            return;
          }
          if (!once.start()) return;
          add.mutate(Number(chosen));
        }}
      >
        Add patron
      </button>
      {error && (
        <p data-testid="person-patron-error" role="alert">
          {error}
        </p>
      )}
    </td>
  );
}

export const contribution = PersonRow.fill({ order: 46, Component: PersonPatrons });
