---
name: resume-changelog
description: Maintain DEV_LOG.md, a gitignored running changelog of this hardening effort written in Zoya's voice, resume-friendly and interview-ready. Use after completing a meaningful chunk of work from containerize-stack, split-services, reliability-hardening, or tests-and-ci — not per-commit.
---

# Resume-friendly dev log

## Purpose
`DEV_LOG.md` at the repo root (gitignored — never committed, it's a personal working doc, not project documentation) is a running narrative of this hardening effort, written so Zoya can skim it before an interview and immediately have the "before → after → why it matters" story for each change, in her own voice rather than a mechanical diff summary.

## When to update
At the end of a meaningful chunk of work — one phase, or a solid sub-piece of a phase (e.g. "Kafka container actually boots now" is worth an entry even before the whole containerize-stack skill is done). Not per commit, not per file edit.

## Voice/style (derived from how Zoya actually writes, not generic "resume speak")
- First person, casual-technical, no marketing fluff ("leveraged synergies" energy is banned).
- Comfortable naming the tradeoff out loud rather than hiding it — e.g. "skipped Eureka on purpose, Compose networking does the job here and is more current anyway."
- Scare-quotes around buzzwords when being a little wry about them (e.g. "data engineer-ish", "cloud-native").
- States what was broken plainly before explaining the fix — doesn't dress up bugs as if they were always part of the plan.
- Short paragraphs or tight bullet points over long prose. No em-dash-heavy corporate cadence.

## Structure per entry
```
## <date> — <short title>
**Before:** what was actually true/broken (be specific — file names, the actual bug, not "the code needed improvement")
**After:** what changed
**Why it's worth mentioning:** the interview-ready framing — what distributed-systems/SDE concept this demonstrates
```

## Checklist
1. Confirm `DEV_LOG.md` is in `.gitignore` before the first entry is ever written (should already be handled during branch setup).
2. First entry gets created the moment real implementation work starts (not during the branch/skill-md prep step).
3. Keep entries honest about what was already broken vs newly added — the "before" framing is what makes this resume-usable, don't lose it by writing everything as if it was smooth sailing.

## Status
Not started — file doesn't exist yet, created on first real implementation entry.
