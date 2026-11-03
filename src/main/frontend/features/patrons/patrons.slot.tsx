import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonColumnHead } from '../../slots/defs/personRow';
import { patronsKey, shownName, usePatrons, usePeople, type Person } from '../people/usePeople';
import { useClaims } from '../claims/useClaims';
import { hundredths, money, sumDues } from '../rates/useRates';
import { useSheetToday } from '../today/useSheetToday';
import { useFigure } from '../locale/useLocale';
import { Col } from '../columns/useColumns';

/** The first day (yyyy-mm-dd) of the 90 days up to and including a day. */
function windowStart(today: string): string {
  return new Date(Date.parse(`${today}T00:00:00Z`) - 89 * 86_400_000).toISOString().slice(0, 10);
}

/** Shares (in hundredths) for patrons in the order they were named: 100.00 split equally, each share rounded
 *  half to even to two decimals, the remainder (100.00 less the shares added up) given to the first-named. */
function shares(count: number): bigint[] {
  if (count === 0) return [];
  const n = BigInt(count);
  let each = 10000n / n;
  const rest = 10000n % n;
  if (rest * 2n > n || (rest * 2n === n && each % 2n === 1n)) each += 1n;
  const all = Array.from({ length: count }, () => each);
  all[0] += 10000n - each * n;
  return all;
}

/** A person's patrons: the other people on the sheet they have named, each with a share (shown as
 *  `<name> (<grade>) <share>`, sorted by name, comma and space separated), their backing (over their patrons, the patron's
 *  figure — the due frozen when each of their SETTLED claims dated within the 90 days up to and including the
 *  sheet's today was settled, added up — times their share divided by 100, added up and rounded half to even;
 *  0.00 with none), and a picker that names one more. Naming themselves, or a patron twice, is refused. */
function PersonPatrons({ sheetId, personId }: { sheetId: string; personId: number }) {
  const fig = useFigure();
  const people = usePeople(sheetId);
  const patrons = usePatrons(sheetId);
  const claims = useClaims(sheetId);
  const today = useSheetToday(sheetId).data?.today;
  const queryClient = useQueryClient();
  const [chosen, setChosen] = useState('');

  const named = (patrons.data ?? [])
    .filter((x) => x.personId === personId)
    .map((x) => people.data?.find((p) => p.id === x.patronId))
    .filter((p): p is Person => p !== undefined);
  const split = shares(named.length);
  const labels = named
    .map((p, i) => ({ name: p.name, shown: shownName(p), share: split[i] }))
    .sort((a, b) => (a.name < b.name ? -1 : a.name > b.name ? 1 : 0))
    .map((x) => `${x.shown} ${money(x.share)}`);
  const from = today === undefined ? undefined : windowStart(today);
  const figure = (patronId: number) => hundredths(sumDues((claims.data ?? [])
    .filter((c) => c.personId === patronId && c.status === 'SETTLED' && c.due !== null
      && from !== undefined && today !== undefined && c.date >= from && c.date <= today)
    .map((c) => c.due as string)));
  // In 1/10000 of a hundredth: figure (hundredths) times share (hundredths of a percent).
  const signed = named.reduce((sum, p, i) => sum + figure(p.id) * split[i], 0n);
  const size = signed < 0n ? -signed : signed;
  let cents = size / 10000n;
  const rest = size % 10000n;
  if (rest > 5000n || (rest === 5000n && cents % 2n === 1n)) cents += 1n;
  const backing = money(signed < 0n ? -cents : cents);

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
      <Col id="patrons"><td data-testid="person-patrons">{labels.join(', ')}</td></Col>
      <Col id="backing"><td data-testid="person-backing">{fig(backing)}</td></Col>
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
        <Col id="patrons"><th>Patrons</th></Col>
        <Col id="backing"><th>Backing</th></Col>
        <th />
      </>
    ),
  }),
  PersonCells.fill({ order: 50, Component: PersonPatrons }),
];
