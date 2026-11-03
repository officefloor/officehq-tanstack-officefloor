import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSingleFlight } from '../../query/useSingleFlight';
import { SheetBody } from '../../slots/defs/sheetBody';
import { PersonCells, PersonColumnHead, PersonDetail } from '../../slots/defs/personRow';
import { claimsKey, shownTags, type Claim } from '../claims/useClaims';
import { useDueTotals } from '../rates/useDueTotals';
import { hundredths } from '../rates/useRates';
import { useShowRetired } from '../retire/useShowRetired';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { GRADES, fetchPeople, peopleKey, shownName, usePeople, type Person } from './usePeople';
import { Col } from '../columns/useColumns';
import { useVisiblePeople } from './useVisiblePeople';

/** The sheet's people: a form that adds a person, or edits one when their editor is open, and the
 *  list of everyone in list order (by position; new people go to the end) (retired people only when "show retired" is on), each
 *  with the grade in force on the sheet's today. An edit records the form's grade from its from-date
 *  (blank: the sheet's today); the first grade a person is given applies from the beginning of time.
 *  A person may have their own terms (days; blank for none), dated by the terms from-date (blank: from the
 *  beginning of time; a dated save keeps the others), the ones in force on the sheet's today shown on their row:
 *  a claim's due-by is fixed when it is created from those in force on its date, ahead of their grade's and the
 *  sheet's terms. A person may have a contact
 *  (stored trimmed and lower-cased; blank for none), shown on their row; another person's contact is refused.
 *  A person may carry tags (comma separated; trimmed, lower-cased, repeats dropped), shown sorted on their row.
 *  The editor remembers the version of the person it loaded; when someone else has saved them since,
 *  the save is refused (409) and the editor shows their current values, at their current version; any other
 *  refused edit closes the editor, leaving its error showing. Each person is a <tbody> so features can add
 *  cells and rows of their own to it (slots/defs/personRow). A quick-add bar takes a name and an amount
 *  and adds a draft claim for that person (adding them with the sheet's default grade, A when blank, when the name is new) in one change.
 *  A restricted user's list shows only the people of their crews (see `useVisiblePeople`), and the people
 *  they added on this screen (by either form) until a sheet is loaded again. */
const SORTS = ['added', 'name', 'grade', 'due-total'] as const;
type SortKey = (typeof SORTS)[number];
const asSort = (raw: unknown): SortKey => {
  const s = asString(raw);
  return (SORTS as readonly string[]).includes(s) ? (s as SortKey) : 'added';
};

/** Names compare trimmed and ignoring case. */
const nameKey = (name: string) => name.trim().toLowerCase();

/** The people in the order the screen shows them (URL key `sortBy`, this user's screen only): list order
 *  ('added'), or by name (as names are compared), grade, or due-total (lowest first, 'n/a' last). The sort
 *  is stable, so equal keys keep list order; it changes nothing else (positions, rollups, actions). */
function sortPeople(people: Person[], by: SortKey, dueTotals: Map<number, string>): Person[] {
  if (by === 'added') return people;
  const cmp = (a: Person, b: Person): number => {
    if (by === 'name') {
      const x = nameKey(a.name);
      const y = nameKey(b.name);
      return x < y ? -1 : x > y ? 1 : 0;
    }
    if (by === 'grade') return a.grade < b.grade ? -1 : a.grade > b.grade ? 1 : 0;
    const x = dueTotals.get(a.id);
    const y = dueTotals.get(b.id);
    const xn = x === undefined || x === 'n/a';
    const yn = y === undefined || y === 'n/a';
    if (xn || yn) return xn === yn ? 0 : xn ? 1 : -1;
    const xc = hundredths(x);
    const yc = hundredths(y);
    return xc < yc ? -1 : xc > yc ? 1 : 0;
  };
  return [...people].sort(cmp);
}

function People({ sheetId, user }: { sheetId: string; user: string }) {
  const people = usePeople(sheetId);
  const visible = useVisiblePeople(sheetId, user, people.data);
  // the people added on this screen since the sheet was loaded: listed even for a restricted user
  const [addedHere, setAddedHere] = useState<{ sheetId: string; ids: ReadonlySet<number> }>({ sheetId, ids: new Set() });
  if (addedHere.sheetId !== sheetId) setAddedHere({ sheetId, ids: new Set() });
  const addHere = (id: number) => setAddedHere((a) => ({ sheetId: a.sheetId, ids: new Set([...a.ids, id]) }));
  const [showRetired] = useShowRetired();
  const [sortBy, setSortBy] = useSearchParam('sortBy', asSort);
  const dueTotals = useDueTotals(sheetId);
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<number | null>(null);
  const [name, setName] = useState('');
  const [grade, setGrade] = useState<string>('A');
  const [gradeFrom, setGradeFrom] = useState('');
  const [terms, setTerms] = useState('');
  const [termsFrom, setTermsFrom] = useState('');
  const [contact, setContact] = useState('');
  const [tags, setTags] = useState('');
  const [version, setVersion] = useState<number | null>(null);

  const submitOnce = useSingleFlight();
  const save = useMutation({
    mutationFn: (f: { id: number | null; name: string; grade: string; gradeFrom: string; terms: string; termsFrom: string; contact: string; tags: string; version: number | null }) => {
      const base = `/api/sheets/${encodeURIComponent(sheetId)}/people`;
      return f.id === null
        ? postJson<Person>(base, { name: f.name, grade: f.grade, terms: f.terms, termsFrom: f.termsFrom, contact: f.contact, tags: f.tags })
        : postJson<Person>(`${base}/${f.id}`, { name: f.name, grade: f.grade, gradeFrom: f.gradeFrom, terms: f.terms, termsFrom: f.termsFrom, contact: f.contact, tags: f.tags, version: f.version });
    },
    onSuccess: async (p, f) => {
      if (f.id === null) addHere(p.id);
      setEditing(null);
      setName('');
      setGrade('A');
      setGradeFrom('');
      setTerms('');
    setTermsFrom('');
      setTermsFrom('');
      setContact('');
      setTags('');
      setVersion(null);
      // a person's tags join the sheet's tags, kept under the claims' key
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
    },
    onError: async (err, f) => {
      if (f.id === null) return;
      if (!(err instanceof HttpError && err.status === 409)) {
        // a refused edit closes the editor, its error still showing, so the form is back to adding
        setEditing(null);
        setName('');
        setGrade('A');
        setGradeFrom('');
        setTerms('');
    setTermsFrom('');
        setTermsFrom('');
      setTermsFrom('');
        setContact('');
        setTags('');
        setVersion(null);
        return;
      }
      const people = await queryClient.fetchQuery({
        queryKey: peopleKey(sheetId),
        queryFn: () => fetchPeople(sheetId),
        staleTime: 0,
      });
      const current = people.find((p) => p.id === f.id);
      if (current) {
        setName(current.name);
        setGrade(current.grade);
        setTerms(current.terms === null ? '' : String(current.terms));
        setContact(current.contact ?? '');
        setTags(shownTags(current.tags));
        setVersion(current.version);
      }
    },
  });

  const [quickName, setQuickName] = useState('');
  const [quickAmount, setQuickAmount] = useState('');
  const quickOnce = useSingleFlight();
  const quick = useMutation({
    mutationFn: (f: { name: string; amount: string }) =>
      postJson<Claim>(`/api/sheets/${encodeURIComponent(sheetId)}/quick`, f),
    onSuccess: async (c) => {
      addHere(c.personId);
      setQuickName('');
      setQuickAmount('');
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
    },
  });

  const openEditor = (p: Person) => {
    save.reset();
    setEditing(p.id);
    setName(p.name);
    setGrade(p.grade);
    setGradeFrom('');
    setTerms(p.terms === null ? '' : String(p.terms));
    setTermsFrom('');
    setContact(p.contact ?? '');
    setTags(shownTags(p.tags));
    setVersion(p.version);
  };

  const cancel = () => {
    save.reset();
    setEditing(null);
    setName('');
    setGrade('A');
    setGradeFrom('');
    setTerms('');
    setTermsFrom('');
    setContact('');
    setTags('');
    setVersion(null);
  };

  return (
    <section data-testid="people">
      <form
        data-testid="person-form"
        onSubmit={(e) => {
          e.preventDefault();
          submitOnce(() => {
            save.reset();
            return save.mutateAsync({ id: editing, name, grade, gradeFrom, terms, termsFrom, contact, tags, version });
          });
        }}
      >
        <label>
          Name{' '}
          <input
            data-testid="person-form-name"
            value={name}
            onChange={(e) => {
              setName(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <label>
          Grade{' '}
          <select
            data-testid="person-form-grade"
            value={grade}
            onChange={(e) => {
              setGrade(e.target.value);
              save.reset();
            }}
          >
            {GRADES.map((g) => (
              <option key={g} value={g}>
                {g}
              </option>
            ))}
          </select>
        </label>{' '}
        <label>
          from{' '}
          <input
            type="date"
            data-testid="person-form-grade-from"
            value={gradeFrom}
            onChange={(e) => {
              setGradeFrom(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <label>
          Terms (business days){' '}
          <input
            data-testid="person-form-terms"
            value={terms}
            onChange={(e) => {
              setTerms(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <label>
          from{' '}
          <input
            type="date"
            data-testid="person-form-terms-from"
            value={termsFrom}
            onChange={(e) => {
              setTermsFrom(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <label>
          Contact{' '}
          <input
            data-testid="person-form-contact"
            value={contact}
            onChange={(e) => {
              setContact(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <label>
          Tags{' '}
          <input
            data-testid="person-form-tags"
            value={tags}
            onChange={(e) => {
              setTags(e.target.value);
              save.reset();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="person-form-submit" disabled={save.isPending}>
          {editing === null ? 'Add person' : 'Save person'}
        </button>
        {editing !== null && (
          <>
            {' '}
            <button type="button" data-testid="person-form-cancel" onClick={cancel}>
              Cancel
            </button>
          </>
        )}
        {save.isError && <p data-testid="person-form-error">{save.error.message}</p>}
      </form>
      <form
        data-testid="quick-add"
        onSubmit={(e) => {
          e.preventDefault();
          quickOnce(() => {
            quick.reset();
            return quick.mutateAsync({ name: quickName, amount: quickAmount });
          });
        }}
      >
        <label>
          Quick add: name{' '}
          <input
            data-testid="quick-name"
            value={quickName}
            onChange={(e) => {
              setQuickName(e.target.value);
              quick.reset();
            }}
          />
        </label>{' '}
        <label>
          Amount{' '}
          <input
            data-testid="quick-amount"
            value={quickAmount}
            onChange={(e) => {
              setQuickAmount(e.target.value);
              quick.reset();
            }}
          />
        </label>{' '}
        <button type="submit" data-testid="quick-submit" disabled={quick.isPending}>
          Add draft claim
        </button>
        {quick.isError && <p data-testid="quick-error">{quick.error.message}</p>}
      </form>
      <label>
        Sort by{' '}
        <select
          data-testid="sort-by"
          value={sortBy}
          onChange={(e) => setSortBy(e.target.value === 'added' ? undefined : (e.target.value as SortKey))}
        >
          {SORTS.map((k) => (
            <option key={k} value={k}>
              {k}
            </option>
          ))}
        </select>
      </label>
      <table data-testid="people-list">
        <thead>
          <tr>
            <th>Name</th>
            <th>Grade</th>
            <Col id="terms"><th>Terms</th></Col>
            <Col id="contact"><th>Contact</th></Col>
            <th>Tags</th>
            <PersonColumnHead.Slot sheetId={sheetId} />
            <th />
          </tr>
        </thead>
        {sortPeople((people.data ?? []).filter((p) => (showRetired || !p.retiredFrom) && (addedHere.ids.has(p.id) || (visible !== undefined && (visible === 'all' || visible.has(p.id))))), sortBy, dueTotals).map((p) => (
          <tbody key={p.id} data-testid={`person-row-${p.id}`}>
            <tr>
              <td data-testid="person-name">{shownName(p)}</td>
              <td data-testid="person-grade">{p.grade}</td>
              <Col id="terms"><td data-testid="person-terms">{p.terms ?? ''}</td></Col>
              <Col id="contact"><td data-testid="person-contact">{p.contact ?? ''}</td></Col>
              <td data-testid="person-tags">{shownTags(p.tags)}</td>
              <PersonCells.Slot sheetId={sheetId} personId={p.id} />
              <td>
                <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => openEditor(p)}>
                  Edit
                </button>
              </td>
            </tr>
            <PersonDetail.Slot sheetId={sheetId} personId={p.id} />
          </tbody>
        ))}
      </table>
      {people.isError && <p data-testid="people-error">{people.error.message}</p>}
    </section>
  );
}

export const contribution = SheetBody.fill({
  order: 10,
  Component: ({ sheetId, user }) => <People sheetId={sheetId} user={user} />,
});
