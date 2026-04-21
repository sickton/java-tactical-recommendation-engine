import joblib
import pandas as pd
import numpy as np
from pathlib import Path
import json
from sklearn.ensemble import HistGradientBoostingClassifier
from sklearn.model_selection import train_test_split, GroupKFold, cross_val_score
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import classification_report, roc_auc_score, recall_score

PASSES_PATH = Path(__file__).parent / "output" / "passes.csv"
MODEL_PATH  = Path(__file__).parent / "output" / "escape_model.joblib"
META_PATH   = Path(__file__).parent / "output" / "escape_model_meta.json"

CATEGORICAL_FEATURES = ["from_pos", "to_pos", "formation", "game_phase", "pitch_zone", "pass_direction"]
NUMERICAL_FEATURES   = ["pass_length", "pass_angle", "pitch_x", "pitch_y"]
LABEL                = "pass_outcome"

def prepare_data(df: pd.DataFrame):
    encoders = {}

    for col in CATEGORICAL_FEATURES:
        le = LabelEncoder()
        df[col] = le.fit_transform(df[col])
        encoders[col] = le

    label_encoder = LabelEncoder()
    y = label_encoder.fit_transform(df[LABEL])

    X = df[CATEGORICAL_FEATURES + NUMERICAL_FEATURES].values

    return X, y, encoders, label_encoder

def find_optimal_threshold(y_test, y_proba, target_recall=0.85):
    thresholds = np.arange(0.1, 0.95, 0.01)
    best_threshold = 0.5
    best_score = float('inf')

    for threshold in thresholds:
        preds = (y_proba >= threshold).astype(int)
        recall_fail    = recall_score(y_test, preds, pos_label=0)
        recall_success = recall_score(y_test, preds, pos_label=1)
        score = abs(recall_fail - target_recall) + abs(recall_success - target_recall)
        if score < best_score:
            best_score = score
            best_threshold = threshold

    return best_threshold

def train_model(X, y, match_ids):
    unique_matches = np.unique(match_ids)
    rng = np.random.default_rng(42)
    rng.shuffle(unique_matches)

    split         = int(len(unique_matches) * 0.8)
    train_matches = set(unique_matches[:split])
    test_matches  = set(unique_matches[split:])

    train_idx = np.array([i for i, m in enumerate(match_ids) if m in train_matches])
    test_idx  = np.array([i for i, m in enumerate(match_ids) if m in test_matches])

    X_train, X_test = X[train_idx], X[test_idx]
    y_train, y_test = y[train_idx], y[test_idx]

    model = HistGradientBoostingClassifier(
        class_weight="balanced",
        max_iter=200,
        learning_rate=0.1,
        max_depth=4,
        random_state=42
    )

    model.fit(X_train, y_train)

    y_proba   = model.predict_proba(X_test)[:, 1]
    threshold = find_optimal_threshold(y_test, y_proba, target_recall=0.80)
    y_pred    = (y_proba >= threshold).astype(int)

    print(f"  Optimal threshold: {threshold:.2f}")
    print(classification_report(y_test, y_pred, target_names=["fail", "success"]))
    print(f"ROC-AUC: {roc_auc_score(y_test, y_proba):.4f}")

    print("\nCross-validation (GroupKFold, 5 folds)...")
    gkf       = GroupKFold(n_splits=5)
    cv_scores = cross_val_score(model, X, y, cv=gkf, groups=match_ids, scoring="roc_auc")
    print(f"  CV ROC-AUC: {cv_scores.mean():.4f} +/- {cv_scores.std():.4f}")

    return model, threshold

def main():
    print("Loading data...")
    df = pd.read_csv(PASSES_PATH)
    match_ids = df["match_id"].values
    print(f"  {len(df)} rows loaded")

    print("\nPreprocessing...")
    X, y, encoders, label_encoder = prepare_data(df)

    print("\nTraining model...")
    model, threshold = train_model(X, y, match_ids)

    print("\nSaving model...")
    joblib.dump(model, MODEL_PATH)
    print(f"  Model saved to {MODEL_PATH}")

    meta = {
        "features": CATEGORICAL_FEATURES + NUMERICAL_FEATURES,
        "label": LABEL,
        "classes": label_encoder.classes_.tolist(),
        "encoders": {
            col: encoders[col].classes_.tolist()
            for col in CATEGORICAL_FEATURES
        },
        "n_estimators": model.max_iter,
        "learning_rate": model.learning_rate,
        "max_depth": model.max_depth,
        "threshold": float(threshold),
    }
    with open(META_PATH, "w") as f:
        json.dump(meta, f, indent=2)
    print(f"  Metadata saved to {META_PATH}")


if __name__ == "__main__":
    main()
