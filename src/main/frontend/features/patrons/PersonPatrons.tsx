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
import { patronsKey, patronsOf, usePatrons } from './patrons';
import type { Patron } from './patrons';
import { useColumnShown } from '../columns/columns';

// A person's patrons (each as their name and share, sorted by name) and the select and button that name one more: anyone else on
// the sheet who is not already their patron.
export function PersonPatrons({ person }: { person: Person }) {
  const showPatrons = useColumnShown('patrons');
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople().data ?? [];
  const mine = patronsOf(usePatrons().data ?? [], person.id);
  const ids = mine.map((p) => p.patronId);
  const [chosen, setChosen] = useState('');
  const [error, setError] = usePersonError(person.id, 'patron');
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const names = people
    .filter((p) => ids.includes(p.id))
    .map((p) => ({ name: p.name, share: mine.find((m) => m.patronId === p.id)?.share ?? '' }))
    .sort((a, b) => a.name.localeCompare(b.name))
    .map((p) => `${p.name} ${p.share}`);
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
      {showPatrons && <span data-testid="person-patrons">{names.join(', ')}</span>}{' '}
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

// A person's backing: for each patron, their figure (their SETTLED claims dated within the 90 days up to and
// including the sheet's today, each at the figure frozen when it was settled, added up) times their share divided
// by 100; those added up and rounded half to even to two decimals; 0.00 with none.
export function PersonBacking({ person }: { person: Person }) {
  const mine = patronsOf(usePatrons().data ?? [], person.id);
  const today = useDueRules().today;
  const showBacking = useColumnShown('backing');
  const claims = useClaims().data ?? [];
  const from = today === '' ? '' : backingStart(today);
  const figureOf = (patronId: number) =>
    total(
      claims
        .filter((c) => c.personId === patronId && c.status === 'SETTLED' && today !== '' && c.date >= from && c.date <= today)
        .map((c) => c.due ?? NA),
    );
  const products = mine.reduce((sum, p) => sum + hundredths(figureOf(p.patronId)) * hundredths(p.share), 0);
  if (!showBacking) return null;
  return <td data-testid="person-backing">{halfEven(products)}</td>;
}

/** A two-decimal value as whole hundredths, exactly. */
const hundredths = (value: string) => Math.round(Number(value) * 100);

/** Cents times basis points (/ 10000) as a two-decimal value, rounded half to even. */
function halfEven(product: number): string {
  const whole = Math.floor(Math.abs(product) / 10000);
  const rest = Math.abs(product) - whole * 10000;
  const up = rest > 5000 || (rest === 5000 && whole % 2 === 1);
  return ((Math.sign(product) * (whole + (up ? 1 : 0))) / 100).toFixed(2);
}
