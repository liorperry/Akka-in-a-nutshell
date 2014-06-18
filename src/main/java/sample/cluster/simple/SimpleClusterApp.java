package sample.cluster.simple;

import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.contrib.pattern.ClusterSingletonManager;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.workshop.example.java.messages.AccountCommand;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class SimpleClusterApp {

    public static void main(String[] args) {
        if (args.length == 0)
            startup(new String[]{"2551", "2552", "0"});
        else
            startup(args);
    }

    public static class Consumer extends UntypedActor {
        LoggingAdapter log = Logging.getLogger(getContext().system(), this);

        public void onReceive(Object message) {
            if (message instanceof AccountCommand)
                log.info(message + "Singleton Consumed " + getSender());
            else unhandled(message);
        }
    }


    public static void startup(String[] ports) {
        ConfigFactory.load("application.conf");
        for (String port : ports) {
            // Override the configuration of the port
            Config config = ConfigFactory.parseString(
                    "akka.remote.netty.tcp.port=" + port).withFallback(
                    ConfigFactory.load());

            // Create an Akka system
            ActorSystem system = ActorSystem.create("ClusterSystem", config);

            // Create an actor that handles cluster domain events
            system.actorOf(Props.create(SimpleClusterListener.class),
                    "clusterListener");

            system.actorOf(ClusterSingletonManager.defaultProps(Props.create(Consumer.class), "consumer",
                    PoisonPill.getInstance(), "worker"), "singleton");
        }
    }
}
