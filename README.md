# Fiji CurveAlign

Fiji CurveAlign exposes tme-quant curvelet and fiber analysis in Fiji through a Swing dialog. Python runs in an Appose-managed Pixi environment.

## Build

Build the plugin with Java 21 and Maven:

```bash
mvn verify
```

## Use

Open a 2D image in Fiji, then choose **Plugins > CurveAlign > Analyze current plane**. Set the curvelet and feature parameters and choose **Extract fiber features**. The first run creates the Pixi environment and can take several minutes; detected curvelet, density, and alignment features appear in Fiji's Results table.

The current implementation analyzes the active image plane. The Python environment is defined in `src/main/resources/com/fiji/curvealign/pixi.toml` and installs `tme-quant` from the `uw-loci/tme-quant` repository with its `curvelops` extra.
