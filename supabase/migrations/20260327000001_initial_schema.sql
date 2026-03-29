-- ============================================================
-- Paragleit Flugbuch – Initial Schema
-- ============================================================

-- ---- Schools -----------------------------------------------
CREATE TABLE public.schools (
    id          UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    name        TEXT        NOT NULL,
    location    TEXT        NOT NULL DEFAULT '',
    invite_code TEXT        UNIQUE NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- ---- User Profiles (extends auth.users) --------------------
-- role: STUDENT | INSTRUCTOR | SCHOOL_ADMIN
CREATE TABLE public.user_profiles (
    id             UUID  REFERENCES auth.users(id) ON DELETE CASCADE PRIMARY KEY,
    full_name      TEXT  NOT NULL,
    license_number TEXT  NOT NULL DEFAULT '',
    role           TEXT  NOT NULL DEFAULT 'STUDENT'
                         CHECK (role IN ('STUDENT', 'INSTRUCTOR', 'SCHOOL_ADMIN')),
    school_id      UUID  REFERENCES public.schools(id) ON DELETE SET NULL,
    created_at     TIMESTAMPTZ DEFAULT NOW()
);

-- ---- Flights -----------------------------------------------
CREATE TABLE public.flights (
    id                  UUID   DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id             UUID   REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    date                BIGINT NOT NULL,          -- Unix-Timestamp in ms
    glider_name         TEXT   NOT NULL,
    duration_minutes    INT    NOT NULL,
    flight_type         TEXT   NOT NULL,
    start_location      TEXT   NOT NULL DEFAULT '',
    landing_location    TEXT   NOT NULL DEFAULT '',
    max_altitude        INT,
    distance            DOUBLE PRECISION,
    wind_conditions     TEXT   NOT NULL DEFAULT '',
    cloud_cover         TEXT   NOT NULL DEFAULT '',
    temperature         INT,
    notes               TEXT   NOT NULL DEFAULT '',
    training_exercises  TEXT,                     -- kommagetrennte Übungsnamen
    pruefung_bestanden  BOOLEAN,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW()
);

-- Aktualisiert updated_at automatisch bei jedem UPDATE
CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER flights_updated_at
    BEFORE UPDATE ON public.flights
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- ============================================================
-- Row Level Security
-- ============================================================

ALTER TABLE public.schools       ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.flights       ENABLE ROW LEVEL SECURITY;

-- ---- Schools: jeder kann lesen, nur Admins dürfen schreiben
CREATE POLICY "Alle können Schulen lesen"
    ON public.schools FOR SELECT
    USING (true);

CREATE POLICY "School-Admin darf eigene Schule bearbeiten"
    ON public.schools FOR ALL
    USING (
        EXISTS (
            SELECT 1 FROM public.user_profiles
            WHERE id = auth.uid()
              AND school_id = schools.id
              AND role = 'SCHOOL_ADMIN'
        )
    );

-- ---- User Profiles: jeder sieht eigenes Profil
CREATE POLICY "Nutzer sieht eigenes Profil"
    ON public.user_profiles FOR ALL
    USING (id = auth.uid());

-- Instruktoren/Admins sehen Profile ihrer Schulschüler
CREATE POLICY "Schule sieht eigene Schüler"
    ON public.user_profiles FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.user_profiles instructor
            WHERE instructor.id = auth.uid()
              AND instructor.role IN ('INSTRUCTOR', 'SCHOOL_ADMIN')
              AND instructor.school_id = user_profiles.school_id
        )
    );

-- ---- Flights: Schüler sehen nur eigene Flüge
CREATE POLICY "Nutzer sieht eigene Flüge"
    ON public.flights FOR ALL
    USING (auth.uid() = user_id);

-- Instruktoren/Admins sehen Flüge aller Schüler ihrer Schule
CREATE POLICY "Schule sieht Schüler-Flüge"
    ON public.flights FOR SELECT
    USING (
        EXISTS (
            SELECT 1
            FROM public.user_profiles instructor
            JOIN public.user_profiles student
              ON student.school_id = instructor.school_id
            WHERE instructor.id    = auth.uid()
              AND instructor.role  IN ('INSTRUCTOR', 'SCHOOL_ADMIN')
              AND student.id       = flights.user_id
        )
    );

-- ============================================================
-- Hilfsfunktion: Profil beim Registrieren anlegen
-- Wird als Trigger auf auth.users aufgerufen
-- ============================================================
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.user_profiles (id, full_name, license_number, role)
    VALUES (
        NEW.id,
        COALESCE(NEW.raw_user_meta_data->>'full_name', ''),
        COALESCE(NEW.raw_user_meta_data->>'license_number', ''),
        COALESCE(NEW.raw_user_meta_data->>'role', 'STUDENT')
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();
