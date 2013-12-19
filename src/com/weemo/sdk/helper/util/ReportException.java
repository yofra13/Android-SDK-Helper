package com.weemo.sdk.helper.util;

/*
 * Exception without stack trace, used for manual error reporting.
 */
public class ReportException extends Exception {

	private static final long serialVersionUID = 1L;

	public ReportException() {
		super("NO EXCEPTION, this is a report");
	}

	@Override
	public Throwable fillInStackTrace() {
		return this;
	}
}
