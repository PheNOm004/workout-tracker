import numpy as np
from sklearn.linear_model import LinearRegression

SHRINKAGE_K = 10


def shrinkage_weight(n: int, k: int = SHRINKAGE_K) -> float:
    return n / (n + k)


def _personal_trend_prediction(one_rep_maxes: list[float]) -> float:
    if len(one_rep_maxes) == 1:
        return one_rep_maxes[0]
    x = np.arange(len(one_rep_maxes)).reshape(-1, 1)
    y = np.array(one_rep_maxes)
    model = LinearRegression().fit(x, y)
    next_index = np.array([[len(one_rep_maxes)]])
    return float(model.predict(next_index)[0])


def predict_next_one_rep_max(
    one_rep_maxes: list[float],
    population_prior: float | None,
    k: int = SHRINKAGE_K,
) -> float:
    if not one_rep_maxes:
        raise ValueError("predict_next_one_rep_max requires at least one data point")

    personal = _personal_trend_prediction(one_rep_maxes)
    if population_prior is None:
        return personal

    w = shrinkage_weight(len(one_rep_maxes), k)
    return w * personal + (1 - w) * population_prior
