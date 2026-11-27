# P-Exceptional — competitor benchmark

Checked: 2026-11-27. Sources are official product pages/help centers where available. This is a feature benchmark, not hands-on parity testing.

## Five reference products

| Product | Strong pattern observed | P-Exceptional implication |
|---|---|---|
| Super Productivity | Offline/local data, task planning, timeboxing, time tracking, focus tools, history/export, multi-platform | Use as primary open-source architecture and daily workflow reference; don't copy code without license/security review |
| Todoist | Quick capture, projects/sections, recurring dates, reminders, filters, calendar/board/table views | Prioritize low-friction capture, task recurrence, saved views and useful reminders |
| TickTick | Lists/tags, flexible reminders, recurring rules, habits, Pomodoro, matrix, calendar, statistics, themes | Integrate task planning, focus and habit review without forcing all modules onto the home screen |
| Notion | Databases, nested pages, calendar/timeline views, relations, templates, permissions and broad workspace features | Provide linked projects/notes and configurable views later; avoid a heavy block editor in the first release |
| Motion | Auto-scheduling tasks by priority/deadline/duration into available calendar time; re-plans when commitments change | Build a transparent capacity-aware planner that proposes schedules and explains conflicts; never silently overbook |

## Product strategy

P-Exceptional should not be a clone of one app. Its differentiator is a **personal execution system**:
1. capture quickly;
2. plan against actual available time and energy;
3. prioritize deadlines and dependencies;
4. focus on one next action;
5. review without shame and adjust;
6. preserve local ownership and export.

## Proposed five major targets

1. **Reliable daily execution:** task/project/subtask, recurring tasks, priorities, estimates, due dates, calendar/timebox, reschedule flow.
2. **Adaptive planning:** capacity-aware schedule, deadline risk, workload warning, flexible plan; user approval for changes.
3. **Focus and recovery:** task-linked timer, breaks, distraction notes, stop/recovery prompts, session history.
4. **Privacy-first ownership:** local-first, offline mode, export/import, no third-party trackers, clear data inventory and optional account/sync.
5. **Challenge and recognition:** owner-published challenges/messages, user opt-in participation, completion rules, shareable certificate with unique verification ID; no public ranking by default.

## Engagement guardrails

Use progress clarity, personal milestones, useful feedback, choice and meaningful rewards. Do not use streak-loss pressure, shame, deceptive scarcity, random reward mechanics, or endless notifications. All notifications are opt-in and configurable. A completed day is not defined by exhausting the user; rest and Human Override are valid.

## Feature priority

### P0 — reliability
Local database, task CRUD, projects/subtasks, due dates, recurrence, calendar, reminders, export/import, accessibility, data deletion, crash-safe migrations, backup/restore.

### P1 — daily value
Timeboxing, task estimates, focus timer, daily/weekly review, workload/capacity planner, smart filters, offline queue, conflict resolution.

### P2 — differentiation
Transparent schedule suggestions, deadline-risk explanation, user-defined challenges, certificates, owner message publishing, local insights.

### P3 — multi-user/hosted
Account system, sync, collaboration, role-based owner/admin console, audit logs, abuse controls, support tools, platform-native apps.

## Source links
- Super Productivity: https://super-productivity.com/ and https://github.com/super-productivity/super-productivity
- Todoist feature guide: https://www.todoist.com/help/todoist/features
- TickTick features: https://www.ticktick.com/features
- Notion feature catalog: https://www.notion.com/product/all-features
- Motion auto-scheduling: https://www.usemotion.com/help
- Offline-first architecture: https://developer.android.com/topic/architecture/data-layer/offline-first

## Limitations
Features above were reviewed from public product pages/help docs. No exhaustive user testing, pricing audit, or claim of 200-site coverage is made. A complete competitor review needs repeatable scenario testing and source-by-source notes.
