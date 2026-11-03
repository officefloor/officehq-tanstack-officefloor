import { useState, type ChangeEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSingleFlight } from '../../query/useSingleFlight';
import { SheetBody } from '../../slots/defs/sheetBody';
import { usePeople, type Person } from '../people/usePeople';
import { useDueTotals } from '../rates/useDueTotals';
import { useFigure } from '../locale/useLocale';
import { crewDue } from './crewDue';
import { crewsKey, useCrews, type Crew } from './useCrews';
import { poolsKey } from '../pools/usePools';

/** One crew: its lead, its members, its due-total (the lead's and the members' due-totals added up, 'n/a'
 *  skipped) and a form that adds a member (a person on the sheet, neither its lead nor already in it). */
function CrewRow({ sheetId, crew, people }: { sheetId: string; crew: Crew; people: Person[] }) {
  const fig = useFigure();
  const queryClient = useQueryClient();
  const dueTotals = useDueTotals(sheetId);
  const [member, setMember] = useState('');
  const due = crewDue(crew, dueTotals);

  const submitOnce = useSingleFlight();
  const add = useMutation({
    mutationFn: (f: { crew: string; personId: number | null }) =>
      postJson<{ crews: Crew[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/crews/members`, f),
    onSuccess: async () => {
      setMember('');
      // A crew is also a pool of its name: its pool (and every pool's cuts) follow it.
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: crewsKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) }),
      ]);
    },
  });

  return (
    <div data-testid={`crew-row-${crew.name}`}>
      <strong data-testid="crew-name">{crew.name}</strong>{' '}
      Lead <span data-testid="crew-lead">{crew.lead}</span>{' '}
      Due-total <span data-testid="crew-due-total">{fig(due)}</span>
      <ul>
        {crew.members.map((m) => (
          <li key={m.personId} data-testid="crew-member-line">
            <span data-testid="crew-member-name">{m.name}</span>
          </li>
        ))}
      </ul>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          submitOnce(() => {
            add.reset();
            return add.mutateAsync({ crew: crew.name, personId: member === '' ? null : Number(member) });
          });
        }}
      >
        <select
          data-testid="crew-member-select"
          value={member}
          onChange={(e: ChangeEvent<HTMLSelectElement>) => {
            setMember(e.target.value);
            add.reset();
          }}
        >
          <option value="">Choose a person</option>
          {people.map((p) => (
            <option key={p.id} value={String(p.id)}>
              {p.name}
            </option>
          ))}
        </select>{' '}
        <button type="submit" data-testid="crew-member-add" disabled={add.isPending}>
          Add member
        </button>
      </form>
      {add.isError && <p data-testid="crew-error">{add.error.message}</p>}
    </div>
  );
}

/** The sheet's crews: a form that adds one (a name, unique on the sheet, and a lead) and each crew with its members. */
function Crews({ sheetId }: { sheetId: string }) {
  const crews = useCrews(sheetId);
  const people = usePeople(sheetId);
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [lead, setLead] = useState('');

  const submitOnce = useSingleFlight();
  const save = useMutation({
    mutationFn: (f: { name: string; leadId: number | null }) =>
      postJson<{ crews: Crew[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/crews`, f),
    onSuccess: async () => {
      setName('');
      setLead('');
      // A crew is also a pool of its name: its pool (and every pool's cuts) follow it.
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: crewsKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: poolsKey(sheetId) }),
      ]);
    },
  });

  return (
    <section data-testid="crews">
      <form
        data-testid="crew-form"
        onSubmit={(e) => {
          e.preventDefault();
          submitOnce(() => {
            save.reset();
            return save.mutateAsync({ name, leadId: lead === '' ? null : Number(lead) });
          });
        }}
      >
        <label>
          Crew{' '}
          <input
            data-testid="crew-form-name"
            value={name}
            onChange={(e) => {
              setName(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <label>
          Lead{' '}
          <select
            data-testid="crew-form-lead"
            value={lead}
            onChange={(e: ChangeEvent<HTMLSelectElement>) => {
              setLead(e.target.value);
              save.reset();
            }}
          >
            <option value="">Choose a person</option>
            {(people.data ?? []).map((p) => (
              <option key={p.id} value={String(p.id)}>
                {p.name}
              </option>
            ))}
          </select>
        </label>{' '}
        <button type="submit" data-testid="crew-form-submit" disabled={save.isPending}>
          Add crew
        </button>
        {save.isError && <p data-testid="crew-form-error">{save.error.message}</p>}
      </form>
      {(crews.data ?? []).map((c) => (
        <CrewRow key={c.name} sheetId={sheetId} crew={c} people={people.data ?? []} />
      ))}
      {crews.isError && <p data-testid="crews-error">{crews.error.message}</p>}
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 7.5, Component: ({ sheetId }) => <Crews sheetId={sheetId} /> });
