package org.aludratest.cloud.web.jsf;

import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerFactory;

public class CloudManagerExceptionHandlerFactory extends ExceptionHandlerFactory {

	private ExceptionHandlerFactory wrapped;

	/**
	 * Construct a new full CloudManagerExceptionHandlerFactory around the given wrapped factory.
	 * 
	 * @param wrapped
	 *            The wrapped factory.
	 */
	public CloudManagerExceptionHandlerFactory(final ExceptionHandlerFactory wrapped) {
		this.wrapped = wrapped;
	}

	/** Returns a new instance {@link CloudManagerExceptionHandler}. */
	@Override
	public ExceptionHandler getExceptionHandler() {
		// TODO can this instance be cached?
		return new CloudManagerExceptionHandler(wrapped.getExceptionHandler());
	}

	/** Returns the wrapped factory. */
	@Override
	public ExceptionHandlerFactory getWrapped() {
		return wrapped;
	}
}
