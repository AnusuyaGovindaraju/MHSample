package com.hub.sample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
 


@RequestMapping("/mh")
@RestController
public class MHController {
	
	private static final Logger logger = LoggerFactory.getLogger(MHController.class);
	
	StringBuffer responseBack=null;
	
	@RequestMapping(value = "/test", method = RequestMethod.GET)
	public String pushNotification( ) {
		return "it worked!!!!";
	}

}