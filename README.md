# fiji-curvealign

Fiji plugin for [tme-quant](https://github.com/uw-loci/tme-quant) CurveAlign workflows. This repo tracks [uw-loci/tme-quant#66](https://github.com/uw-loci/tme-quant/issues/66).

The current command is a placeholder. `Plugins > CurveAlign` opens a dialog so you can confirm Fiji loaded the plugin. It does not call tme-quant yet.

## Build

Java 21 or newer.

```bash
mvn -B package
```

Copy `target/fiji-curvealign-0.1.0-SNAPSHOT.jar` into the `plugins` folder of a Fiji that is running Java 21, then restart Fiji.
