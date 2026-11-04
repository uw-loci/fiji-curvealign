# Fiji CurveAlign

Fiji plugin integrating tme-quant workflows, with a Java front end and a Python backend powered by [Appose](https://docs.apposed.org/).

## Development

The Java project targets Java 17 and uses Maven. Build and run its tests with:

```shell
mvn verify
```

The Maven project includes the SciJava, ImageJ, and Appose Java APIs. Appose requires Python 3.10 or newer when running Python workers. The Python environment and backend dependencies are not defined yet.
