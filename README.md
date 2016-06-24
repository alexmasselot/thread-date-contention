# A benchmark to date getOffset method

Run three flavors of getOffset:

 * `java.util.SimpleTimeZone` with day light saving
 * `java.util.SimpleTimeZone` without day light saving
 * `joda.time`
 
The goal is to measure the efficiency of "old fashion" java multi threading

## how to run?

    git clone https://github.com/alexmasselot/thread-date-contention.git
    cd thread-date-contention
    mvn package
  
    # first argument is the produced tsv file
    # second arguments is log_2 size of the data array
    java target/thread-contention-0.0.2-SNAPSHOT.jar /tmp/measures.txt 16