import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { poolsKey } from '../pools/usePools';
import { usePersonAction } from './usePersonAction';
import { peopleKey, type HoldFrom, type Person } from './usePeople';

// The person's holds, one line each (from – to, open-ended when no to-date), and the form that adds
// one: a from-date (blank takes the sheet's today) and an optional to-date. The person's OPEN claims
// dated within a hold, unless bound by a statement, count in no live total; a hold ends on its
// to-date. A refusal shows the person's error.
export function HoldControl({ personId, holds }: { personId: string; holds: HoldFrom[] }) {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const hold = useMutation({
    mutationFn: (dates: { from: string; to: string }) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${encodeURIComponent(personId)}/hold`, {
        ...dates,
        user,
      }),
    onSuccess: () => {
      setFrom('');
      setTo('');
      void queryClient.invalidateQueries({ queryKey: poolsKey(sheet) });
      return queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
    },
  });
  const taken = usePersonAction(personId, () => hold.reset());

  const changed = (set: (v: string) => void) => (v: string) => {
    set(v);
    hold.reset();
  };

  return (
    <>
      {holds.map((h, i) => (
        <span key={i} data-testid="person-hold-line">
          held {h.from} – {h.to || 'open'}{' '}
        </span>
      ))}
      <input
        type="date"
        data-testid="person-hold-from"
        value={from}
        title="Hold from (blank: the sheet's today)"
        onChange={(e) => changed(setFrom)(e.target.value)}
      />{' '}
      <input
        type="date"
        data-testid="person-hold-to"
        value={to}
        title="Hold to (blank: open-ended)"
        onChange={(e) => changed(setTo)(e.target.value)}
      />{' '}
      <button
        type="button"
        data-testid="person-hold-toggle"
        onClick={() => {
          taken();
          once(() => hold.mutateAsync({ from: from.trim(), to: to.trim() }));
        }}
      >
        Hold
      </button>
      {hold.isError && (
        <span data-testid="person-error" role="alert">
          {' '}
          The person could not be put on hold.
        </span>
      )}
    </>
  );
}
