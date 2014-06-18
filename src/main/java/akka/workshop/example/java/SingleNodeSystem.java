package akka.workshop.example.java;

import akka.actor.*;
import akka.routing.ConsistentHashingRouter;
import akka.workshop.example.java.actors.AccountProcessor;
import akka.workshop.example.java.actors.RateStreamer;
import akka.workshop.example.java.actors.TradingProcessor;
import akka.workshop.example.java.messages.AccountCreation;
import akka.workshop.example.java.utils.forex.SymbolStream;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

/**
 * Created by user on 11/02/14.
 */
public class SingleNodeSystem {
    public static void main(String[] args) throws InterruptedException {
        // Create an Akka system
        ActorSystem system = ActorSystem.create("SingleNodeTradingSystem");

        // create the router actor
        final ActorRef subject = system.actorOf(Props.create(TradingProcessor.class).
                withRouter(new ConsistentHashingRouter(10).withHashMapper(TradingProcessor.hashMapper)), "TradingRouter");

        //tick actor
        final ActorRef rateStreamer = system.actorOf(Props.create(new RateStreamer.RateStreamerCreator(system.eventStream())));

        //begin ticker & get cancellable
        final Cancellable tick = system.scheduler().schedule(Duration.Zero(),
                Duration.create(500, TimeUnit.MILLISECONDS), rateStreamer, "Tick",
                system.dispatcher(), system.deadLetters());


        for (int i = 0; i < 10; i++) {
            String accountName = "TestDummy_" + i;
            final AccountCreation dummy = new AccountCreation(accountName, 100);
            // “inject” the probe by passing it to the test subject
            // like a real resource would be passed in production
            subject.tell(dummy, system.deadLetters());
            Thread.sleep(1000);
        }


    }
}
