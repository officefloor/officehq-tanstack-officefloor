import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { usePersonAction } from './usePersonAction';
import { peopleKey, type Person } from './usePeople';

// The person's flag while in force (its reason; nothing after its until date), and the form a steward flags them
// with: a reason and an until date. While it shows, only a steward may open the person's claims. Anyone else, or a
// blank reason, is refused with the person's error.
export function FlagControl({ personId, shown }: { personId: string; shown: string }) {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const [reason, setReason] = useState('');
  const [until, setUntil] = useState('');
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const flag = useMutation({
    mutationFn: (body: { reason: string; until: string }) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${encodeURIComponent(personId)}/flag`, {
        ...body,
        user,
      }),
    onSuccess: () => {
      setReason('');
      setUntil('');
      return queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
    },
  });
  const taken = usePersonAction(personId, () => flag.reset());

  const changed = (set: (v: string) => void) => (v: string) => {
    set(v);
    flag.reset();
  };

  return (
    <>
      <span data-testid="person-flag-shown">{shown}</span>{' '}
      <input
        data-testid="person-flag-reason"
        value={reason}
        placeholder="Flag reason"
        onChange={(e) => changed(setReason)(e.target.value)}
      />{' '}
      <input
        type="date"
        data-testid="person-flag-until"
        value={until}
        title="Flagged until"
        onChange={(e) => changed(setUntil)(e.target.value)}
      />{' '}
      <button
        type="button"
        data-testid="person-flag"
        onClick={() => {
          taken();
          once(() => flag.mutateAsync({ reason: reason.trim(), until: until.trim() }));
        }}
      >
        Flag
      </button>
      {flag.isError && (
        <span data-testid="person-error" role="alert">
          {' '}
          The person could not be flagged.
        </span>
      )}
    </>
  );
}
