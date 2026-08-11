from src.backtest import backtest_exercise, summarize_backtest


def test_backtest_produces_one_result_per_held_out_point():
    # 6 points, min_history=3 -> held-out points are indices 3,4,5 -> 3 results
    one_rms = [100.0, 102.0, 104.0, 106.0, 108.0, 110.0]
    results = backtest_exercise(one_rms, population_prior=None)
    assert len(results) == 3
    for r in results:
        assert set(r.keys()) == {"index", "actual", "ml_pred", "baseline_pred", "ml_error", "baseline_error"}
        assert r["ml_error"] >= 0
        assert r["baseline_error"] >= 0


def test_summarize_backtest_computes_mean_absolute_error():
    results = [
        {"index": 3, "actual": 100.0, "ml_pred": 101.0, "baseline_pred": 105.0, "ml_error": 1.0, "baseline_error": 5.0},
        {"index": 4, "actual": 110.0, "ml_pred": 109.0, "baseline_pred": 100.0, "ml_error": 1.0, "baseline_error": 10.0},
    ]
    summary = summarize_backtest(results)
    assert summary == {"ml_mae": 1.0, "baseline_mae": 7.5, "n": 2}


def test_too_short_history_returns_no_results():
    assert backtest_exercise([100.0, 102.0], population_prior=None) == []
