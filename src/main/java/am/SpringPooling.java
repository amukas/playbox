package am;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.profile.HotspotCompilationProfiler;
import org.openjdk.jmh.profile.HotspotMemoryProfiler;
import org.openjdk.jmh.profile.HotspotRuntimeProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static java.lang.ThreadLocal.withInitial;

@Fork(3)
@Warmup(time = 5)
@Measurement(time = 5)
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
                .addProfiler(HotspotRuntimeProfiler.class)
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
    private final String asString;
    private final static ThreadLocal<SomeComposite[]> cache = withInitial(() -> new SomeComposite[100]);

    public SomeComposite(String s1, int i1, String s2) {
        this.s1 = s1;
        this.i1 = i1;
        this.s2 = s2;
        this.asString = s1 + i1 + s2;
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
                Objects.equals(s1, that.s1) &&
                Objects.equals(s2, that.s2);
    }

    @Override
    public int hashCode() {
        return this.asString.hashCode();
    }

    public String asString() {
        return this.asString;
    }
}


/*

# JMH version: 1.20
# VM version: JDK 1.8.0_172-ea, VM 25.172-b03

Benchmark                                                    Mode  Cnt      Score     Error     Units
SpringPooling.builder_thr                                   thrpt    5  14222.191 ± 225.519    ops/ms
SpringPooling.builder_thr:·gc.alloc.rate                    thrpt    5   1975.631 ±  32.876    MB/sec
SpringPooling.builder_thr:·gc.alloc.rate.norm               thrpt    5    160.000 ±   0.001      B/op
SpringPooling.builder_thr:·gc.churn.PS_Eden_Space           thrpt    5   1990.853 ±  32.149    MB/sec
SpringPooling.builder_thr:·gc.churn.PS_Eden_Space.norm      thrpt    5    161.233 ±   0.895      B/op
SpringPooling.builder_thr:·gc.churn.PS_Survivor_Space       thrpt    5      0.924 ±   0.215    MB/sec
SpringPooling.builder_thr:·gc.churn.PS_Survivor_Space.norm  thrpt    5      0.075 ±   0.017      B/op
SpringPooling.builder_thr:·gc.count                         thrpt    5   1342.000              counts
SpringPooling.builder_thr:·gc.time                          thrpt    5    416.000                  ms
SpringPooling.builder_thr:·rt.safepointSyncTime             thrpt    5      1.600                  ms
SpringPooling.builder_thr:·rt.safepointTime                 thrpt    5      5.201                  ms
SpringPooling.builder_thr:·rt.safepoints                    thrpt    5   2696.000              counts
SpringPooling.builder_thr:·rt.sync.contendedLockAttempts    thrpt    5    410.000               locks
SpringPooling.builder_thr:·rt.sync.fatMonitors              thrpt    5    512.000            monitors
SpringPooling.builder_thr:·rt.sync.futileWakeups            thrpt    5     70.000              counts
SpringPooling.builder_thr:·rt.sync.monitorDeflations        thrpt    5     86.000            monitors
SpringPooling.builder_thr:·rt.sync.monitorInflations        thrpt    5     88.000            monitors
SpringPooling.builder_thr:·rt.sync.notifications            thrpt    5    142.000              counts
SpringPooling.builder_thr:·rt.sync.parks                    thrpt    5    545.000              counts

SpringPooling.pooled_thr                                    thrpt    5  13920.134 ±  78.680    ops/ms
SpringPooling.pooled_thr:·gc.alloc.rate                     thrpt    5      0.001 ±   0.001    MB/sec
SpringPooling.pooled_thr:·gc.alloc.rate.norm                thrpt    5     ≈ 10⁻⁴                B/op
SpringPooling.pooled_thr:·gc.count                          thrpt    5        ≈ 0              counts
SpringPooling.pooled_thr:·rt.safepointSyncTime              thrpt    5      0.006                  ms
SpringPooling.pooled_thr:·rt.safepointTime                  thrpt    5      0.009                  ms
SpringPooling.pooled_thr:·rt.safepoints                     thrpt    5     19.000              counts
SpringPooling.pooled_thr:·rt.sync.contendedLockAttempts     thrpt    5    108.000               locks
SpringPooling.pooled_thr:·rt.sync.fatMonitors               thrpt    5    384.000            monitors
SpringPooling.pooled_thr:·rt.sync.futileWakeups             thrpt    5      3.000              counts
SpringPooling.pooled_thr:·rt.sync.monitorDeflations         thrpt    5     78.000            monitors
SpringPooling.pooled_thr:·rt.sync.monitorInflations         thrpt    5     80.000            monitors
SpringPooling.pooled_thr:·rt.sync.notifications             thrpt    5     41.000              counts
SpringPooling.pooled_thr:·rt.sync.parks                     thrpt    5    136.000              counts

SpringPooling.simple_thr                                    thrpt    5  15505.966 ± 138.718    ops/ms
SpringPooling.simple_thr:·gc.alloc.rate                     thrpt    5   1290.685 ±   9.684    MB/sec
SpringPooling.simple_thr:·gc.alloc.rate.norm                thrpt    5     96.000 ±   0.001      B/op
SpringPooling.simple_thr:·gc.churn.PS_Eden_Space            thrpt    5   1302.258 ±  19.991    MB/sec
SpringPooling.simple_thr:·gc.churn.PS_Eden_Space.norm       thrpt    5     96.861 ±   0.922      B/op
SpringPooling.simple_thr:·gc.churn.PS_Survivor_Space        thrpt    5      0.344 ±   0.122    MB/sec
SpringPooling.simple_thr:·gc.churn.PS_Survivor_Space.norm   thrpt    5      0.026 ±   0.009      B/op
SpringPooling.simple_thr:·gc.count                          thrpt    5    929.000              counts
SpringPooling.simple_thr:·gc.time                           thrpt    5    270.000                  ms
SpringPooling.simple_thr:·rt.safepointSyncTime              thrpt    5      1.234                  ms
SpringPooling.simple_thr:·rt.safepointTime                  thrpt    5      3.635                  ms
SpringPooling.simple_thr:·rt.safepoints                     thrpt    5   1886.000              counts
SpringPooling.simple_thr:·rt.sync.contendedLockAttempts     thrpt    5    277.000               locks
SpringPooling.simple_thr:·rt.sync.fatMonitors               thrpt    5    512.000            monitors
SpringPooling.simple_thr:·rt.sync.futileWakeups             thrpt    5     24.000              counts
SpringPooling.simple_thr:·rt.sync.monitorDeflations         thrpt    5     86.000            monitors
SpringPooling.simple_thr:·rt.sync.monitorInflations         thrpt    5     88.000            monitors
SpringPooling.simple_thr:·rt.sync.notifications             thrpt    5    148.000              counts
SpringPooling.simple_thr:·rt.sync.parks                     thrpt    5    393.000              counts

SpringPooling.builder_avg                                    avgt    5      0.712 ±   0.005     us/op
SpringPooling.builder_avg:·gc.alloc.rate                     avgt    5   1976.014 ±   2.906    MB/sec
SpringPooling.builder_avg:·gc.alloc.rate.norm                avgt    5    160.000 ±   0.001      B/op
SpringPooling.builder_avg:·gc.churn.PS_Eden_Space            avgt    5   1989.939 ±   2.829    MB/sec
SpringPooling.builder_avg:·gc.churn.PS_Eden_Space.norm       avgt    5    161.128 ±   0.033      B/op
SpringPooling.builder_avg:·gc.churn.PS_Survivor_Space        avgt    5      0.993 ±   0.161    MB/sec
SpringPooling.builder_avg:·gc.churn.PS_Survivor_Space.norm   avgt    5      0.080 ±   0.013      B/op
SpringPooling.builder_avg:·gc.count                          avgt    5   1341.000              counts
SpringPooling.builder_avg:·gc.time                           avgt    5    399.000                  ms
SpringPooling.builder_avg:·rt.safepointSyncTime              avgt    5      1.426                  ms
SpringPooling.builder_avg:·rt.safepointTime                  avgt    5      4.911                  ms
SpringPooling.builder_avg:·rt.safepoints                     avgt    5   2708.000              counts
SpringPooling.builder_avg:·rt.sync.contendedLockAttempts     avgt    5    396.000               locks
SpringPooling.builder_avg:·rt.sync.fatMonitors               avgt    5    384.000            monitors
SpringPooling.builder_avg:·rt.sync.futileWakeups             avgt    5     44.000              counts
SpringPooling.builder_avg:·rt.sync.monitorDeflations         avgt    5     86.000            monitors
SpringPooling.builder_avg:·rt.sync.monitorInflations         avgt    5     88.000            monitors
SpringPooling.builder_avg:·rt.sync.notifications             avgt    5    131.000              counts
SpringPooling.builder_avg:·rt.sync.parks                     avgt    5    516.000              counts

SpringPooling.pooled_avg                                     avgt    5      0.714 ±   0.013     us/op
SpringPooling.pooled_avg:·gc.alloc.rate                      avgt    5      0.001 ±   0.001    MB/sec
SpringPooling.pooled_avg:·gc.alloc.rate.norm                 avgt    5     ≈ 10⁻⁴                B/op
SpringPooling.pooled_avg:·gc.count                           avgt    5        ≈ 0              counts
SpringPooling.pooled_avg:·rt.safepointSyncTime               avgt    5      0.008                  ms
SpringPooling.pooled_avg:·rt.safepointTime                   avgt    5      0.011                  ms
SpringPooling.pooled_avg:·rt.safepoints                      avgt    5     21.000              counts
SpringPooling.pooled_avg:·rt.sync.contendedLockAttempts      avgt    5    137.000               locks
SpringPooling.pooled_avg:·rt.sync.fatMonitors                avgt    5    384.000            monitors
SpringPooling.pooled_avg:·rt.sync.futileWakeups              avgt    5      3.000              counts
SpringPooling.pooled_avg:·rt.sync.monitorDeflations          avgt    5     84.000            monitors
SpringPooling.pooled_avg:·rt.sync.monitorInflations          avgt    5     86.000            monitors
SpringPooling.pooled_avg:·rt.sync.notifications              avgt    5     57.000              counts
SpringPooling.pooled_avg:·rt.sync.parks                      avgt    5    171.000              counts
SpringPooling.simple_avg                                     avgt    5      0.681 ±   0.008     us/op
SpringPooling.simple_avg:·gc.alloc.rate                      avgt    5   1224.744 ±  17.981    MB/sec
SpringPooling.simple_avg:·gc.alloc.rate.norm                 avgt    5     96.000 ±   0.001      B/op
SpringPooling.simple_avg:·gc.churn.PS_Eden_Space             avgt    5   1234.670 ±  28.853    MB/sec
SpringPooling.simple_avg:·gc.churn.PS_Eden_Space.norm        avgt    5     96.778 ±   1.314      B/op
SpringPooling.simple_avg:·gc.churn.PS_Survivor_Space         avgt    5      0.311 ±   0.198    MB/sec
SpringPooling.simple_avg:·gc.churn.PS_Survivor_Space.norm    avgt    5      0.024 ±   0.015      B/op
SpringPooling.simple_avg:·gc.count                           avgt    5    883.000              counts
SpringPooling.simple_avg:·gc.time                            avgt    5    317.000                  ms
SpringPooling.simple_avg:·rt.safepointSyncTime               avgt    5      1.424                  ms
SpringPooling.simple_avg:·rt.safepointTime                   avgt    5      3.960                  ms
SpringPooling.simple_avg:·rt.safepoints                      avgt    5   1811.000              counts
SpringPooling.simple_avg:·rt.sync.contendedLockAttempts      avgt    5    254.000               locks
SpringPooling.simple_avg:·rt.sync.fatMonitors                avgt    5    512.000            monitors
SpringPooling.simple_avg:·rt.sync.futileWakeups              avgt    5     25.000              counts
SpringPooling.simple_avg:·rt.sync.monitorDeflations          avgt    5     81.000            monitors
SpringPooling.simple_avg:·rt.sync.monitorInflations          avgt    5     83.000            monitors
SpringPooling.simple_avg:·rt.sync.notifications              avgt    5    130.000              counts
SpringPooling.simple_avg:·rt.sync.parks                      avgt    5    361.000              counts

*/