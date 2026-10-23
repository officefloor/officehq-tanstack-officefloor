import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';
import { usePeople } from '../people/people';
import { cutThen, poolsKey, usePeriodCuts, usePools } from './pools';
import type { Pool, PoolMember } from './pools';

/** A share as typed: a percent of at most two decimals, more than 0.00 and at most 100.00. */
function validShare(share: string): boolean {
  if (!/^\d+(\.\d{1,2})?$/.test(share)) return false;
  const n = Number(share);
  return n > 0 && n <= 100;
}

/** A member's key on screen: the person's id, or pool-<name> for a member pool. */
function memberKey(m: PoolMember): string {
  return m.poolId !== null ? `pool-${m.name}` : String(m.personId);
}

/** One pool: its total share, whether balanced, each member's share, and the entry a member (a person, or another
 *  pool chosen as pool:<name>) is added with — none for a crew's pool, whose members are the crew's. */
function PoolRow({ sheetId, user, pool, pools, periodCuts }: { sheetId: string; user: string; pool: Pool; pools: Pool[];
    periodCuts: { period: string; pools: Pool[] }[] }) {
  const queryClient = useQueryClient();
  const people = usePeople(sheetId).data ?? [];
  // the chosen member: a person's id, or pool:<id>
  const [personId, setPersonId] = useState('');
  const [share, setShare] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  /** Refuse the add: show why, and drop the dates typed for it (a refused membership's dates are not kept). */
  const refuse = (message: string) => {
    setError(message);
    setFrom('');
    setTo('');
  };

  const add = useMutation({
    mutationFn: (body: { personId?: number; memberPoolId?: number; share: string; from?: string; to?: string }) =>
      postJson(`/api/sheets/${encodeURIComponent(sheetId)}/pools/${pool.id}/members`, { user, ...body }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) });
      setPersonId('');
      setShare('');
      setFrom('');
      setTo('');
    },
    onError: (e) => refuse(e instanceof HttpError && e.body ? e.body : 'Could not add the pool member.'),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (personId === '') {
      refuse('Choose the person or pool to add.');
      return;
    }
    const s = share.trim();
    if (!validShare(s)) {
      refuse('A share is more than 0.00 and at most 100.00, with at most two decimals.');
      return;
    }
    if (from !== '' && to !== '' && to < from) {
      refuse('The to-date cannot be before the from-date.');
      return;
    }
    const memberPoolId = personId.startsWith('pool:') ? Number(personId.slice(5)) : null;
    if (memberPoolId === null && pool.members.some((m) => m.poolId === null && String(m.personId) === personId)) {
      refuse('That person is already in the pool.');
      return;
    }
    if (!once.start()) return;
    const dates = { ...(from !== '' && { from }), ...(to !== '' && { to }) };
    add.mutate(
      memberPoolId !== null ? { memberPoolId, share: s, ...dates } : { personId: Number(personId), share: s, ...dates },
    );
  };

  return (
    <li data-testid={`pool-row-${pool.name}`}>
      <strong>{pool.name}</strong> — total share <span data-testid="pool-total-share">{pool.totalShare}</span>,
      balanced <span data-testid="pool-balanced">{pool.balanced ? 'yes' : 'no'}</span>
      <ul>
        {pool.members.map((m) => (
          <li key={memberKey(m)} data-testid={`pool-member-${pool.name}-${memberKey(m)}`}>
            {m.poolId !== null ? `pool ${m.name}` : m.name}: <span data-testid="pool-share">{m.share}</span>, cut <span data-testid="pool-cut">{m.cut}</span>
            {periodCuts.map((pc) => (
              <span key={pc.period}>
                , {pc.period} <span data-testid={`pool-cut-${pc.period}`}>{cutThen(pc.pools, pool.id, m)}</span>
              </span>
            ))}
            {(m.from || m.to) && ` (${m.from || '…'} to ${m.to || '…'})`}
            {m.retired && ' (retired)'}
            {m.outside && ' (not a member today)'}
          </li>
        ))}
      </ul>
      {pool.crewId === null && (
        <>
          <form onSubmit={submit}>
            <select
              data-testid="pool-member-select"
              value={personId}
              onChange={(e) => {
                setPersonId(e.target.value);
                setError('');
              }}
            >
              <option value="">Person or pool…</option>
              {people.map((p) => (
                <option key={p.id} value={String(p.id)}>
                  {p.name}
                </option>
              ))}
              {pools.filter((p) => p.crewId === null).map((p) => (
                <option key={`pool:${p.id}`} value={`pool:${p.id}`}>
                  {`pool:${p.name}`}
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
            <input
              type="date"
              data-testid="pool-member-from"
              aria-label="Member from"
              value={from}
              onChange={(e) => {
                setFrom(e.target.value);
                setError('');
              }}
            />{' '}
            <input
              type="date"
              data-testid="pool-member-to"
              aria-label="Member to"
              value={to}
              onChange={(e) => {
                setTo(e.target.value);
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
        </>
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
 *  today counts their share as given) and whether it is balanced, and the entry a pool is added with. */
function Pools({ sheetId, user }: { sheetId: string; user: string }) {
  const pools = usePools(sheetId).data ?? [];
  const periodCuts = usePeriodCuts(sheetId);
  return (
    <section data-testid="pools">
      <h2>Pools</h2>
      <PoolForm sheetId={sheetId} user={user} />
      <ul>
        {pools.map((p) => (
          <PoolRow key={p.id} sheetId={sheetId} user={user} pool={p} pools={pools} periodCuts={periodCuts} />
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 7.8, Component: Pools });
