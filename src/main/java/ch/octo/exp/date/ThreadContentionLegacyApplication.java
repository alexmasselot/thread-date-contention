package ch.octo.exp.date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadContentionLegacyApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadContentionLegacyApplication.class);


    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            LOGGER.error("must pass two arguments: filename log_2(nbDates)");
            System.exit(1);
        }
        String filename = args[0];
        int nDates = (int) Math.pow(2, Integer.parseInt(args[1]));


        LOGGER.info("output filename=" + filename);
        LOGGER.info("nDates=" + nDates + " (2**" + args[1] + ")");
        ThreadContentionLegacyBenchmark bench = new ThreadContentionLegacyBenchmark(nDates, 3, filename);

        bench.go();


    }
}
