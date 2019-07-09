package com.example.queue.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MessageQueue {
	
	private List<Record> messages = Collections.synchronizedList(new ArrayList<Record>());
	private String queueName;
	
	public MessageQueue(String queueName) {
		
		this.queueName = queueName;
	}
	
	public void addMessage(String message) {
		
		Record record = new Record(message);
		
		messages.add(record);
	}
	
	public void setMessages(List<Record> messages) {
		
		this.messages = messages;
	}
	
	public List<Record> getMessages() {
		
		return messages;
	}

	@Override
	public int hashCode() {
		
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((queueName == null) ? 0 : queueName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MessageQueue other = (MessageQueue) obj;
		if (queueName == null) {
			if (other.queueName != null)
				return false;
		} else if (!queueName.equals(other.queueName))
			return false;
		return true;
	}
}
