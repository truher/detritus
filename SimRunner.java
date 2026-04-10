package ffm_test;

/**
 * Run the simulation in a loop and time it.
 */
public class SimRunner {
    private static final long DT_MS = 20;
    private static final long DT_NS = DT_MS * 1000000;
    private final Stats stats;

    public SimRunner() {
        stats = new Stats();
    }

    void run() {
        System.out.printf(".");
        System.out.flush();
    }

    void timed() {
        long t0_ns = System.nanoTime();
        run();
        long t1_ns = System.nanoTime();
        long et_ns = t1_ns - t0_ns;
        System.out.printf("et (ms) %7.5f\n", (double) et_ns / 1000000);
        stats.update(et_ns);
    }

    public static void main(String[] args) throws Throwable {
        SimRunner sim = new SimRunner();
        long start_ns = System.nanoTime();
        for (int i = 0; i < 1000; ++i) {
            long deadline_ns = start_ns + DT_NS;
            sim.timed();
            long now_ns = System.nanoTime();
            long slack_ms = (deadline_ns - now_ns) / 1000000;
            if (slack_ms > 0) {
                // sleep until the deadline
                Thread.sleep(slack_ms);
                start_ns = deadline_ns;
            } else {
                // overran, reset
                start_ns = now_ns;
            }
        }
        System.out.println();
        System.out.printf("ET mean (ms) %7.5f\n", sim.stats.mean() / 1000000);
        System.out.printf("ET stddev (ms) %7.5f\n", sim.stats.populationStdev() / 1000000);
    }

}
