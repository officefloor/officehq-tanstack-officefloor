import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonRow } from '../../slots/defs/personRow';
import { live, useClaims } from '../claims/claims';
import { usePeople } from '../people/people';
import type { Person } from '../people/people';
import { patronShares, recentSettledSum, sharedSum, useRates } from '../rates/rates';
import { useFloor, useLate } from '../settings/settings';
import { useSheetToday } from '../sheet/sheet';
import { patronsKey, usePatrons } from './patrons';
import type { Patron } from './patrons';

/** A person's patrons, each as <name> <share> sorted by name — 100.00 split equally in the order they were named, the
 *  first-named taking the remainder (see patronShares) — their backing — each patron's settled figures over their
 *  SETTLED claims dated within the 90 days up to and including the sheet's today (see recentSettledSum) times their
 *  share / 100, added up and rounded half to even (see sharedSum); 0.00 with none — and the picker that names one more. */
function PersonPatrons({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const people = usePeople(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const claims = live(useClaims(sheetId).data ?? [], people, today);
  const rates = useRates(sheetId).data ?? [];
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  // the server lists each person's patrons in the order they were named
  const named = (usePatrons(sheetId).data ?? [])
    .filter((p) => p.personId === person.id)
    .map((p) => people.find((q) => q.id === p.patronId))
    .filter((p): p is Person => p !== undefined);
  const shares = patronShares(named.length);
  const patrons = named
    .map((p, i) => ({ person: p, share: shares[i] }))
    .sort((a, b) => (a.person.name < b.person.name ? -1 : a.person.name > b.person.name ? 1 : 0));
  const backing = sharedSum(patrons.map((p) => ({
    figure: recentSettledSum([p.person], claims, rates, today, floor, late),
    share: p.share,
  })));
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
      Patrons <span data-testid="person-patrons">{patrons.map((p) => `${p.person.name} ${p.share}`).join(', ')}</span>{' '}
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
