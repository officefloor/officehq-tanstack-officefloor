import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { usePersonError } from '../people/usePersonError';
import { useUser } from '../session/useUser';
import { useSheetToday } from '../sheet/useSheetToday';
import { flagsKey, flagsQuery } from './flagsQuery';
import type { Flag } from './flagsQuery';

const DATE = /^\d{4}-\d{2}-\d{2}$/;

/**
 * The person's flag reason while it is in force (the sheet's today on or before its until-date), and the control a
 * steward flags them with: a reason and an until-date (blank: the sheet's today). While flagged, only a steward opens
 * their claims.
 */
function PersonFlag({ sheetId, personId }: { sheetId: string; personId: number }) {
  const queryClient = useQueryClient();
  const [user] = useUser();
  const today = useSheetToday(sheetId);
  const [reason, setReason] = useState('');
  const [until, setUntil] = useState('');
  const [error, setError] = usePersonError(personId);
  const once = useSubmitOnce();
  const flag = (useQuery(flagsQuery(sheetId)).data ?? []).find((f) => f.personId === personId);
  const shown = flag && today && today <= flag.until ? flag.reason : '';
  const save = useMutation({
    mutationFn: (body: { reason: string; until: string }) =>
      putJson<Flag>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/flag`, body, user),
    onSuccess: async () => {
      setReason('');
      setUntil('');
      await queryClient.invalidateQueries({ queryKey: flagsKey(sheetId) });
    },
    onError: () => setError('The person could not be flagged.'),
  });

  return (
    <td>
      <span data-testid="person-flag-shown">{shown}</span>{' '}
      <input
        data-testid="person-flag-reason"
        placeholder="reason"
        value={reason}
        onChange={(e) => {
          setReason(e.target.value);
          setError('');
        }}
      />{' '}
      <input
        data-testid="person-flag-until"
        placeholder="until yyyy-mm-dd"
        value={until}
        onChange={(e) => {
          setUntil(e.target.value);
          setError('');
        }}
      />{' '}
      <button
        type="button"
        data-testid="person-flag"
        disabled={save.isPending}
        onClick={() => {
          setError('');
          const r = reason.trim();
          const u = until.trim();
          if (r === '') {
            setError('Give a reason to flag the person.');
            return;
          }
          if (u !== '' && !DATE.test(u)) {
            setError('The until-date must be yyyy-mm-dd.');
            return;
          }
          once(() => save.mutateAsync({ reason: r, until: u }));
        }}
      >
        Flag
      </button>
    </td>
  );
}

export const contributions = [
  PersonHeaders.fill({ order: 19.2, Component: () => <th>Flag</th> }),
  PersonCells.fill({ order: 19.2, Component: PersonFlag }),
];
