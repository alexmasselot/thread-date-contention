package ch.octo.exp.date;

import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class ThreadContentionLegacyBenchmark {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadContentionLegacyBenchmark.class);

    private final int nDates;
    private final int nMeasures;
    private final String filename;


    int allreadyProcessed = 0;
    final Object experience = new Object();

    public ThreadContentionLegacyBenchmark(int nDates, int nMeasures, String filename) {
        this.nDates = nDates;
        this.nMeasures = nMeasures;
        this.filename = filename;
    }

    /** launch the actual measures*/
    public  void go() throws IOException, InterruptedException {
        Writer writer = initWriter(filename);
        benchmark(writer, new JodaTZ(), nMeasures, 100, 1024);
        benchmark(writer, new SimpleTZ(true), nMeasures, 100, 1024);
        benchmark(writer, new SimpleTZ(false), nMeasures, 1000, 1024);

        writer.close();
    }

    //java.util & joda.time timzone don't share much. So we make them do it.
    interface TZ {
        int getOffset(Long date);

        String getName();
    }

    static class SimpleTZ implements TZ {
        final private SimpleTimeZone simpleTimeZone;
        final String name;

        public SimpleTZ(boolean hasDST) {
            SimpleTimeZone stz = new SimpleTimeZone(3600 * 1000, "CET");
            if (hasDST) {
                stz.setStartRule(Calendar.MARCH, 1, Calendar.MONDAY, 3 * 60 * 60 * 1000);
                stz.setEndRule(Calendar.NOVEMBER, -1, Calendar.SUNDAY, 3 * 60 * 60 * 1000);
                this.name = "util.SimpleTimeZone/with DST";
            } else {
                this.name = "util.SimpleTimeZone/no DST";
            }
            this.simpleTimeZone = stz;
        }

        @Override
        public int getOffset(Long date) {
            return simpleTimeZone.getOffset(date);
        }

        @Override
        public String getName() {
            return name;
        }
    }

    static class JodaTZ implements TZ {
        final private DateTimeZone dateTimeZone;

        public JodaTZ() {
            this.dateTimeZone = DateTimeZone.forID("Australia/Melbourne");
        }

        @Override
        public int getOffset(Long date) {
            return dateTimeZone.getOffset(date);
        }

        @Override
        public String getName() {
            return "joda.DateTimeZone";
        }
    }

    private synchronized void processed(int newDates) {
        allreadyProcessed += newDates;
        if (allreadyProcessed == nDates) {
            synchronized (experience) {
                experience.notify();
            }
        }
    }

    Runnable getTimeZoneRunnable(final List<Long> dates, final int nTime, final TZ tz) {
        return new Runnable() {
            @Override
            public void run() {
                long t = 0L;
                for (int i = 0; i < nTime; i++) {
                    for (Long date : dates) {
                        t += tz.getOffset(date);
                    }
                }
                processed(dates.size());
            }
        };
    }


    Writer initWriter(String filename) throws IOException {
        Writer writer = new FileWriter(filename);
        writer.write("mode\ti.exp\tnb.dates\tnb.loops\tnb.threads\tt.ms\n");
        return writer;
    }

    void benchmark(Writer writer, TZ tz, int nMeasures, int nLoops, int maxThread) throws InterruptedException, IOException {
        for (int iMeasure = 0; iMeasure < nMeasures; iMeasure++) {
            LOGGER.info(tz.getName() + ": " + (iMeasure + 1) + "/" + nMeasures);
            List<Long> dates = new ArrayList<>(nDates);
            Random random = new Random();
            for (int i = 0; i < nDates; i++) {
                dates.add(random.nextLong());
            }
            LOGGER.info(tz.getName() + ": end radomization");
            for (int nThreads = 1; nThreads <= maxThread; nThreads *= 2) {
                experience(writer, tz, dates, iMeasure, nLoops, nThreads);
            }
        }
    }

    void experience(Writer writer, TZ tz, List<Long> allDates, int iMeasure, int nLoops, int nThreads) throws InterruptedException, IOException {
        List<Runnable> runnables = new ArrayList<>();

        int datesPerThreads = allDates.size() / nThreads;
        for (int i = 0; i < nThreads; i++) {
            List<Long> dates = allDates.subList(i * datesPerThreads, Math.min((i + 1) * datesPerThreads, allDates.size()));
            runnables.add(new Thread(getTimeZoneRunnable(dates, nLoops, tz)));
        }
        long t0 = System.currentTimeMillis();

        for (Runnable r : runnables) {
            new Thread(r).start();
        }
        synchronized (experience) {
            LOGGER.info("All threads started, waiting");
            experience.wait();
        }
        long t1 = System.currentTimeMillis();
        writer.write(tz.getName() + "\t" + iMeasure + "\t" + nDates + "\t" + nLoops + "\t" + nThreads + "\t" + (t1 - t0) + "\n");
        LOGGER.info("nThreads=" + nThreads + " processed " + allreadyProcessed + " dates in " + (t1 - t0) + " ms");
        synchronized (this) {
            allreadyProcessed = 0;
        }
    }

}
