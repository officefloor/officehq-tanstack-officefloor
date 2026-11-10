import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import { gradeOn, usePeople, peopleKey } from '../people/people';
import type { Person } from '../people/people';
import { useClaims } from '../claims/claims';
import { addDays, claimDue, rateOn, sumDue, useRates } from '../rates/rates';
import { useSheetToday } from '../sheet/sheet';
import { useFloor, useLate } from '../access/access';
import { useUser } from '../session/session';

function PatronsHead() {
  return (
    <>
      <th>Patrons</th>
      <th>Backing</th>
      <th />
    </>
  );
}

// A person's patrons (names, sorted), their backing (the patrons' settled figures over their SETTLED
// claims dated within the 90 days up to and including the sheet's today, added up, skipping n/a), and a picker that names one more person on the sheet as a patron.
function PersonPatrons({ sheetId, person }: { sheetId: string; person: Person }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const [choice, setChoice] = useState('');
  const [error, setError] = useState('');
  const once = useOnce();

  const patrons = people.filter((p) => person.patronIds.includes(p.id));
  const names = patrons.map((p) => p.name).sort((a, b) => a.localeCompare(b)).join(', ');
  const since = today === '' ? '' : addDays(today, -89);
  const backing = sumDue(
    patrons.flatMap((p) =>
      claims
        .filter((c) => c.personId === p.id && c.status === 'SETTLED' && c.date >= since && c.date <= today)
        .map((c) => claimDue(c, rateOn(rates, gradeOn(p.grades, c.date), c.date), floor, late, gradeOn(p.grades, c.date))),
    ),
  );

  const add = useMutation({
    mutationFn: (patronId: string) =>
      postJson<Person>(
        `/api/sheets/${encodeURIComponent(sheetId)}/people/${encodeURIComponent(person.id)}/patrons`,
        { patronId },
        user,
      ),
    onSuccess: () => {
      setChoice('');
      return queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
    onError: () => setError('That person could not be named as a patron.'),
  });

  const submit = () => {
    setError('');
    once((done) => add.mutate(choice, { onSettled: done }));
  };

  return (
    <>
      <td data-testid="person-patrons">{names}</td>
      <td data-testid="person-backing">{backing}</td>
      <td>
        <select
          data-testid="person-patron-select"
          value={choice}
          onChange={(e) => {
            setChoice(e.target.value);
            setError('');
          }}
        >
          <option value="">—</option>
          {people.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}
            </option>
          ))}
        </select>
        <button type="button" data-testid="person-patron-add" onClick={submit} disabled={add.isPending}>
          Add patron
        </button>
        {error && (
          <p role="alert" data-testid="person-patron-error">
            {error}
          </p>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonHead.fill({ order: 60, Component: PatronsHead }),
  PersonRow.fill({ order: 60, Component: PersonPatrons }),
];
