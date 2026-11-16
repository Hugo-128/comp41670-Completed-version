# Meitheal — a community time-bank

COMP41670 Group Software Project · UCD School of Computer Science · Trimester 1, 2026/27.

Meitheal (pronounced *meh-hal*) is a community time-bank: members offer skills to one another and settle
in **time credits** at a flat rate of 60 credits per hour. The problem is not pricing — it is keeping the
ledger honest. Credits are conserved, there is an overdraft floor of −180, both parties must confirm before
credits move, and a small levy is taken with a rounding rule we have to get right.

## Build and test

The project targets **Java 21** and builds with **Maven**. From a clean checkout:

```bash
mvn clean test          # compile + run the whole JUnit 5 suite
mvn -q package          # build the runnable jar (target/meitheal-1.0.0.jar)
```

There is no web UI, no database, and no framework: the deliverable is a correct, well-tested domain model
behind a command-line interface (see `meitheal.cli.Main`).

## Layout

```
meitheal/
  domain/                       pure Java — no I/O, no framework, no clock reads
    model/       Credits · Duration · FulfilmentWindow · MemberId · CategoryName · Email
    member/      Member (aggregate) · MemberStatus
    engagement/  Engagement (aggregate) · EngagementState
    request/     Request · Offer
    ledger/      Ledger (aggregate) · LedgerEntry · EntryType · Account
    fund/        CommunityFund (aggregate)
    catalogue/   SkillCatalogue (aggregate)
    policy/      LevyPolicy · CancellationPolicy          pure decision functions
  application/   MeithealService · MeithealRepository · CommandResult · Clock · SystemClock · InMemoryMeithealRepository
  cli/           Main — the command-line entry point
```

Dependencies point inward only. `domain` depends on nothing but the JDK; business rules never appear
outside `domain`; `application` orchestrates the aggregates; `cli` is a thin shell.

## Where each invariant lives

The invariant list is the first design deliverable. Each one is enforced in exactly one place and has a
named test that fails if the enforcement is removed. The mapping is documented in the Design Report
(§1, "Enforced at" column). In the code, look for the `// INV-n` markers.

## AI use

AI use is expected on this project (UCD AI Assessment Scale Level 4). Every significant use goes in the
team's AI Usage Log with its commit SHA. See the module's Project Brief, Section 10.
