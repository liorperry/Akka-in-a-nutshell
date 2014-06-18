package akka.workshop.example.java.actors;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.workshop.example.java.messages.DealClosed;
import akka.workshop.example.java.messages.DealClosingWithPosition;
import akka.workshop.example.java.messages.DealOpeningWithPosition;
import akka.workshop.example.java.utils.forex.Deal;
import akka.workshop.example.java.utils.forex.DealServices;
import akka.workshop.example.java.utils.forex.SymbolPosition;
import akka.workshop.example.java.utils.forex.SymbolStream;

import java.math.BigDecimal;
import java.util.Date;

public class DealProcessor extends UntypedActor implements DealServices{
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private Deal deal;

    public void onReceive(Object message) {
        log.debug(message.toString());
        if (message instanceof SymbolStream) {
            final BigDecimal value = applyRates((SymbolStream) message);
            getSender().tell(value, getSelf());
        } else if (message instanceof DealClosingWithPosition) {
            //validate deal closing with correct rate
            SymbolPosition symbol = ((DealClosingWithPosition) message).getSymbol();
            BigDecimal revenue = Deal.calculateValue(deal, symbol);
            DealClosed dealClosed = new DealClosed(((DealClosingWithPosition) message).getDealId(), ((DealClosingWithPosition) message).getCloseDate(), revenue);
            getSender().tell(dealClosed,getSelf());
        } else if (message instanceof DealOpeningWithPosition) {
            final DealOpeningWithPosition dealOpening = (DealOpeningWithPosition) message;
            deal = addDeal(dealOpening.getSymbol(), dealOpening.getAmount(), dealOpening.getType(), dealOpening.getOpenDate());
            getSender().tell(deal, getSelf());
        } else {
            unhandled(message);
        }
    }

    @Override
    public void postStop() throws Exception {
//    todo cleaning stuff
        log.info("Deal "+deal.getId() +" is terminating");
    }

    @Override
    public Deal addDeal(SymbolPosition symbol, BigDecimal amount, Deal.DealType type, Date date) {
        return new Deal(symbol,amount,type, date);
    }

    @Override
    public Deal closeDeal(String dealId) {
        //@todo
        return null;
    }

    @Override
    public BigDecimal applyRates(SymbolStream stream) {
        BigDecimal bigDecimal = Deal.calculateValue(deal, stream.getSymbol(deal.getSymbol()));
        log.info(Thread.currentThread().getName() +" Deal ["+deal.getId() +"] value:"+bigDecimal);
        return bigDecimal;
    }
}