import { asString, useSearchParam } from '../../url/useSearchParam';
import { ClaimForm } from '../claims/ClaimForm';
import { BulkOpenButton, BulkReassignButton, BulkRedateButton, BulkSettleButton, BulkVoidButton, ClaimSelection } from '../claims/BulkOpen';
import { ClaimList } from '../claims/ClaimList';
import { sumAmounts, tagList } from '../claims/useClaims';
import { claimDue, formatCents, showDue, toCents } from '../rates/due';
import { claimRate } from '../rates/useRates';
import { StatementButton } from '../statements/StatementButton';
import { UnstateButton } from '../statements/UnstateButton';
import { SummaryPanel } from '../summary/SummaryPanel';
import { AliasControl } from './AliasControl';
import { openedEditor } from './editorOpens';
import { BulkGrade, BulkRetire, PersonSelect, PersonSelection } from './BulkGrade';
import { FlagControl } from './FlagControl';
import { HoldControl } from './HoldControl';
import { InlineGrade } from './InlineGrade';
import { MergeControl } from './MergeControl';
import { MoveControl } from './MoveControl';
import { PasteClaims } from './PasteClaims';
import { GeneratePlans } from '../plans/GeneratePlans';
import { PlanControl } from '../plans/PlanControl';
import { ImportPeople } from './ImportPeople';
import { PatronControl } from './PatronControl';
import { QuickAdd } from './QuickAdd';
import { RetireControl, UnretireButton } from './RetireControl';
import { TransferControl } from './TransferControl';
import { TransferUndoControl } from './TransferUndoControl';
import { WatchControl } from './WatchControl';
import { useWatches } from './useWatches';
import { downloadCsv } from './csv';
import { ShowRetired, useShowRetired } from './ShowRetired';
import { SortBy, useSortBy } from './SortBy';
import { ColumnChooser, useColumns } from './ColumnChooser';
import { gradeOn, patronShares, sharedCents, shownName } from './usePeople';
import { usePersonPeriodCarried, usePersonPeriodDueTotal } from '../periods/usePeriodDueTotal';
import { periodOf, usePeriods } from '../periods/usePeriods';
import { useSheetFigures } from './useSheetFigures';
import { dueBy } from '../claims/overdue';
import { useGrace } from '../claims/useGrace';
import { useFloor, useLatePct, usePolicy } from '../settings/useSettings';
import { usePools } from '../pools/usePools';

// Every person on the sheet in the order they were added, with their name shown as "<name> (<grade in force today>)", grade (the one in force on the sheet's today), claimed (the
// sum of their OPEN and SETTLED claims, leaving out corrections), due-total (the
// sum of their counting claims' already-rounded dues, at most the sheet's cap — their excess shows how far the sum is above it, else 0.00; n/a when their grade has no rate today and no settled or stated claim keeps a figure),
// their due-total for each period (fixed when the period was closed, 0.00 for a person added after),
// their settled-total and open-total (the dues of their counting SETTLED claims, and of their counting OPEN and PENDING
// claims; together the sum of their figures before the cap),
// the dues of their counting OPEN and PENDING claims by age on today (0 to 30 days, 31 to 60, 61 or more; dated after today is 0 to 30),
// whether they are retired and from when, whether they have any hold, and their holds (a person's OPEN claims dated within a hold, unless bound by a
// statement, keep their claimed but their dues leave their figures and the sheet's totals; their other claims still count), their overdue count and
// aliases (sorted as names are compared, comma and space separated), whether they clash (their name or an alias is used by someone else on a sheet of the tree), patrons (each as <name> <share>, sorted by name, comma and space separated; shares split 100.00 equally in the order named, rounded half to even, the remainder to the first-named) and backing (each patron's settled figure — their SETTLED claims dated within the 90 days up to and including today, at their frozen figures — times their share over 100, added up and rounded half to even; 0.00 with none),
// their overdue count and overdue-total (how many of their counting claims are overdue, and the sum of those claims' dues with their late charges, n/a adding nothing), their statements (the statement button, how many were taken, the last one's due-total and date) and their claims; then the
// sheet's claimed-total, due-total (the people's due-totals added up, skipping n/a), settled-total and open-total (everyone's added up, untrimmed), overdue count and overdue-total (the
// due-total and overdue-total leave out the single highest and lowest person's figure when three or more have one).
// Then, for every tag ever used on the sheet's claims, its due-total: the dues of the counting claims carrying it. Retired
// people have no row unless "show retired" is on, and from their retirement date their OPEN and PENDING claims, unless bound by a statement, leave every live total.
// Then the summary panel, worked from these same figures. The export button downloads every person (retired too), in list
// order, as CSV: name, grade in force today, retired-from (or nothing) and due-total as shown. The export claims button
// downloads every claim as CSV — people in list order (retired too), each person's claims oldest first — with person,
// number, date, amount, due, status and period as the claim's row shows them. Claims ticked
// with their select checkbox are opened together with the bulk open button, or moved to one person with the bulk reassign; people ticked with theirs
// are given a grade together with the bulk grade.
// Each person also shows their cut total: their cuts across every pool they are a member of, added up (a pool showing
// no cut adds nothing); the sheet's cut total is everyone's added up.
// Names sorted without regard to case, then by case (as the server sorts them).
const cmp = (a: string, b: string) => (a < b ? -1 : a > b ? 1 : 0);
const BY_NAME = (a: string, b: string) => cmp(a.toLowerCase(), b.toLowerCase()) || cmp(a, b);

export function PeopleList() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const [, setEditing] = useSearchParam('editPerson', asString);
  const watched = useWatches(sheet, user);
  const { people, claims, rates, today, byId, live, claimedLive, totalOf, excessOf, overdue, overdueTotal, sheetDueTotal, sheetOverdueTotal, statusCount, reviewLateCount, voidReasonCount, isActive, recentSettledOf, agedOf, settledTotalOf, openTotalOf, sheetSettledTotal, sheetOpenTotal, tagTotals, monthTotals } = useSheetFigures(sheet);
  const [showRetired] = useShowRetired();
  const periods = usePeriods(sheet);
  const periodDueOf = usePersonPeriodDueTotal(sheet);
  const periodCarriedOf = usePersonPeriodCarried(sheet);
  const pools = usePools(sheet);
  const cutOf = (personId: string) =>
    pools.reduce(
      (sum, pool) => pool.members.reduce((s, m) => (m.personId === personId && m.cut !== '' ? s + toCents(m.cut) : s), sum),
      0n,
    );
  const sheetCutTotal = people.reduce((sum, p) => sum + cutOf(p.id), 0n);
  const [sortKey] = useSortBy();
  const [shown] = useColumns();
  // The rows as shown: list order, or sorted by the user's chosen key (by grade in force today, by
  // name as names are compared, by due-total with n/a last); equal keys keep list order.
  const bySort = (a: (typeof people)[number], b: (typeof people)[number]): number => {
    switch (sortKey) {
      case 'name':
        return BY_NAME(a.name, b.name);
      case 'grade':
        return cmp(gradeOn(a, today), gradeOn(b, today));
      case 'due-total': {
        const da = totalOf(a);
        const db = totalOf(b);
        if (da === null || db === null) return da === db ? 0 : da === null ? 1 : -1;
        return da < db ? -1 : da > db ? 1 : 0;
      }
      default:
        return 0;
    }
  };
  const listed = (showRetired ? people : people.filter((p) => !p.retired)).slice().sort(bySort);
  const backing = (p: (typeof people)[number]) =>
    formatCents(sharedCents(patronShares(p).reduce((sum, { id, share }) => {
      const patron = byId.get(id);
      return patron ? sum + recentSettledOf(patron) * share : sum;
    }, 0n)));
  const patronsOf = (p: (typeof people)[number]) =>
    patronShares(p)
      .flatMap(({ id, share }) => {
        const patron = byId.get(id);
        return patron ? [{ name: patron.name, shown: shownName(patron, today), share }] : [];
      })
      .sort((a, b) => BY_NAME(a.name, b.name))
      .map(({ shown, share }) => `${shown} ${formatCents(share)}`)
      .join(', ');

  const floor = useFloor(sheet);
  const latePct = useLatePct(sheet);
  const grace = useGrace(sheet);
  const policy = usePolicy(sheet);
  const claimRows = () =>
    people.flatMap((p) =>
      claims
        .filter((c) => c.personId === p.id)
        .reverse()
        .map((c) => [
          p.name,
          c.number,
          c.date,
          c.amount,
          showDue(claimDue(c, claimRate(rates, gradeOn(p, c.date), c), floor, { pct: latePct, today, grace }, policy)),
          c.status,
          periodOf(periods, dueBy(c))?.name ?? '',
        ]),
    );

  const overdueOf = (personId: string) => overdue.filter((c) => c.personId === personId);
  return (
    <ClaimSelection>
      <PersonSelection>
      <QuickAdd />
      <PasteClaims />
      <GeneratePlans />
      <ImportPeople />
      <ShowRetired />
      <SortBy />
      <ColumnChooser />
      <button
        type="button"
        data-testid="export-people"
        onClick={() =>
          downloadCsv(
            'people.csv',
            [['name', 'grade', 'retired-from', 'due-total'], ...people.map((p) => [p.name, gradeOn(p, today), p.retiredFrom, showDue(totalOf(p))])],
          )
        }
      >
        Export CSV
      </button>
      <button
        type="button"
        data-testid="export-claims"
        onClick={() => downloadCsv('claims.csv', [['person', 'number', 'date', 'amount', 'due', 'status', 'period'], ...claimRows()])}
      >
        Export claims
      </button>
      <BulkOpenButton />
      <BulkSettleButton />
      <BulkReassignButton />
      <BulkRedateButton />
      <BulkVoidButton />
      <BulkGrade />
      <BulkRetire />
      <table data-testid="people">
        <thead>
          <tr>
            <th />
            <th>Name</th>
            {shown('aliases') && <th>Aliases</th>}
            <th>Clashing</th>
            <th>From sheet</th>
            <th>Grade</th>
            {shown('terms') && <th>Terms</th>}
            {shown('contact') && <th>Contact</th>}
            <th>Tags</th>
            {shown('claimed') && <th>Claimed</th>}
            {shown('due-total') && <th>Due</th>}
            {shown('excess') && <th>Excess</th>}
            <th>Settled</th>
            <th>Open</th>
            <th>Active</th>
            <th>Aged 0-30</th>
            <th>Aged 31-60</th>
            <th>Aged 61+</th>
            {periods.map((period) => (
              <th key={period.name}>Due {period.name}</th>
            ))}
            {periods.map((period) => (
              <th key={period.name}>Carried {period.name}</th>
            ))}
            <th>Cut</th>
            {shown('patrons') && <th>Patrons</th>}
            {shown('backing') && <th>Backing</th>}
            {shown('overdue-count') && <th>Overdue</th>}
            {shown('overdue-total') && <th>Overdue due</th>}
            <th />
            <th>Watched</th>
            {shown('retired') && <th>Retired</th>}
            <th>Retired from</th>
            <th>Past retirements</th>
            <th>Snapshots</th>
            {shown('hold') && <th>Hold</th>}
            {shown('statement-count') && <th>Statements</th>}
            {shown('stated-total') && <th>Last stated</th>}
            {shown('statement-date') && <th>Stated on</th>}
            <th>Statement lines</th>
            <th>Claims</th>
          </tr>
        </thead>
        <tbody>
          {listed.map((p) => {
            const own = claims.filter((c) => c.personId === p.id);
            const aged = agedOf(p);
            return (
              <tr key={p.id} data-testid={`person-row-${p.id}`}>
                <td>
                  <PersonSelect personId={p.id} />
                </td>
                <td data-testid="person-name">{shownName(p, today)}</td>
                {shown('aliases') && <td data-testid="person-aliases">{p.aliases.join(', ')}</td>}
                <td data-testid="person-clashing">{p.clashing ? 'yes' : 'no'}</td>
                <td data-testid="person-origin">{p.origin}</td>
                <td data-testid="person-grade">{gradeOn(p, today)}</td>
                {shown('terms') && <td data-testid="person-terms">{p.terms}</td>}
                {shown('contact') && <td data-testid="person-contact">{p.contact}</td>}
                <td data-testid="person-tags">{tagList(p.tags)}</td>
                {shown('claimed') && <td data-testid="person-claimed">{sumAmounts(claimedLive.filter((c) => c.personId === p.id))}</td>}
                {shown('due-total') && <td data-testid="person-due-total">{showDue(totalOf(p))}</td>}
                {shown('excess') && <td data-testid="person-excess">{formatCents(excessOf(p))}</td>}
                <td data-testid="person-settled-total">{formatCents(settledTotalOf(p))}</td>
                <td data-testid="person-open-total">{formatCents(openTotalOf(p))}</td>
                <td data-testid="person-active">{isActive(p) ? 'yes' : 'no'}</td>
                <td data-testid="person-aged-0-30">{formatCents(aged[0])}</td>
                <td data-testid="person-aged-31-60">{formatCents(aged[1])}</td>
                <td data-testid="person-aged-61">{formatCents(aged[2])}</td>
                {periods.map((period) => (
                  <td key={period.name} data-testid={`person-period-${period.name}-due-total`}>
                    {formatCents(periodDueOf(period, p.id))}
                  </td>
                ))}
                {periods.map((period) => (
                  <td key={period.name} data-testid={`person-period-${period.name}-carried`}>
                    {formatCents(periodCarriedOf(period, p.id))}
                  </td>
                ))}
                <td data-testid="person-cut-total">{formatCents(cutOf(p.id))}</td>
                {shown('patrons') && <td data-testid="person-patrons">{patronsOf(p)}</td>}
                {shown('backing') && <td data-testid="person-backing">{backing(p)}</td>}
                {shown('overdue-count') && <td data-testid="person-overdue-count">{overdueOf(p.id).length}</td>}
                {shown('overdue-total') && <td data-testid="person-overdue-total">{overdueTotal(overdueOf(p.id))}</td>}
                <td>
                  <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => {
                    setEditing(p.id);
                    openedEditor();
                  }}>
                    Edit
                  </button>{' '}
                  <InlineGrade person={p} shown={gradeOn(p, today)} />{' '}
                  <MoveControl personId={p.id} />{' '}
                  <StatementButton personId={p.id} />{' '}
                  <UnstateButton personId={p.id} />{' '}
                  <RetireControl personId={p.id} />{' '}
                  <UnretireButton personId={p.id} />{' '}
                  <HoldControl personId={p.id} holds={p.holds} />{' '}
                  <FlagControl personId={p.id} shown={p.flag ?? ''} />{' '}
                  <PatronControl personId={p.id} people={people} />{' '}
                  <AliasControl personId={p.id} />{' '}
                  <MergeControl personId={p.id} people={people} />{' '}
                  <TransferControl personId={p.id} />{' '}
                  <TransferUndoControl personId={p.id} />{' '}
                  <WatchControl personId={p.id} watched={watched.includes(p.id)} />
                </td>
                <td data-testid="person-watched">{watched.includes(p.id) ? 'yes' : 'no'}</td>
                {shown('retired') && <td data-testid="person-retired">{p.retired ? 'yes' : 'no'}</td>}
                <td data-testid="person-retired-from">{p.retiredFrom}</td>
                <td>
                  {(p.retirements ?? []).map((line, i) => (
                    <div key={i} data-testid="person-retirement-line">
                      {line}
                    </div>
                  ))}
                </td>
                <td>
                  {(p.snapshots ?? []).map((line, i) => (
                    <div key={i} data-testid="person-snapshot-line">
                      {line}
                    </div>
                  ))}
                </td>
                {shown('hold') && <td data-testid="person-hold">{p.held ? 'yes' : 'no'}</td>}
                {shown('statement-count') && <td data-testid="person-statement-count">{p.statementCount}</td>}
                {shown('stated-total') && <td data-testid="person-stated-total">{p.statedTotal}</td>}
                {shown('statement-date') && <td data-testid="person-statement-date">{p.statementDate}</td>}
                <td>
                  {(p.statements ?? []).map((line, i) => (
                    <div key={i} data-testid="person-statement-line-line">
                      {line}
                    </div>
                  ))}
                </td>
                <td>
                  <PlanControl personId={p.id} />
                  <ClaimList claims={own} people={people} rateOn={(c) => claimRate(rates, gradeOn(p, c.date), c)} />
                  <ClaimForm personId={p.id} claims={own} />
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
      <p>
        Claimed total: <span data-testid="rollup-claimed-total">{sumAmounts(claimedLive)}</span>
      </p>
      <p>
        Due total: <span data-testid="rollup-due-total">{sheetDueTotal}</span>
      </p>
      <p>
        Settled total: <span data-testid="rollup-settled-total">{sheetSettledTotal}</span>
      </p>
      <p>
        Open total: <span data-testid="rollup-open-total">{sheetOpenTotal}</span>
      </p>
      <p>
        Cut total: <span data-testid="rollup-cut-total">{formatCents(sheetCutTotal)}</span>
      </p>
      <p>
        Overdue: <span data-testid="rollup-overdue-count">{overdue.length}</span>
      </p>
      <p>
        Overdue total: <span data-testid="rollup-overdue-total">{sheetOverdueTotal}</span>
      </p>
      <ul>
        {tagTotals.map(({ tag, total }) => (
          <li key={tag}>
            Tag {tag} due total: <span data-testid={`tag-${tag}-due-total`}>{total}</span>
          </li>
        ))}
      </ul>
      <ul>
        {monthTotals.map(({ month, total }) => (
          <li key={month}>
            Month {month} due total: <span data-testid={`month-${month}-due-total`}>{total}</span>
          </li>
        ))}
      </ul>
      <SummaryPanel people={people} today={today} totalOf={totalOf} isActive={isActive} statusCount={statusCount} reviewLateCount={reviewLateCount} voidReasonCount={voidReasonCount} />
      </PersonSelection>
    </ClaimSelection>
  );
}
