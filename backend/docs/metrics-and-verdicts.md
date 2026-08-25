# Measurement and verdict contract

## Capture acceptance

Every accepted frame must satisfy all hard checks:

| Check | Acceptance rule |
|---|---|
| Face | client mesh reports one usable subject |
| Pose | absolute yaw and pitch <= 12 degrees |
| Light | server brightness between 0.20 and 0.85 |
| Sharpness | server gradient score >= 0.35 |
| Distance | 30-80 cm client estimate |
| Expression | neutral expression flag |

The quality score is stored with every capture and contributes to confidence.
Failed frames never enter a user's history.

## Measurement families

Skin captures store blemish proxy count, redness score, dark-spot area, and
texture score. These are cosmetic longitudinal proxies, not clinical limits or
lesion classifiers.

Every metric snapshot stores its model version and noise floor. The complete
workspace exposes those values in history, reprocessing, labels, experiments,
and evidence explanations.

## Attribution rules

1. A product must have a `start` or `change` event.
2. Post-event frames count only after the product stabilization window.
3. A second product start/change during that window makes the result
   `evidence_unclear`.
4. Before and after medians are compared metric by metric against the stored
   noise floor.
5. Improvement requires normalized effect >= 1.5 and adequate confidence.
6. Worsening beyond the floor produces `investigate` unless the evidence is
   otherwise confounded.
7. Movement within the floor becomes `keep` only with sufficient sample size;
   otherwise it remains `evidence_unclear`.

Each verdict stores its evidence window, sample sizes, differences, normalized
effects, confounders, confidence, generated text, and product identity. The
language layer can explain evidence but cannot invent or override the label.
