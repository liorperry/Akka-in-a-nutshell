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
public class TestTradingServices {


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
    public void testAccountQuery() {
        /*
         * Wrap the whole test procedure within a testkit constructor
         * if you want to receive actor replies or use Within(), etc.
         */
        new JavaTestKit(system) {{
            final AccountCreation dummy = new AccountCreation("TestDummy", 100);

            final ActorRef subject  = system.actorOf(Props.create(TradingProcessor.class).
                    withRouter(new ConsistentHashingRouter(1).withHashMapper(TradingProcessor.hashMapper)),"TradingRouter:testAccountQuery");
            // “inject” the probe by passing it to the test subject
            // like a real resource would be passed in production
            subject.tell(dummy, getRef());
            // await the correct response
            expectMsgClass(duration("2 second"), AccountStatusResponse.class);
//            expectMsgAllOf(duration("1 second"), new AccountStatusResponse(new Account(dummy.getName(),dummy.getBalance())));

            subject.tell(new AccountStatusQuery(dummy.getName()),getRef());
            // await the correct response
            expectMsgClass(duration("1 second"), AccountStatusResponse.class);

        }};
    }


    @Test
    public void testRouterCreateAccount() {
        /*
         * Wrap the whole test procedure within a testkit constructor
         * if you want to receive actor replies or use Within(), etc.
         */
        new JavaTestKit(system) {{
        //workshop actor router
            final ActorRef subject  = system.actorOf(Props.create(TradingProcessor.class).
                    withRouter(new ConsistentHashingRouter(1).withHashMapper(TradingProcessor.hashMapper)),"TradingRouter");
            // “inject” the probe by passing it to the test subject
            // like a real resource would be passed in production
            for (int i = 0; i < 10; i++) {
                final AccountCreation dummy = new AccountCreation("TestDummy:" + i, 100 + i);
                subject.tell(dummy, getRef());
                // await the correct response
//                expectMsgAllOf(duration("1 second"), new AccountStatusResponse(new Account(dummy.getName(),dummy.getBalance())));
                expectMsgAnyClassOf(duration("1 second"), AccountStatusResponse.class);
            }

            for (int i = 0; i < 10; i++) {
                final AccountStatusQuery dummy = new AccountStatusQuery("TestDummy:" + i);
                subject.tell(dummy, getRef());
                // await the correct response
                expectMsgAnyClassOf(duration("1 second"), AccountStatusResponse.class);
            }

        }};
    }

    @Test
    public void testAccountRates() {
        /*
         * Wrap the whole test procedure within a testkit constructor
         * if you want to receive actor replies or use Within(), etc.
         */
        new JavaTestKit(system) {{
            //workshop actor router
            final ActorRef subject  = system.actorOf(Props.create(TradingProcessor.class).
                    withRouter(new ConsistentHashingRouter(1).withHashMapper(TradingProcessor.hashMapper)),"TradingRouter:testAccountRates");
            // “inject” the probe by passing it to the test subject
            // like a real resource would be passed in production
            for (int i = 0; i < 10; i++) {
                final AccountCreation dummy = new AccountCreation("TestDummy:" + i, 100 + i);
                subject.tell(dummy, getRef());
                // await the correct response
                expectMsgAnyClassOf(duration("1 second"), AccountStatusResponse.class);
//                expectMsgAllOf(duration("1 second"), new AccountStatusResponse(new Account(dummy.getName(),dummy.getBalance())));
            }

            for (int i = 0; i < 10; i++) {
                final AccountStatusQuery dummy = new AccountStatusQuery("TestDummy:" + i);
                subject.tell(dummy, getRef());
                // await the correct response
                expectMsgAnyClassOf(duration("1 second"), AccountStatusResponse.class);
            }

            //publish rate stream
            SymbolStream stream = RateStreamBuilder.initialStream();
            system.eventStream().publish(stream);
            expectNoMsg(duration("1 second"));

        }};
    }

    @Test
    public void testAccountDealOpening() {
        /*
         * Wrap the whole test procedure within a testkit constructor
         * if you want to receive actor replies or use Within(), etc.
         */
        new JavaTestKit(system) {{
            //tick actor
            final ActorRef rateStreamer = system.actorOf(Props.create(new RateStreamer.RateStreamerCreator(system.eventStream())));

            final ActorRef subject  = system.actorOf(Props.create(TradingProcessor.class).
                    withRouter(new ConsistentHashingRouter(1).withHashMapper(TradingProcessor.hashMapper)),"TradingRouter:testAccountDealOpening");

            final AccountCreation dummy = new AccountCreation("TestDummy", 1000);
            // “inject” the probe by passing it to the test subject
            // like a real resource would be passed in production
            subject.tell(dummy, getRef());
            // await the correct response
            //todo check why this failes
            expectMsgClass(duration("2 second"), AccountStatusResponse.class);

            //publish rate stream
            SymbolStream stream = RateStreamBuilder.initialStream();
            rateStreamer.tell("Tick",ActorRef.noSender());
            expectNoMsg(duration("3 second"));

            //request deal opening
            final DealOpening dealOpening = new DealOpening(dummy.getName(), Symbol.AUDCAD, Deal.DealType.BUY, BigDecimal.valueOf(10), new Date());
            subject.tell(dealOpening,getRef());

            final Deal deal = new ExpectMsg<Deal>("match hint") {
                  protected Deal match(Object in) {
                    if (in instanceof Deal)  {
                      return (Deal) in;
                    } else {
                      throw noMatch();
                    }
                  }
                }.get(); // this extracts the received message

            // update rate stream
            stream = RateStreamBuilder.buildTimelyStream(stream);
            system.eventStream().publish(stream);
            expectNoMsg(duration("1 second"));

            subject.tell(new AccountStatusQuery("TestDummy"), getRef());
            final AccountStatusResponse out = new ExpectMsg<AccountStatusResponse>("match hint") {
                  protected AccountStatusResponse match(Object in) {
                    if (in instanceof AccountStatusResponse)  {
                      return (AccountStatusResponse) in;
                    } else {
                      throw noMatch();
                    }
                  }
                }.get();

            final SymbolPosition symbol = stream.getSymbol(Symbol.AUDCAD);
            final BigDecimal calculateValue = Deal.calculateValue(deal, symbol);
            assertEquals(out.getAccount().getBalance(), BigDecimal.valueOf(dummy.getBalance()).add(calculateValue));
        }};
    }

    @Test
    public void testAccountCalibration() {
        /*
         * Wrap the whole test procedure within a testkit constructor
         * if you want to receive actor replies or use Within(), etc.
         */
        new JavaTestKit(system) {{
            final ActorRef rateStreamer = system.actorOf(Props.create(new RateStreamer.RateStreamerCreator(system.eventStream())));
            final AccountCreation dummy = new AccountCreation("TestDummy", 100);

            final ActorRef subject  = system.actorOf(Props.create(TradingProcessor.class).
                    withRouter(new ConsistentHashingRouter(1).withHashMapper(TradingProcessor.hashMapper)),"TradingRouter_testAccountCalibration");
            // “inject” the probe by passing it to the test subject
            // like a real resource would be passed in production
            subject.tell(dummy, getRef());
            // await the correct response
            expectMsgClass(duration("2 second"), AccountStatusResponse.class);
//            expectMsgAllOf(duration("1 second"), new AccountStatusResponse(new Account(dummy.getName(),dummy.getBalance())));

            subject.tell(new AccountStatusQuery(dummy.getName()),getRef());
            // await the correct response
            expectMsgClass(duration("1 second"), AccountStatusResponse.class);

            //publish rate stream
            SymbolStream stream = RateStreamBuilder.initialStream();

            rateStreamer.tell("Tick",ActorRef.noSender());
            expectNoMsg(duration("2 second"));

            //request deal opening
            final DealOpening dealOpening = new DealOpening(dummy.getName(), Symbol.AUDCAD, Deal.DealType.BUY, BigDecimal.valueOf(10), new Date());
            subject.tell(dealOpening,getRef());

            final Deal deal = new ExpectMsg<Deal>("match hint") {
                  protected Deal match(Object in) {
                    if (in instanceof Deal)  {
                      return (Deal) in;
                    } else {
                      throw noMatch();
                    }
                  }
                }.get(); // this extracts the received message

            // update rate stream
            stream = RateStreamBuilder.buildTimelyStream(stream);
            system.eventStream().publish(stream);
            expectNoMsg(duration("1 second"));

            subject.tell(new AccountStatusQuery("TestDummy"), getRef());
            final AccountStatusResponse out = new ExpectMsg<AccountStatusResponse>("match hint") {
                  protected AccountStatusResponse match(Object in) {
                    if (in instanceof AccountStatusResponse)  {
                      return (AccountStatusResponse) in;
                    } else {
                      throw noMatch();
                    }
                  }
                }.get();

            final SymbolPosition symbol = stream.getSymbol(Symbol.AUDCAD);
            final BigDecimal calculateValue = Deal.calculateValue(deal, symbol);
            assertEquals(out.getAccount().getBalance(), BigDecimal.valueOf(dummy.getBalance()).add(calculateValue));
        }};
    }
}
