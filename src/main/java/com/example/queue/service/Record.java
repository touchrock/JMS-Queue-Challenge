package com.example.queue.service;

public class Record {
	
	public static final String DELIMITER = "|:|";
	
	private long timePolledInNano = 0l;
	private String message;
	
	public Record(String message) {
		this.message = message;
	}
	
	public long getTimePolledInNano() {
		return timePolledInNano;
	}

	public void setTimePolledInNano(long timePolledInNano) {
		this.timePolledInNano = timePolledInNano;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	public static Record parseRecord(String record) {
		
		Record returnRecord = null;
		
		if (record == null || record.isEmpty()) {
			throw new IllegalArgumentException("Input can't be null or an empty string.");
		}
		
		String[] parts = record.split("\\|:\\|");
		
		if (parts.length != 2) {
			throw new IllegalArgumentException("The record needs to have the pattern millis |:| message, but was " + record);
		}
		else {
			returnRecord = new Record(parts[1]);
			returnRecord.setTimePolledInNano(Long.parseLong(parts[0]));
		}
		
		return returnRecord;
	}
	
	@Override
	public int hashCode() {
		
		final int prime = 31;
		int result = 1;
		result = prime * result + ((message == null) ? 0 : message.hashCode());
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
		Record other = (Record) obj;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		return true;
	}

	public String toString() {
		
		return timePolledInNano + DELIMITER + message;
	}

}
