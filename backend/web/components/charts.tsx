"use client";

/**
 * Charts for the appearance history.
 *
 * Deliberate choices:
 * - One series per plot. The four metrics live on different scales, so they are
 *   shown as small multiples rather than a dual-axis or four-line chart.
 * - The line colour is --chart-line (honey-700 light / honey-400 dark), not
 *   honey-500: the brand yellow measures 1.66:1 against white and is unreadable
 *   as a stroke. Both chosen steps clear 3:1.
 * - Every plot carries a hover crosshair and tooltip, plus a table fallback for
 *   screen readers.
 */

import { useMemo, useState } from "react";
import { formatMetric, type HistoryPoint, type MetricKey } from "@/lib/api";

interface Series {
  x: number[];
  y: number[];
}

function toSeries(history: HistoryPoint[], metric: MetricKey): Series {
  return {
    x: history.map((h) => new Date(h.captured_at).getTime()),
    y: history.map((h) => Number(h[metric])),
  };
}

/** Nice-looking padded domain; a flat series still gets a visible band. */
function domain(values: number[]): [number, number] {
  const min = Math.min(...values);
  const max = Math.max(...values);
  if (min === max) {
    const pad = Math.abs(min) * 0.1 || 0.5;
    return [min - pad, max + pad];
  }
  const pad = (max - min) * 0.18;
  return [min - pad, max + pad];
}

function path(pts: [number, number][], close?: { baseline: number }): string {
  if (!pts.length) return "";
  const d = pts.map((p, i) => `${i ? "L" : "M"}${p[0].toFixed(2)} ${p[1].toFixed(2)}`).join(" ");
  if (!close) return d;
  const first = pts[0];
  const last = pts[pts.length - 1];
  return `${d} L${last[0].toFixed(2)} ${close.baseline} L${first[0].toFixed(2)} ${close.baseline} Z`;
}

/* ------------------------------------------------------------- TrendChart */

export function TrendChart({
  history,
  metric,
  height = 240,
}: {
  history: HistoryPoint[];
  metric: MetricKey;
  height?: number;
}) {
  const [hover, setHover] = useState<number | null>(null);

  const W = 720;
  const H = height;
  const M = { top: 18, right: 16, bottom: 26, left: 44 };

  const series = useMemo(() => toSeries(history, metric), [history, metric]);

  const geometry = useMemo(() => {
    if (series.y.length === 0) return null;
    const [lo, hi] = domain(series.y);
    const innerW = W - M.left - M.right;
    const innerH = H - M.top - M.bottom;

    // A single capture has no x-range, so pin it to the middle of the plot.
    const xAt = (i: number) =>
      series.x.length === 1
        ? M.left + innerW / 2
        : M.left + (i / (series.x.length - 1)) * innerW;
    const yAt = (v: number) => M.top + innerH - ((v - lo) / (hi - lo)) * innerH;

    const pts = series.y.map((v, i) => [xAt(i), yAt(v)] as [number, number]);
    const ticks = [lo, lo + (hi - lo) / 2, hi];
    return { pts, ticks, yAt, innerH, lo, hi };
  }, [series, H, M.left, M.right, M.top, M.bottom]);

  if (!geometry) return null;

  const { pts, ticks } = geometry;
  const baseline = H - M.bottom;
  const active = hover !== null ? history[hover] : null;

  return (
    <figure className="m-0">
      <svg
        viewBox={`0 0 ${W} ${H}`}
        className="w-full touch-none"
        style={{ height }}
        role="img"
        aria-label={`${metric} over ${history.length} captures`}
        onMouseLeave={() => setHover(null)}
        onMouseMove={(e) => {
          const rect = e.currentTarget.getBoundingClientRect();
          const svgX = ((e.clientX - rect.left) / rect.width) * W;
          // Nearest point wins, so the hit target is wider than the marker.
          let best = 0;
          let bestD = Infinity;
          pts.forEach(([px], i) => {
            const d = Math.abs(px - svgX);
            if (d < bestD) {
              bestD = d;
              best = i;
            }
          });
          setHover(best);
        }}
      >
        {/* Recessive gridlines and y labels */}
        {ticks.map((t, i) => (
          <g key={i}>
            <line
              x1={M.left}
              x2={W - M.right}
              y1={geometry.yAt(t)}
              y2={geometry.yAt(t)}
              stroke="var(--chart-grid)"
              strokeWidth={1}
            />
            <text
              x={M.left - 9}
              y={geometry.yAt(t) + 4}
              textAnchor="end"
              fill="var(--fg-subtle)"
              fontSize={11}
              style={{ fontVariantNumeric: "tabular-nums" }}
            >
              {metric === "blemish_count" ? t.toFixed(0) : t.toFixed(2)}
            </text>
          </g>
        ))}

        <path d={path(pts, { baseline })} fill="var(--chart-fill)" />
        <path
          d={path(pts)}
          fill="none"
          stroke="var(--chart-line)"
          strokeWidth={2}
          strokeLinecap="round"
          strokeLinejoin="round"
        />

        {/* Baseline captures get a ring so the reference frame is findable */}
        {history.map((h, i) =>
          h.is_baseline ? (
            <circle
              key={`b${i}`}
              cx={pts[i][0]}
              cy={pts[i][1]}
              r={5.5}
              fill="var(--surface)"
              stroke="var(--chart-line)"
              strokeWidth={2}
            />
          ) : null,
        )}

        {/* First and last points are directly labelled; never every point */}
        {[0, pts.length - 1]
          .filter((i, idx, arr) => pts.length > 1 || idx === 0)
          .filter((i, idx, arr) => arr.indexOf(i) === idx)
          .map((i) => (
            <text
              key={`l${i}`}
              x={pts[i][0]}
              y={pts[i][1] - 11}
              textAnchor={i === 0 ? "start" : "end"}
              fill="var(--fg-muted)"
              fontSize={11}
              fontWeight={700}
              style={{ fontVariantNumeric: "tabular-nums" }}
            >
              {formatMetric(metric, series.y[i])}
            </text>
          ))}

        {/* x labels: only the ends, to avoid collisions */}
        <text x={M.left} y={H - 7} fill="var(--fg-subtle)" fontSize={11}>
          {new Date(series.x[0]).toLocaleDateString(undefined, {
            month: "short",
            day: "numeric",
          })}
        </text>
        {series.x.length > 1 && (
          <text
            x={W - M.right}
            y={H - 7}
            textAnchor="end"
            fill="var(--fg-subtle)"
            fontSize={11}
          >
            {new Date(series.x[series.x.length - 1]).toLocaleDateString(undefined, {
              month: "short",
              day: "numeric",
            })}
          </text>
        )}

        {/* Crosshair */}
        {hover !== null && (
          <g pointerEvents="none">
            <line
              x1={pts[hover][0]}
              x2={pts[hover][0]}
              y1={M.top}
              y2={baseline}
              stroke="var(--chart-baseline)"
              strokeWidth={1}
              strokeDasharray="3 3"
            />
            <circle
              cx={pts[hover][0]}
              cy={pts[hover][1]}
              r={5}
              fill="var(--chart-line)"
              stroke="var(--surface)"
              strokeWidth={2}
            />
          </g>
        )}
      </svg>

      <figcaption className="mt-1 min-h-[20px] text-[12px] text-muted">
        {active ? (
          <span className="tnum">
            {new Date(active.captured_at).toLocaleDateString()} ·{" "}
            <strong className="text-fg">
              {formatMetric(metric, Number(active[metric]))}
            </strong>{" "}
            · {active.confidence_label ?? "directional comparison"} · {active.confidence_message ?? "Small changes may be capture noise."}
          </span>
        ) : (
          "Hover a point for the capture behind it."
        )}
      </figcaption>
    </figure>
  );
}

/* -------------------------------------------------------------- Sparkline */

/** Small multiple: one metric, own scale, no axes. */
export function Sparkline({
  history,
  metric,
  width = 132,
  height = 40,
}: {
  history: HistoryPoint[];
  metric: MetricKey;
  width?: number;
  height?: number;
}) {
  const series = useMemo(() => toSeries(history, metric), [history, metric]);
  if (series.y.length < 2) {
    return (
      <div
        style={{ height }}
        className="grid place-items-center text-[11px] text-subtle"
      >
        Needs 2+ captures
      </div>
    );
  }

  const [lo, hi] = domain(series.y);
  const pad = 3;
  const pts = series.y.map(
    (v, i) =>
      [
        (i / (series.y.length - 1)) * width,
        pad + (height - pad * 2) - ((v - lo) / (hi - lo)) * (height - pad * 2),
      ] as [number, number],
  );

  return (
    <svg
      viewBox={`0 0 ${width} ${height}`}
      width="100%"
      height={height}
      preserveAspectRatio="none"
      aria-hidden="true"
      focusable="false"
    >
      <path d={path(pts, { baseline: height })} fill="var(--chart-fill-soft)" />
      <path
        d={path(pts)}
        fill="none"
        stroke="var(--chart-line)"
        strokeWidth={1.75}
        strokeLinecap="round"
        strokeLinejoin="round"
        vectorEffect="non-scaling-stroke"
      />
      <circle
        cx={pts[pts.length - 1][0]}
        cy={pts[pts.length - 1][1]}
        r={2.6}
        fill="var(--chart-line)"
      />
    </svg>
  );
}

/* -------------------------------------------------------------- StreakRing */

/** Progress ring for the capture streak against its 7-day cadence window. */
export function StreakRing({
  streak,
  target = 7,
  size = 104,
}: {
  streak: number;
  target?: number;
  size?: number;
}) {
  const stroke = 9;
  const r = (size - stroke) / 2;
  const c = 2 * Math.PI * r;
  const pct = Math.min(1, target > 0 ? streak / target : 0);

  return (
    <div className="relative shrink-0" style={{ width: size, height: size }}>
      <svg width={size} height={size} aria-hidden="true" focusable="false">
        <circle
          cx={size / 2}
          cy={size / 2}
          r={r}
          fill="none"
          stroke="var(--line)"
          strokeWidth={stroke}
        />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={r}
          fill="none"
          stroke="var(--honey-500)"
          strokeWidth={stroke}
          strokeLinecap="round"
          strokeDasharray={c}
          strokeDashoffset={c * (1 - pct)}
          transform={`rotate(-90 ${size / 2} ${size / 2})`}
          style={{
            transition: "stroke-dashoffset var(--dur-slow) var(--ease)",
          }}
        />
      </svg>
      <div className="absolute inset-0 grid place-content-center text-center">
        <span className="tnum display text-[26px]">{streak}</span>
        <span className="text-[10px] font-bold tracking-wide text-subtle uppercase">
          day streak
        </span>
      </div>
    </div>
  );
}

/** Accessible table fallback, rendered alongside the hero chart. */
export function HistoryTable({ history }: { history: HistoryPoint[] }) {
  return (
    <table className="w-full text-left text-[13px]">
      <thead>
        <tr className="text-[11px] font-bold tracking-wide text-subtle uppercase">
          <th className="py-2 font-bold">Date</th>
          <th className="py-2 font-bold">Redness</th>
          <th className="py-2 font-bold">Blemish</th>
          <th className="py-2 font-bold">Spots</th>
          <th className="py-2 font-bold">Texture</th>
          <th className="py-2 text-right font-bold">Reading quality</th>
        </tr>
      </thead>
      <tbody className="tnum">
        {history
          .slice()
          .reverse()
          .map((h, i) => (
            <tr key={i} className="border-t border-line">
              <td className="py-2 whitespace-nowrap">
                {new Date(h.captured_at).toLocaleDateString()}
                {h.is_baseline && (
                  <span className="ml-1.5 text-[10px] font-bold text-honey-700">
                    BASE
                  </span>
                )}
              </td>
              <td className="py-2">{h.redness_score.toFixed(3)}</td>
              <td className="py-2">{h.blemish_count.toFixed(0)}</td>
              <td className="py-2">{h.darkspot_area.toFixed(3)}</td>
              <td className="py-2">{h.texture_score.toFixed(3)}</td>
              <td className="py-2 text-right text-[11px]">{h.confidence_label ?? "directional"}</td>
            </tr>
          ))}
      </tbody>
    </table>
  );
}
