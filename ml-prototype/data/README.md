# Prototype data (not committed)

## OpenPowerlifting CSV
Download the OpenPowerlifting dataset (e.g. via the sergeimakarovv/ML-Powerlifting
Kaggle mirror referenced in the design spec) and place it here as `openpowerlifting.csv`.
Expected columns used by this prototype: Sex, Equipment, BodyweightKg, BestSquatKg,
BestBenchKg, BestDeadliftKg.

## Personal SetLog export
One-time manual export from the TimeGo Room DB (device: SM-S918B):

    adb shell "run-as com.lsing.timego cat /data/data/com.lsing.timego/databases/timego.db" > timego.db
    sqlite3 timego.db ".mode csv" ".headers on" "SELECT exerciseName, timestamp, weightKg, reps FROM SetLog ORDER BY timestamp;" > setlog_export.csv

Place the resulting `setlog_export.csv` in this folder. Not a shipped app feature —
manual dump only, per the design spec's explicit scoping.
