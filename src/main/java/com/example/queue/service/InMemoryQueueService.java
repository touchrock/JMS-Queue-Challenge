package com.example.queue.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.example.queue.service.exception.QueueNotFoundException;

public class InMemoryQueueService implements QueueService {
	
	//All methods that touch this object must be synchronized for multi-threaded instances
	private List<MessageQueue> queues = new ArrayList<MessageQueue>();
	
	private Clock clock;
	
	//Default timer to 30 seconds
	private long timerLength = TimeUnit.SECONDS.convert(30, TimeUnit.NANOSECONDS);
	
	public final static String TIMER_LENGTH_PROP = "Timer.Length.Nano";
	
	public InMemoryQueueService(Clock clock) {
		
		this.clock = clock;
		
		String timerLengthStr = System.getProperty(TIMER_LENGTH_PROP);
		
		if (timerLengthStr != null && !timerLengthStr.isEmpty()) {
			try {
				timerLength = Long.parseLong(timerLengthStr);
			}
			catch (NumberFormatException e) {
				//Move forward with default timer value.
				System.out.println("The System Property = " + TIMER_LENGTH_PROP + " should be a number, but was " + timerLengthStr);
			}
		}
	}

	public synchronized void push(String queueName, String message) {
		
		MessageQueue queue = null;
		
		try {
			queue = getMessageQueue(queueName);
		}
		catch (QueueNotFoundException e) {
			queue = new MessageQueue(queueName);
			queues.add(queue);
		}
		
		queue.addMessage(message);
	}
	
	public synchronized String pull(String queueName) throws QueueNotFoundException {
		
		MessageQueue queue = getMessageQueue(queueName);
		
		List<Record> messages = queue.getMessages();
		
		for (Record message : messages) {
			
			if (message.getTimePolledInNano() != 0l) {
				if (message.getTimePolledInNano() + timerLength < clock.getTimeInNano()) {
					
					message.setTimePolledInNano(clock.getTimeInNano());
					return message.getMessage();
				}
			}
			else {
			
				message.setTimePolledInNano(clock.getTimeInNano());
				return  message.getMessage();
			}	
		}
		
		return "";
	}

	public synchronized void delete(String queueName, String message) throws QueueNotFoundException {
		
		MessageQueue queue = getMessageQueue(queueName);
		Record record = new Record(message);
		
		queue.getMessages().remove(record);
	}
	
	private synchronized MessageQueue getMessageQueue(String queueName) throws QueueNotFoundException {
		
		MessageQueue queue = new MessageQueue(queueName);
		
		if (queues.contains(queue)) {
			queue = queues.get(queues.indexOf(queue));
		}
		else {
			throw new QueueNotFoundException("Queue named : " + queueName + " was not found.");
		}
		
		return queue;
	}
}
