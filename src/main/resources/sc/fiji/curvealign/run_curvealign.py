import sys

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

from pathlib import Path

import matplotlib

matplotlib.use("Agg")

import numpy as np
from pycurvelets.models import (
    AdvancedAnalysisOptions,
    BoundaryParameters,
    FiberAnalysisParameters,
    ImageInputParameters,
    OutputControlParameters,
)
from pycurvelets.process_image import process_image

image = np.asarray(img).T
mask = None if boundary_mask is None else np.asarray(boundary_mask).T
output = Path(output_dir)
output.mkdir(parents=True, exist_ok=True)

result = process_image(
    image_params=ImageInputParameters(img=image, img_name=img_name),
    fiber_params=FiberAnalysisParameters(
        fiber_mode=fiber_mode,
        keep=keep,
        fire_directory=fire_dir,
    ),
    output_params=OutputControlParameters(
        output_directory=str(output),
        make_associations=True,
        make_map=True,
        make_overlay=True,
        make_feature_file=True,
    ),
    boundary_params=BoundaryParameters(
        distance_threshold=distance_threshold,
        tif_boundary=3 if mask is not None else 0,
        boundary_img=mask,
    ),
    advanced_options=AdvancedAnalysisOptions(
        exclude_fibers_in_mask=mask is not None,
        heatmap_STD_filter_size=16,
    ),
)

features = None if result is None else result.get("fib_feat_df")
if features is None:
    raise RuntimeError("No fibers detected.")

overlay_path = output / f"{img_name}_overlay.tiff"
if not overlay_path.is_file():
    raise RuntimeError(f"Expected overlay was not created: {overlay_path}")

angles = features["fiber_absolute_angle"]
summary = f"Fibers: {len(features)}; angle mean: {angles.mean():.2f}; std: {angles.std():.2f} degrees"
if "alignment_mean" in features:
    summary += f"; alignment mean: {features['alignment_mean'].mean():.4f}"

task.outputs["overlay_path"] = str(overlay_path)
task.outputs["summary"] = summary