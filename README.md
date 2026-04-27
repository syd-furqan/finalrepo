# Project 4: Campus Gate Access System
**Team GLITCH** | CS360 Software Engineering (LUMS)

## Overview
The Campus Gate Access System is a security management solution designed to provide a centralized digital platform for monitoring and authorizing campus entry and exit. This project replaces manual gatekeeping protocols with a synchronized system to improve security integrity and operational efficiency. 

The application facilitates secure communication between university residents (Faculty, Staff, and Students) and security personnel to ensure that all individuals on campus are verified and logged in real-time.



---

## Core Features
* **Guard Dashboard:** A real-time interface for security personnel to search visitor records and verify entry credentials.
* **Visitor Request Portal:** A web-based submission system for faculty and staff to pre-authorize guests.
* **Real-Time Credential Verification:** A mission-critical module that validates visitor data against central university databases.
* **Entry and Exit Logging:** An automated system that maintains a high-integrity audit trail with precise timestamps for every event.
* **Student Guest Pass System:** A specialized module for students to generate and manage time-bound, single-use access passes.
* **Security Auditing:** Tools for administrators to review system activity, export logs for compliance, and receive automated alerts for security breaches.

---

## Primary User Roles
* **Security Guards:** Execute operational gate control, visitor lookup, and real-time logging.
* **Faculty and Staff:** Act as sponsors by submitting and managing visitor authorization requests.
* **Students:** Utilize the system to issue guest passes and manage access for personal visitors.
* **Security Administrators:** Manage user permissions, system configurations, and conduct comprehensive security audits.

---

## Part 2 Deliverables

| # | Deliverable | Link |
|---|-------------|------|
| 1 | **Product Backlog** — User stories with story points, risk levels, and release planning | [wiki/backlog.md](wiki/backlog.md) |
| 2 | **UI Mockups & Storyboard Sequences** — Interface layouts, storyboard flows, and requirement traceability | [wiki/storyboards.md](wiki/storyboards.md) |
| 3 | **Object-Oriented Analysis (CRC Cards)** — Class-Responsibility-Collaborator cards for core system classes | [wiki/oo-analysis.md](wiki/oo-analysis.md) |
| 4 | **GitHub Issues & Task Tracking** — All user stories tracked as issues with assignees and labels | [Issues](../../issues) |
| 5 | **Meeting Minutes** | [wiki/meeting-minutes.md](wiki/meeting-minutes.md) |

---

## Part 3 Deliverables (Half-Way Checkpoint)

| # | Deliverable | Where to find it |
|---|-------------|------------------|
| 1 | **Addressing Feedback** | Reflected through updated docs/code/tests in this repository (see links below). |
| 2 | **Code Base of Prototype** | Android project is at the repository root; app module is in [`app/`](app/). |
| 3 | **Code Documentation** | Intro comments across source files; Javadoc for model classes in [`app/src/main/java/com/example/glitch/model/`](app/src/main/java/com/example/glitch/model/). |
| 4 | **Test Cases** | See [`doc/testing.md`](doc/testing.md). Unit tests: `app/src/test/java`. Instrumented tests: `app/src/androidTest/java`. |
| 5 | **Object-Oriented Design (UML)** | UML artifacts in [`doc/uml/`](doc/uml/) (rendered diagram: [`doc/uml/image.png`](doc/uml/image.png)). |
| 6 | **Product Backlog Updated** | [`wiki/backlog.md`](wiki/backlog.md) (stories and checkpoint status). |
| 7 | **UI Mockups & Storyboards Updated** | Storyboard write-up: [`wiki/storyboards.md`](wiki/storyboards.md). Self-contained UI artifacts: [`doc/ui/`](doc/ui/). |
| 8 | **Sprint Planning and Reviews** | [`wiki/sprints.md`](wiki/sprints.md). |
| 9 | **Demonstration** | Demo performed in lab with TA mentor (team-attended). |
| 10 | **Tool Use (GitHub)** | Work tracked via commits/issues/projects in this repo. |

### Known limitations
See [`doc/limitations.md`](doc/limitations.md).

---

## Build & Test

Run commands from the repository root:

```bash
./gradlew :app:assembleDebug
```

```bash
./gradlew :app:testDebugUnitTest
```

```bash
./gradlew connectedAndroidTest
```

For additional notes, see [`doc/testing.md`](doc/testing.md).

---

## Firebase/Firestore Setup Notes (Checkpoint)
- This repository includes `app/google-services.json` for Firebase configuration.
- Login requires:
  1) A Firebase Authentication user (email/password), and  
  2) A Firestore profile document at `users/{uid}` with `isActive=true` and a supported role (guard/faculty/staff/student/admin).

---

## Team GLITCH

| Member | Role | GitHub |
|--------|------|--------|
| Syed Furqan Abdullah (26100229) | Scrum Master | [@syd-furqan](https://github.com/syd-furqan) |
| Muhammad Aaffan Khan Niazi (26100015) | Requirement Lead | [@NiaziWtf](https://github.com/NiaziWtf) |
| Mohammad Ali Wajid (27100125) | UI Designer A (Mockups) | [@alooboii](https://github.com/alooboii) |
| Abyaan Shivjani (27100110) | UI Designer B (Storyboards) | [@abyssnn](https://github.com/abyssnn) |
| Afnan Ahmad Baig (27100206) | System Architect (CRC) | [@Afnan27100206](https://github.com/Afnan27100206) |

**Course:** CS360 Software Engineering, LUMS (2026)
