-- The grades added to each sheet, on top of A, B and C which every sheet has: a code of one to three
-- capital letters, at most once per sheet, listed after A, B and C in the order they were added (seq).
-- Grade columns widen to hold such a code.
CREATE TABLE sheet_grade (
    seq      BIGINT       AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL,
    grade    VARCHAR(3)   NOT NULL
);
CREATE UNIQUE INDEX sheet_grade_key ON sheet_grade (sheet_id, grade);
ALTER TABLE public.person ALTER COLUMN grade SET DATA TYPE VARCHAR(3);
ALTER TABLE public.person_grade ALTER COLUMN grade SET DATA TYPE VARCHAR(3);
ALTER TABLE public.rate ALTER COLUMN grade SET DATA TYPE VARCHAR(3);
ALTER TABLE public.rate_version ALTER COLUMN grade SET DATA TYPE VARCHAR(3);
ALTER TABLE public.sheet_grade_terms ALTER COLUMN grade SET DATA TYPE VARCHAR(3);
ALTER TABLE public.sheet_setting ALTER COLUMN default_grade SET DATA TYPE VARCHAR(3);
ALTER TABLE asof.person ALTER COLUMN grade SET DATA TYPE VARCHAR(3);
ALTER TABLE asof.person_grade ALTER COLUMN grade SET DATA TYPE VARCHAR(3);
ALTER TABLE asof.rate ALTER COLUMN grade SET DATA TYPE VARCHAR(3);
ALTER TABLE asof.rate_version ALTER COLUMN grade SET DATA TYPE VARCHAR(3);
ALTER TABLE asof.sheet_grade_terms ALTER COLUMN grade SET DATA TYPE VARCHAR(3);
ALTER TABLE asof.sheet_setting ALTER COLUMN default_grade SET DATA TYPE VARCHAR(3);
ALTER TABLE asof_hist.person ALTER COLUMN grade SET DATA TYPE VARCHAR(3);
ALTER TABLE asof_hist.person_grade ALTER COLUMN grade SET DATA TYPE VARCHAR(3);
ALTER TABLE asof_hist.rate ALTER COLUMN grade SET DATA TYPE VARCHAR(3);
ALTER TABLE asof_hist.rate_version ALTER COLUMN grade SET DATA TYPE VARCHAR(3);
ALTER TABLE asof_hist.sheet_grade_terms ALTER COLUMN grade SET DATA TYPE VARCHAR(3);
ALTER TABLE asof_hist.sheet_setting ALTER COLUMN default_grade SET DATA TYPE VARCHAR(3);
