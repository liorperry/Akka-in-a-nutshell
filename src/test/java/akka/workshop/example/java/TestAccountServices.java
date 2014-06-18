package akka.workshop.example.java;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import akka.workshop.example.java.actors.AccountProcessor;
import akka.workshop.example.java.actors.TradingProcessor;
import akka.workshop.example.java.messages.*;
import akka.workshop.example.java.utils.forex.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static junit.framework.Assert.assertEquals;


/**
 * Created by user on 12/02/14.
 */
public class TestAccountServices {


    public static final int DEALS_SIZE = 10;
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
    public void testAccountQuery() {
        /*
         * Wrap the whole test procedure within a testkit constructor
         * if you want to receive actor replies or use Within(), etc.
         */
        new JavaTestKit(system) {{
            final AccountCreation dummy = new AccountCreation("TestDummy", 100);

            final Props props = Props.create(TradingProcessor.class);
            final ActorRef subject = system.actorOf(props);
            // “inject” the probe by passing it to the test subject
            // like a real resource would be passed in production
            subject.tell(dummy, getRef());
            // await the correct response
            final Account expected = new Account(dummy.getName(), dummy.getBalance());
            expectNoMsg(duration("1 second"));

            subject.tell(new AccountStatusQuery(dummy.getName()),getRef());
            // await the correct response
//            expectMsgAnyOf(duration("1 second"), new Deal(stream.getSymbol(Symbol.AUDCAD), 100, Deal.DealType.BUY));

            final AccountStatusResponse out = new ExpectMsg<AccountStatusResponse>("match hint") {
                protected AccountStatusResponse match(Object in) {
                    if (in instanceof Account)  {
                        return (AccountStatusResponse) in;
                    } else {
                        throw noMatch();
                    }
                }
            }.get(); // this extracts the received message

            assertEquals(expected, out);

        }};
    }

    @Test
    public void testRegistration() {
        /*
         * Wrap the whole test procedure within a testkit constructor
         * if you want to receive actor replies or use Within(), etc.
         */
        new JavaTestKit(system) {{
            final AccountCreation dummy = new AccountCreation("TestDummy", 100);
            final Props props = AccountProcessor.props(dummy);
            final ActorRef subject = system.actorOf(props);
            // “inject” the probe by passing it to the test subject
            // like a real resource would be passed in production
            subject.tell(new AccountCreation("TestDummy", 100), getRef());
            // await the correct response
//            expectMsgAllOf(duration("1 second"), new Account("TestDummy", 100));
            expectMsgAnyClassOf(duration("1 second"), AccountStatusResponse.class);

        }};
    }


    @Test
    public void testDealOpening() {
        /*
         * Wrap the whole test procedure within a testkit constructor
         * if you want to receive actor replies or use Within(), etc.
         */
        new JavaTestKit(system) {{
            final AccountCreation dummy = new AccountCreation("TestDummy:testDealOpening", 100);
            final Props props = AccountProcessor.props(dummy);
            final ActorRef subject = system.actorOf(props, dummy.getName());
            // “inject” the probe by passing it to the test subject
            // like a real resource would be passed in production
            subject.tell(dummy, getRef());
            // await the correct response
            expectMsgAnyClassOf(duration("1 second"), AccountStatusResponse.class);
//            expectMsgAllOf(duration("1 second"), new Account("TestDummy", 100));

            //publish rate stream
            final SymbolStream stream = RateStreamBuilder.initialStream();
            subject.tell(stream, getRef());
            //not expecting answer (via event stream)
            expectNoMsg();

            //request deal opening
            final DealOpening dealOpening = new DealOpening(dummy.getName(), Symbol.AUDCAD, Deal.DealType.BUY, BigDecimal.valueOf(100), new Date());
            subject.tell(dealOpening, getRef());
            // await the correct response
//            expectMsgAnyOf(duration("1 second"), new Deal(stream.getSymbol(Symbol.AUDCAD), 100, Deal.DealType.BUY));

            final Deal out = new ExpectMsg<Deal>("match hint") {
                protected Deal match(Object in) {
                    if (in instanceof Deal) {
                        return (Deal) in;
                    } else {
                        throw noMatch();
                    }
                }
            }.get(); // this extracts the received message

            assertEquals(new Deal(stream.getSymbol(dealOpening.getSymbol()), dealOpening.getAmount(), dealOpening.getType(), dealOpening.getOpenDate()), out);

        }};
    }


    @Test
    public void testDealRateUpdates() {
        /*
         * Wrap the whole test procedure within a testkit constructor
         * if you want to receive actor replies or use Within(), etc.
         */
        new JavaTestKit(system) {{
            final AccountCreation dummy = new AccountCreation("TestDummy", 100);
            final Props props = AccountProcessor.props(dummy);
            final ActorRef subject = system.actorOf(props, dummy.getName());
            // “inject” the probe by passing it to the test subject
            // like a real resource would be passed in production
            subject.tell(dummy, getRef());
            // await the correct response
//            expectMsgAnyOf(duration("1 second"), new AccountStatusResponse(new Account(dummy.getName(), dummy.getBalance())));
            expectMsgAnyClassOf(duration("1 second"), AccountStatusResponse.class);

            //publish rate stream
            SymbolStream stream = RateStreamBuilder.initialStream();
            subject.tell(stream, getRef());
            //not expecting answer (via event stream)
            expectNoMsg();

            //request deal opening
            DealOpening dealOpening = new DealOpening(dummy.getName(), Symbol.AUDCAD, Deal.DealType.BUY, BigDecimal.valueOf(15), new Date());
            subject.tell(dealOpening, getRef());

            Deal deal = new ExpectMsg<Deal>("match hint") {
                protected Deal match(Object in) {
                    if (in instanceof Deal) {
                        return (Deal) in;
                    } else {
                        throw noMatch();
                    }
                }
            }.get(); // this extracts the received message


            // update rate stream
            stream = RateStreamBuilder.buildTimelyStream(stream);
            subject.tell(stream, getRef());
            //not expecting answer (via event stream)
            expectNoMsg(duration("1 second"));

            subject.tell(new AccountStatusQuery("TestDummy"), getRef());
            final AccountStatusResponse out = new ExpectMsg<AccountStatusResponse>("match hint") {
                protected AccountStatusResponse match(Object in) {
                    if (in instanceof AccountStatusResponse) {
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
    public void testDealOpeningAndClosing() {
        /*
         * Wrap the whole test procedure within a testkit constructor
         * if you want to receive actor replies or use Within(), etc.
         */
        new JavaTestKit(system) {{
            final AccountCreation dummy = new AccountCreation("TestDummy:testDealOpeningAndClosing", 100);
            final Props props = AccountProcessor.props(dummy);
            final ActorRef subject = system.actorOf(props, dummy.getName());
            // “inject” the probe by passing it to the test subject
            // like a real resource would be passed in production
            subject.tell(dummy, getRef());
            // await the correct response
//            expectMsgAnyOf(duration("1 second"), new AccountStatusResponse(new Account(dummy.getName(), dummy.getBalance())));
            expectMsgAnyClassOf(duration("1 second"), AccountStatusResponse.class);

            //publish rate stream
            SymbolStream stream = RateStreamBuilder.initialStream();
            subject.tell(stream, getRef());
            //not expecting answer (via event stream)
            expectNoMsg();

            //request deal opening
            DealOpening dealOpening = new DealOpening(dummy.getName(), Symbol.AUDCAD, Deal.DealType.BUY, BigDecimal.valueOf(15), new Date());
            subject.tell(dealOpening, getRef());

            Deal deal = new ExpectMsg<Deal>("match hint") {
                protected Deal match(Object in) {
                    if (in instanceof Deal) {
                        return (Deal) in;
                    } else {
                        throw noMatch();
                    }
                }
            }.get(); // this extracts the received message


            // update rate stream
            stream = RateStreamBuilder.buildTimelyStream(stream);
            subject.tell(stream, getRef());
            //not expecting answer (via event stream)
            expectNoMsg(duration("1 second"));

            subject.tell(new AccountStatusQuery("TestDummy:testDealOpeningAndClosing"), getRef());
            final AccountStatusResponse out = new ExpectMsg<AccountStatusResponse>("match hint") {
                protected AccountStatusResponse match(Object in) {
                    if (in instanceof AccountStatusResponse) {
                        return (AccountStatusResponse) in;
                    } else {
                        throw noMatch();
                    }
                }
            }.get();

            final SymbolPosition symbol = stream.getSymbol(Symbol.AUDCAD);
            final BigDecimal calculateValue = Deal.calculateValue(deal, symbol);
            assertEquals(out.getAccount().getBalance(), BigDecimal.valueOf(dummy.getBalance()).add(calculateValue));
            expectNoMsg(duration("1 second"));

            subject.tell(new DealClosing(dummy.getName(), deal.getId(), new Date()), getRef());
            expectNoMsg(duration("3 second"));
        }};
    }

    @Test
    public void testDoubleDealRateUpdates() {
        /*
         * Wrap the whole test procedure within a testkit constructor
         * if you want to receive actor replies or use Within(), etc.
         */
        new JavaTestKit(system) {{
            final AccountCreation dummy = new AccountCreation("TestDummy", 100);
            final Props props = AccountProcessor.props(dummy);
            final ActorRef subject = system.actorOf(props);
            // “inject” the probe by passing it to the test subject
            // like a real resource would be passed in production
            subject.tell(dummy, getRef());
            // await the correct response
//            expectMsgAnyOf(duration("1 second"), new Account(dummy.getName(), dummy.getBalance()));
            expectMsgAnyClassOf(duration("1 second"), AccountStatusResponse.class);

            //publish rate stream
            SymbolStream stream = RateStreamBuilder.initialStream();
            subject.tell(stream, getRef());
            //not expecting answer (via event stream)
            expectNoMsg();

            //request deal opening
            DealOpening dealOpening = new DealOpening(dummy.getName(), Symbol.AUDCAD, Deal.DealType.BUY, BigDecimal.valueOf(10), new Date());
            subject.tell(dealOpening, getRef());

            Deal deal1 = new ExpectMsg<Deal>("match hint") {
                protected Deal match(Object in) {
                    if (in instanceof Deal) {
                        return (Deal) in;
                    } else {
                        throw noMatch();
                    }
                }
            }.get(); // this extracts the received message


            //request deal opening
            dealOpening = new DealOpening(dummy.getName(), Symbol.EURAUD, Deal.DealType.SELL, BigDecimal.valueOf(5), new Date());
            subject.tell(dealOpening, getRef());

            Deal deal2 = new ExpectMsg<Deal>("match hint") {
                protected Deal match(Object in) {
                    if (in instanceof Deal) {
                        return (Deal) in;
                    } else {
                        throw noMatch();
                    }
                }
            }.get(); // this extracts the received message

            // update rate stream
            stream = RateStreamBuilder.buildTimelyStream(stream);
            subject.tell(stream, getRef());
            //not expecting answer (via event stream)
            expectNoMsg(duration("1 second"));

            subject.tell(new AccountStatusQuery("TestDummy"), getRef());
            final AccountStatusResponse out = new ExpectMsg<AccountStatusResponse>("match hint") {
                protected AccountStatusResponse match(Object in) {
                    if (in instanceof AccountStatusResponse) {
                        return (AccountStatusResponse) in;
                    } else {
                        throw noMatch();
                    }
                }
            }.get();

            SymbolPosition symbol = stream.getSymbol(Symbol.AUDCAD);
            final BigDecimal calculateValue1 = Deal.calculateValue(deal1, symbol);
            symbol = stream.getSymbol(Symbol.EURAUD);
            final BigDecimal calculateValue2 = Deal.calculateValue(deal2, symbol);
            assertEquals(out.getAccount().getBalance(), BigDecimal.valueOf(dummy.getBalance()).add(calculateValue1).add(calculateValue2));
        }};
    }

    @Test
    public void testTenDealRateUpdates() {
        final List<Deal> deals = new ArrayList<>();
        /*
         * Wrap the whole test procedure within a testkit constructor
         * if you want to receive actor replies or use Within(), etc.
         */
        new JavaTestKit(system) {
            {
                final AccountCreation dummy = new AccountCreation("TestDummy", 100);
                final Props props = AccountProcessor.props(dummy);
                final ActorRef subject = system.actorOf(props);
                // “inject” the probe by passing it to the test subject
                // like a real resource would be passed in production
                subject.tell(dummy, getRef());
                // await the correct response
//                expectMsgAllOf(duration("1 second"), new Account(dummy.getName(), dummy.getBalance()));
                expectMsgAnyClassOf(duration("1 second"), AccountStatusResponse.class);

                //publish rate stream
                SymbolStream stream = RateStreamBuilder.initialStream();
                subject.tell(stream, getRef());
                //not expecting answer (via event stream)
                expectNoMsg();

                for (int i = 0; i < DEALS_SIZE; i++) {
                    //request deal opening
                    DealOpening dealOpening = new DealOpening(dummy.getName(), Symbol.AUDCAD, Deal.DealType.BUY, BigDecimal.valueOf(i), new Date());
                    subject.tell(dealOpening, getRef());

                    deals.add(new ExpectMsg<Deal>("match hint") {
                        protected Deal match(Object in) {
                            if (in instanceof Deal) {
                                return (Deal) in;
                            } else {
                                throw noMatch();
                            }
                        }
                    }.get()); // this extracts the received message

                }

                // update rate stream
                stream = RateStreamBuilder.buildTimelyStream(stream);
                subject.tell(stream, getRef());
                //not expecting answer (via event stream)
                expectNoMsg(duration("1 second"));

                subject.tell(new AccountStatusQuery("TestDummy"), getRef());
                final AccountStatusResponse out = new ExpectMsg<AccountStatusResponse>("match hint") {
                    protected AccountStatusResponse match(Object in) {
                        if (in instanceof AccountStatusResponse) {
                            return (AccountStatusResponse) in;
                        } else {
                            throw noMatch();
                        }
                    }
                }.get();

                BigDecimal sum = BigDecimal.ZERO;
                for (int i = 0; i < DEALS_SIZE; i++) {
                    sum = sum.add(Deal.calculateValue(deals.get(i), stream.getSymbol(deals.get(i).getSymbol())));
                }
                assertEquals(out.getAccount().getBalance(), BigDecimal.valueOf(dummy.getBalance()).add(sum));
            }
        }

        ;
    }

}
