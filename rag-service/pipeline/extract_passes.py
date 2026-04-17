import json
import csv
import os
import math
from pathlib import Path

STATSBOMB_DIR = Path(r"C:\Users\sriva\UserssrivaDesktopopen-data\data")
OUTPUT_PATH = Path(__file__).parent / "output" / "passes.csv"

COMPETITIONS = [
    (11, 27,  "La Liga 2015/16"),
    (11, 42,  "La Liga 2019/20"),
    (11, 4,   "La Liga 2018/19"),
    (11, 1,   "La Liga 2017/18"),
    (11, 2,   "La Liga 2016/17"),
    (11, 26,  "La Liga 2014/15"),
    (11, 25,  "La Liga 2013/14"),
    (11, 24,  "La Liga 2012/13"),
    (11, 23,  "La Liga 2011/12"),
    (11, 22,  "La Liga 2010/11"),
    (11, 21,  "La Liga 2009/10"),
    (11, 41,  "La Liga 2008/09"),
    (11, 40,  "La Liga 2007/08"),
    (2,  27,  "Premier League 2015/16"),
]

POSITION_MAP = {
    "Goalkeeper":                "GK",
    "Right Back":                "RB",
    "Right Center Back":         "CB",
    "Center Back":               "CB",
    "Left Center Back":          "CB",
    "Left Back":                 "LB",
    "Right Wing Back":           "RWB",
    "Left Wing Back":            "LWB",
    "Center Defensive Midfield": "CDM",
    "Right Defensive Midfield":  "CDM",
    "Left Defensive Midfield":   "CDM",
    "Right Center Midfield":     "CM",
    "Center Midfield":           "CM",
    "Left Center Midfield":      "CM",
    "Right Midfield":            "RM",
    "Left Midfield":             "LM",
    "Right Attacking Midfield":  "CAM",
    "Center Attacking Midfield": "CAM",
    "Left Attacking Midfield":   "CAM",
    "Right Wing":                "RW",
    "Left Wing":                 "LW",
    "Right Center Forward":      "CF",
    "Left Center Forward":       "CF",
    "Center Forward":            "ST",
    "Secondary Striker":         "ST",
}

FORMATION_MAP = {
    433:   "F_4_3_3",
    4321:  "F_4_3_3",
    4312:  "F_4_3_3",
    4231:  "F_4_2_3_1",
    4141:  "F_4_2_3_1",
    4411:  "F_4_2_3_1",
    41212: "F_4_2_3_1",
    352:   "F_3_5_2",
    343:   "F_3_4_3",
    532:   "F_5_3_2",
    523:   "F_5_3_2",
    442:   "F_4_4_2",
}

def get_game_phase(minute: int) -> str:
    if minute <= 15:
        return "EARLY_MINUTES"
    elif minute <= 44:
        return "CLOSING_HALF"
    elif minute <= 50:
        return "HALF_TIME"
    elif minute <= 60:
        return "BUILD_PHASE"
    elif minute <= 70:
        return "TENSION_TIME"
    elif minute <= 87:
        return "LATE_GAME"
    else:
        return "STOPPAGE_TIME"

def normalise_position(verbose: str) -> str | None:
    return POSITION_MAP.get(verbose)

def normalise_formation(formation_int: int) -> str | None:
    return FORMATION_MAP.get(formation_int)

def load_json(path: Path) -> list | dict:
    with open(path, encoding="utf-8") as f:
        return json.load(f)

def get_pitch_zone(x) -> str:
    if x is None:
        return "unknown"
    if x < 40:
        return "defensive_third"
    elif x < 80:
        return "middle_third"
    else:
        return "attacking_third"

def get_pass_direction(angle: float) -> str:
    if -math.pi / 4 < angle < math.pi / 4:
        return "forward"
    elif angle > 3 * math.pi / 4 or angle < -3 * math.pi / 4:
        return "backward"
    else:
        return "sideways"

def get_pass_outcome(pass_data: dict) -> str:
    outcome = pass_data.get("outcome")
    if outcome is None:
        return "success"
    return "fail"

def extract_from_match(match_id: int, competition_label: str) -> list[dict]:
    events_path = STATSBOMB_DIR / "events" / f"{match_id}.json"
    if not events_path.exists():
        print(f"  [SKIP] events file not found: {match_id}")
        return []

    events = load_json(events_path)

    player_position: dict[str, str] = {}
    team_formations: dict[str, int] = {}

    for event in events:
        event_type = event.get("type", {}).get("name")

        if event_type == "Starting XI":
            tactics = event.get("tactics", {})
            formation_int = tactics.get("formation")
            team_name = event.get("team", {}).get("name", "")
            if formation_int:
                team_formations[team_name] = formation_int
            for player_entry in tactics.get("lineup", []):
                name = player_entry.get("player", {}).get("name", "")
                pos  = player_entry.get("position", {}).get("name", "")
                if name and pos:
                    player_position[name] = pos

        elif event_type == "Substitution":
            replacement = event.get("substitution", {}).get("replacement", {}).get("name", "")
            position    = event.get("position", {}).get("name", "")
            if replacement and position:
                player_position[replacement] = position

        elif event_type == "Tactical Shift":
            tactics = event.get("tactics", {})
            formation_int = tactics.get("formation")
            team_name = event.get("team", {}).get("name", "")
            if formation_int and team_name:
                team_formations[team_name] = formation_int
            for player_entry in tactics.get("lineup", []):
                name = player_entry.get("player", {}).get("name", "")
                pos  = player_entry.get("position", {}).get("name", "")
                if name and pos:
                    player_position[name] = pos

    rows = []

    for event in events:
        if event.get("type", {}).get("name") != "Pass":
            continue
        if not event.get("under_pressure"):
            continue

        pass_data   = event.get("pass", {})
        minute      = event.get("minute", 0)
        team_name   = event.get("team", {}).get("name", "")
        passer_pos  = event.get("position", {}).get("name", "")
        location    = event.get("location", [None, None])
        recipient   = pass_data.get("recipient", {}).get("name", "")

        from_pos = normalise_position(passer_pos)
        if not from_pos:
            continue

        to_pos = normalise_position(player_position.get(recipient, ""))
        if not to_pos:
            continue

        formation_int = team_formations.get(team_name)
        if not formation_int:
            continue
        formation = normalise_formation(formation_int)
        if not formation:
            continue

        pass_length = pass_data.get("length")
        pass_angle  = pass_data.get("angle")
        if pass_length is None or pass_angle is None:
            continue

        rows.append({
            "match_id":       match_id,
            "competition":    competition_label,
            "minute":         minute,
            "game_phase":     get_game_phase(minute),
            "formation":      formation,
            "from_pos":       from_pos,
            "to_pos":         to_pos,
            "pass_length":    round(pass_length, 4),
            "pass_angle":     round(pass_angle, 4),
            "pitch_x":        location[0],
            "pitch_y":        location[1],
            "pitch_zone":     get_pitch_zone(location[0]),
            "pass_direction": get_pass_direction(pass_angle),
            "pass_outcome":   get_pass_outcome(pass_data),
        })

    return rows

def main():
    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)

    fieldnames = [
        "match_id", "competition", "minute", "game_phase", "formation",
        "from_pos", "to_pos", "pass_length", "pass_angle",
        "pitch_x", "pitch_y", "pitch_zone", "pass_direction", "pass_outcome",
    ]

    total_rows    = 0
    total_matches = 0

    with open(OUTPUT_PATH, "w", newline="", encoding="utf-8") as csvfile:
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        writer.writeheader()

        for comp_id, season_id, label in COMPETITIONS:
            matches_path = STATSBOMB_DIR / "matches" / str(comp_id) / f"{season_id}.json"
            if not matches_path.exists():
                print(f"[SKIP] {label}")
                continue

            matches = load_json(matches_path)
            print(f"\n{label} — {len(matches)} matches")

            for match in matches:
                match_id = match["match_id"]
                rows = extract_from_match(match_id, label)
                writer.writerows(rows)
                total_rows    += len(rows)
                total_matches += 1
                print(f"  match {match_id}: {len(rows)} rows")

    print(f"\nDone. {total_matches} matches, {total_rows} total rows → {OUTPUT_PATH}")


if __name__ == "__main__":
    main()