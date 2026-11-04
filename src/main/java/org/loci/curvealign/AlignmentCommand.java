/*-
 * #%L
 * Fiji integration for tme-quant workflows.
 * %%
 * Copyright (C) 2026 CurveAlign for Fiji developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.loci.curvealign;

import java.io.File;
import java.util.Map;
import org.apposed.appose.Appose;
import org.apposed.appose.Environment;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.log.LogService;
import org.scijava.app.StatusService;
import org.scijava.table.DefaultGenericTable;
import org.scijava.table.GenericTable;
import org.scijava.ItemIO;

@Plugin(type = Command.class, menuPath = "Plugins>CurveAlign>Alignment Check")
public class AlignmentCommand extends ContextCommand {
    static final String MANAGED = "Create / reuse managed environment";
    static final String EXISTING = "Use existing environment";

    @Parameter(label = "Python setup", choices = {MANAGED, EXISTING})
    private String setup = MANAGED;

    @Parameter(label = "tme-quant source checkout (managed setup)", style = "directory", required = false)
    private File source;

    @Parameter(label = "Existing Python environment directory", style = "directory", required = false)
    private File environmentDirectory;

    @Parameter(label = "Fiber angles (degrees, comma separated)")
    private String angles = "0, 10, 170";

    @Parameter private LogService log;
    @Parameter private StatusService status;
    @Parameter(type = ItemIO.OUTPUT) private GenericTable results;

    @Override
    public void run() {
        try {
            double[] values = parseAngles(angles);
            status.showStatus("Preparing CurveAlign Python environment...");
            Environment environment;
            if (EXISTING.equals(setup)) {
                if (environmentDirectory == null || !environmentDirectory.isDirectory())
                    throw new IllegalArgumentException("Select an existing Python environment directory");
                environment = Appose.wrap(environmentDirectory);
            }
            else {
                if (source == null || !new File(source, "pyproject.toml").isFile())
                    throw new IllegalArgumentException("Select the tme-quant source checkout containing pyproject.toml");
                environment = Appose.uv().python("3.11")
                    .include("appose==0.10.0", source.getCanonicalPath())
                    .name("fiji-curvealign-dev")
                    .subscribeOutput(log::info).subscribeError(log::warn).build();
            }
            status.showStatus("Computing alignment with tme-quant...");
            Map<String, Object> output = AlignmentBridge.measure(environment, values);
            results = new DefaultGenericTable();
            results.appendColumn("Fiber count");
            results.appendColumn("Alignment (0–1)");
            results.appendColumn("tme-quant version");
            results.appendRow();
            results.set(0, 0, output.get("count"));
            results.set(1, 0, output.get("alignment"));
            results.set(2, 0, output.get("tme_quant_version"));
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cancel("CurveAlign execution interrupted");
        }
        catch (Exception e) {
            log.error(e);
            cancel("CurveAlign failed: " + e.getMessage() + ". See the Log for details.");
        }
        finally {
            status.clearStatus();
        }
    }

    static double[] parseAngles(String text) {
        if (text == null || text.trim().isEmpty())
            throw new IllegalArgumentException("Enter at least one angle");
        String[] parts = text.split(",", -1);
        double[] values = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            values[i] = Double.parseDouble(parts[i].trim());
            if (!Double.isFinite(values[i]))
                throw new IllegalArgumentException("Angles must be finite numbers");
        }
        return values;
    }
}
