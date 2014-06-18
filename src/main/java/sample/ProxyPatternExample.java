package sample;

/**
 * Created by lior.perry on 18/06/2014.
 */

import akka.actor.*;
import akka.contrib.pattern.ReliableProxy;
import akka.japi.Creator;
import akka.workshop.example.HelloAkka;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;


public class ProxyPatternExample {

    public static class Meeter extends UntypedActor {
        private ActorRef proxy;
        public String meeting = "";

        public Meeter(ActorPath targetPath) {
            proxy = getContext().actorOf(
                    ReliableProxy.props(targetPath,
                            Duration.create(100, TimeUnit.MILLISECONDS), Duration.create(100, TimeUnit.MILLISECONDS), 5));
        }

        public void onReceive(Object message) {
            if (message instanceof HelloAkka.WhoToGreet)
                meeting = "hello, we have a meeting with " + ((HelloAkka.WhoToGreet) message).who;

            else if (message instanceof HelloAkka.Greet)
                // Send the current greeting back to the sender
                proxy.tell(new HelloAkka.Greet(meeting), getSender());

            else unhandled(message);
        }

        public static Props props(final ActorPath targetPath) {
            return Props.create(new Creator<Meeter>() {
                private static final long serialVersionUID = 1L;

                @Override
                public Meeter create() throws Exception {
                    return new Meeter(targetPath);
                }
            });
        }

    }

    public static void main(String[] args) {
        // Create the 'helloakka' actor system
        final ActorSystem system = ActorSystem.create("hello-akka");

        // Create the 'greeter' actor
        final ActorRef greeter = system.actorOf(Props.create(HelloAkka.Greeter.class), "greeter");
        final ActorRef meeter = system.actorOf(Meeter.props(greeter.path()), "meeter");

        // Tell the 'greeter' to change its 'greeting' message
        greeter.tell(new HelloAkka.WhoToGreet("akka"), ActorRef.noSender());

        // Ask the 'greeter for the latest 'greeting'
        // Reply should go to the "actor-in-a-box"
        meeter.tell(new HelloAkka.WhoToGreet("Jhone"), ActorRef.noSender());

        // Create the "actor-in-a-box"
        final Inbox inbox = Inbox.create(system);
        // Ask the 'greeter for the latest 'greeting'
        // Reply should go to the "actor-in-a-box"
        inbox.send(meeter, new HelloAkka.Greet());
        // Wait 5 seconds for the reply with the 'greeting' message
        HelloAkka.Greeting greeting1 = (HelloAkka.Greeting) inbox.receive(Duration.create(5, TimeUnit.SECONDS));
        System.out.println("Greeting: " + greeting1.message);

    }
}
