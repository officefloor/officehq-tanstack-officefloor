-- A sheet as it stood at the end of a date: every change made while the sheet's today was on or
-- before it counts, every later one does not (see sheet.AsOf).
--
-- asof_hist.<table> keeps every row each change to <table> removed ('D') or added ('I'), with the
-- sheet's today when it was made (an update removes the old row and adds the new one). It is
-- written by the trigger sheet.History on each table of a sheet's data.
--
-- asof.<table> has the same columns as <table>: a sheet's rows as of a date are rebuilt into it, and
-- the app's own reads run against it in place of the live tables. A later migration that changes one
-- of these tables must change its asof and asof_hist copies the same way.
CREATE SCHEMA asof;
CREATE SCHEMA asof_hist;
CREATE SEQUENCE asof_hist.change_seq;
CREATE TABLE asof.sheet AS SELECT * FROM public.sheet WHERE FALSE;

CREATE TABLE asof.claim AS SELECT * FROM public.claim WHERE FALSE;
CREATE TABLE asof_hist.claim AS SELECT * FROM public.claim WHERE FALSE;
ALTER TABLE asof_hist.claim ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.claim ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.claim ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX claim_hist_sheet ON asof_hist.claim (sheet_id, h_seq);
CREATE TRIGGER claim_history AFTER INSERT, UPDATE, DELETE ON public.claim FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';

CREATE TABLE asof.person AS SELECT * FROM public.person WHERE FALSE;
CREATE TABLE asof_hist.person AS SELECT * FROM public.person WHERE FALSE;
ALTER TABLE asof_hist.person ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.person ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.person ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX person_hist_sheet ON asof_hist.person (sheet_id, h_seq);
CREATE TRIGGER person_history AFTER INSERT, UPDATE, DELETE ON public.person FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';

CREATE TABLE asof.person_grade AS SELECT * FROM public.person_grade WHERE FALSE;
CREATE TABLE asof_hist.person_grade AS SELECT * FROM public.person_grade WHERE FALSE;
ALTER TABLE asof_hist.person_grade ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.person_grade ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.person_grade ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX person_grade_hist_sheet ON asof_hist.person_grade (sheet_id, h_seq);
CREATE TRIGGER person_grade_history AFTER INSERT, UPDATE, DELETE ON public.person_grade FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';

CREATE TABLE asof.person_hold AS SELECT * FROM public.person_hold WHERE FALSE;
CREATE TABLE asof_hist.person_hold AS SELECT * FROM public.person_hold WHERE FALSE;
ALTER TABLE asof_hist.person_hold ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.person_hold ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.person_hold ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX person_hold_hist_sheet ON asof_hist.person_hold (sheet_id, h_seq);
CREATE TRIGGER person_hold_history AFTER INSERT, UPDATE, DELETE ON public.person_hold FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';

CREATE TABLE asof.person_patron AS SELECT * FROM public.person_patron WHERE FALSE;
CREATE TABLE asof_hist.person_patron AS SELECT * FROM public.person_patron WHERE FALSE;
ALTER TABLE asof_hist.person_patron ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.person_patron ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.person_patron ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX person_patron_hist_sheet ON asof_hist.person_patron (sheet_id, h_seq);
CREATE TRIGGER person_patron_history AFTER INSERT, UPDATE, DELETE ON public.person_patron FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';

CREATE TABLE asof.person_watch AS SELECT * FROM public.person_watch WHERE FALSE;
CREATE TABLE asof_hist.person_watch AS SELECT * FROM public.person_watch WHERE FALSE;
ALTER TABLE asof_hist.person_watch ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.person_watch ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.person_watch ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX person_watch_hist_sheet ON asof_hist.person_watch (sheet_id, h_seq);
CREATE TRIGGER person_watch_history AFTER INSERT, UPDATE, DELETE ON public.person_watch FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';

CREATE TABLE asof.person_statement AS SELECT * FROM public.person_statement WHERE FALSE;
CREATE TABLE asof_hist.person_statement AS SELECT * FROM public.person_statement WHERE FALSE;
ALTER TABLE asof_hist.person_statement ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.person_statement ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.person_statement ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX person_statement_hist_sheet ON asof_hist.person_statement (sheet_id, h_seq);
CREATE TRIGGER person_statement_history AFTER INSERT, UPDATE, DELETE ON public.person_statement FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';

CREATE TABLE asof.period AS SELECT * FROM public.period WHERE FALSE;
CREATE TABLE asof_hist.period AS SELECT * FROM public.period WHERE FALSE;
ALTER TABLE asof_hist.period ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.period ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.period ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX period_hist_sheet ON asof_hist.period (sheet_id, h_seq);
CREATE TRIGGER period_history AFTER INSERT, UPDATE, DELETE ON public.period FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';

CREATE TABLE asof.period_person_total AS SELECT * FROM public.period_person_total WHERE FALSE;
CREATE TABLE asof_hist.period_person_total AS SELECT * FROM public.period_person_total WHERE FALSE;
ALTER TABLE asof_hist.period_person_total ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.period_person_total ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.period_person_total ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX period_person_total_hist_sheet ON asof_hist.period_person_total (sheet_id, h_seq);
CREATE TRIGGER period_person_total_history AFTER INSERT, UPDATE, DELETE ON public.period_person_total FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';

CREATE TABLE asof.period_number AS SELECT * FROM public.period_number WHERE FALSE;
CREATE TABLE asof_hist.period_number AS SELECT * FROM public.period_number WHERE FALSE;
ALTER TABLE asof_hist.period_number ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.period_number ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.period_number ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX period_number_hist_sheet ON asof_hist.period_number (sheet_id, h_seq);
CREATE TRIGGER period_number_history AFTER INSERT, UPDATE, DELETE ON public.period_number FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';

CREATE TABLE asof.rate AS SELECT * FROM public.rate WHERE FALSE;
CREATE TABLE asof_hist.rate AS SELECT * FROM public.rate WHERE FALSE;
ALTER TABLE asof_hist.rate ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.rate ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.rate ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX rate_hist_sheet ON asof_hist.rate (sheet_id, h_seq);
CREATE TRIGGER rate_history AFTER INSERT, UPDATE, DELETE ON public.rate FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';

CREATE TABLE asof.sheet_setting AS SELECT * FROM public.sheet_setting WHERE FALSE;
CREATE TABLE asof_hist.sheet_setting AS SELECT * FROM public.sheet_setting WHERE FALSE;
ALTER TABLE asof_hist.sheet_setting ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.sheet_setting ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.sheet_setting ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX sheet_setting_hist_sheet ON asof_hist.sheet_setting (sheet_id, h_seq);
CREATE TRIGGER sheet_setting_history AFTER INSERT, UPDATE, DELETE ON public.sheet_setting FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';

CREATE TABLE asof.sheet_setting_dated AS SELECT * FROM public.sheet_setting_dated WHERE FALSE;
CREATE TABLE asof_hist.sheet_setting_dated AS SELECT * FROM public.sheet_setting_dated WHERE FALSE;
ALTER TABLE asof_hist.sheet_setting_dated ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.sheet_setting_dated ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.sheet_setting_dated ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX sheet_setting_dated_hist_sheet ON asof_hist.sheet_setting_dated (sheet_id, h_seq);
CREATE TRIGGER sheet_setting_dated_history AFTER INSERT, UPDATE, DELETE ON public.sheet_setting_dated FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';

CREATE TABLE asof.sheet_role AS SELECT * FROM public.sheet_role WHERE FALSE;
CREATE TABLE asof_hist.sheet_role AS SELECT * FROM public.sheet_role WHERE FALSE;
ALTER TABLE asof_hist.sheet_role ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.sheet_role ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.sheet_role ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX sheet_role_hist_sheet ON asof_hist.sheet_role (sheet_id, h_seq);
CREATE TRIGGER sheet_role_history AFTER INSERT, UPDATE, DELETE ON public.sheet_role FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';

CREATE TABLE asof.sheet_permission AS SELECT * FROM public.sheet_permission WHERE FALSE;
CREATE TABLE asof_hist.sheet_permission AS SELECT * FROM public.sheet_permission WHERE FALSE;
ALTER TABLE asof_hist.sheet_permission ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.sheet_permission ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.sheet_permission ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX sheet_permission_hist_sheet ON asof_hist.sheet_permission (sheet_id, h_seq);
CREATE TRIGGER sheet_permission_history AFTER INSERT, UPDATE, DELETE ON public.sheet_permission FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';

CREATE TABLE asof.sheet_delegation AS SELECT * FROM public.sheet_delegation WHERE FALSE;
CREATE TABLE asof_hist.sheet_delegation AS SELECT * FROM public.sheet_delegation WHERE FALSE;
ALTER TABLE asof_hist.sheet_delegation ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.sheet_delegation ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.sheet_delegation ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX sheet_delegation_hist_sheet ON asof_hist.sheet_delegation (sheet_id, h_seq);
CREATE TRIGGER sheet_delegation_history AFTER INSERT, UPDATE, DELETE ON public.sheet_delegation FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';

CREATE TABLE asof.claim_log AS SELECT * FROM public.claim_log WHERE FALSE;
CREATE TABLE asof_hist.claim_log AS SELECT * FROM public.claim_log WHERE FALSE;
ALTER TABLE asof_hist.claim_log ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.claim_log ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.claim_log ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX claim_log_hist_sheet ON asof_hist.claim_log (sheet_id, h_seq);
CREATE TRIGGER claim_log_history AFTER INSERT, UPDATE, DELETE ON public.claim_log FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';

CREATE TABLE asof.sheet_grade_terms AS SELECT * FROM public.sheet_grade_terms WHERE FALSE;
CREATE TABLE asof_hist.sheet_grade_terms AS SELECT * FROM public.sheet_grade_terms WHERE FALSE;
ALTER TABLE asof_hist.sheet_grade_terms ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.sheet_grade_terms ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.sheet_grade_terms ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX sheet_grade_terms_hist_sheet ON asof_hist.sheet_grade_terms (sheet_id, h_seq);
CREATE TRIGGER sheet_grade_terms_history AFTER INSERT, UPDATE, DELETE ON public.sheet_grade_terms FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
