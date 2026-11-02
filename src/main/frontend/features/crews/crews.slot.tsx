import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { usePeople } from '../people/people';
import { usePersonDueCentsOf } from '../claims/claims';
import { twoDecimals } from '../rates/rates';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { crewDueCents, crewMembersPath, crewsKey, crewsPath, useCrews, type Crew } from './crews';
import { Fig } from '../locale/Fig';
import { poolsKey } from '../pools/pools';

// One crew's row: its lead, its members, its due-total (the lead's and the members' due-totals added up, an n/a one
// skipped), and a form that adds a member chosen from the sheet's people.
function CrewRow({ sheet, user, crew }: { sheet: string; user: string; crew: Crew }) {
  const queryClient = useQueryClient();
  const { data: people = [] } = usePeople(sheet);
  const dueOf = usePersonDueCentsOf(sheet);
  const [member, setMember] = useState('');

  const add = useMutation({
    mutationFn: (body: { crew: string; person: number | null }) =>
      postJson<Crew[]>(byUser(crewMembersPath(sheet), user), body),
    onSuccess: (updated) => {
      queryClient.setQueryData(crewsKey(sheet), updated);
      // Each crew is also a pool, its members the crew's.
      void queryClient.invalidateQueries({ queryKey: poolsKey(sheet) });
      setMember('');
    },
  });
  const send = useSubmitOnce(add);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    // The server refuses the crew's lead or a person already in the crew.
    send({ crew: crew.name, person: member === '' ? null : Number(member) });
  };

  const due = crewDueCents(crew, people, dueOf);

  return (
    <li data-testid={`crew-row-${crew.name}`}>
      <strong>{crew.name}</strong> — lead <span data-testid="crew-lead">{crew.lead.name}</span>, due total{' '}
      <span data-testid="crew-due-total"><Fig>{twoDecimals(due)}</Fig></span>
      <ul>
        {crew.members.map((m) => (
          <li key={m.person} data-testid="crew-member-line">
            {m.name}
          </li>
        ))}
      </ul>
      <form onSubmit={submit}>
        <select
          data-testid="crew-member-select"
          value={member}
          onChange={(e) => {
            setMember(e.target.value);
            add.reset();
          }}
        >
          <option value="">Choose a member</option>
          {people.map((p) => (
            <option key={p.id} value={String(p.id)}>
              {p.name}
            </option>
          ))}
        </select>{' '}
        <button type="submit" data-testid="crew-member-add" disabled={add.isPending}>
          Add member
        </button>
        {add.isError && (
          <span data-testid="crew-error" role="alert">
            {' '}A member must be neither the crew's lead nor already in the crew.
          </span>
        )}
      </form>
    </li>
  );
}

// The sheet's crews, and a form that adds one with its lead.
function Crews({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const { data: crews = [] } = useCrews(sheet);
  const { data: people = [] } = usePeople(sheet);
  const [name, setName] = useState('');
  const [lead, setLead] = useState('');

  const add = useMutation({
    mutationFn: (body: { name: string; lead: number | null }) =>
      postJson<Crew[]>(byUser(crewsPath(sheet), user), body),
    onSuccess: (updated) => {
      queryClient.setQueryData(crewsKey(sheet), updated);
      // Each crew is also a pool, its members the crew's.
      void queryClient.invalidateQueries({ queryKey: poolsKey(sheet) });
      setName('');
      setLead('');
    },
  });
  const send = useSubmitOnce(add);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    // The server refuses a blank name, one already a crew, or no lead.
    send({ name, lead: lead === '' ? null : Number(lead) });
  };

  return (
    <section>
      <h2>Crews</h2>
      <form onSubmit={submit}>
        <label>
          Name{' '}
          <input
            data-testid="crew-form-name"
            value={name}
            onChange={(e) => {
              setName(e.target.value);
              add.reset();
            }}
          />
        </label>{' '}
        <label>
          Lead{' '}
          <select
            data-testid="crew-form-lead"
            value={lead}
            onChange={(e) => {
              setLead(e.target.value);
              add.reset();
            }}
          >
            <option value="">Choose a lead</option>
            {people.map((p) => (
              <option key={p.id} value={String(p.id)}>
                {p.name}
              </option>
            ))}
          </select>
        </label>{' '}
        <button type="submit" data-testid="crew-form-submit" disabled={add.isPending}>
          Add crew
        </button>
        {add.isError && (
          <span data-testid="crew-form-error" role="alert">
            {' '}A crew needs a lead, and its name must not be blank nor already a crew.
          </span>
        )}
      </form>
      <ul>
        {crews.map((c) => (
          <CrewRow key={c.name} sheet={sheet} user={user} crew={c} />
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetPanels.fill({ order: 9.9, Component: Crews });
