# Agent Hard Rules (CS360 / CMPUT301 — Project)

This file defines **non-negotiable rules** for any AI agent (or human) making suggestions, generating code snippets, or proposing repo changes for this project.  
If a request conflicts with these rules, the agent must **stop** and ask for clarification or propose an alternative that complies.

---

## 0) Priority Order (Conflict Resolution)

When rules conflict, follow this order:

1. **Course / project spec requirements** (CMPUT301 / CS360 project handout and TA instructions)
2. **These hard rules** in this file
3. **Repository conventions** (existing architecture, naming, patterns)
4. **Personal/team preferences**

If a user request violates any rule above, the agent must:
- Explain *which rule is violated*, and
- Offer the closest compliant alternative, and/or ask permission to proceed differently.

---

## 1) Tooling + Environment (Android Studio Compatibility)

- The project **must build and run in Android Studio** without unusual setup.
- Use **Gradle (Android)** standard structure (e.g., `app/`, `build.gradle`, `settings.gradle`).
- Do **not** require custom IDEs, custom build systems, or non-standard scripts to run the app.
- Do not introduce dependencies that require manual installation outside Gradle unless explicitly approved.

**Hard requirement:** The repository should remain **self-contained** for marking and TA builds.

---

## 2) Language Rules: Java Only

- **All app source code must be Java.**
- **Do not add Kotlin** files or Kotlin-only libraries.
- Do not convert Java classes to Kotlin.
- If a library strongly nudges Kotlin usage, propose a Java-friendly alternative.

---

## 3) UI Rules: XML Layouts Only (No Compose)

- UI must use **XML layouts** (`res/layout/*.xml`) and standard Android Views.
- **Do not use Jetpack Compose**.
- Avoid Compose dependencies and Compose-specific architecture.

Allowed:
- ViewBinding (if already enabled/approved)
- RecyclerView, Fragments/Activities, Material Components (Java usage)
- Standard Android UI widgets

If asked to implement UI, provide:
- XML layout(s)
- Java Activity/Fragment code
- Any required adapters (RecyclerView) in Java

---

## 4) Backend Rules: Firebase / Firestore

- Backend is **Firebase**, with **Cloud Firestore** as the database.
- Do not introduce a different backend (REST API server, custom DB, etc.) unless explicitly approved by the course staff.

Hard rule:
- **Ask before changing Firestore data model / collection structure** (see Section 10).

Allowed:
- Firebase Authentication (if permitted by the project; ask if uncertain)
- Firebase Storage (only if needed and approved)
- Offline persistence / caching as long as it remains simple and documented

---

## 5) Git Safety Rules (Do Not Perform Repo Writes)

This agent must **never** perform Git operations or repo write actions.

- **Never push**
- **Never commit**
- **Never open PRs**
- **Never modify Git remotes**
- **Never force-push**
- **Never rewrite history**
- **Never run destructive cleanup commands**
- **Never edit `.git/` metadata**

If the user wants changes applied:
- The agent may **suggest** changes and provide code snippets,
- but must not execute write operations through automation tools.

---

## 6) Deletion & Destructive Changes

- **Never delete documentation** (`docs/`, `README`, handouts, rubrics, diagrams, deliverable artifacts) unless explicitly told.
- Avoid deleting code files unless explicitly instructed.
- Prefer incremental changes over rewrites.

If removal is requested:
- Confirm the exact files to delete and why,
- and propose an alternative (deprecate, comment, or archive) when appropriate.

---

## 7) Code Style & Readability (Beginner-Friendly Java)

This project is graded in an educational context.

Hard rules:
- Keep code **modular** and **beginner-readable**.
- Prefer clarity over cleverness.
- Avoid complex meta-programming, heavy reflection, or over-abstract frameworks.

Guidelines:
- Use clear class/method names.
- Keep methods short and single-purpose.
- Add comments where intent isn’t obvious.
- Prefer explicit types over overly clever generics.

Avoid:
- Over-engineered architecture that the team cannot explain in a demo.
- Advanced patterns introduced “because it’s best practice” without clear value.

---

## 8) Testing Requirements (Runnable + Realistic)

- Provide **runnable tests** for model and control classes.
- Provide **instrumentation/intent tests** for implemented requirements (as applicable).
- Test data should be **realistic** (not empty strings everywhere, not trivial toy objects only).
- Tests must be deterministic: avoid relying on live network unless explicitly designed (use fakes/mocks where practical).

If a feature uses Firestore:
- Prefer repository/service abstraction to allow unit testing without real Firebase calls.
- If mocking Firebase is complex, document the testing strategy and include at least basic integration/instrumentation coverage.

---

## 9) Documentation Rules (Per-File Intro + Javadoc)

Hard requirements:
- Each source file should have a **brief introductory comment** describing:
  - Purpose/role in the app
  - Relevant design pattern (if any)
  - Known issues / TODOs (if any)

- Model classes must have **Javadoc** for:
  - The class itself
  - Public methods (at least)
  - Key fields when helpful

Rule:
- Do not add large blocks of useless boilerplate comments; comments must be meaningful.

---

## 10) Firestore Schema Change Policy (Ask First)

Because schema changes can break the app and tests:

- **Ask before changing Firestore collections/documents/field names/types.**
- **Ask before changing security rules structure** or introducing a new rules model.
- If schema changes are necessary:
  - Propose a migration plan (even if manual)
  - Update documentation
  - Update tests (or at least note impacts)
  - Keep backward compatibility where possible

Minimum required when proposing schema:
- Collections used
- Document IDs strategy
- Required vs optional fields
- Index needs (if any)
- Example documents

---

## 11) Consistency Across Deliverables

The repository must remain consistent across:
- Product backlog vs implemented features
- UI mockups/storyboards vs screens
- UML diagram vs code structure
- Tests vs features delivered
- Sprint planning notes vs issues/PR history (even if not graded directly, it’s a stated requirement)

If inconsistencies are discovered:
- The agent should point them out explicitly and propose a fix.

---

## 12) Scrum / Planning Artifacts Expectations (GitHub Use)

- Use GitHub regularly and consistently (issues, milestones/labels if used).
- Sprint planning/review records should be maintained and kept current.
- Agent may suggest:
  - Issue templates
  - Label sets
  - Sprint board conventions

But per Section 5:
- The agent must not actually create/modify issues or boards unless the user explicitly wants instructions and performs the actions themselves.

---

## 13) Dependency Management Rules

- Prefer stable, commonly used Android libraries that work well with Java.
- Avoid adding many new dependencies “just because”.
- Any new dependency should have:
  - A clear reason tied to a requirement
  - A minimal footprint
  - No Kotlin requirement

Do not add:
- Experimental libraries
- Unmaintained libraries
- Libraries that require complex setup for TAs

---

## 14) Security, Privacy, and Secrets

Hard rules:
- **Never commit secrets** (API keys, service account JSON, private tokens).
- Do not place sensitive credentials in the repo.
- Use `google-services.json` as required for Firebase setup, but confirm course/team policy for including it.
- Avoid logging personal data.

If authentication or user profiles exist:
- Store only the minimum necessary data.
- Follow Firebase best practices and least privilege.

---

## 15) Feature Scope Rule (Half-Way Checkpoint)

For “prototype / half requirements” stage:
- Focus on delivering a **working, demoable** subset.
- Connectivity to Firebase must be present (at least for a core flow).
- Avoid expanding scope beyond what can be tested and explained.

If a request is large:
- Break it into smaller milestones aligned with backlog and sprints.

---

## 16) When the Agent Must Ask Questions

The agent must ask before:
- Changing Firestore schema or rules (Section 10)
- Introducing major architectural shifts (e.g., switching Activities → Fragments everywhere, adding a new framework)
- Adding many dependencies
- Deleting or moving docs
- Converting anything to Kotlin or Compose (usually disallowed anyway)

---

## 17) Output / Patch Format Rules (In Chat)

When providing code in chat:
- Provide **complete files** when feasible for copy/paste.
- Otherwise provide clear snippets with file names and insertion points.
- Mention any required Gradle changes explicitly (but do not perform them).
- Include brief “How to verify” steps (build, run, test) that work in Android Studio.

---

## 18) Non-Goals

The agent should not:
- Optimize prematurely
- Over-engineer
- Introduce advanced patterns the team cannot explain in a TA demo
- Replace large parts of the codebase without explicit permission

---

## 19) Quick Checklist Before Suggesting a Change

Before proposing any change, the agent must verify:

- [ ] Java-only (no Kotlin)
- [ ] XML UI (no Compose)
- [ ] Android Studio compatible
- [ ] Firebase/Firestore compatible
- [ ] Beginner-readable and modular
- [ ] No destructive Git actions performed by the agent
- [ ] Documentation expectations met (file intro + Javadoc for models)
- [ ] Testing impact considered
- [ ] Firestore schema/rules change? If yes → ask first

---

## 20) Acknowledgement

By using an agent on this repository, we agree the agent will follow these rules and will stop to ask for clarification when uncertain or when a request conflicts with these constraints.