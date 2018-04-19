package am;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static java.lang.ThreadLocal.withInitial;

@Threads(10)
@State(Scope.Benchmark)
public class SpringPooling {

    public Random r = new Random();

    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Benchmark
    public void simple_avg(Blackhole b) {
        int idx = r.nextInt(10);
        b.consume("Some string" + idx + " another string");
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Benchmark
    public void simple_thr(Blackhole b) {
        int idx = r.nextInt(10);
        b.consume("Some string" + idx + " another string");
    }

    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Benchmark
    public void builder_avg(Blackhole b) {
        int idx = r.nextInt(10);
        b.consume(new StringBuilder().append("Some string").append(idx).append(" another string"));
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Benchmark
    public void builder_thr(Blackhole b) {
        int idx = r.nextInt(10);
        b.consume(new StringBuilder().append("Some string").append(idx).append(" another string"));
    }

    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Benchmark
    public void pooled_avg(Blackhole b) {
        int idx = r.nextInt(10);
        b.consume(SomeComposite.valueOf("Some string", idx, " another string").asString());
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Benchmark
    public void pooled_thr(Blackhole b) {
        int idx = r.nextInt(10);
        b.consume(SomeComposite.valueOf("Some string", idx, " another string").asString());
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SpringPooling.class.getSimpleName())
                .warmupIterations(5)
                .measurementIterations(5)
                .addProfiler(GCProfiler.class)
                .jvmArgs("-Xmx128M")
                .forks(1)
                .build();

        new Runner(opt).run();
    }
}

class SomeComposite {
    private final String s1;
    private final int i1;
    private final String s2;
    private final int _hash;
    private final String asString;
    private final static ThreadLocal<SomeComposite[]> cache = withInitial(() -> new SomeComposite[100]);

    public SomeComposite(String s1, int i1, String s2) {
        this.s1 = s1;
        this.i1 = i1;
        this.s2 = s2;
        this.asString = s1 + i1 + s2;
        this._hash = this.asString.hashCode();
    }

    public static SomeComposite valueOf(String s1, int i1, String s2) {
        SomeComposite[] c = cache.get();
        for (int i = 0; i < c.length; i++) {
            SomeComposite cached = c[i];
            if (cached == null) {
                c[i] = new SomeComposite(s1, i1, s2);
                return c[i];
            }
            if (cached.s1.equals(s1) && cached.i1 == i1 && cached.s2.equals(s2)) {
                return cached;
            }
        }

        return new SomeComposite(s1, i1, s2);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SomeComposite that = (SomeComposite) o;
        return i1 == that.i1 &&
                _hash == that._hash &&
                Objects.equals(s1, that.s1) &&
                Objects.equals(s2, that.s2) &&
                Objects.equals(asString, that.asString);
    }

    @Override
    public int hashCode() {
        return this._hash;
    }

    public String asString() {
        return this.asString;
    }
}


/*

# JMH version: 1.20
# VM version: JDK 1.8.0_172-ea, VM 25.172-b03
# VM invoker: D:\dev\jdk\jdk-8u172\jre\bin\java.exe
# VM options: -Xmx128M
# Warmup: 5 iterations, 1 s each
# Measurement: 5 iterations, 1 s each
# Timeout: 10 min per iteration
# Threads: 10 threads, will synchronize iterations
# Benchmark mode: Throughput, ops/time
# Benchmark: am.SpringPooling.builder_thr

Benchmark                                                    Mode  Cnt      Score     Error   Units
SpringPooling.builder_thr                                   thrpt    5  14157.195 ± 218.194  ops/ms
SpringPooling.builder_thr:·gc.alloc.rate                    thrpt    5   1483.155 ±  60.281  MB/sec
SpringPooling.builder_thr:·gc.alloc.rate.norm               thrpt    5    160.003 ±   0.019    B/op
SpringPooling.builder_thr:·gc.churn.PS_Eden_Space           thrpt    5   1497.222 ±  51.901  MB/sec
SpringPooling.builder_thr:·gc.churn.PS_Eden_Space.norm      thrpt    5    161.524 ±   3.108    B/op
SpringPooling.builder_thr:·gc.churn.PS_Survivor_Space       thrpt    5      0.700 ±   0.346  MB/sec
SpringPooling.builder_thr:·gc.churn.PS_Survivor_Space.norm  thrpt    5      0.076 ±   0.036    B/op
SpringPooling.builder_thr:·gc.count                         thrpt    5    288.000            counts
SpringPooling.builder_thr:·gc.time                          thrpt    5     94.000                ms

SpringPooling.pooled_thr                                    thrpt    5  15748.477 ± 252.326  ops/ms
SpringPooling.pooled_thr:·gc.alloc.rate                     thrpt    5      0.003 ±   0.001  MB/sec
SpringPooling.pooled_thr:·gc.alloc.rate.norm                thrpt    5     ≈ 10⁻⁴              B/op
SpringPooling.pooled_thr:·gc.count                          thrpt    5        ≈ 0            counts

SpringPooling.simple_thr                                    thrpt    5  13399.054 ± 135.133  ops/ms
SpringPooling.simple_thr:·gc.alloc.rate                     thrpt    5    847.681 ±  28.690  MB/sec
SpringPooling.simple_thr:·gc.alloc.rate.norm                thrpt    5     96.102 ±   0.871    B/op
SpringPooling.simple_thr:·gc.churn.PS_Eden_Space            thrpt    5    853.020 ±  60.844  MB/sec
SpringPooling.simple_thr:·gc.churn.PS_Eden_Space.norm       thrpt    5     96.710 ±   7.104    B/op
SpringPooling.simple_thr:·gc.churn.PS_Survivor_Space        thrpt    5      0.321 ±   0.160  MB/sec
SpringPooling.simple_thr:·gc.churn.PS_Survivor_Space.norm   thrpt    5      0.036 ±   0.018    B/op
SpringPooling.simple_thr:·gc.count                          thrpt    5    197.000            counts
SpringPooling.simple_thr:·gc.time                           thrpt    5     55.000                ms

----

SpringPooling.builder_avg                                    avgt    5      0.666 ±   0.017   us/op
SpringPooling.builder_avg:·gc.alloc.rate                     avgt    5   1571.912 ±  53.751  MB/sec
SpringPooling.builder_avg:·gc.alloc.rate.norm                avgt    5    160.000 ±   0.001    B/op
SpringPooling.builder_avg:·gc.churn.PS_Eden_Space            avgt    5   1580.900 ±  67.039  MB/sec
SpringPooling.builder_avg:·gc.churn.PS_Eden_Space.norm       avgt    5    160.918 ±   5.896    B/op
SpringPooling.builder_avg:·gc.churn.PS_Survivor_Space        avgt    5      0.753 ±   0.324  MB/sec
SpringPooling.builder_avg:·gc.churn.PS_Survivor_Space.norm   avgt    5      0.077 ±   0.034    B/op
SpringPooling.builder_avg:·gc.count                          avgt    5    302.000            counts
SpringPooling.builder_avg:·gc.time                           avgt    5     85.000                ms

SpringPooling.pooled_avg                                     avgt    5      0.739 ±   0.082   us/op
SpringPooling.pooled_avg:·gc.alloc.rate                      avgt    5      0.003 ±   0.001  MB/sec
SpringPooling.pooled_avg:·gc.alloc.rate.norm                 avgt    5     ≈ 10⁻³              B/op
SpringPooling.pooled_avg:·gc.count                           avgt    5        ≈ 0            counts

SpringPooling.simple_avg                                     avgt    5      0.725 ±   0.010   us/op
SpringPooling.simple_avg:·gc.alloc.rate                      avgt    5    872.445 ±  30.258  MB/sec
SpringPooling.simple_avg:·gc.alloc.rate.norm                 avgt    5     96.000 ±   0.001    B/op
SpringPooling.simple_avg:·gc.churn.PS_Eden_Space             avgt    5    878.034 ±  45.729  MB/sec
SpringPooling.simple_avg:·gc.churn.PS_Eden_Space.norm        avgt    5     96.628 ±   7.765    B/op
SpringPooling.simple_avg:·gc.churn.PS_Survivor_Space         avgt    5      0.318 ±   0.301  MB/sec
SpringPooling.simple_avg:·gc.churn.PS_Survivor_Space.norm    avgt    5      0.035 ±   0.034    B/op
SpringPooling.simple_avg:·gc.count                           avgt    5    200.000            counts
SpringPooling.simple_avg:·gc.time                            avgt    5     57.000                ms


*/