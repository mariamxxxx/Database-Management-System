# DBMS2 — Java DBMS

DBMS2 solves the problem of storing and querying structured data without relying on a full external database. It provides a small, understandable DBMS core that can be embedded into experiments, demos, or learning scenarios where you want to control the storage engine behavior end-to-end.

Why it is good

- Focused scope: implements the essentials without heavy infrastructure.
- Clear structure: source is organized around tables, pages, and storage helpers.
- Practical foundation: file-backed persistence makes it easy to inspect and debug.

What you can do with it

- Create tables and manage pages of records.
- Persist data to disk and reload it reliably.
- Experiment with indexing and storage strategies in a small codebase.

Repository layout

- src/ — Java source files (primary code under src/DBMS).
- lib/ — Third-party libraries and JAR dependencies.
- bin/ — Compiled classes (generated locally).

Requirements

- Java Development Kit (JDK) 11 or newer.
