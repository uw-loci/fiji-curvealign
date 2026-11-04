import json

import numpy as np
import pandas as pd
import tifffile

from pycurvelets.get_ct import get_ct
from pycurvelets.models import CurveletControlParameters, FeatureControlParameters
from pycurvelets.new_curv import new_curv


image = np.asarray(tifffile.imread(image_path), dtype=np.float32)
if image.ndim != 2:
    raise ValueError(f"Expected a 2D image plane, received shape {image.shape}.")

curvelet_parameters = CurveletControlParameters(keep=keep, scale=scale, radius=radius)
try:
    features = get_ct(
        image,
        curvelet_parameters,
        FeatureControlParameters(
            minimum_nearest_fibers=minimum_nearest_fibers,
            minimum_box_size=minimum_box_size,
            fiber_midpoint_estimate=1,
        ),
    )
except ValueError as error:
    if "n_neighbors" not in str(error):
        raise
    fibers, _, _ = new_curv(image, curvelet_parameters)
    features = (fibers, pd.DataFrame(), pd.DataFrame(), None)

if isinstance(features, tuple) and len(features) == 4:
    fiber_structure, density, alignment, _ = features
    if len(fiber_structure) == 0:
        result = []
    else:
        combined = pd.concat(
            [fiber_structure.reset_index(drop=True), density, alignment], axis=1
        )
        result = json.loads(combined.to_json(orient="records"))
else:
    result = []

with open(results_path, "w", encoding="utf-8") as output_file:
    json.dump(result, output_file)