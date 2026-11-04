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

import java.util.Map;
import org.apposed.appose.Appose;
import org.junit.Test;
import net.imagej.ImageJ;
import org.scijava.command.CommandModule;
import org.scijava.table.GenericTable;
import java.io.File;
import java.nio.file.Files;
import org.apposed.appose.Environment;
import org.junit.Assume;
import static org.junit.Assert.*;

public class AlignmentBridgeTest {
    @Test public void parsesAngles() {
        assertArrayEquals(new double[] {0, 10, 170},
            AlignmentCommand.parseAngles("0, 10, 170"), 0);
    }

    @Test public void rejectsNonFiniteAngles() {
        try {
            AlignmentCommand.parseAngles("NaN");
            fail("Expected invalid input rejection");
        }
        catch (IllegalArgumentException expected) { }
    }

    @Test public void pythonRoundTrip() throws Exception {
        String directory = System.getProperty("curvealign.pythonEnvironment");
        Assume.assumeTrue("Set curvealign.pythonEnvironment to run Python integration", directory != null);
        Map<String, Object> aligned = AlignmentBridge.measure(Appose.wrap(directory), new double[] {0, 180});
        assertEquals(1.0, ((Number) aligned.get("alignment")).doubleValue(), 1e-12);
        Map<String, Object> perpendicular = AlignmentBridge.measure(Appose.wrap(directory), new double[] {0, 90});
        assertEquals(0.0, ((Number) perpendicular.get("alignment")).doubleValue(), 1e-12);
        assertEquals(2, ((Number) aligned.get("count")).intValue());
        assertNotNull(aligned.get("tme_quant_version"));
    }
    @Test public void commandReturnsTable() throws Exception {
        String directory = System.getProperty("curvealign.pythonEnvironment");
        Assume.assumeTrue(directory != null);
        ImageJ ij = new ImageJ();
        try {
            CommandModule module = ij.command().run(AlignmentCommand.class, false,
                "setup", AlignmentCommand.EXISTING,
                "environmentDirectory", new File(directory), "angles", "0, 180").get();
            assertFalse(module.isCanceled());
            GenericTable table = (GenericTable) module.getOutput("results");
            assertEquals(1.0, ((Number) table.get(1, 0)).doubleValue(), 1e-12);
        }
        finally { ij.context().dispose(); }
    }

    @Test public void managedProvisioning() throws Exception {
        String source = System.getProperty("curvealign.source");
        Assume.assumeTrue("Set curvealign.source to test managed provisioning", source != null);
        File base = Files.createTempDirectory("curvealign-managed-").toFile();
        Environment env = Appose.uv().python("3.11")
            .include("appose==0.10.0", new File(source).getCanonicalPath())
            .base(base).build();
        try {
            Map<String, Object> output = AlignmentBridge.measure(env, new double[] {0, 180});
            assertEquals(1.0, ((Number) output.get("alignment")).doubleValue(), 1e-12);
        }
        finally { env.delete(); }
    }

}
