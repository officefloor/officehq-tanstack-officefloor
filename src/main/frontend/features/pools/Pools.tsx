import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { usePeople } from '../people/people';
import { poolsKey, usePools } from './pools';
import type { Pool } from './pools';
import { useOneAtATime } from '../../query/useOneAtATime';

const SHARE = /^\d{1,3}(\.\d{1,2})?$/;

// The sheet's pools and the form that adds one; a blank name, or one already a pool's, is refused.
export function Pools() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const pools = usePools().data ?? [];
  const [name, setName] = useState('');
  const [error, setError] = useState('');
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const add = useMutation({
    mutationFn: (input: { name: string }) =>
      postJson<Pool[]>(`/api/sheets/${encodeURIComponent(sheet)}/pools?user=${encodeURIComponent(user)}`, input),
    onSuccess: async (saved) => {
      setName('');
      setError('');
      queryClient.setQueryData(poolsKey(sheet), saved);
      await queryClient.invalidateQueries({ queryKey: poolsKey(sheet) });
    },
    onError: () => setError('That pool was refused: it needs a name no other pool has.'),
  });

  if (!user || !sheet) return null;

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const trimmed = name.trim();
    if (!trimmed) {
      setError('Type the pool name.');
      return;
    }
    if (pools.some((p) => p.name === trimmed)) {
      setError(`There is already a pool named ${trimmed}.`);
      return;
    }
    once(() => add.mutateAsync({ name }));
  };

  return (
    <section data-testid="pools">
      <ul data-testid="pool-list">
        {pools.map((p) => (
          <PoolRow key={p.id} pool={p} />
        ))}
      </ul>
      <form onSubmit={submit} data-testid="pool-form">
        <input
          data-testid="pool-form-name"
          placeholder="Pool name"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            setError('');
          }}
        />
        <button type="submit" data-testid="pool-form-submit" disabled={add.isPending}>
          Add pool
        </button>
        {error && <span data-testid="pool-form-error">{error}</span>}
      </form>
    </section>
  );
}

const EMPTY = { personId: '', share: '' };

// A pool's row: its members with their shares and cuts of the pot (blank: an unbalanced pool, or no pot), its total share and whether it is balanced, and the form that adds a
// member; a person already in the pool, or a share not more than 0.00 and at most 100.00 (two decimals), is refused.
function PoolRow({ pool }: { pool: Pool }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople().data ?? [];
  const [form, setForm] = useState(EMPTY);
  const [error, setError] = useState('');
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const add = useMutation({
    mutationFn: (input: { personId: number; share: string }) =>
      postJson<Pool[]>(
        `/api/sheets/${encodeURIComponent(sheet)}/pools/${pool.id}/members?user=${encodeURIComponent(user)}`,
        input,
      ),
    onSuccess: async (saved) => {
      setForm(EMPTY);
      setError('');
      queryClient.setQueryData(poolsKey(sheet), saved);
      await queryClient.invalidateQueries({ queryKey: poolsKey(sheet) });
    },
    onError: () => setError('That member was refused: a person not yet in the pool, with a share over 0.00 and at most 100.00.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const personId = Number(form.personId);
    if (!form.personId) {
      setError('Choose a person.');
      return;
    }
    if (pool.members.some((m) => m.personId === personId)) {
      setError('That person is already in the pool.');
      return;
    }
    const share = form.share.trim();
    if (!SHARE.test(share) || Number(share) <= 0 || Number(share) > 100) {
      setError('Type a share more than 0.00 and at most 100.00, with at most two decimals.');
      return;
    }
    once(() => add.mutateAsync({ personId, share }));
  };

  const change = (patch: Partial<typeof EMPTY>) => {
    setForm((f) => ({ ...f, ...patch }));
    setError('');
  };

  return (
    <li data-testid={`pool-row-${pool.name}`}>
      <span data-testid="pool-name">{pool.name}</span> total <span data-testid="pool-total-share">{pool.totalShare}</span>{' '}
      balanced <span data-testid="pool-balanced">{pool.balanced ? 'yes' : 'no'}</span>
      <ul>
        {pool.members.map((m) => (
          <li key={m.personId} data-testid={`pool-member-${pool.name}-${m.personId}`}>
            <span data-testid="pool-member-name">{m.name}</span> <span data-testid="pool-share">{m.share}</span>
            {m.retired && ' (retired)'} cut <span data-testid="pool-cut">{m.cut}</span>
          </li>
        ))}
      </ul>
      <form onSubmit={submit}>
        <select data-testid="pool-member-select" value={form.personId} onChange={(e) => change({ personId: e.target.value })}>
          <option value="">Choose a person</option>
          {people.map((p) => (
            <option key={p.id} value={String(p.id)}>
              {p.name}
            </option>
          ))}
        </select>
        <input data-testid="pool-member-share" placeholder="Share %" value={form.share} onChange={(e) => change({ share: e.target.value })} />
        <button type="submit" data-testid="pool-member-add" disabled={add.isPending}>
          Add member
        </button>
        {error && <span data-testid="pool-error">{error}</span>}
      </form>
    </li>
  );
}
