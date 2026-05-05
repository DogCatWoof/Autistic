# Web Interface Requirements

Companion web view for the Autistic Android app. Data lives in Firestore and is
read/written through the same collections the Android app syncs with. Auth via
Firebase (same account as the app).

---

## Tech Stack

### Core
- **Vite + React + TypeScript** — client-side SPA (no SSR needed; everything behind Firebase Auth)
- **Firebase JS SDK v9** (modular) — Firestore + Auth; same collections as the Android app
- **Tailwind CSS + shadcn/ui** — table, collapsible, dialog, form components
- **Firebase Hosting** — deploy target; free tier sufficient for personal use

### Supporting libraries
- **React Router v7** — client-side routing for the 7 sections
- **TanStack Query** — caching and loading states for Firestore reads (or `onSnapshot` listeners directly)
- **date-fns** — timestamp formatting (`loggedAt`, `dueAt`, `createdAt`, `completedAt`, etc.)
- **Recharts** — mood summary chart (section 3.2)
- **dnd-kit** — drag-to-reorder sequence steps (section 5.3)
- **react-hook-form + zod** — food log and sequence forms

### Tooling
- **ESLint + Prettier** — linting and formatting
- **Vitest + React Testing Library** — unit and component tests
- **vite-plugin-pwa** — installable PWA (natural fit as a mobile companion)

---

## Access levels

| Section              | Web access  |
|----------------------|-------------|
| Tasks (today's list) | Read-only   |
| Daily task templates | Read-only   |
| Mood history         | Read-only   |
| Vitals / Health      | Read-only   |
| Notes                | Read / write |
| Food log             | Read / write |
| Sequences            | Read / write |
| Scanned products     | Read-only   |
| Food cache           | Read-only   |

---

## 1. Tasks

### 1.1 Today's task list (read-only)

Display all tasks with `completedAt IS NULL` for today.

**Fields shown per task:**
- Title (`task`)
- Category (`category`)
- Due time — formatted as clock time if `dueAt` is set, otherwise "all day"
- Expected duration (`expectedTimeMinutes`) — shown as e.g. "30 min" if set
- Important flag (`isImportant`) — visual indicator (e.g. amber star)
- Notes (`notes`) — shown collapsed/expandable
- Origin — badge "Daily" when `dailyTaskId IS NOT NULL`, otherwise none

**Grouping / ordering:**
- Group by category, alphabetical category order
- Within a group: important tasks first, then by `dueAt` ascending, then undated

**Completed tasks:**
- Separate collapsible section showing tasks completed today (`completedAt` is today's date)

### 1.2 Daily task templates (read-only)

List all rows from `daily_tasks`, sorted alphabetically by title.

**Fields shown:**
- Title
- Category
- Scheduled time (`timeMinutes`) — displayed as clock time (e.g. "8:30 AM"), or "any time" if null
- Expected duration (`expectedTimeMinutes`)
- Required (`isRequired`) — visual badge

---

## 2. Notes (read/write)

### 2.1 Note list

Display all notes where `isDeleted = false`, ordered by `updatedAt` descending.

**Fields shown per row:**
- Display title — `title` if non-blank, otherwise first non-blank line of `content`, otherwise "(empty)"
- Last updated timestamp

**Filtering / search:** free-text search across title and content.

### 2.2 Note detail / editor

Clicking a note opens it for reading. Switching to edit mode allows:
- Edit `title`
- Edit `content` (multi-line plain text; preserve line breaks)
- Save — updates `updatedAt` to now, sets `pendingFirestoreSync = true`
- Delete — soft-delete: sets `isDeleted = true`, `updatedAt = now`

### 2.3 Create note

- Button to create a new note
- Fields: optional title, body content
- On save: `createdAt = updatedAt = now`, `pendingFirestoreSync = true`

---

## 3. Mood (read-only)

### 3.1 Mood history list

All rows from `moods` where `isDeleted = false`, ordered by `createdAt` descending.

**Fields shown per row:**
- Emoji and label (one of 14 options — Happy 😊, Sad 😢, Angry 😠, Calm 😌, Tired 😴, Bored 😑, Lonely 🥺, Playful 😜, Content 😇, Loved 🥰, Sick 🤒, Stressed 😓, Anxious 😰, Overwhelmed 🤯)
- Activity / note text (`activity`, `notes`) — shown if non-empty
- Timestamp (`createdAt`)

### 3.2 Mood summary (optional panel)

- Count per emoji for the past 7 days and 30 days
- Timeline chart showing entries per day (bar or dot chart)

---

## 4. Vitals (read-only)

Combines daily health snapshots from Health Connect with daily food-log totals.
Displayed as a reverse-chronological list (most recent date first).

### 4.1 Health snapshot per day

Source: `health_snapshots` table, keyed by date (`yyyy-MM-dd`).

**Fields shown:**
- Steps (`steps`) — with a step-count icon
- Sleep (`sleepMinutes`) — displayed as hours and minutes (e.g. "7h 23m")
- Avg heart rate (`avgHeartRateBpm`) — in bpm
- Weight (`weightKg`) — in kg
- Calories burned (`caloriesBurned`) — in kcal
- Blood glucose (`bloodGlucoseMmol`) — in mmol/L

Any null field is hidden rather than shown as zero.

### 4.2 Food log totals per day

Source: derived from `food_log_items` grouped by `date`.

**Macro totals shown:**
- Calories (kcal)
- Protein (g)
- Total fat (g)
- Net carbs (g) — `totalCarbs − fiber − sugarAlcohols`, floored at 0
- Fiber (g)
- Total sugars (g)
- Added sugars (g)
- Sugar alcohols (g)

### 4.3 Food log items per day (expandable)

Expand a day to see individual `food_log_items` rows, ordered by `loggedAt` ascending.

**Fields per item:**
- Description (`description`)
- Logged time (`loggedAt`)
- Per-item macro values (same set as 4.2)
- AI analysis result (`aiAnalysisResult`) — shown collapsed if non-null
- Pending AI badge (`isAiPending`) — shown when AI analysis not yet returned

### 4.4 Add / edit food log item (write)

From the expanded day view:

**Create:**
- Description (text)
- Log time (defaults to now)
- Nutrition fields: calories, protein, total fat, total carbs, fiber, total sugars, added sugars, sugar alcohols
- On save: `loggedAt = now`, `lastModifiedAt = now`, `pendingFirestoreSync = true`

**Edit:**
- All fields from create, pre-filled
- On save: `lastModifiedAt = now`, `pendingFirestoreSync = true`

**Delete:**
- Soft-delete: sets `isDeleted = true`, `lastModifiedAt = now`, `pendingFirestoreSync = true`

---

## 5. Sequences (read/write)

### 5.1 Sequence list

All sequences where `isDeleted = false`, sorted alphabetically by `name`.

**Fields per row:**
- Name
- Step count (derived from `sequence_steps` count for that sequence)
- Active run indicator — badge if there is a run with `completedAt IS NULL` for this sequence

### 5.2 Sequence detail

Clicking a sequence shows its ordered steps.

**Fields per step:**
- Position number
- Instruction text
- Estimated duration (`estimatedMinutes`) — shown if set

### 5.3 Create / edit sequence (write)

**Create sequence:**
- Name field
- Add steps inline: instruction text, optional estimated minutes, drag-to-reorder

**Edit sequence:**
- Rename
- Add, edit, delete, reorder steps
- On save: `lastModifiedAt = now`, `pendingFirestoreSync = true` for the sequence and any changed steps

**Delete sequence:**
- Soft-delete: sets `isDeleted = true`, `lastModifiedAt = now`, `pendingFirestoreSync = true`
- Cascades soft-delete to all steps of that sequence

### 5.4 Run history (read-only)

For each sequence, a collapsible section showing past runs from `sequence_runs`.

**Fields per run:**
- Started at (`startedAt`)
- Completed at (`completedAt`) — or "In progress" if null
- Duration — `completedAt − startedAt` if both present
- Steps completed — count of `sequence_step_progress` rows for that run

---

## 6. Scanned products (read-only)

Source: `products` table (barcode scan cache from Open Food Facts / USDA).

### 6.1 Product list

All cached products, sorted alphabetically by name.

**Fields per row:**
- Barcode (EAN/UPC)
- Product name (`name`)
- Brand (`brands`)
- Serving size / quantity (`quantity`)

### 6.2 Product detail

Clicking a product shows full detail:

- All label fields: barcode, name, brands, quantity, servings per container (`servingsPerContainer`), ingredients, food group (`foodGroups`)
- Nutrition label from `nutriments` (per serving):
  - Energy (kcal)
  - Total fat, saturated fat, trans fat, polyunsaturated fat, monounsaturated fat
  - Cholesterol, sodium
  - Total carbohydrates, dietary fiber, total sugars
  - Protein
  - Vitamins & minerals: A, D, calcium, iron, potassium, magnesium, phosphorus, riboflavin, folate, B12
  - Daily value % shown alongside each value where available

---

## 7. Food cache (read-only)

Source: `food_cache` table — AI-analysed food entries accepted by the user.
Each entry stores per-serving nutrition for a named food.

### 7.1 Cache list

All rows, sorted alphabetically by `description`.

**Fields per row:**
- Food name (`description`)
- Calories
- Key macros: protein (g), total fat (g), total carbs (g), fiber (g)

### 7.2 Cache detail

Clicking an entry shows all stored nutrition values:
- Calories
- Protein (g)
- Total fat (g)
- Total carbs (g)
- Fiber (g)
- Total sugars (g)
- Added sugars (g)
- Sugar alcohols (g)
- Net carbs (g) — computed: `totalCarbs − fiber − sugarAlcohols`, floored at 0

---

## Data sync notes

- All writes set `pendingFirestoreSync = true` and update `lastModifiedAt` (or `updatedAt` for notes).
- The Android app is the sync authority for Health Connect data — the web never writes to `health_snapshots`.
- The web does not trigger barcode scans or AI photo analysis — `products` and `food_cache` are populated by the Android app only.
- Soft-deleted records (`isDeleted = true`) are never shown in the web UI.
- Food log items older than 14 days are purged by the Android app's daily reset worker; the web should reflect whatever Firestore contains.
