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
const DATE = /^\d{4}-\d{2}-\d{2}$/;

// The sheet's pools (a crew among them, after the pools) and the form that adds one; a blank name, or one already a
// pool's or a crew's, is refused.
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
    onError: () => setError('That pool was refused: it needs a name no other pool or crew has.'),
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
          <PoolRow key={p.id} pool={p} pools={pools} />
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

const EMPTY = { member: '', share: '', from: '', to: '' };

// A pool's row: its members with their shares and cuts of the pot (blank: an unbalanced pool, or no pot), its total share and whether it is balanced, and the form that adds a
// member — a person, or another pool chosen as pool:<name>; a person already in the pool, a pool already a member of
// one, one that would contain itself or make a chain of pools more than three deep, or a share not more than 0.00 and
// at most 100.00 (two decimals), or a to-date before the from-date (both optional, yyyy-mm-dd), is refused. A member
// whose dates leave out the sheet's today is not in the total share and has no cut.
function PoolRow({ pool, pools }: { pool: Pool; pools: Pool[] }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople().data ?? [];
  const [form, setForm] = useState(EMPTY);
  const [error, setError] = useState('');
  const queryClient = useQueryClient();
  const once = useOneAtATime();

  const add = useMutation({
    mutationFn: (input: { personId?: number; poolId?: number; share: string; from: string | null; to: string | null }) =>
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
    onError: () =>
      setError(
        'That member was refused: a person not yet in the pool, or a pool in no other that does not contain this one and keeps chains at most three deep, with a share over 0.00 and at most 100.00.',
      ),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (!form.member) {
      setError('Choose a person or a pool.');
      return;
    }
    const [kind, idText] = form.member.split(':');
    const id = Number(idText);
    if (kind === 'person' && pool.members.some((m) => m.personId === id)) {
      setError('That person is already in the pool.');
      return;
    }
    if (kind === 'pool' && pools.some((p) => p.members.some((m) => m.poolId === id))) {
      setError('That pool is already a member of a pool.');
      return;
    }
    const share = form.share.trim();
    if (!SHARE.test(share) || Number(share) <= 0 || Number(share) > 100) {
      setError('Type a share more than 0.00 and at most 100.00, with at most two decimals.');
      return;
    }
    const from = form.from.trim();
    const to = form.to.trim();
    if ((from && !DATE.test(from)) || (to && !DATE.test(to))) {
      setError('Type the dates as yyyy-mm-dd.');
      return;
    }
    if (from && to && to < from) {
      setError('The to-date is before the from-date.');
      return;
    }
    const dates = { from: from || null, to: to || null };
    once(() => add.mutateAsync(kind === 'pool' ? { poolId: id, share, ...dates } : { personId: id, share, ...dates }));
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
          <li
            key={m.poolId !== null ? `pool-${m.poolId}` : m.personId}
            data-testid={`pool-member-${pool.name}-${m.poolId !== null ? `pool-${m.name}` : m.personId}`}
          >
            <span data-testid="pool-member-name">{m.name}</span> <span data-testid="pool-share">{m.share}</span>
            {m.retired && ' (retired)'}
            {(m.from || m.to) && ` (${m.from ?? 'start'} to ${m.to ?? 'no end'})`} cut <span data-testid="pool-cut">{m.cut}</span>
          </li>
        ))}
      </ul>
      {!pool.crew && (
        <form onSubmit={submit}>
          <select data-testid="pool-member-select" value={form.member} onChange={(e) => change({ member: e.target.value })}>
            <option value="">Choose a person or a pool</option>
            {people.map((p) => (
              <option key={p.id} value={`person:${p.id}`}>
                {p.name}
              </option>
            ))}
            {pools.filter((p) => !p.crew).map((p) => (
              <option key={`pool-${p.id}`} value={`pool:${p.id}`}>
                pool:{p.name}
              </option>
            ))}
          </select>
          <input data-testid="pool-member-share" placeholder="Share %" value={form.share} onChange={(e) => change({ share: e.target.value })} />
          <input data-testid="pool-member-from" placeholder="From yyyy-mm-dd (blank: no start)" value={form.from} onChange={(e) => change({ from: e.target.value })} />
          <input data-testid="pool-member-to" placeholder="To yyyy-mm-dd (blank: no end)" value={form.to} onChange={(e) => change({ to: e.target.value })} />
          <button type="submit" data-testid="pool-member-add" disabled={add.isPending}>
            Add member
          </button>
          {error && <span data-testid="pool-error">{error}</span>}
        </form>
      )}
    </li>
  );
}
