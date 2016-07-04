package org.aludratest.cloud.web.jsf;

import java.util.Iterator;

import javax.faces.FacesException;
import javax.faces.application.ViewExpiredException;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.FacesContext;
import javax.faces.event.ExceptionQueuedEvent;
import javax.servlet.http.HttpServletRequest;

import org.primefaces.extensions.component.ajaxerrorhandler.AjaxExceptionHandler;

public class CloudManagerExceptionHandler extends AjaxExceptionHandler {

	public CloudManagerExceptionHandler(ExceptionHandler wrapped) {
		super(wrapped);
	}

	@Override
	public void handle() throws FacesException {
		FacesContext context = FacesContext.getCurrentInstance();

		// if AJAX request of /resourceModule.xhtml and view is expired, send redirect
		HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

		if (request.getRequestURI() != null && request.getRequestURI().endsWith("/resourceGroup.jsf")) {
			if (context.getPartialViewContext() != null && context.getPartialViewContext().isAjaxRequest()) {
				Iterable<ExceptionQueuedEvent> exceptionQueuedEvents = getUnhandledExceptionQueuedEvents();
				if (exceptionQueuedEvents != null && exceptionQueuedEvents.iterator() != null) {
					Iterator<ExceptionQueuedEvent> unhandledExceptionQueuedEvents = getUnhandledExceptionQueuedEvents()
							.iterator();

					if (unhandledExceptionQueuedEvents.hasNext()) {
						Throwable exception = unhandledExceptionQueuedEvents.next().getContext().getException();
						if (exception instanceof ViewExpiredException) {
							handleViewExpiredException(context, request);
							unhandledExceptionQueuedEvents.remove();

							while (unhandledExceptionQueuedEvents.hasNext()) {
								// Any remaining unhandled exceptions are not interesting. First fix the first.
								unhandledExceptionQueuedEvents.next();
								unhandledExceptionQueuedEvents.remove();
							}
							return;
						}
					}
				}
			}
		}

		super.handle();
	}

	private void handleViewExpiredException(FacesContext context, HttpServletRequest request) {
		try {
			String url = request.getContextPath() + "/index.jsf";
			String redirectUrl = context.getExternalContext().encodeRedirectURL(url, null);
			context.getExternalContext().redirect(redirectUrl);
		}
		catch (Exception e) {
		}
	}

}
