package org.gearman.helpers;

import org.gearman.GearmanJobEvent;
import org.gearman.GearmanJobReturn;

public class GearmanClientUtils {
	
	public static byte[] simpleJobReturnHandler(GearmanJobReturn jobReturn) throws Exception {
		while(!jobReturn.isEOF()) {
			GearmanJobEvent event = jobReturn.poll();
			
			switch(event.getEventType()) {
			
			// return value
			case GEARMAN_JOB_SUCCESS:
				return event.getData();
			
			// ignore events
			case GEARMAN_JOB_DATA:
			case GEARMAN_JOB_STATUS:
			case GEARMAN_JOB_WARNING:
			case GEARMAN_SUBMIT_SUCCESS:
				break;
			
			// exception event
			case GEARMAN_JOB_FAIL:
			case GEARMAN_SUBMIT_FAIL:
				throw new Exception(event.getEventType() + ": " + new String(event.getData()));
			case GEARMAN_EOF:
			default:
				throw new IllegalStateException("Unexspected Event Type: " + event.getEventType());
			}
		}
		
		throw new IllegalStateException();
	}
}
