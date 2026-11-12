import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { HomeMain } from '../../slots/defs/homeMain';
import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person, peopleKey, personActionKey, useLatestPersonAction, useShowRetired } from '../people/people';
import { useCurrentSheet, useCurrentUser } from '../../session/session';

// The "show retired" toggle: the people list leaves retired people out unless it is on. It belongs
// to the screen, so loading a sheet leaves it as it is.
function ShowRetired() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [showRetired, setShowRetired] = useShowRetired();

  if (!user || !sheet) return null;
  return (
    <label>
      <input
        type="checkbox"
        data-testid="show-retired"
        checked={showRetired}
        onChange={(e) => setShowRetired(e.target.checked || undefined)}
      />{' '}
      Show retired
    </label>
  );
}

function RetiredHead() {
  return <th>Retired</th>;
}

// Whether the person is retired and from when, their past retirements (one line each), the date and
// button that retire them (a blank date takes the sheet's today) and the button that unretires them on
// the sheet's today. A refused retirement or unretirement shows its error until the next is tried.
function PersonRetired({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [date, setDate] = useState('');
  const queryClient = useQueryClient();

  const retire = useMutation({
    mutationKey: personActionKey(sheet, person.id),
    mutationFn: (from: string) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/retire`, { date: from, user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
      setDate('');
    },
  });

  const unretire = useMutation({
    mutationKey: personActionKey(sheet, person.id),
    mutationFn: () =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/unretire`, { user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
    },
  });

  const latest = useLatestPersonAction(sheet, person.id);

  return (
    <td>
      <span data-testid="person-retired">{person.retired ? 'yes' : 'no'}</span>{' '}
      <span data-testid="person-retired-from">{person.retiredFrom ?? ''}</span>{' '}
      {person.pastRetirements.map((r, i) => (
        <div key={i} data-testid="person-retirement-line">
          {r.from} to {r.to}
        </div>
      ))}
      <input
        type="date"
        data-testid="person-retire-date"
        value={date}
        onChange={(e) => {
          retire.reset();
          setDate(e.target.value);
        }}
      />
      <button
        type="button"
        data-testid="person-retire"
        disabled={retire.isPending}
        onClick={() => {
          retire.reset();
          retire.mutate(date);
        }}
      >
        Retire
      </button>
      <button
        type="button"
        data-testid="person-unretire"
        disabled={unretire.isPending}
        onClick={() => {
          unretire.reset();
          unretire.mutate();
        }}
      >
        Unretire
      </button>
      {retire.isError && retire.submittedAt >= latest && (
        <span data-testid="person-error" role="alert">
          {' '}
          {retire.error instanceof HttpError && retire.error.reason ? `${retire.error.reason}.` : 'The retirement was refused.'}
        </span>
      )}
      {unretire.isError && unretire.submittedAt >= latest && (
        <span data-testid="person-error" role="alert">
          {' '}
          {unretire.error instanceof HttpError && unretire.error.reason
            ? `${unretire.error.reason}.`
            : 'The unretirement was refused.'}
        </span>
      )}
    </td>
  );
}

export const contributions = [
  HomeMain.fill({ order: 18, Component: ShowRetired }),
  PersonColumnHeads.fill({ order: 19, Component: RetiredHead }),
  PersonColumns.fill({ order: 19, Component: PersonRetired }),
];
