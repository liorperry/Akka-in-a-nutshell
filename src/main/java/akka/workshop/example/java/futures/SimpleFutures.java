package akka.workshop.example.java.futures;

/**
 * Created by lior.perry on 15/06/2014.
 */

import static akka.dispatch.Futures.future;
import static akka.dispatch.Futures.sequence;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import akka.dispatch.OnSuccess;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

import scala.concurrent.duration.Duration;
import akka.dispatch.ExecutionContexts;
import akka.dispatch.Mapper;

public class SimpleFutures {
    public static void main(String[] args) {

        ExecutorService executor = Executors.newFixedThreadPool(4);
        ExecutionContext ec = ExecutionContexts.fromExecutorService(executor);

        ArrayList<Future<Long>> futures = new ArrayList<>();

        System.out.println("Akka Futures says: Adding futures for two random length pauses");

        futures.add(future(new RandomPause(), ec));
        futures.add(future(new RandomPause(), ec));

        System.out.println("Akka Futures says: There are " + futures.size()
                + " RandomPause's currently running");

        // compose a sequence of the futures
        Future<Iterable<Long>> futuresSequence = sequence(futures, ec);

        // Find the sum of the odd numbers
        Future<Long> futureSum = futuresSequence.map(
                new Mapper<Iterable<Long>, Long>() {
                    public Long apply(Iterable<Long> ints) {
                        long sum = 0;
                        for (Long i : ints)
                            sum += i;
                        return sum;
                    }
                }, ec);

        // block until the futures come back
        futureSum.onSuccess(new OnSuccess<Long>() {
            public void onSuccess(Long value) {
                System.out.println("PrintResults says: Total pause was for " + (value)
                        + " milliseconds");
            }
        }, ec);

        try {
            System.out.println("Result :" + Await.result(futureSum, Duration.apply(5, TimeUnit.SECONDS)));
        } catch (Exception e) {
            e.printStackTrace();
        }

        executor.shutdown();
    }

}

