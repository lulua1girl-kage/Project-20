# P-Exceptional 1.0.0 — Local-first release candidate

Date: 2026-11-28

The complete runnable package is delivered as the conversation artifact **P-Exceptional-Release-1.0.0.zip**. It contains a responsive PWA, browser-local task data, focus timer, daily capacity planner, challenge publishing limited to the local device, completion certificate SVG/print, backup/restore, privacy settings, offline shell, and a separate loopback-only Python/SQLite API.

## QA
- Python API tests: 7/7 passed
- Planning tests: 4/4 passed
- JavaScript syntax, HTML parsing, 100 principles, and ZIP integrity: passed

## Important boundary
This is a local-use release candidate, not the complete original multi-user vision. It does not include hosted account registration, sync, a central admin console, remote challenge publishing, native Android/iOS/desktop binaries, guaranteed background notifications, or independent security certification. Browser localStorage and SQLite API are separate and not synchronized.

The current branch contains the incremental implementation and specifications. The conversation ZIP is the cohesive runnable release package.
