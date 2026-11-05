import sys

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

from pathlib import Path

import matplotlib

matplotlib.use("Agg")

import numpy as np
from appose import NDArray
from pycurvelets.models import (
    AdvancedAnalysisOptions,
    BoundaryParameters,
    FiberAnalysisParameters,
    ImageInputParameters,
    OutputControlParameters,
)
from pycurvelets.process_image import process_image

image = np.asarray(img.ndarray()).T
if image.ndim != 2:
    raise ValueError(f"Expected a 2D input image, got shape {image.shape}.")
mask = None if boundary_mask is None else np.asarray(boundary_mask.ndarray()).T
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

def to_appose_array(image):
    image = np.asarray(image)
    if image.ndim == 3 and image.shape[-1] >= 3:
        rgb = image[..., :3].astype(np.uint32)
        image = (
            (np.uint32(255) << 24)
            | (rgb[..., 0] << 16)
            | (rgb[..., 1] << 8)
            | rgb[..., 2]
        )
    else:
        image = image.astype(np.float32, copy=False)
    shared = NDArray(image.dtype.name, list(image.shape))
    np.copyto(shared.ndarray(), image)
    return shared

for name, image in result["images"].items():
    task.outputs[name] = to_appose_array(image)

angles = features["fiber_absolute_angle"]
summary = f"Fibers: {len(features)}; angle mean: {angles.mean():.2f}; std: {angles.std():.2f} degrees"
if "alignment_mean" in features:
    summary += f"; alignment mean: {features['alignment_mean'].mean():.4f}"

task.outputs["summary"] = summary