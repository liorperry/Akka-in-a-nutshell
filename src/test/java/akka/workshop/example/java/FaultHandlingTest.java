package akka.workshop.example.java;

/**
 * Created by lior.perry on 15/06/2014.
 */

import akka.actor.*;

import static akka.actor.SupervisorStrategy.resume;
import static akka.actor.SupervisorStrategy.restart;
import static akka.actor.SupervisorStrategy.stop;
import static akka.actor.SupervisorStrategy.escalate;

import akka.actor.SupervisorStrategy.Directive;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import akka.workshop.example.java.actors.TradingProcessor;
import akka.workshop.example.java.messages.AccountCreation;
import akka.workshop.example.java.messages.AccountDeposit;
import akka.workshop.example.java.messages.AccountStatusQuery;
import akka.workshop.example.java.messages.AccountStatusResponse;
import akka.workshop.example.java.utils.forex.Account;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import scala.collection.immutable.Seq;
import scala.concurrent.Await;

import static akka.pattern.Patterns.ask;

import scala.concurrent.duration.Duration;
import akka.testkit.TestProbe;
import scala.concurrent.duration.FiniteDuration;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

public class FaultHandlingTest {
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
    @Ignore
    public void mustEmploySupervisorStrategy() throws Exception {
        // code here
        new JavaTestKit(system) {
            {
                Props superprops = Props.create(TradingProcessor.class);
                TestActorRef subject = TestActorRef.create(system, superprops, "TradingProcessor");
                String accountName = "TestDummy_Actor";
                final AccountCreation dummy = new AccountCreation(accountName, 100);

                // “inject” the probe by passing it to the test subject
                // like a real resource would be passed in production
                subject.tell(dummy, getRef());
                expectMsgAnyOf(new FiniteDuration(100, TimeUnit.MILLISECONDS), new Account(dummy.getName(), dummy.getBalance()));
                subject.tell(new AccountDeposit(accountName, BigDecimal.valueOf(-1)),getRef());
                //expecting actor being resumed by supervisor
//                expectNoMsg(new FiniteDuration(100, TimeUnit.MILLISECONDS));
                subject.tell(new AccountStatusQuery(accountName),getRef());
                expectMsgAnyClassOf(new FiniteDuration(1000, TimeUnit.MILLISECONDS), AccountStatusResponse.class);
            }
        };

    }
}
