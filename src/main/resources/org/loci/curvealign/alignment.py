"""Protocol v1: axial angles in degrees -> scalar alignment in [0, 1]."""
import importlib.metadata
import math
import numpy as np
from pycurvelets.utils.math.circ_r import circ_r

values = np.asarray(angles, dtype=float)
if values.ndim != 1 or values.size == 0 or not np.isfinite(values).all():
    raise ValueError("Provide at least one finite angle in degrees")
# Fibers are axial: 0 and 180 degrees describe the same orientation.
alignment = float(circ_r(np.deg2rad(2 * values)))
if not math.isfinite(alignment):
    raise ValueError("Alignment calculation returned a non-finite value")
task.outputs["alignment"] = alignment
task.outputs["count"] = int(values.size)
task.outputs["tme_quant_version"] = importlib.metadata.version("tme-quant")
