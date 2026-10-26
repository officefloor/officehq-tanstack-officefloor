import { useState, useSyncExternalStore } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { ClaimCells } from '../../slots/defs/claimRow';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useUser } from '../session/useSession';
import { claimsKey, useLastSettleBatch } from '../claims/useClaims';
import { usePeople } from '../people/people.slot';
import { useVoidReasons } from '../voidreasons/voidReasons.slot';

// The claims selected for a bulk action. Kept in a small store rather than the URL so a checkbox
// shows its new state on the same click (a URL update lands a render later). It belongs to one sheet:
// loading another starts with nothing selected.
const NONE: ReadonlySet<number> = new Set();
let selected: { sheetId: string; ids: ReadonlySet<number> } = { sheetId: '', ids: NONE };
const listeners = new Set<() => void>();
const selection = {
  subscribe: (l: () => void) => {
    listeners.add(l);
    return () => {
      listeners.delete(l);
    };
  },
  get: () => selected,
  set: (sheetId: string, ids: ReadonlySet<number>) => {
    selected = { sheetId, ids };
    listeners.forEach((l) => l());
  },
};
function useSelected(sheetId: string): ReadonlySet<number> {
  const current = useSyncExternalStore(selection.subscribe, selection.get);
  return current.sheetId === sheetId ? current.ids : NONE;
}

// A claim's checkbox selecting it for a bulk action.
function ClaimSelect({ sheetId, claim }: { sheetId: string; user: string; personId: number; claim: { id: number } }) {
  const chosen = useSelected(sheetId);
  return (
    <input
      type="checkbox"
      data-testid="claim-select"
      checked={chosen.has(claim.id)}
      onChange={(e) => {
        const next = new Set(chosen);
        if (e.target.checked) next.add(claim.id);
        else next.delete(claim.id);
        selection.set(sheetId, next);
      }}
    />
  );
}

// The number of the bulk settle batch that settled the claim; nothing when none did.
function ClaimBatch({ claim }: { sheetId: string; user: string; personId: number; claim: { settleBatch?: number | null } }) {
  return <span data-testid="claim-batch">{claim.settleBatch ?? ''}</span>;
}

type BulkOpenResult = { opened: number; refused: number };

// Opens the selected claims together; the server numbers them in list order and leaves any it cannot open
// as they are. Shows how many were opened and how many refused; the counts are gone while it runs.
function BulkOpen({ sheetId }: { sheetId: string; user: string }) {
  const chosen = useSelected(sheetId);
  const [user] = useUser();
  const queryClient = useQueryClient();
  const open = useMutation({
    mutationFn: (ids: number[]) => {
      if (ids.length === 0) throw new Error('Select the claims to open');
      return postJson<BulkOpenResult>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/bulk-open`, { ids, by: user ?? '' });
    },
    onSuccess: async () => {
      selection.set(sheetId, NONE);
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });

  return (
    <section data-testid="bulk-actions">
      <button type="button" data-testid="bulk-open" disabled={open.isPending} onClick={() => open.mutate([...chosen])}>
        Open selected
      </button>{' '}
      {open.isSuccess && (
        <span>
          Opened <strong data-testid="bulk-opened">{open.data.opened}</strong>, refused{' '}
          <strong data-testid="bulk-refused">{open.data.refused}</strong>
        </span>
      )}
      {open.isError && (
        <span data-testid="bulk-open-error" role="alert">
          {open.error.message}
        </span>
      )}
    </section>
  );
}

type BulkSettleResult = { settled: number; refused: number };

// Settles the selected claims together; the server settles them in list order exactly as settling each alone
// (a clerk's settle above the limit leaves one PENDING, counted as settled) — all or nothing: if any one cannot be
// settled, none is and all count as refused. Shows how many were settled and how many refused; the counts are gone while it runs.
function BulkSettle({ sheetId }: { sheetId: string; user: string }) {
  const chosen = useSelected(sheetId);
  const [user] = useUser();
  const lastBatch = useLastSettleBatch(sheetId);
  const queryClient = useQueryClient();
  const settle = useMutation({
    mutationFn: (ids: number[]) => {
      if (ids.length === 0) throw new Error('Select the claims to settle');
      return postJson<BulkSettleResult>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/bulk-settle`, { ids, by: user ?? '' });
    },
    onSuccess: async () => {
      selection.set(sheetId, NONE);
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });

  return (
    <section data-testid="bulk-settle-actions">
      <button type="button" data-testid="bulk-settle" disabled={settle.isPending} onClick={() => settle.mutate([...chosen])}>
        Settle selected
      </button>{' '}
      <span data-testid="bulk-settle-batch">{lastBatch.data?.batch ?? ''}</span>{' '}
      {settle.isSuccess && (
        <span>
          Settled <strong data-testid="bulk-settled">{settle.data.settled}</strong>, refused{' '}
          <strong data-testid="bulk-settle-refused">{settle.data.refused}</strong>
        </span>
      )}
      {settle.isError && (
        <span data-testid="bulk-settle-error" role="alert">
          {settle.error.message}
        </span>
      )}
    </section>
  );
}

type BulkReassignResult = { reassigned: number; refused: number };

// Moves the selected claims to the chosen person together; the server moves them in list order exactly as moving
// each alone and leaves any it cannot move as they are. Shows how many were moved and how many refused; the counts
// are gone while it runs.
function BulkReassign({ sheetId }: { sheetId: string; user: string }) {
  const chosen = useSelected(sheetId);
  const [user] = useUser();
  const people = usePeople(sheetId);
  const [moveTo, setMoveTo] = useState('');
  const queryClient = useQueryClient();
  const reassign = useMutation({
    mutationFn: ({ ids, personId }: { ids: number[]; personId: string }) => {
      if (ids.length === 0) throw new Error('Select the claims to move');
      if (personId === '') throw new Error('Choose the person to move the claims to');
      return postJson<BulkReassignResult>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/bulk-reassign`,
        { ids, personId: Number(personId), by: user ?? '' });
    },
    onSuccess: async () => {
      selection.set(sheetId, NONE);
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });

  return (
    <section data-testid="bulk-reassign-actions">
      <select
        data-testid="bulk-reassign-to"
        value={moveTo}
        onChange={(e) => {
          reassign.reset();
          setMoveTo(e.target.value);
        }}
      >
        <option value="">Move to…</option>
        {(people.data ?? []).map((p) => (
          <option key={p.id} value={String(p.id)}>
            {p.name}
          </option>
        ))}
      </select>{' '}
      <button
        type="button"
        data-testid="bulk-reassign"
        disabled={reassign.isPending}
        onClick={() => reassign.mutate({ ids: [...chosen], personId: moveTo })}
      >
        Move selected
      </button>{' '}
      {reassign.isSuccess && (
        <span>
          Moved <strong data-testid="bulk-reassigned">{reassign.data.reassigned}</strong>, refused{' '}
          <strong data-testid="bulk-reassign-refused">{reassign.data.refused}</strong>
        </span>
      )}
      {reassign.isError && (
        <span data-testid="bulk-reassign-error" role="alert">
          {reassign.error.message}
        </span>
      )}
    </section>
  );
}

type BulkRedateResult = { redated: number; refused: number };

// Re-dates the selected claims to one date together; the server re-dates them in list order exactly as each row's
// re-date and leaves any it cannot re-date as they are. Shows how many were re-dated and how many refused; the counts
// are gone while it runs.
function BulkRedate({ sheetId }: { sheetId: string; user: string }) {
  const chosen = useSelected(sheetId);
  const [user] = useUser();
  const [date, setDate] = useState('');
  const queryClient = useQueryClient();
  const redate = useMutation({
    mutationFn: ({ ids, date }: { ids: number[]; date: string }) => {
      if (ids.length === 0) throw new Error('Select the claims to re-date');
      if (date.trim() === '') throw new Error("Enter the claims' new date");
      return postJson<BulkRedateResult>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/bulk-redate`,
        { ids, date: date.trim(), by: user ?? '' });
    },
    onSuccess: async () => {
      selection.set(sheetId, NONE);
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });

  return (
    <section data-testid="bulk-redate-actions">
      <input
        type="text"
        data-testid="bulk-redate-date"
        placeholder="yyyy-mm-dd"
        value={date}
        onChange={(e) => {
          redate.reset();
          setDate(e.target.value);
        }}
      />{' '}
      <button
        type="button"
        data-testid="bulk-redate"
        disabled={redate.isPending}
        onClick={() => redate.mutate({ ids: [...chosen], date })}
      >
        Re-date selected
      </button>{' '}
      {redate.isSuccess && (
        <span>
          Re-dated <strong data-testid="bulk-redated">{redate.data.redated}</strong>, refused{' '}
          <strong data-testid="bulk-redate-refused">{redate.data.refused}</strong>
        </span>
      )}
      {redate.isError && (
        <span data-testid="bulk-redate-error" role="alert">
          {redate.error.message}
        </span>
      )}
    </section>
  );
}

type BulkVoidResult = { voided: number; refused: number };

// Voids the selected claims together with the chosen void reason (offered while the sheet has void reasons); the server
// voids them in list order exactly as each row's void and leaves any it cannot void as they are. Shows how many were
// voided and how many refused; the counts are gone while it runs.
function BulkVoid({ sheetId }: { sheetId: string; user: string }) {
  const chosen = useSelected(sheetId);
  const [user] = useUser();
  const voidReasons = useVoidReasons(sheetId);
  const [reason, setReason] = useState('');
  const queryClient = useQueryClient();
  const voidAll = useMutation({
    mutationFn: ({ ids, reason }: { ids: number[]; reason: string }) => {
      if (ids.length === 0) throw new Error('Select the claims to void');
      return postJson<BulkVoidResult>(`/api/sheets/${encodeURIComponent(sheetId)}/claims/bulk-void`,
        { ids, reason, by: user ?? '' });
    },
    onSuccess: async () => {
      selection.set(sheetId, NONE);
      setReason('');
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });
  const reasons = voidReasons.data ?? [];

  return (
    <section data-testid="bulk-void-actions">
      {reasons.length > 0 && (
        <>
          <select
            data-testid="bulk-void-reason"
            value={reason}
            onChange={(e) => {
              voidAll.reset();
              setReason(e.target.value);
            }}
          >
            <option value="">Void reason…</option>
            {reasons.map((r) => (
              <option key={r.reason} value={r.reason}>
                {r.reason}
              </option>
            ))}
          </select>{' '}
        </>
      )}
      <button
        type="button"
        data-testid="bulk-void"
        disabled={voidAll.isPending}
        onClick={() => voidAll.mutate({ ids: [...chosen], reason })}
      >
        Void selected
      </button>{' '}
      {voidAll.isSuccess && (
        <span>
          Voided <strong data-testid="bulk-voided">{voidAll.data.voided}</strong>, refused{' '}
          <strong data-testid="bulk-void-refused">{voidAll.data.refused}</strong>
        </span>
      )}
      {voidAll.isError && (
        <span data-testid="bulk-void-error" role="alert">
          {voidAll.error.message}
        </span>
      )}
    </section>
  );
}

export const contributions = [
  ClaimCells.fill({ order: 0, Component: ClaimSelect }),
  ClaimCells.fill({ order: 21, Component: ClaimBatch }),
  SheetBody.fill({ order: 15, Component: BulkOpen }),
  SheetBody.fill({ order: 16, Component: BulkSettle }),
  SheetBody.fill({ order: 16.5, Component: BulkReassign }),
  SheetBody.fill({ order: 16.6, Component: BulkRedate }),
  SheetBody.fill({ order: 16.7, Component: BulkVoid }),
];
