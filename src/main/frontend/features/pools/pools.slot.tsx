import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { usePeople, type Person } from '../people/usePeople';
import { poolsKey, usePools, SHARE, type Pool } from './usePools';

/**
 * The sheet's pools and a form to add one by name (trimmed; a blank name, or one already a pool, is
 * refused). Each pool's row adds a member: a person with a share percent, more than 0.00 and at most
 * 100.00 with up to two decimals; a person already in the pool, or a bad share, is refused. A pool
 * shows its total share and whether it is balanced (exactly 100.00); a retired member counts 0.00.
 * A member may be another pool, chosen as pool:<name>: a pool is a member of at most one pool, cannot
 * contain itself through any chain, and a chain of pools is at most three deep (the server refuses).
 * Each member of a balanced pool shows their cut of the pool's pot; unbalanced, or no pot, none. A pool
 * that is a member of none has the sheet's pot; one that is a member of another has its cut there.
 */
function PoolsPanel({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const { data: pools = [] } = usePools(sheet);
  const { data: people = [] } = usePeople(sheet);
  const [name, setName] = useState('');
  const [invalid, setInvalid] = useState(false);

  const submitOnce = useSubmitOnce();
  const add = useMutation({
    mutationFn: (n: string) => postJson<Pool[]>(`/api/sheets/${encodeURIComponent(sheet)}/pools`, { name: n, user }),
    onSuccess: async () => {
      setName('');
      await queryClient.invalidateQueries({ queryKey: poolsKey(sheet) });
    },
  });

  // An error goes as soon as its form is changed or submitted again.
  const clearError = () => {
    add.reset();
    setInvalid(false);
  };

  return (
    <section data-testid="pools-panel">
      <h2>Pools</h2>
      <form
        data-testid="pool-form"
        onSubmit={(e) => {
          e.preventDefault();
          const n = name.trim();
          if (n === '' || pools.some((p) => p.name === n)) {
            // Refused here, before anything is sent: the store and the audit file are untouched.
            add.reset();
            setInvalid(true);
            return;
          }
          setInvalid(false);
          submitOnce(() => add.mutateAsync(n));
        }}
      >
        <input
          data-testid="pool-form-name"
          aria-label="Pool name"
          placeholder="name"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            clearError();
          }}
        />{' '}
        <button type="submit" data-testid="pool-form-submit" disabled={add.isPending}>
          Add pool
        </button>
        {(invalid || add.isError) && (
          <span data-testid="pool-form-error" role="alert">
            {' '}
            {invalid ? 'Enter a name not already a pool.' : 'Could not add this pool.'}
          </span>
        )}
      </form>
      <table data-testid="pool-list">
        <tbody>
          {pools.map((p) => (
            <PoolRow key={p.name} user={user} sheet={sheet} pool={p} pools={pools} people={people} />
          ))}
        </tbody>
      </table>
    </section>
  );
}

/** A pool's row: its total share, balanced, its members with their shares, and a form to add one. */
function PoolRow({
  user,
  sheet,
  pool,
  pools,
  people,
}: {
  user: string;
  sheet: string;
  pool: Pool;
  pools: Pool[];
  people: Person[];
}) {
  const queryClient = useQueryClient();
  // A person's id, or pool:<name> for another pool.
  const [personId, setPersonId] = useState('');
  const [share, setShare] = useState('');
  const [invalid, setInvalid] = useState(false);

  const submitOnce = useSubmitOnce();
  const add = useMutation({
    mutationFn: (m: { personId?: number; memberPool?: string; share: string }) =>
      postJson<Pool[]>(`/api/sheets/${encodeURIComponent(sheet)}/pools/members`, { pool: pool.name, ...m, user }),
    onSuccess: async () => {
      setPersonId('');
      setShare('');
      await queryClient.invalidateQueries({ queryKey: poolsKey(sheet) });
    },
  });

  const clearError = () => {
    add.reset();
    setInvalid(false);
  };

  return (
    <tr data-testid={`pool-row-${pool.name}`}>
      <td data-testid="pool-name">{pool.name}</td>
      <td>
        total <span data-testid="pool-total-share">{pool.totalShare}</span>
      </td>
      <td>
        balanced <span data-testid="pool-balanced">{pool.balanced ? 'yes' : 'no'}</span>
      </td>
      <td>
        <ul>
          {pool.members.map((m) => (
            <li
              key={m.pool !== null ? `pool-${m.pool}` : m.personId}
              data-testid={`pool-member-${pool.name}-${m.pool !== null ? `pool-${m.pool}` : m.personId}`}
            >
              <span>{m.name}</span>{' '}
              <span data-testid="pool-share">{m.share}</span>{' '}
              <span data-testid="pool-cut">{m.cut ?? ''}</span>
              {m.retired ? ' (retired)' : ''}
            </li>
          ))}
        </ul>
      </td>
      <td>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            const memberPool = personId.startsWith(POOL_PREFIX) ? personId.slice(POOL_PREFIX.length) : null;
            const id = Number(personId);
            const s = share.trim();
            const n = Number(s);
            if (
              personId === '' ||
              (memberPool === null
                ? pool.members.some((m) => m.personId === id)
                : memberPool === pool.name || pool.members.some((m) => m.pool === memberPool)) ||
              !SHARE.test(s) ||
              !(n > 0 && n <= 100)
            ) {
              // Refused here, before anything is sent: the store and the audit file are untouched.
              add.reset();
              setInvalid(true);
              return;
            }
            setInvalid(false);
            submitOnce(() =>
              add.mutateAsync(memberPool === null ? { personId: id, share: s } : { memberPool, share: s }),
            );
          }}
        >
          <select
            data-testid="pool-member-select"
            aria-label="Member"
            value={personId}
            onChange={(e) => {
              setPersonId(e.target.value);
              clearError();
            }}
          >
            <option value="">choose…</option>
            {people.map((p) => (
              <option key={p.id} value={String(p.id)}>
                {p.name}
              </option>
            ))}
            {pools.map((p) => (
              <option key={`pool-${p.name}`} value={POOL_PREFIX + p.name}>
                {POOL_PREFIX + p.name}
              </option>
            ))}
          </select>{' '}
          <input
            data-testid="pool-member-share"
            aria-label="Share percent"
            placeholder="0.00"
            value={share}
            onChange={(e) => {
              setShare(e.target.value);
              clearError();
            }}
          />{' '}
          <button type="submit" data-testid="pool-member-add" disabled={add.isPending}>
            Add member
          </button>
          {(invalid || add.isError) && (
            <span data-testid="pool-error" role="alert">
              {' '}
              {invalid
                ? 'Choose a person or pool not already in the pool, with a share more than 0.00 and at most 100.00.'
                : 'Could not add this member (a pool is in at most one pool, never in itself, at most three deep).'}
            </span>
          )}
        </form>
      </td>
    </tr>
  );
}

/** The member select's value for another pool. */
const POOL_PREFIX = 'pool:';

export const contribution = SheetPanels.fill({ order: 10, Component: PoolsPanel });
