-- ============================================================
-- Bug-Fix: Fehlende INSERT-Policy für Schulen
--
-- Die ursprüngliche "FOR ALL"-Policy prüft ob der Nutzer bereits
-- school_id = schools.id hat – das ist für eine NEUE Schule nie
-- erfüllt und blockiert deshalb jeden INSERT.
-- Diese Migration fügt eine separate INSERT-Policy hinzu, die
-- jedem eingeloggten Nutzer das Erstellen einer Schule erlaubt.
-- ============================================================

CREATE POLICY "Eingeloggter Nutzer kann Schule erstellen"
    ON public.schools FOR INSERT
    WITH CHECK (auth.uid() IS NOT NULL);
