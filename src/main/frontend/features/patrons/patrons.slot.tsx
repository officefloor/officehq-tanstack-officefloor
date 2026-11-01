import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { useClaims } from '../claims/useClaims';
import { peopleKey, shownName, usePeople, type Person } from '../people/usePeople';
import { hundredths, totalDue } from '../rates/useRates';
import { useSheetToday } from '../sheet/useSheet';
import { Col } from '../columns/useColumns';

/**
 * A person's patrons: the other people on the sheet they have named, each with a share of 100.00
 * split equally in the order they were named (each rounded half to even to two decimals, the
 * remainder to the first-named), shown as `<name> <share>` (sorted by name, comma and space
 * separated). Their backing is the sum over the patrons of the patron's figure (their SETTLED claims
 * dated within the 90 days up to and including the sheet's today added up, each at the due fixed
 * when it was settled) times their share divided by 100, rounded half to even (0.00 with none). One
 * patron is named at a time; the server refuses the person themselves and someone already their
 * patron.
 */
/** `n / d` rounded half to even (d > 0). */
function divHalfEven(n: bigint, d: bigint): bigint {
  const negative = n < 0n;
  const abs = negative ? -n : n;
  let q = abs / d;
  const twice = (abs % d) * 2n;
  if (twice > d || (twice === d && q % 2n === 1n)) q += 1n;
  return negative ? -q : q;
}

/** Each of `count` patrons' share of 100.00 in hundredths, in the order they were named. */
function shares(count: number): bigint[] {
  if (count === 0) return [];
  const each = divHalfEven(10_000n, BigInt(count));
  const result = Array.from({ length: count }, () => each);
  result[0] += 10_000n - each * BigInt(count);
  return result;
}

/** The first day (yyyy-mm-dd) of the 90 days up to and including `today`. */
function backingSince(today: string): string {
  if (today === '') return '';
  return new Date(Date.parse(`${today}T00:00:00Z`) - 89 * 86_400_000).toISOString().slice(0, 10);
}

function PersonPatrons({ user, sheet, person }: { user: string; sheet: string; person: Person }) {
  const queryClient = useQueryClient();
  const { data: people = [] } = usePeople(sheet);
  const { data: all = [] } = useClaims(sheet);
  const today = useSheetToday(sheet);
  const [chosen, setChosen] = useState('');

  const named = person.patrons;
  const split = shares(named.length);
  const shareOf = new Map(named.map((id, i) => [id, split[i]]));
  const labels = people
    .filter((p) => shareOf.has(p.id))
    .map((p) => ({ name: p.name, label: `${shownName(p, today)} ${totalDue([shareOf.get(p.id)!])}` }))
    .sort((a, b) => (a.name < b.name ? -1 : a.name > b.name ? 1 : 0))
    .map((p) => p.label);
  const since = backingSince(today);
  const figure = (id: number) =>
    all
      .filter((c) => c.personId === id && c.status === 'SETTLED' && c.due !== null)
      .filter((c) => c.date >= since && c.date <= today)
      .reduce((sum, c) => sum + hundredths(c.due!), 0n);
  // figure (hundredths) × share (hundredths) is in millionths of the share-weighted sum; ÷ 10000.
  const backing = divHalfEven(
    named.reduce((sum, id) => sum + figure(id) * shareOf.get(id)!, 0n),
    10_000n,
  );

  const add = useMutation({
    mutationFn: (patronId: number) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/patrons`, { patronId, user }),
    onSuccess: () => setChosen(''),
    onSettled: () => queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
  });

  return (
    <>
      <Col user={user} id="patrons"><td data-testid="person-patrons">{labels.join(', ')}</td></Col>
      <Col user={user} id="backing"><td data-testid="person-backing">{totalDue([backing])}</td></Col>
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
    Component: ({ user }: { user: string; sheet: string }) => (
      <>
        <Col user={user} id="patrons"><th>Patrons</th></Col>
        <Col user={user} id="backing"><th>Backing</th></Col>
        <th />
      </>
    ),
  }),
  PersonCells.fill({ order: 40, Component: PersonPatrons }),
];
