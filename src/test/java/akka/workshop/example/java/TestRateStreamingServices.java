package akka.workshop.example.java;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.routing.ConsistentHashingRouter;
import akka.testkit.JavaTestKit;
import akka.workshop.example.java.actors.RateStreamer;
import akka.workshop.example.java.actors.TradingProcessor;
import akka.workshop.example.java.messages.AccountCreation;
import akka.workshop.example.java.messages.AccountStatusQuery;
import akka.workshop.example.java.messages.AccountStatusResponse;
import akka.workshop.example.java.messages.DealOpening;
import akka.workshop.example.java.utils.forex.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Date;

import static junit.framework.Assert.assertEquals;


/**
 * Created by user on 12/02/14.
 */
public class TestRateStreamingServices {


    static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void teardown() {
        JavaTestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void testAccountRateStreaming() {
        /*
         * Wrap the whole test procedure within a testkit constructor
         * if you want to receive actor replies or use Within(), etc.
         */
        new JavaTestKit(system) {{
            //tick actor
            final ActorRef rateStreamer = system.actorOf(Props.create(new RateStreamer.RateStreamerCreator(system.eventStream())));
            //workshop actor router
            final ActorRef subject  = system.actorOf(Props.create(TradingProcessor.class).
                    withRouter(new ConsistentHashingRouter(10).withHashMapper(TradingProcessor.hashMapper)),"TradingRouter");
            // “inject” the probe by passing it to the test subject
            // like a real resource would be passed in production
            for (int i = 0; i < 3; i++) {
                final AccountCreation dummy = new AccountCreation("TestDummy:" + i, 100 + i);
                subject.tell(dummy, getRef());
                // await the correct response
//                expectMsgAllOf(duration("1 second"), new AccountStatusResponse(new Account(dummy.getName(),dummy.getBalance())));
                expectMsgAnyClassOf(duration("1 second"), AccountStatusResponse.class);
            }

            for (int i = 0; i < 3; i++) {
                final AccountStatusQuery dummy = new AccountStatusQuery("TestDummy:" + i);
                subject.tell(dummy, getRef());
                // await the correct response
                expectMsgAnyClassOf(duration("1 second"), AccountStatusResponse.class);
            }

            rateStreamer.tell("Tick",ActorRef.noSender());
            expectNoMsg(duration("1 second"));

        }};
    }
}
