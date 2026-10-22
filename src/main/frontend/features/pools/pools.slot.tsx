import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';
import { usePeople } from '../people/people';
import { poolsKey, usePools } from './pools';
import type { Pool } from './pools';

/** A share as typed: a percent of at most two decimals, more than 0.00 and at most 100.00. */
function validShare(share: string): boolean {
  if (!/^\d+(\.\d{1,2})?$/.test(share)) return false;
  const n = Number(share);
  return n > 0 && n <= 100;
}

/** One pool: its total share, whether balanced, each member's share, and the entry a member is added with. */
function PoolRow({ sheetId, user, pool }: { sheetId: string; user: string; pool: Pool }) {
  const queryClient = useQueryClient();
  const people = usePeople(sheetId).data ?? [];
  const [personId, setPersonId] = useState('');
  const [share, setShare] = useState('');
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const add = useMutation({
    mutationFn: (body: { personId: number; share: string }) =>
      postJson(`/api/sheets/${encodeURIComponent(sheetId)}/pools/${pool.id}/members`, { user, ...body }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) });
      setPersonId('');
      setShare('');
    },
    onError: (e) => setError(e instanceof HttpError && e.body ? e.body : 'Could not add the pool member.'),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (personId === '') {
      setError('Choose the person to add.');
      return;
    }
    const s = share.trim();
    if (!validShare(s)) {
      setError('A share is more than 0.00 and at most 100.00, with at most two decimals.');
      return;
    }
    if (pool.members.some((m) => String(m.personId) === personId)) {
      setError('That person is already in the pool.');
      return;
    }
    if (!once.start()) return;
    add.mutate({ personId: Number(personId), share: s });
  };

  return (
    <li data-testid={`pool-row-${pool.name}`}>
      <strong>{pool.name}</strong> — total share <span data-testid="pool-total-share">{pool.totalShare}</span>,
      balanced <span data-testid="pool-balanced">{pool.balanced ? 'yes' : 'no'}</span>
      <ul>
        {pool.members.map((m) => (
          <li key={m.personId} data-testid={`pool-member-${pool.name}-${m.personId}`}>
            {m.name}: <span data-testid="pool-share">{m.share}</span>, cut <span data-testid="pool-cut">{m.cut}</span>
            {m.retired && ' (retired)'}
          </li>
        ))}
      </ul>
      <form onSubmit={submit}>
        <select
          data-testid="pool-member-select"
          value={personId}
          onChange={(e) => {
            setPersonId(e.target.value);
            setError('');
          }}
        >
          <option value="">Person…</option>
          {people.map((p) => (
            <option key={p.id} value={String(p.id)}>
              {p.name}
            </option>
          ))}
        </select>{' '}
        <input
          data-testid="pool-member-share"
          placeholder="Share %"
          value={share}
          onChange={(e) => {
            setShare(e.target.value);
            setError('');
          }}
        />{' '}
        <button type="submit" data-testid="pool-member-add" disabled={add.isPending}>
          Add member
        </button>
      </form>
      {error && (
        <p data-testid="pool-error" role="alert">
          {error}
        </p>
      )}
    </li>
  );
}

function PoolForm({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const save = useMutation({
    mutationFn: (n: string) => postJson<Pool>(`/api/sheets/${encodeURIComponent(sheetId)}/pools`, { user, name: n }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) });
      setName('');
    },
    onError: (e) => setError(e instanceof HttpError && e.body ? e.body : 'Could not add the pool.'),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (name.trim() === '') {
      setError('Give the pool a name.');
      return;
    }
    if (!once.start()) return;
    save.mutate(name.trim());
  };

  return (
    <form data-testid="pool-form" onSubmit={submit}>
      <input
        data-testid="pool-form-name"
        placeholder="Pool name"
        value={name}
        onChange={(e) => {
          setName(e.target.value);
          setError('');
        }}
      />{' '}
      <button type="submit" data-testid="pool-form-submit" disabled={save.isPending}>
        Add pool
      </button>
      {error && (
        <p data-testid="pool-form-error" role="alert">
          {error}
        </p>
      )}
    </form>
  );
}

/** The sheet's pools: each with its members and their shares, its total share (a member retired by the sheet's
 *  today counts 0.00) and whether it is balanced, and the entry a pool is added with. */
function Pools({ sheetId, user }: { sheetId: string; user: string }) {
  const pools = usePools(sheetId).data ?? [];
  return (
    <section data-testid="pools">
      <h2>Pools</h2>
      <PoolForm sheetId={sheetId} user={user} />
      <ul>
        {pools.map((p) => (
          <PoolRow key={p.id} sheetId={sheetId} user={user} pool={p} />
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 7.8, Component: Pools });
