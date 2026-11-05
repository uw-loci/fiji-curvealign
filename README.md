# Fiji CurveAlign

Fiji plugin integrating tme-quant workflows, with a Java front end and a Python backend powered by [Appose](https://docs.apposed.org/).

## Python backend setup

The first version uses an existing tme-quant `uv` environment. Install CurveLab
locally according to the [tme-quant installation instructions](https://github.com/uw-loci/tme-quant/blob/main/doc/INSTALL.md),
then run `bash bin/install.sh` from the tme-quant checkout. The installer builds
the proprietary curvelet backend locally and includes the Python Appose worker.
CurveLab itself is not distributed with this plugin.

In Fiji, run **Plugins > CurveAlign > Run**, choose a 2D scalar input image, and
select the tme-quant checkout's `.venv` directory as the Python environment.
The command returns and displays the generated overlay as an ImageJ2 `Dataset`.
FIRE modes additionally require a CT-FIRE results directory; boundary analysis
accepts a matching 2D mask.

The image is copied to Appose shared memory as a 32-bit floating-point array.
The generated overlay and feature files are written to the selected output
directory by the current Python API.

## TODO

- Test UDCT python library backend
- Update python API to make file output optional
- Provision and manage the tme-quant Python environment through Appose instead
	of requiring users to select an existing `.venv`.
- Improve CurveLab setup while respecting its license and the requirement that
	users obtain and build the proprietary dependency locally.
- Get something working on Windows!
- Create Update Site for fiji plugin

## Development

The Java project targets Java 21 and uses Maven. Build and run its tests with:

```shell
mvn verify
```

The Maven project includes the SciJava, ImageJ, Appose, and ImgLib2-Appose APIs.
