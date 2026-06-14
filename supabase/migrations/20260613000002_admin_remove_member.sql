-- ============================================================
-- School-Admin darf Mitglieder aus seiner Flugschule entfernen
--
-- Eine reine UPDATE-Policy auf user_profiles (school_id -> NULL)
-- erwies sich als unzuverlässig (WITH-CHECK schlug trotz erfüllter
-- Bedingungen fehl, sobald die neue school_id NULL ist und mehrere
-- permissive Policies zusammenspielen).
--
-- Stattdessen: eine SECURITY-DEFINER-RPC. Sie läuft mit Owner-Rechten,
-- umgeht damit RLS und erzwingt die Berechtigung explizit im Code:
--   * Aufrufer muss SCHOOL_ADMIN sein
--   * Ziel muss Mitglied DERSELBEN Schule sein
--   * der Admin kann sich nicht selbst entfernen
-- Die Flüge des entfernten Mitglieds bleiben in dessen Flugbuch.
-- ============================================================

-- Frühere (fragile) Policy entfernen, falls vorhanden
DROP POLICY IF EXISTS "School-Admin darf Mitglieder verwalten" ON public.user_profiles;

CREATE OR REPLACE FUNCTION public.remove_school_member(target_user UUID)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    admin_school UUID;
BEGIN
    IF public.current_user_role() <> 'SCHOOL_ADMIN' THEN
        RAISE EXCEPTION 'Nur ein School-Admin darf Mitglieder entfernen';
    END IF;

    admin_school := public.current_user_school_id();
    IF admin_school IS NULL THEN
        RAISE EXCEPTION 'Admin ist keiner Schule zugeordnet';
    END IF;

    UPDATE public.user_profiles
       SET school_id = NULL
     WHERE id = target_user
       AND id <> auth.uid()            -- nicht sich selbst
       AND school_id = admin_school;   -- nur eigene Schule
END;
$$;

-- Eingeloggte Nutzer dürfen die Funktion aufrufen; die Berechtigung
-- wird intern geprüft (siehe oben).
GRANT EXECUTE ON FUNCTION public.remove_school_member(UUID) TO authenticated;
