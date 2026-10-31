import { createContext, useContext, useState, type ReactNode } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { claimsKey, settleBatchKey, useLastSettleBatch } from './useClaims';
import { peopleKey, usePeople } from '../people/usePeople';
import { useVoidReasons } from '../voidreasons/useVoidReasons';

type Selection = { selected: Set<string>; toggle: (id: string, on: boolean) => void; clear: () => void };

const SelectionContext = createContext<Selection | null>(null);

/** The claims ticked with their select checkbox, shared by every claim row and the bulk buttons. */
export function ClaimSelection({ children }: { children: ReactNode }) {
  const [selected, setSelected] = useState<Set<string>>(() => new Set());
  const toggle = (id: string, on: boolean) =>
    setSelected((was) => {
      const next = new Set(was);
      if (on) next.add(id);
      else next.delete(id);
      return next;
    });
  const clear = () => setSelected(new Set());
  return <SelectionContext.Provider value={{ selected, toggle, clear }}>{children}</SelectionContext.Provider>;
}

export function useClaimSelection(): Selection {
  const selection = useContext(SelectionContext);
  if (!selection) throw new Error('useClaimSelection outside ClaimSelection');
  return selection;
}

/** A claim row's select checkbox. */
export function ClaimSelect({ claimId }: { claimId: string }) {
  const { selected, toggle } = useClaimSelection();
  return (
    <input
      type="checkbox"
      data-testid="claim-select"
      checked={selected.has(claimId)}
      onChange={(e) => toggle(claimId, e.target.checked)}
    />
  );
}

// Opens the selected claims together: the server opens each that can be opened (unnumbered until settled), in list
// order (people in the order they were added, newest claim first within a person), and leaves each
// that cannot as it is. Then shows how many were opened and how many were refused, and clears the
// selection; pressing again removes the old counts until the new ones are in.
export function BulkOpenButton() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const queryClient = useQueryClient();
  const once = useSubmitOnce();
  const { selected, clear } = useClaimSelection();

  const open = useMutation({
    mutationFn: (claimIds: string[]) =>
      postJson<{ opened: number; refused: number }>(
        `/api/sheets/${encodeURIComponent(sheet)}/claims/bulk-open`,
        { claimIds, user },
      ),
    onSuccess: () => {
      clear();
      return queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
  });

  return (
    <p>
      <button
        type="button"
        data-testid="bulk-open"
        onClick={() => {
          open.reset();
          const ids = [...selected];
          once(() => open.mutateAsync(ids));
        }}
      >
        Open selected
      </button>
      {open.isSuccess && (
        <>
          {' '}
          Opened <span data-testid="bulk-opened">{open.data.opened}</span>, refused{' '}
          <span data-testid="bulk-refused">{open.data.refused}</span>
        </>
      )}
      {open.isError && (
        <span data-testid="bulk-open-error" role="alert">
          {' '}
          The selected claims could not be opened.
        </span>
      )}
    </p>
  );
}

// Settles the selected claims together: the server settles each by the rules of settling one claim, in
// list order, by the signed-in user; a claim sent to PENDING counts as settled, one that cannot be
// settled is left as it is. Then shows how many were settled and how many were refused, and clears
// the selection; pressing again removes the old counts until the new ones are in. Beside it, the number of the
// sheet's last batch: a bulk settle that settled at least one claim.
export function BulkSettleButton() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const queryClient = useQueryClient();
  const once = useSubmitOnce();
  const { selected, clear } = useClaimSelection();
  const lastBatch = useLastSettleBatch(sheet);

  const settle = useMutation({
    mutationFn: (claimIds: string[]) =>
      postJson<{ settled: number; refused: number }>(
        `/api/sheets/${encodeURIComponent(sheet)}/claims/bulk-settle`,
        { claimIds, user },
      ),
    onSuccess: () => {
      clear();
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: settleBatchKey(sheet) }),
      ]);
    },
  });

  return (
    <p>
      <button
        type="button"
        data-testid="bulk-settle"
        onClick={() => {
          settle.reset();
          const ids = [...selected];
          once(() => settle.mutateAsync(ids));
        }}
      >
        Settle selected
      </button>{' '}
      Last batch <span data-testid="bulk-settle-batch">{lastBatch}</span>
      {settle.isSuccess && (
        <>
          {' '}
          Settled <span data-testid="bulk-settled">{settle.data.settled}</span>, refused{' '}
          <span data-testid="bulk-settle-refused">{settle.data.refused}</span>
        </>
      )}
      {settle.isError && (
        <span data-testid="bulk-settle-error" role="alert">
          {' '}
          The selected claims could not be settled.
        </span>
      )}
    </p>
  );
}

// Moves the selected claims to the chosen person together: the server moves each by the rules of moving one claim,
// in list order, by the signed-in user, and leaves each that cannot move as it is. Then shows how many were moved
// and how many were refused, and clears the selection; pressing again removes the old counts until the new ones are in.
export function BulkReassignButton() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const queryClient = useQueryClient();
  const once = useSubmitOnce();
  const people = usePeople(sheet);
  const { selected, clear } = useClaimSelection();
  const [toPersonId, setToPersonId] = useState('');

  const reassign = useMutation({
    mutationFn: ({ claimIds, to }: { claimIds: string[]; to: string }) =>
      postJson<{ reassigned: number; refused: number }>(
        `/api/sheets/${encodeURIComponent(sheet)}/claims/bulk-reassign`,
        { claimIds, toPersonId: to, user },
      ),
    onSuccess: () => {
      clear();
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
      ]);
    },
  });

  return (
    <p>
      <select
        data-testid="bulk-reassign-to"
        value={toPersonId}
        onChange={(e) => {
          reassign.reset();
          setToPersonId(e.target.value);
        }}
      >
        <option value="">Move to…</option>
        {people.map((p) => (
          <option key={p.id} value={p.id}>
            {p.name}
          </option>
        ))}
      </select>{' '}
      <button
        type="button"
        data-testid="bulk-reassign"
        onClick={() => {
          reassign.reset();
          const claimIds = [...selected];
          const to = toPersonId;
          once(() => reassign.mutateAsync({ claimIds, to }));
        }}
      >
        Reassign selected
      </button>
      {reassign.isSuccess && (
        <>
          {' '}
          Moved <span data-testid="bulk-reassigned">{reassign.data.reassigned}</span>, refused{' '}
          <span data-testid="bulk-reassign-refused">{reassign.data.refused}</span>
        </>
      )}
      {reassign.isError && (
        <span data-testid="bulk-reassign-error" role="alert">
          {' '}
          The selected claims could not be moved.
        </span>
      )}
    </p>
  );
}

// Re-dates the selected claims to the entered date together: the server re-dates each by the rules of re-dating one
// claim, in list order, by the signed-in user, and leaves each that cannot be re-dated as it is. Then shows how many
// were re-dated and how many were refused, and clears the selection; pressing again removes the old counts until the
// new ones are in.
export function BulkRedateButton() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const queryClient = useQueryClient();
  const once = useSubmitOnce();
  const { selected, clear } = useClaimSelection();
  const [date, setDate] = useState('');

  const redate = useMutation({
    mutationFn: ({ claimIds, date }: { claimIds: string[]; date: string }) =>
      postJson<{ redated: number; refused: number }>(
        `/api/sheets/${encodeURIComponent(sheet)}/claims/bulk-redate`,
        { claimIds, date, user },
      ),
    onSuccess: () => {
      clear();
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
      ]);
    },
  });

  return (
    <p>
      <input
        type="date"
        data-testid="bulk-redate-date"
        value={date}
        onChange={(e) => {
          redate.reset();
          setDate(e.target.value);
        }}
      />{' '}
      <button
        type="button"
        data-testid="bulk-redate"
        onClick={() => {
          redate.reset();
          const claimIds = [...selected];
          const to = date;
          once(() => redate.mutateAsync({ claimIds, date: to }));
        }}
      >
        Re-date selected
      </button>
      {redate.isSuccess && (
        <>
          {' '}
          Re-dated <span data-testid="bulk-redated">{redate.data.redated}</span>, refused{' '}
          <span data-testid="bulk-redate-refused">{redate.data.refused}</span>
        </>
      )}
      {redate.isError && (
        <span data-testid="bulk-redate-error" role="alert">
          {' '}
          The selected claims could not be re-dated.
        </span>
      )}
    </p>
  );
}

// Voids the selected claims together: the server voids each by the rules of voiding one claim, in list order, by the
// signed-in user, with the reason chosen beside the button (shown while the sheet has void reasons), and leaves each
// that cannot be voided as it is. Then shows how many were voided and how many were refused, and clears the
// selection; pressing again removes the old counts until the new ones are in.
export function BulkVoidButton() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const queryClient = useQueryClient();
  const once = useSubmitOnce();
  const voidReasons = useVoidReasons(sheet);
  const { selected, clear } = useClaimSelection();
  const [reason, setReason] = useState('');

  const voiding = useMutation({
    mutationFn: ({ claimIds, reason }: { claimIds: string[]; reason?: string }) =>
      postJson<{ voided: number; refused: number }>(
        `/api/sheets/${encodeURIComponent(sheet)}/claims/bulk-void`,
        { claimIds, reason, user },
      ),
    onSuccess: () => {
      clear();
      setReason('');
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
      ]);
    },
  });

  return (
    <p>
      {voidReasons.length > 0 && (
        <>
          <select
            data-testid="bulk-void-reason"
            value={reason}
            onChange={(e) => {
              voiding.reset();
              setReason(e.target.value);
            }}
          >
            <option value="">Void reason…</option>
            {voidReasons.map((r) => (
              <option key={r} value={r}>
                {r}
              </option>
            ))}
          </select>{' '}
        </>
      )}
      <button
        type="button"
        data-testid="bulk-void"
        onClick={() => {
          voiding.reset();
          const claimIds = [...selected];
          const chosen = voidReasons.length > 0 ? reason : undefined;
          once(() => voiding.mutateAsync({ claimIds, reason: chosen }));
        }}
      >
        Void selected
      </button>
      {voiding.isSuccess && (
        <>
          {' '}
          Voided <span data-testid="bulk-voided">{voiding.data.voided}</span>, refused{' '}
          <span data-testid="bulk-void-refused">{voiding.data.refused}</span>
        </>
      )}
      {voiding.isError && (
        <span data-testid="bulk-void-error" role="alert">
          {' '}
          The selected claims could not be voided.
        </span>
      )}
    </p>
  );
}
