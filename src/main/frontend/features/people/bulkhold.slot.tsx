import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';
import { claimsKey } from '../claims/claims';
import { poolsKey } from '../pools/pools';
import { useChangeToday } from '../sheet/sheet';
import { peopleKey, usePeople, useShowRetired } from './people';
import { clearSelectedPeople, useSelectedPeople } from './selection';

type BulkHoldResult = { held: number };

/** The bar that holds every selected person together from a from-date (blank: the sheet's today) to an optional
 *  to-date (blank: open-ended). The server holds each, in list order, as the row's hold would; the bar then shows
 *  how many were held. */
function BulkHold({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const people = usePeople(sheetId).data ?? [];
  const [showRetired] = useShowRetired();
  const chosen = useSelectedPeople(sheetId);
  const ids = people.filter((p) => (showRetired || p.retiredFrom === null) && chosen.has(p.id)).map((p) => p.id);
  const today = useChangeToday(sheetId);
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [result, setResult] = useState<BulkHoldResult | null>(null);
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const apply = useMutation({
    mutationFn: (body: { user: string; personIds: number[]; from: string; to: string }) =>
      postJson<BulkHoldResult>(`/api/sheets/${encodeURIComponent(sheetId)}/bulk-hold`, body),
    onSuccess: async (r) => {
      clearSelectedPeople(sheetId);
      setFrom('');
      setTo('');
      setResult(r);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) }),
      ]);
    },
    onError: (e) => setError(e instanceof HttpError && e.body ? e.body : 'Could not hold the selected people.'),
    onSettled: once.done,
  });

  const change = (set: (v: string) => void) => (e: { target: { value: string } }) => {
    set(e.target.value);
    setError('');
    setResult(null);
  };

  return (
    <p>
      <label>
        Hold selected from{' '}
        <input data-testid="bulk-hold-from" placeholder={today || 'yyyy-mm-dd'} value={from} onChange={change(setFrom)} />
      </label>{' '}
      <label>
        to{' '}
        <input data-testid="bulk-hold-to" placeholder="to (optional)" value={to} onChange={change(setTo)} />
      </label>{' '}
      <button
        type="button"
        data-testid="bulk-hold"
        disabled={apply.isPending}
        onClick={() => {
          setError('');
          setResult(null);
          if (ids.length === 0) {
            setError('Select the people to hold.');
            return;
          }
          if (!once.start()) return;
          apply.mutate({ user, personIds: ids, from: from.trim(), to: to.trim() });
        }}
      >
        Hold
      </button>
      {result && (
        <>
          {' '}Held <span data-testid="bulk-held">{result.held}</span>
        </>
      )}
      {error && (
        <span data-testid="person-error" role="alert">
          {' '}{error}
        </span>
      )}
    </p>
  );
}

export const contribution = SheetBody.fill({ order: 11.7, Component: BulkHold });
