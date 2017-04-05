package com.hub.sample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;

@RequestMapping("/mh")
@RestController
public class MHController {
	
	private static final Logger logger = LoggerFactory.getLogger(MHController.class);
	
	
	/** The topic we publish on to send data to the back-end */
	private static final String PUBLISH_TOPIC = "mqlight/sample/words";
  
	/** The topic we subscribe on to receive notifications from the back-end */
	private static final String SUBSCRIBE_TOPIC = "mqlight/sample/wordsuppercase";
  
	
	/** JVM-wide initialisation of our subscription */
	private static boolean subInitialised = false;

	/** Client that will send and receive messages */
	private NonBlockingClient mqlightClient;

	/** Holds the messages we've recieved from the backend */
	private LinkedList<String> receivedMessages;
	
	
	@Autowired
    private MHFrontEndTask objFrontEndTask;
  
	
	StringBuffer responseBack=null;
	
	@RequestMapping(value = "/test", method = RequestMethod.GET)
	public String pushNotification( ) {
		return "it worked!!!!";
	}
	
	/**
     * POST on the words resource publishes the content of the POST to our topic
     */
    
	@RequestMapping(value = "/words", method = RequestMethod.POST, produces = { MediaType.APPLICATION_JSON_VALUE}, consumes = MediaType.ALL_VALUE)
     public ResponseEntity publishWords(Map<String, String> jsonInput) {
        
      logger.info("Publishing words" + jsonInput);
      
      // Check the caller supplied some words
      String words = jsonInput.get("words");
      if (words == null) throw new RuntimeException("No words sent");

      // Before we connect to publish, we need to ensure our subscription has been
      // initialised, otherwise we might miss responses.
      while (!(objFrontEndTask.getSubInitialised())) {
        try {
          logger.info("Sleep for a sec while we wait for sub to complete");
          Thread.sleep(1000);
        } catch (InterruptedException ie) {
          ie.printStackTrace();
        }
      }
	  SendOptions opts = SendOptions.builder().setQos(QOS.AT_LEAST_ONCE).build();

      // We send a separate message for each word in the request
      StringTokenizer strtok = new StringTokenizer(words, " ");
      int tokens = strtok.countTokens();
      while (strtok.hasMoreTokens()) {

        String word = strtok.nextToken();
        com.google.gson.JsonObject message = new com.google.gson.JsonObject();
        message.addProperty("word", word);
        message.addProperty("frontend", "JavaAPI: " + toString());

        mqlightClient.send(PUBLISH_TOPIC, message.toString(), null, opts, new CompletionListener<Void>() {
          public void onSuccess(NonBlockingClient client, Void context) {
            logger.info("Client id: " + client.getId() + " sent message!");
          }
          public void onError(NonBlockingClient client, Void context, Exception exception) {
            logger.info("Error!." + exception.toString());
          }
        }, null);
      }

      HashMap<String, Integer> jsonOutput = new HashMap<>();
      jsonOutput.put("msgCount", tokens);
	  
	  return new ResponseEntity<HashMap<String, Integer>>(jsonOutput, HttpStatus.OK);

    }
	
	/**
     * GET on the wordsuppercase resource checks for any publications that have been
     * returned on our subscription. Replies with either a single word, or a 204 (No Content).
     * Call repeatedly to get all the responses
     */
    
	@RequestMapping(value = "/wordsuppercase", method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE}, consumes = MediaType.ALL_VALUE)
	 public ResponseEntity checkForPublications() {
      // Delegate to a static method synchronized across the JVM
      return singleThreadedCheckForPublications();
    }
    
  /**
   * A synchronized method for consuming messages we have received.
   */
    private synchronized ResponseEntity singleThreadedCheckForPublications() {
      ResponseEntity response;
      if (receivedMessages == null || receivedMessages.isEmpty()) {
        response = new ResponseEntity(null, HttpStatus.OK);
        return response;
      }
      String msg = receivedMessages.remove();
      response =new ResponseEntity(msg, HttpStatus.OK); 
      return response;
    }
    

}