import json
import os
import pandas as pd
from pathlib import Path

PASSES_PATH = Path(__file__).parent / "output" / "passes.csv"
MATRICES_DIR = Path(__file__).parent / "output" / "matrices"

def get_formations(df: pd.DataFrame) -> list:
    return df["formation"].unique().tolist()

def build_matrix(df: pd.DataFrame, formation: str) -> dict:
    formation_df = df[df["formation"] == formation]

    counts = formation_df.groupby(["from_pos", "to_pos"]).size()

    matrix = {}
    for (from_pos, to_pos), count in counts.items():
        if from_pos not in matrix:
            matrix[from_pos] = {}
        matrix[from_pos][to_pos] = count

    for from_pos in matrix:
        total = sum(matrix[from_pos].values())
        for to_pos in matrix[from_pos]:
            matrix[from_pos][to_pos] = round(matrix[from_pos][to_pos] / total, 4)

    return matrix

def main():
    MATRICES_DIR.mkdir(parents=True, exist_ok=True)

    df = pd.read_csv(PASSES_PATH)
    print(f"Loaded {len(df)} rows from passes.csv")

    formations = get_formations(df)
    print(f"Formations found: {formations}")

    for formation in formations:
        matrix = build_matrix(df, formation)

        output = {
            "formation": formation,
            "matrix": matrix,
            "sample_size": int(len(df[df["formation"] == formation]))
        }

        out_path = MATRICES_DIR / f"{formation}.json"
        with open(out_path, "w", encoding="utf-8") as f:
            json.dump(output, f, indent=2)

        print(f"  {formation}: {len(matrix)} positions → {out_path.name}")

    print(f"\nDone. {len(formations)} matrix files written to {MATRICES_DIR}")


if __name__ == "__main__":
    main()