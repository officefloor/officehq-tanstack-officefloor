import { useState } from 'react';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { holdPerson, peopleKey, usePeople } from './people';

function HoldColumns() {
  return (
    <>
      <th>On hold</th>
      <th>Hold</th>
    </>
  );
}

// Whether a person is on hold, and a button that puts them on hold or releases them. A refusal shows
// on the row.
function PersonHold({ personId }: { personId: number }) {
  const [sheet] = useCurrentSheet();
  const [user] = useCurrentUser();
  const person = (usePeople().data ?? []).find((p) => p.id === personId);
  const [error, setError] = useState('');
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const hold = useMutation({
    mutationFn: (held: boolean) => holdPerson(sheet!, user, personId, held),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: peopleKey(sheet!) }),
    onError: async () => {
      setError('The hold could not be changed: someone may have changed it already.');
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheet!) });
    },
  });

  const toggle = () => {
    setError('');
    if (!person) return;
    once(() => hold.mutateAsync(!person.held));
  };

  return (
    <>
      <td data-testid="person-hold">{person?.held ? 'yes' : 'no'}</td>
      <td>
        <button type="button" data-testid="person-hold-toggle" onClick={toggle} disabled={hold.isPending}>
          {person?.held ? 'Release' : 'Hold'}
        </button>
        {error && <span data-testid="person-error">{error}</span>}
      </td>
    </>
  );
}

export const contributions = [
  PersonColumns.fill({ order: 35, Component: HoldColumns }),
  PersonCells.fill({ order: 35, Component: PersonHold }),
];
