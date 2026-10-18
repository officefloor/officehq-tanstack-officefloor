import { useState } from 'react';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { PeopleBar } from '../../slots/defs/peopleBar';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { peopleKey, retirePerson, usePeople, useShowRetired } from './people';

const DATE = /^\d{4}-\d{2}-\d{2}$/;

function RetiredColumns() {
  return (
    <>
      <th>Retired</th>
      <th>Retired from</th>
      <th>Retire</th>
    </>
  );
}

// Whether a person is retired and from when; and, while not retired, a date (empty: the sheet's today)
// and a button that retires them. A refusal shows on the row.
function PersonRetired({ personId }: { personId: number }) {
  const [sheet] = useCurrentSheet();
  const [user] = useCurrentUser();
  const person = (usePeople().data ?? []).find((p) => p.id === personId);
  const [date, setDate] = useState('');
  const [error, setError] = useState('');
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const retire = useMutation({
    mutationFn: (from: string) => retirePerson(sheet!, user, personId, from),
    onSuccess: async () => {
      setDate('');
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet!) });
    },
    onError: async () => {
      setError('The person could not be retired: they may already be retired.');
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet!) });
    },
  });

  const submit = () => {
    setError('');
    if (person?.retiredFrom) {
      setError(`${person.name} is already retired.`);
      return;
    }
    const from = date.trim();
    if (from !== '' && !DATE.test(from)) {
      setError('The retired-from date must be a date (yyyy-mm-dd).');
      return;
    }
    once(() => retire.mutateAsync(from));
  };

  return (
    <>
      <td data-testid="person-retired">{person?.retiredFrom ? 'yes' : 'no'}</td>
      <td data-testid="person-retired-from">{person?.retiredFrom ?? ''}</td>
      <td>
        <input
          type="date"
          data-testid="person-retire-date"
          value={date}
          onChange={(e) => {
            setDate(e.target.value);
            setError('');
          }}
        />
        <button type="button" data-testid="person-retire" onClick={submit} disabled={retire.isPending}>
          Retire
        </button>
        {error && <span data-testid="person-error">{error}</span>}
      </td>
    </>
  );
}

// The "show retired" toggle: retired people are listed only while it is on.
function ShowRetired() {
  const [sheet] = useCurrentSheet();
  const [show, setShow] = useShowRetired();
  if (!sheet) return null;
  return (
    <label>
      <input
        type="checkbox"
        data-testid="show-retired"
        checked={show}
        onChange={(e) => setShow(e.target.checked ? true : undefined)}
      />{' '}
      Show retired
    </label>
  );
}

export const contributions = [
  PersonColumns.fill({ order: 30, Component: RetiredColumns }),
  PersonCells.fill({ order: 30, Component: PersonRetired }),
  PeopleBar.fill({ order: 20, Component: ShowRetired }),
];
