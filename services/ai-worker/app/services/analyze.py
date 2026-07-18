"""Descriptive data analysis with pandas (Phase 8, FR-DATA-1..4). Given a CSV, infer column types,
count missing values, and compute descriptive statistics + chart-ready distributions. All numbers
here are computed from the data — the AI interpretation layer must only cite these, never invent."""

from __future__ import annotations

import io

import pandas as pd


def _num_stats(s: pd.Series) -> dict:
    s = pd.to_numeric(s, errors="coerce").dropna()
    if s.empty:
        return {}
    # Histogram bins (up to 10) for charting.
    counts, edges = _histogram(s)
    return {
        "count": int(s.count()),
        "mean": round(float(s.mean()), 4),
        "median": round(float(s.median()), 4),
        "std": round(float(s.std(ddof=1)), 4) if s.count() > 1 else 0.0,
        "min": round(float(s.min()), 4),
        "max": round(float(s.max()), 4),
        "histogram": {"bins": [round(float(e), 4) for e in edges], "counts": [int(c) for c in counts]},
    }


def _histogram(s: pd.Series, bins: int = 10):
    try:
        import numpy as np

        counts, edges = np.histogram(s, bins=min(bins, max(1, s.nunique())))
        return counts.tolist(), edges.tolist()
    except Exception:  # noqa: BLE001
        return [], []


def _freq(s: pd.Series, top: int = 15) -> list[dict]:
    vc = s.dropna().astype(str).value_counts().head(top)
    total = int(vc.sum()) or 1
    return [{"value": str(k), "count": int(v), "pct": round(100.0 * int(v) / total, 1)} for k, v in vc.items()]


def analyze_csv(data: bytes) -> dict:
    try:
        df = pd.read_csv(io.BytesIO(data))
    except Exception as e:  # noqa: BLE001
        return {"error": f"Could not parse CSV: {e}", "columns": [], "row_count": 0}

    columns = []
    for name in df.columns:
        col = df[name]
        missing = int(col.isna().sum())
        numeric = pd.to_numeric(col, errors="coerce")
        is_numeric = numeric.notna().sum() >= max(1, int(0.7 * col.notna().sum()))  # mostly numbers
        if is_numeric:
            columns.append({
                "name": str(name), "type": "numeric", "missing": missing, "stats": _num_stats(col),
            })
        else:
            columns.append({
                "name": str(name), "type": "categorical", "missing": missing, "frequencies": _freq(col),
            })

    return {
        "row_count": int(len(df)),
        "column_count": int(len(df.columns)),
        "columns": columns,
    }
