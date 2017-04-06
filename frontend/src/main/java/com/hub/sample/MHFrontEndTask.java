package com.hub.sample;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.LinkedList;

 

import com.ibm.mqlight.api.ClientException;
import com.ibm.mqlight.api.ClientOptions;
import com.ibm.mqlight.api.CompletionListener;
import com.ibm.mqlight.api.NonBlockingClient;
import com.ibm.mqlight.api.NonBlockingClientAdapter;
import com.ibm.mqlight.api.QOS;
import com.ibm.mqlight.api.SendOptions;
import com.ibm.mqlight.api.SendOptions.SendOptionsBuilder;
import com.ibm.mqlight.api.DestinationAdapter;
import com.ibm.mqlight.api.StringDelivery;
import com.ibm.mqlight.api.JsonDelivery;
import com.ibm.mqlight.api.BytesDelivery;
import com.ibm.mqlight.api.MalformedDelivery;
import com.ibm.mqlight.api.Delivery;
import org.springframework.stereotype.Component;

import com.google.gson.*;

/**
 * This file forms part of the MQ Light Sample Messaging Application - Worker offload pattern sample
 * provided to demonstrate the use of the IBM Bluemix MQ Light Service.
 * 
 * It provides a simple JAX-RS REST service that:
 * - Publishes messages to a back-end worker upon a REST POST
 * - Consumes notifications from the back-end asynchronously upon a REST GET
 * 
 * The REST interface is very simple, using plain text.
 * The posted text is split into separate words, and each sent to the back-end in a separate message.
 * The responses are returned one at a time to REST GET requests. 
 */
/**
 * Servlet implementation class FrontEndServlet
 */
 
@Component
public class MHFrontEndTask{
  
  /** The topic we publish on to send data to the back-end */
  private static final String PUBLISH_TOPIC = "mqlight/sample/words";
  
  /** The topic we subscribe on to receive notifications from the back-end */
  private static final String SUBSCRIBE_TOPIC = "mqlight/sample/wordsuppercase";
  
  /** Simple logging */
  private final static Logger logger = Logger.getLogger(MHFrontEndTask.class.getName());
  
  /** JVM-wide initialisation of our subscription */
  private static boolean subInitialised = false;

  /** Client that will send and receive messages */
  private NonBlockingClient mqlightClient;

  /** Holds the messages we've recieved from the backend */
  private LinkedList<String> receivedMessages;
  
    /**
     * Default Constructor
     */
    public MHFrontEndTask() {
      logger.log(Level.INFO, "Initialising...");
      receivedMessages = new LinkedList<String>();
        
      try {

        logger.log(Level.INFO,"Creating an MQ Light client...");

        String service = null;
        if (System.getenv("VCAP_SERVICES") == null) {
          service = "amqp://localhost:5672";
        }
        
        mqlightClient = NonBlockingClient.create(service, new NonBlockingClientAdapter<Void>() {

          @Override
          public void onStarted(NonBlockingClient client, Void context) {
            System.out.printf("Connected to %s using client-id %s\n", client.getService(), client.getId());

            client.subscribe(SUBSCRIBE_TOPIC, new DestinationAdapter<Void>() {
              public void onMessage(NonBlockingClient client, Void context, Delivery delivery) {
                logger.log(Level.INFO,"Received message of type: " + delivery.getType());
                switch (delivery.getType()) {
                  case JSON:
                    JsonDelivery jd = (JsonDelivery)delivery;
                    logger.log(Level.INFO,"Data: " + jd.getData());
                    receivedMessages.add(jd.getData().toString());
                    break;
                  case STRING:
                    StringDelivery sd = (StringDelivery)delivery;
                    logger.log(Level.INFO,"Data: " + sd.getData());
                    receivedMessages.add(sd.getData());
                    break;
                  case BYTES:
                    BytesDelivery bd = (BytesDelivery)delivery;
                    logger.log(Level.INFO,"Data: " + bd.getData());
                    receivedMessages.add(bd.getData().toString());
                    break;
                  case MALFORMED:
                    MalformedDelivery md = (MalformedDelivery)delivery;
                    logger.log(Level.WARNING,"Malformed message: " + md.getDescription());
                    receivedMessages.add("MALFORMED");
                    break;
                }                
              }
            }, new CompletionListener<Void>() {
              @Override
              public void onSuccess(NonBlockingClient c, Void ctx) {
                logger.log(Level.INFO, "Subscribed!");
                subInitialised = true;
              }
              @Override
              public void onError(NonBlockingClient c, Void ctx, Exception exception) {
                logger.log(Level.SEVERE, "Exception while subscribing. ", exception);
              }
            }, null);
          }

          @Override
          public void onRetrying(NonBlockingClient client, Void context, ClientException throwable) {
              System.out.println("*** error ***");
              if (throwable != null) System.err.println(throwable.getMessage());
          }

          @Override
          public void onStopped(NonBlockingClient client, Void context, ClientException throwable) {
              if (throwable != null) {
                  System.err.println("*** error ***");
                  System.err.println(throwable.getMessage());
              }
              logger.log(Level.INFO,"MQ Light client stopped.");
          }
        }, null);
        logger.log(Level.INFO,"MQ Light client created. Current state: " + mqlightClient.getState());
      }
      catch (Exception e) {
        logger.log(Level.SEVERE, "Failed to initialise", e);
        throw new RuntimeException(e);
      }
      logger.log(Level.INFO,"Completed initialisation.");
    }

	public boolean getSubInitialised(){
		return subInitialised;
	}
	
	public NonBlockingClient getMQClient(){
		return mqlightClient;
	}
	
	public LinkedList<String> getReceivedMessages(){
		return receivedMessages;
	}
	
   
    
}
