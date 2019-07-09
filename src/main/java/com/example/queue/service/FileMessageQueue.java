package com.example.queue.service;

import java.io.File;

public class FileMessageQueue {
	
	public static final String LOCK_FILE = ".lock";
	public static final String MESSAGE_FILE = "messages";
	
	private String queueName;
	
	private File lockFile;
	private File messageFile;
	private File queueDir;
	
	public FileMessageQueue(String queueName) {
		this.queueName = queueName;
	}
	
	public File getQueueDir() {
		return queueDir;
	}
	
	public void setQueueDir(File queueDir) {
		this.queueDir = queueDir;
	}

	public String getQueueName() {
		return queueName;
	}
	
	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}
	
	public File getLockFile() {
		return lockFile;
	}
	
	public void setLockFile(File lockFile) {
		this.lockFile = lockFile;
	}
	
	public File getMessageFile() {
		return messageFile;
	}
	
	public void setMessageFile(File messageFile) {
		this.messageFile = messageFile;
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
		FileMessageQueue other = (FileMessageQueue) obj;
		if (queueName == null) {
			if (other.queueName != null)
				return false;
		} else if (!queueName.equals(other.queueName))
			return false;
		return true;
	}
}
