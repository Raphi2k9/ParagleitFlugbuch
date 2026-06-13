-- ============================================================
-- Bug-Fix: Endlos-Rekursion in RLS-Policies
--
-- Die Policy "Schule sieht eigene Schüler" fragt user_profiles
-- innerhalb ihrer eigenen Policy ab. Postgres bricht dadurch
-- JEDES SELECT auf user_profiles ab ("infinite recursion
-- detected in policy") – und weil die Flights-Policy ebenfalls
-- user_profiles abfragt, schlägt auch jedes SELECT auf flights
-- fehl. Profil-Laden, Flug-Sync und Dashboard waren damit kaputt.
--
-- Lösung: SECURITY-DEFINER-Funktionen lesen Rolle und Schule des
-- eingeloggten Nutzers ohne RLS; die Policies nutzen nur noch
-- diese Funktionen und rekursieren nicht mehr.
-- ============================================================

-- Fehlende Spalte: Die App liest und schreibt instructor_approved
-- (Bestätigung durch den Fluglehrer), die Spalte fehlte aber im Schema.
ALTER TABLE public.flights ADD COLUMN IF NOT EXISTS instructor_approved BOOLEAN;

CREATE OR REPLACE FUNCTION public.current_user_role()
RETURNS TEXT
LANGUAGE sql STABLE SECURITY DEFINER
SET search_path = public
AS $$
    SELECT role FROM public.user_profiles WHERE id = auth.uid();
$$;

CREATE OR REPLACE FUNCTION public.current_user_school_id()
RETURNS UUID
LANGUAGE sql STABLE SECURITY DEFINER
SET search_path = public
AS $$
    SELECT school_id FROM public.user_profiles WHERE id = auth.uid();
$$;

DROP POLICY IF EXISTS "Schule sieht eigene Schüler" ON public.user_profiles;
CREATE POLICY "Schule sieht eigene Schüler"
    ON public.user_profiles FOR SELECT
    USING (
        public.current_user_role() IN ('INSTRUCTOR', 'SCHOOL_ADMIN')
        AND school_id IS NOT NULL
        AND school_id = public.current_user_school_id()
    );

DROP POLICY IF EXISTS "Schule sieht Schüler-Flüge" ON public.flights;
CREATE POLICY "Schule sieht Schüler-Flüge"
    ON public.flights FOR SELECT
    USING (
        public.current_user_role() IN ('INSTRUCTOR', 'SCHOOL_ADMIN')
        AND EXISTS (
            SELECT 1 FROM public.user_profiles student
            WHERE student.id = flights.user_id
              AND student.school_id = public.current_user_school_id()
        )
    );

-- Fehlende UPDATE-Policy: Das Dashboard erlaubt Instruktoren das
-- Bestätigen (instructor_approved) und Admins das Bearbeiten von
-- Schüler-Flügen – ohne diese Policy liefen beide Updates ins Leere
-- (0 betroffene Zeilen), weil nur der Eigentümer schreiben durfte.
DROP POLICY IF EXISTS "Schule darf Schüler-Flüge bearbeiten" ON public.flights;
CREATE POLICY "Schule darf Schüler-Flüge bearbeiten"
    ON public.flights FOR UPDATE
    USING (
        public.current_user_role() IN ('INSTRUCTOR', 'SCHOOL_ADMIN')
        AND EXISTS (
            SELECT 1 FROM public.user_profiles student
            WHERE student.id = flights.user_id
              AND student.school_id = public.current_user_school_id()
        )
    )
    WITH CHECK (
        public.current_user_role() IN ('INSTRUCTOR', 'SCHOOL_ADMIN')
        AND EXISTS (
            SELECT 1 FROM public.user_profiles student
            WHERE student.id = flights.user_id
              AND student.school_id = public.current_user_school_id()
        )
    );
