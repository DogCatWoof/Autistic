# Manual Test Plan — App → Firestore Sync

## Prerequisites
- Firebase console open at the project's Firestore database
- App signed in with a Google account (so `uid` is available)
- Network connected
- Know how to trigger sync: use the manual sync button in Sync Settings, or wait for the periodic worker

---

## Test Cases

### 1. Tasks
| Step | Action | Expected in Firestore |
|---|---|---|
| Create | Add a new task in the app | Document appears in `tasks/{uid}/items/` with correct fields |
| Update | Edit the task title or due date | Document updated with new values |
| Complete | Mark the task complete | `completed` field set to `true` |
| Delete | Delete the task | Document removed from Firestore |

### 2. Notes
| Step | Action | Expected in Firestore |
|---|---|---|
| Create | Create a new note | Document appears in `notes/{uid}/items/` |
| Update | Edit note body | Document body field updated |
| Delete | Delete the note | Document removed |

### 3. Moods
| Step | Action | Expected in Firestore |
|---|---|---|
| Log | Log a mood entry | Document appears in `moods/{uid}/items/` with timestamp and rating |
| Delete | Delete a mood entry | Document removed |

### 4. Food Log
| Step | Action | Expected in Firestore |
|---|---|---|
| Log | Log a food item | Document appears in `food/{uid}/items/` |
| Delete | Delete a food log entry | Document removed |

### 5. Sequences
| Step | Action | Expected in Firestore |
|---|---|---|
| Create | Create a sequence | Sequence document in `sequences/{uid}/items/`, steps in sub-collection |
| Update | Add/remove/reorder a step | Steps sub-collection updated |
| Run | Complete a sequence run | Run document appears in `sequenceRuns/{uid}/items/` |
| Delete | Delete a sequence | Sequence and steps removed |

### 6. Daily Tasks
| Step | Action | Expected in Firestore |
|---|---|---|
| Create | Add a daily task | Document in `dailyTasks/{uid}/items/` |
| Update | Edit it | Document updated |
| Delete | Remove it | Document removed |

---

## Sync Trigger Verification
Before each check in Firestore:
1. Tap the manual sync button in Sync Settings
2. Wait for sync to complete (check last-sync timestamp updates)
3. Refresh Firestore console

---

## Edge Cases
| Scenario | Steps | Expected |
|---|---|---|
| Offline create | Turn off network → create item → reconnect → sync | Item appears in Firestore after sync |
| Multiple changes before sync | Make 3 edits without syncing → sync once | All 3 changes reflected in Firestore |
| Delete unsynced item | Create item → delete before syncing → sync | Item does not appear in Firestore |
