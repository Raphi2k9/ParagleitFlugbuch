---
name: Paragleit Flugbuch Android App
description: Android Kotlin/Compose + KMP App für Paragliding-Logbuch – mit Supabase Auth und Cloud-Sync
type: project
---

Vollständige Android-App wurde erstellt (März 2026), Supabase + KMP-Basis hinzugefügt März 2026.

**Why:** Nutzer wollte Login/Registrierung, Cloud-Datenbank (PostgreSQL via Supabase), und Flugschul-Dashboard. iOS-Support war wichtig → KMP-Struktur gewählt.

**Architektur:**
- Package: `com.example.flugbuch`
- Module: `:app` (Android) + `:shared` (KMP – Android + iOS)
- AGP 8.13.2 / Kotlin 2.1.0 / Supabase SDK 3.0.0 / Ktor 3.0.3
- Compose BOM 2025.01.00, Room 2.7.0 (version 4), Navigation 2.8.5

**Datenbank:**
- Supabase (PostgreSQL) als primärer Speicher → `supabase/migrations/`
- Room als lokaler Offline-Cache
- Row Level Security: Schüler sehen nur eigene Flüge; Instruktoren/Admins sehen Flüge ihrer Schulschüler

**Auth:**
- Supabase Auth (E-Mail/Passwort)
- Rollen: STUDENT, INSTRUCTOR, SCHOOL_ADMIN
- Schule-Beitritt via Invite-Code (8-stellig, auto-generiert)
- Beim Login: Flüge von Supabase in Room synchronisiert
- Beim Logout: lokale Flüge des Nutzers gelöscht

**Neue Screens:**
- LoginScreen, RegisterScreen, JoinSchoolScreen, CreateSchoolScreen, SchoolDashboardScreen

**Neue ViewModels:**
- AuthViewModel (login/register/logout/joinSchool)
- SchoolViewModel (loadSchoolData/createSchool)
- FlightViewModel erweitert: Supabase-Sync bei insert/update/delete, currentUserId

**Neue Felder in FlightEntity (Room v4):**
- `userId: String` – Supabase User-UUID
- `supabaseId: String?` – UUID in Supabase (null = noch nicht synchronisiert)

**Supabase lokal testen:**
- `supabase start` → Studio auf http://localhost:54323
- Android Emulator: URL = http://10.0.2.2:54321
- URL/Key in `shared/src/commonMain/kotlin/com/example/flugbuch/SupabaseConfig.kt` anpassen

**How to apply:** Beim Erweitern: neue Screens in NavGraph + Routes eintragen. Neue Supabase-Tabellen in `supabase/migrations/` als SQL-Migration anlegen. iOS-App kann durch Erstellen eines `iosApp/`-Ordners mit Xcode-Projekt ergänzt werden.
