-- ============================================================
-- Fluglehrer-Unterschrift auf Schüler-Flügen
--
-- Bisher gab es nur ein anonymes Boolean (instructor_approved).
-- Eine echte Unterschrift hält zusätzlich fest, WELCHER Fluglehrer
-- WANN signiert hat. Der Name wird denormalisiert mitgespeichert,
-- damit der Schüler ihn anzeigen kann, ohne das Lehrer-Profil lesen
-- zu müssen (das verbietet die RLS für Schüler).
-- ============================================================

ALTER TABLE public.flights
    ADD COLUMN IF NOT EXISTS approved_by      UUID REFERENCES auth.users(id) ON DELETE SET NULL;

ALTER TABLE public.flights
    ADD COLUMN IF NOT EXISTS approved_by_name TEXT;

ALTER TABLE public.flights
    ADD COLUMN IF NOT EXISTS approved_at      TIMESTAMPTZ;
