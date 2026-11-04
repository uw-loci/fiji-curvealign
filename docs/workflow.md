# Fiji user workflow

Source: https://github.com/uw-loci/tme-quant/issues/66

## Intended release workflow

1. Install CurveAlign/CT-FIRE from a Fiji update site.
2. Open an image and launch the plugin.
3. The plugin checks the Python environment and backend availability.
4. If a selected backend needs external licensed software, guide the user through
   accepting its license, obtaining it separately, and selecting its installation.
5. Provision a compatible Python environment through Appose.
6. Configure analysis parameters using SciJava parameter dialogs.
7. Execute the tme-quant API and display returned tables/images in Fiji.

The first image analysis and its parameters remain to be selected. The Fiji UI
must use an adapter contract rather than import backend implementation details.

## Initial development slice: Alignment Check

This command validates provisioning, Python execution, and table display. It
accepts measured fiber angles, rather than extracting fibers from an image.

1. Launch the command at Plugins > CurveAlign > Alignment Check.
2. Select managed setup and the local tme-quant checkout. On first run Appose
   provisions Python 3.11, Appose 0.10.0, and the local package. Downloads and
   package builds require internet access and may take several minutes.
   Alternatively select an existing environment containing appose and tme-quant.
3. Enter comma-separated fiber angles in degrees; default: `0, 10, 170`.
4. Run the command. Fiji displays count, axial alignment, and library version.
   Alignment is near 0.96 for the default inputs; `0, 180` gives 1 and `0, 90`
   gives approximately 0.
5. Inspect the Fiji Log if provisioning or Python execution fails.

No CurveLab dependency is required for this measurement. Managed setup attempts
installation of the package's native C++ extension; compiler/OpenMP requirements
and optional backend availability are governed by tme-quant's installer.

## Adapter contract v1

Java sends a one-dimensional list of finite angles in degrees. Python doubles
angles because fiber orientations are axial, calls tme-quant's `circ_r`, and
returns JSON-compatible count, alignment, and package version. Python workers
are closed after each operation, including failure. No image data is transferred
yet. Before adding images, specify axes, dtype, calibration, and ownership of
shared memory. The adapter resource is the only location that imports tme-quant.

## Remaining release work

- Select the first image workflow and define the public tme-quant analysis API.
- Transfer an ImageJ2 Dataset and return images, overlays, and tables.
- Report backend capabilities and implement external-license setup.
- Pin a distributable tme-quant artifact and lock transitive Python dependencies;
  the development environment currently installs a snapshot of a local checkout.
- Validate clean provisioning on macOS, Linux, and Windows, add cancellation and
  more detailed provisioning progress. The local installed Fiji passed plugin
  discovery, managed/existing-environment execution, menu registration, and GUI
  result-table display checks with its bundled dependencies.
- Package dependencies and publish an update site.
