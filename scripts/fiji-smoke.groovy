// Run with Fiji --headless --run scripts/fiji-smoke.groovy.
// Optional script arguments: environmentDirectory (existing venv) or source (checkout).
#@ org.scijava.Context context
#@ String (required=false) environmentDirectory
#@ String (required=false) source

import org.scijava.command.CommandService
import org.scijava.table.GenericTable
import org.apposed.appose.Appose

def exitCode = 1
try {
    def commands = context.service(CommandService)
    def info = commands.getCommand('org.loci.curvealign.AlignmentCommand')
    assert info != null : 'CurveAlign command was not discovered in Fiji'
    println "CurveAlign command: ${info.menuPath}"
    println "Appose runtime: ${Appose.version()}"
    println "Appose JAR: ${Appose.protectionDomain.codeSource.location}"
    def inputs = environmentDirectory != null ?
        ['setup', 'Use existing environment', 'environmentDirectory', new File(environmentDirectory)] :
        ['setup', 'Create / reuse managed environment', 'source', new File(source)]
    assert environmentDirectory != null || source != null : 'Supply environmentDirectory or source'
    [[0d, '0, 90'], [1d, '0, 180']].each { expected, angles ->
        def args = inputs + ['angles', angles]
        def module = commands.run(info, false, args.toArray()).get()
        assert !module.isCanceled() : module.cancelReason
        def table = (GenericTable) module.getOutput('results')
        assert table != null
        assert ((Number) table.get(0, 0)).intValue() == 2
        assert Math.abs(((Number) table.get(1, 0)).doubleValue() - expected) < 1e-12
        println "PASS: angles=${angles}; alignment=${table.get(1, 0)}; tme-quant=${table.get(2, 0)}"
    }
    println 'PASS: Fiji discovered and executed the installed CurveAlign plugin'
    exitCode = 0
}
catch (Throwable failure) {
    failure.printStackTrace()
}
finally {
    context.dispose()
    System.exit(exitCode)
}
