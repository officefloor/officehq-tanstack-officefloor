import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { useUser } from '../session/useUser';
import { peopleKey, peopleQuery } from './peopleQuery';
import type { Person } from './peopleQuery';

/**
 * Whether the person is on hold, and the control that puts them on hold or takes them off it. While held,
 * their OPEN claims count in no live total; their SETTLED ones still do.
 */
function PersonHold({ sheetId, personId }: { sheetId: string; personId: number }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const [error, setError] = useState('');
  const once = useSubmitOnce();
  const person = (useQuery(peopleQuery(sheetId)).data ?? []).find((p) => p.id === personId);
  const hold = useMutation({
    mutationFn: () =>
      putJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/hold`, {}, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
    onError: () => setError('The hold could not be changed.'),
  });

  return (
    <td>
      <span data-testid="person-hold">{person?.held ? 'yes' : 'no'}</span>{' '}
      <button
        type="button"
        data-testid="person-hold-toggle"
        disabled={hold.isPending}
        onClick={() => {
          setError('');
          once(() => hold.mutateAsync());
        }}
      >
        {person?.held ? 'Release' : 'Hold'}
      </button>
      {error && <span data-testid="person-error">{error}</span>}
    </td>
  );
}

export const contributions = [
  PersonHeaders.fill({ order: 19, Component: () => <th>Hold</th> }),
  PersonCells.fill({ order: 19, Component: PersonHold }),
];
