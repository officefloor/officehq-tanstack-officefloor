import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import { counts, useClaims, useTagsUsed } from '../claims/claims';
import { gradeOn, liveHolds, usePeople } from '../people/people';
import type { Grade, Person } from '../people/people';
import { useUser } from '../session/session';
import { useSheetToday } from '../sheet/sheet';
import { useCap, useFloor, useLate } from '../access/access';
import { NA, PCT, claimDue, heldParts, personDueTotal, personExcess, personSplitTotals, rateOn, ratesKey, sumDue, trimmedSumDue, useRates } from './rates';
import type { Rate } from './rates';
import { Col } from '../columns/columns';
import { useCutTotals } from '../pools/pools';
import { useGradeCodes } from '../grades/grades';

type Fields = { grade: Grade; pct: string; from: string; to: string };
const EMPTY: Fields = { grade: 'A', pct: '', from: '', to: '' };

// The sheet's rates: a form that sets a grade's rate from a date (or from the beginning of time)
// through a date (or with no end),
// every rate set, and each grade's rate in force on the sheet's today.
function Rates({ sheetId }: { sheetId: string }) {
  const GRADES = useGradeCodes(sheetId);
  const queryClient = useQueryClient();
  const rates = useRates(sheetId);
  const today = useSheetToday(sheetId);
  const [fields, setFields] = useState<Fields>(EMPTY);
  const [error, setError] = useState('');
  const [user] = useUser();

  const once = useOnce();
  const save = useMutation({
    mutationFn: (body: Fields) =>
      postJson<Rate>(`/api/sheets/${encodeURIComponent(sheetId)}/rates`, body, user),
    onSuccess: () => {
      setFields((f) => ({ ...f, pct: '', from: '', to: '' }));
      void queryClient.invalidateQueries({ queryKey: ratesKey(sheetId) });
    },
    onError: () => setError('That rate could not be set.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    const pct = fields.pct.trim();
    if (pct === '') {
      setError('A rate is required.');
      return;
    }
    if (!PCT.test(pct)) {
      setError('A rate is a percent of 0.00 or more, with up to two decimals.');
      return;
    }
    const from = fields.from.trim();
    const to = fields.to.trim();
    if (to !== '' && from !== '' && to < from) {
      setError('A to-date is on or after the from-date.');
      return;
    }
    once((done) =>
      save.mutate({ grade: fields.grade, pct, from, to }, { onSettled: done }),
    );
  };

  const change = (next: Partial<Fields>) => {
    setFields((f) => ({ ...f, ...next }));
    setError('');
  };

  const all = rates.data ?? [];

  return (
    <section data-testid="rates">
      <h2>Rates</h2>
      <form onSubmit={submit} data-testid="rate-form">
        <label>
          Grade{' '}
          <select
            data-testid="rate-form-grade"
            value={fields.grade}
            onChange={(e) => change({ grade: e.target.value as Grade })}
          >
            {GRADES.map((g) => (
              <option key={g} value={g}>
                {g}
              </option>
            ))}
          </select>
        </label>{' '}
        <label>
          Rate %{' '}
          <input
            data-testid="rate-form-pct"
            inputMode="decimal"
            value={fields.pct}
            onChange={(e) => change({ pct: e.target.value })}
          />
        </label>{' '}
        <label>
          From{' '}
          <input
            type="date"
            data-testid="rate-form-from"
            value={fields.from}
            onChange={(e) => change({ from: e.target.value })}
          />
        </label>{' '}
        <label>
          To{' '}
          <input
            type="date"
            data-testid="rate-form-to"
            value={fields.to}
            onChange={(e) => change({ to: e.target.value })}
          />
        </label>{' '}
        <button type="submit" data-testid="rate-form-submit" disabled={save.isPending}>
          Set rate
        </button>
        {error && (
          <p role="alert" data-testid="rate-form-error">
            {error}
          </p>
        )}
      </form>
      <RatePreview sheetId={sheetId} />
      {rates.isError && (
        <p role="alert" data-testid="rates-error">
          The rates could not be read.
        </p>
      )}
      <table data-testid="rate-today">
        <thead>
          <tr>
            <th>Grade</th>
            <th>Rate % today</th>
          </tr>
        </thead>
        <tbody>
          {GRADES.map((g) => (
            <tr key={g}>
              <td>{g}</td>
              <td data-testid={`rate-current-${g}`}>{rateOn(all, g, today) ?? NA}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <RateCalendar grades={GRADES} rates={all} today={today} />
      <table data-testid="rate-list">
        <thead>
          <tr>
            <th>Grade</th>
            <th>From</th>
            <th>To</th>
            <th>Rate %</th>
            <th>Version</th>
          </tr>
        </thead>
        <tbody>
          {all.filter((r) => !r.level).map((r) => (
            <tr key={`${r.grade}-${r.from}`} data-testid={`rate-row-${r.grade}-${r.from || 'start'}`}>
              <td>{r.grade}</td>
              <td data-testid="rate-from">{r.from || 'start'}</td>
              <td data-testid="rate-to">{r.to}</td>
              <td data-testid="rate-pct">{r.pct}</td>
              <td data-testid="rate-version">{r.version}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

// For each grade and each month of the sheet's today's calendar year, the rate in force on the
// first day of that month (n/a when none).
function RateCalendar({ grades, rates, today }: { grades: string[]; rates: Rate[]; today: string }) {
  if (today === '') return null;
  const year = today.slice(0, 4);
  const months = Array.from({ length: 12 }, (_, i) => `${year}-${String(i + 1).padStart(2, '0')}`);
  return (
    <table data-testid="rate-calendar">
      <thead>
        <tr>
          <th>Grade</th>
          {months.map((m) => (
            <th key={m}>{m}</th>
          ))}
        </tr>
      </thead>
      <tbody>
        {grades.map((g) => (
          <tr key={g}>
            <td>{g}</td>
            {months.map((m) => (
              <td key={m} data-testid={`rate-month-${g}-${m}`}>
                {rateOn(rates, g, `${m}-01`) ?? NA}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}

type Preview = { count: number; diff: string };

// A preview of a candidate rate for a grade: how many OPEN and PENDING claims on that grade would
// show a different due were it the grade's rate on every date, and the differences added up.
// Nothing is saved.
function RatePreview({ sheetId }: { sheetId: string }) {
  const GRADES = useGradeCodes(sheetId);
  const [grade, setGrade] = useState<Grade>('A');
  const [pct, setPct] = useState('');
  const [result, setResult] = useState<Preview | null>(null);
  const [error, setError] = useState('');
  const [user] = useUser();

  const preview = useMutation({
    mutationFn: (body: { grade: Grade; pct: string }) =>
      postJson<Preview>(`/api/sheets/${encodeURIComponent(sheetId)}/rates/preview`, body, user),
    onSuccess: (data) => setResult(data),
    onError: () => setError('That rate could not be previewed.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setResult(null);
    const value = pct.trim();
    if (value === '') {
      setError('A rate is required.');
      return;
    }
    if (!PCT.test(value)) {
      setError('A rate is a percent of 0.00 or more, with up to two decimals.');
      return;
    }
    preview.mutate({ grade, pct: value });
  };

  // A changed grade or rate makes the shown result stale: it goes, so the next preview is told apart.
  const changed = () => {
    setError('');
    setResult(null);
  };

  return (
    <form onSubmit={submit} data-testid="rate-preview-form">
      <label>
        Preview grade{' '}
        <select
          data-testid="rate-preview-grade"
          value={grade}
          onChange={(e) => {
            setGrade(e.target.value as Grade);
            changed();
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
        Rate %{' '}
        <input
          data-testid="rate-preview-pct"
          inputMode="decimal"
          value={pct}
          onChange={(e) => {
            setPct(e.target.value);
            changed();
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="rate-preview" disabled={preview.isPending}>
        Preview
      </button>
      {result && (
        <p>
          Claims changed: <strong data-testid="rate-preview-count">{result.count}</strong>, difference:{' '}
          <strong data-testid="rate-preview-diff">{result.diff}</strong>
        </p>
      )}
      {error && (
        <p role="alert" data-testid="rate-preview-error">
          {error}
        </p>
      )}
    </form>
  );
}

function DueHead() {
  return <Col name="due-total"><th>Due</th></Col>;
}

// A person's due-total: the due figures of their claims that count, added up, held to the sheet's cap less their cut total.
function PersonDueTotal({ sheetId, person }: { sheetId: string; person: Person }) {
  const claims = (useClaims(sheetId).data ?? []).filter((c) => c.personId === person.id);
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const cap = useCap(sheetId);
  const cutOf = useCutTotals(sheetId);
  const held = heldParts(person.id, usePeople(sheetId).data ?? [], useClaims(sheetId).data ?? [], rates, today, floor, late);
  return <Col name="due-total"><td data-testid="person-due-total">{personDueTotal(rates, person.grades, today, claims, liveHolds(person, today), floor, late, cap, held, cutOf(person.id))}</td></Col>;
}

function ExcessHead() {
  return <Col name="excess"><th>Excess</th></Col>;
}

// A person's excess: how far the due figures of their claims that count, with their cut total, go above the sheet's cap.
function PersonExcess({ sheetId, person }: { sheetId: string; person: Person }) {
  const claims = (useClaims(sheetId).data ?? []).filter((c) => c.personId === person.id);
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const cap = useCap(sheetId);
  const cutOf = useCutTotals(sheetId);
  const held = heldParts(person.id, usePeople(sheetId).data ?? [], useClaims(sheetId).data ?? [], rates, today, floor, late);
  return <Col name="excess"><td data-testid="person-excess">{personExcess(rates, person.grades, today, claims, liveHolds(person, today), floor, late, cap, held, cutOf(person.id))}</td></Col>;
}

// The sheet's due-total: everyone's due-total added up, skipping those with no rate, trimmed of the highest and lowest.
function DueTotal({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const cap = useCap(sheetId);
  const cutOf = useCutTotals(sheetId);
  const totals = people.map((p) =>
    personDueTotal(rates, p.grades, today, claims.filter((c) => c.personId === p.id), liveHolds(p, today), floor, late, cap,
      heldParts(p.id, people, claims, rates, today, floor, late), cutOf(p.id)),
  );
  return (
    <p>
      Due in all: <strong data-testid="rollup-due-total">{trimmedSumDue(totals)}</strong>
    </p>
  );
}

function SplitHead() {
  return (
    <>
      <th>Settled</th>
      <th>Open</th>
    </>
  );
}

// A person's settled-total and open-total: the due of their SETTLED claims that count, and of their
// OPEN and PENDING ones, before the sheet's cap (together their due-total before the cap).
function PersonSplitTotals({ sheetId, person }: { sheetId: string; person: Person }) {
  const claims = (useClaims(sheetId).data ?? []).filter((c) => c.personId === person.id);
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const held = heldParts(person.id, usePeople(sheetId).data ?? [], useClaims(sheetId).data ?? [], rates, today, floor, late);
  const split = personSplitTotals(rates, person.grades, today, claims, liveHolds(person, today), floor, late, held);
  return (
    <>
      <td data-testid="person-settled-total">{split.settled}</td>
      <td data-testid="person-open-total">{split.open}</td>
    </>
  );
}

// The sheet's settled-total and open-total: everyone's figure simply added up (not trimmed).
function SplitTotals({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const splits = people.map((p) =>
    personSplitTotals(rates, p.grades, today, claims.filter((c) => c.personId === p.id), liveHolds(p, today), floor, late,
      heldParts(p.id, people, claims, rates, today, floor, late)),
  );
  return (
    <p>
      Settled in all: <strong data-testid="rollup-settled-total">{sumDue(splits.map((s) => s.settled))}</strong>, open in
      all: <strong data-testid="rollup-open-total">{sumDue(splits.map((s) => s.open))}</strong>
    </p>
  );
}

// For every tag ever used on the sheet's claims, or carried by one of its people, its due-total: the due
// of the claims that count which carry it, or whose person carries it, added up, skipping n/a (a claim
// counts once in each of its tags and its person's tags).
function TagTotals({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const used = useTagsUsed(sheetId).data ?? [];
  const tags = [...new Set([...used, ...people.flatMap((p) => p.tags)])].sort();
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const dues = people.flatMap((p) =>
    claims
      .filter((c) => c.personId === p.id && (c.tags.length > 0 || p.tags.length > 0) && counts(c, liveHolds(p, today), today))
      .map((c) => ({ tags: [...new Set([...c.tags, ...p.tags])], due: claimDue(c, rateOn(rates, gradeOn(p.grades, c.date), c.date, c.id), floor, late, gradeOn(p.grades, c.date)) })),
  );
  if (tags.length === 0) return null;
  return (
    <section data-testid="tag-totals">
      <h3>Due by tag</h3>
      <ul>
        {tags.map((t) => (
          <li key={t}>
            {t}: <strong data-testid={`tag-${t}-due-total`}>{sumDue(dues.filter((d) => d.tags.includes(t)).map((d) => d.due))}</strong>
          </li>
        ))}
      </ul>
    </section>
  );
}

// For every month (yyyy-mm) any claim on the sheet is dated in, its due-total: the due of the
// claims dated in it that count, added up, skipping n/a.
function MonthTotals({ sheetId }: { sheetId: string }) {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const months = [...new Set(claims.map((c) => c.date.slice(0, 7)))].sort();
  const dues = people.flatMap((p) =>
    claims
      .filter((c) => c.personId === p.id && counts(c, liveHolds(p, today), today))
      .map((c) => ({ month: c.date.slice(0, 7), due: claimDue(c, rateOn(rates, gradeOn(p.grades, c.date), c.date, c.id), floor, late, gradeOn(p.grades, c.date)) })),
  );
  if (months.length === 0) return null;
  return (
    <section data-testid="month-totals">
      <h3>Due by month</h3>
      <ul>
        {months.map((m) => (
          <li key={m}>
            {m}: <strong data-testid={`month-${m}-due-total`}>{sumDue(dues.filter((d) => d.month === m).map((d) => d.due))}</strong>
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contributions = [
  SheetBody.fill({ order: 20.7, Component: MonthTotals }),
  SheetBody.fill({ order: 20.6, Component: TagTotals }),
  SheetBody.fill({ order: -10, Component: Rates }),
  SheetBody.fill({ order: 20, Component: DueTotal }),
  PersonHead.fill({ order: 20, Component: DueHead }),
  PersonRow.fill({ order: 20, Component: PersonDueTotal }),
  PersonHead.fill({ order: 21, Component: ExcessHead }),
  PersonRow.fill({ order: 21, Component: PersonExcess }),
  SheetBody.fill({ order: 20.5, Component: SplitTotals }),
  PersonHead.fill({ order: 20.5, Component: SplitHead }),
  PersonRow.fill({ order: 20.5, Component: PersonSplitTotals }),
];
