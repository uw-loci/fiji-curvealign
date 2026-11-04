# CurveAlign for Fiji

An initial ImageJ2/SciJava plugin calling the Python **tme-quant** library through
Appose. See [the workflow and current scope](docs/workflow.md).

## Build and launch

Requirements: Maven, a JDK (Java 17 recommended for development), and network
access for dependencies. Java sources require Java 17, following the fiji-llm reference configuration. The latest
Fiji release is the intended deployment target; compatibility still needs a
manual check in Fiji.

On macOS, select the development JDK before running Maven:

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

```sh
mvn clean verify
mvn exec:java
```

The development launcher opens ImageJ2 with the command available at
**Plugins > CurveAlign > Alignment Check**. It is a development UI, not a bundled
Fiji distribution. Choose the local `tme-quant` checkout in the command dialog
and use managed setup, or choose an existing Python environment.

Managed setup uses Appose's uv builder and caches an environment named
`fiji-curvealign-dev`. Python 3.11 and `appose==0.10.0` are requested, along with
an installation of the selected local checkout (not an editable install).
Changes to Python source may require recreating the cached development
environment. The installer may require a C++ compiler and OpenMP; see the
[tme-quant installation documentation](../tme-quant/doc/INSTALL.md).
No napari/Qt extra or proprietary curvelet backend is requested.

## Python integration test

By default Python integration is skipped so ordinary Java builds need no Python.
Use a separate environment with Appose and the local tme-quant package installed:

```sh
uv venv --python 3.11 /tmp/curvealign-python
uv pip install --python /tmp/curvealign-python/bin/python 'appose==0.10.0' ../tme-quant
mvn verify -Dcurvealign.pythonEnvironment=/tmp/curvealign-python
```

On Windows use the environment's `Scripts/python.exe` for installation; pass the
environment directory to Maven. The test exercises real Java–Python execution
and verifies aligned and perpendicular fiber orientations and SciJava table output.
To additionally test fresh Appose-managed provisioning, pass
`-Dcurvealign.source=/absolute/path/to/tme-quant`. This creates and deletes a
temporary managed environment.

## Deployment status

The plugin is installed and tested locally in `/Applications/Fiji` (ImageJ
2.18.0, Appose JAR 0.12.0). Both existing-environment and managed-environment
execution passed the installed-Fiji smoke test. The GUI also displayed the
alignment result table and registered the command in the Plugins menu.
Update-site distribution is
pending. Image analysis, backend selection, license guidance, and image/ROI
transfer remain future work.

### Install in Fiji

Close Fiji before replacing its plugin JAR, then build and copy:

```sh
mvn clean verify
cp target/fiji-curvealign-0.1.0-SNAPSHOT.jar /Applications/Fiji/plugins/
```

This local Fiji already includes Appose and its dependencies. If another Fiji
installation lacks them, install compatible Appose dependencies before running
this command; do not add a second Appose JAR alongside an existing version.
Restart Fiji and choose **Plugins > CurveAlign > Alignment Check**. Select
managed setup and the `tme-quant` checkout, then try `0, 180` (alignment 1) or
`0, 90` (alignment approximately 0). A result table appears inside Fiji.

### Repeat the installed-Fiji smoke test

Run from this repository root. This uses Fiji's launcher, plugin discovery, and
bundled dependency versions, without adding the Maven runtime classpath:

```sh
/Applications/Fiji/fiji --headless --allow-multiple \
  --run scripts/fiji-smoke.groovy \
  'source="/Users/andrewkim/LOCI/tme-quant"'
```

For an existing environment, replace the last argument with
`'environmentDirectory="/tmp/curvealign-python"'`. Successful output includes
`PASS: Fiji discovered and executed the installed CurveAlign plugin`. The smoke
test exits its own Fiji process when finished; `--allow-multiple` keeps it
separate from an open GUI instance.

## License and CI

This plugin uses the [BSD-2-Clause license](LICENSE.txt). Its Maven metadata,
Java 17 baseline, license-header tooling, and GitHub Actions build follow
[fiji-llm](https://github.com/fiji/fiji-llm). The workflow runs the shared SciJava
setup/build scripts on main-branch pushes, pull requests, release tags, and
manual dispatch. Release deployment uses repository secrets as in the reference;
credentials and repository permissions must be configured before publishing.
Python integration remains opt-in through the Maven properties above.

Update Java license headers with `mvn license:update-file-header`.
