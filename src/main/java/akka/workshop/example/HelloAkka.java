package akka.workshop.example;

/**
 * Created by lior.perry on 17/06/2014.
 */

import akka.actor.*;
import com.google.common.base.Optional;
import scala.concurrent.duration.Duration;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class HelloAkka {

    public static class Greet implements Serializable {
        public Optional<String> what;

        public Greet() {
            what = Optional.absent();
        }

        public Greet(String what) {
            this.what = Optional.of(what);
        }

    }

    public static class WhoToGreet implements Serializable {
        public final String who;

        public WhoToGreet(String who) {
            this.who = who;
        }
    }

    public static class Greeting implements Serializable {
        public final String message;

        public Greeting(String message) {
            this.message = message;
        }
    }

    public static class Greeter extends UntypedActor {
        public String greeting = "";

        public void onReceive(Object message) {
            if (message instanceof WhoToGreet)
                greeting = "hello, " + ((WhoToGreet) message).who;

            else if (message instanceof Greet) {
                // Send the current greeting back to the sender
                Optional<String> what = ((Greet) message).what;
                if(what.isPresent()) {
                    greeting = what.get() +" " +greeting;
                }
                getSender().tell(new Greeting(greeting), getSelf());

            } else unhandled(message);
        }
    }

    public static void main(String[] args) {
        // Create the 'helloakka' actor system
        final ActorSystem system = ActorSystem.create("hello-akka");

        // Create the 'greeter' actor
        final ActorRef greeter = system.actorOf(Props.create(Greeter.class), "greeter");

        // Create the "actor-in-a-box"
        final Inbox inbox = Inbox.create(system);

        // Tell the 'greeter' to change its 'greeting' message
        greeter.tell(new WhoToGreet("akka"), ActorRef.noSender());

        // Ask the 'greeter for the latest 'greeting'
        // Reply should go to the "actor-in-a-box"
        inbox.send(greeter, new Greet());

        // Wait 5 seconds for the reply with the 'greeting' message
        Greeting greeting1 = (Greeting) inbox.receive(Duration.create(5, TimeUnit.SECONDS));
        System.out.println("Greeting: " + greeting1.message);

        // Change the greeting and ask for it again
        greeter.tell(new WhoToGreet("typesafe"), ActorRef.noSender());
        inbox.send(greeter, new Greet());

        Greeting greeting2 = (Greeting) inbox.receive(Duration.create(5, TimeUnit.SECONDS));
        System.out.println("Greeting: " + greeting2.message);

        // after zero seconds, send a Greet message every second to the greeter with a sender of the GreetPrinter
        ActorRef greetPrinter = system.actorOf(Props.create(GreetPrinter.class));
        system.scheduler().schedule(Duration.Zero(), Duration.create(1, TimeUnit.SECONDS), greeter, new Greet(), system.dispatcher(), greetPrinter);
    }

    public static class GreetPrinter extends UntypedActor {
        public void onReceive(Object message) {
            if (message instanceof Greeting)
                System.out.println(((Greeting) message).message);
        }
    }
}

