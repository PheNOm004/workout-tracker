from scipy import stats

PLATEAU_WINDOW = 5
PLATEAU_ALPHA = 0.05


def classify_plateau(
    values: list[float], window: int = PLATEAU_WINDOW, alpha: float = PLATEAU_ALPHA
) -> str:
    if len(values) < 2:
        return "PROGRESSING"

    if len(values) < window:
        return "REGRESSING" if values[-1] < values[-2] else "PROGRESSING"

    recent = values[-window:]
    if recent[-1] < recent[-2] and recent[-2] < recent[-3]:
        return "REGRESSING"

    x = list(range(window))
    slope, _intercept, _r, p_value, _stderr = stats.linregress(x, recent)
    if p_value < alpha and slope > 0:
        return "PROGRESSING"
    if p_value < alpha and slope < 0:
        return "REGRESSING"
    return "PLATEAUING"
