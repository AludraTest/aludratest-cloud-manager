package org.aludratest.cloud.web.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.aludratest.cloud.app.CloudManagerApp;
import org.aludratest.cloud.module.ResourceModule;
import org.aludratest.cloud.user.StoreException;
import org.aludratest.cloud.user.User;
import org.aludratest.cloud.util.JSFUtil;

@ManagedBean(name = "applicationBean")
@SessionScoped
public class ApplicationBean implements Serializable {

	private static final long serialVersionUID = 5695944150661971769L;

	private Integer selectedGroupId;

	private List<String> moduleNames;

	public ApplicationBean() {
	}

	public CloudManagerApp getApplication() {
		return CloudManagerApp.getInstance();
	}

	public void open(ActionEvent event) {
		// determine group ID from component
		String sGroupId = (String) JSFUtil.getParamValue(event.getComponent(), "groupId");
		if (sGroupId == null) {
			return;
		}

		selectedGroupId = Integer.valueOf(sGroupId);
	}

	public Integer getSelectedGroupId() {
		return selectedGroupId;
	}

	public void setSelectedGroupId(Integer selectedGroupId) {
		this.selectedGroupId = selectedGroupId;
	}

	public List<String> getModuleNames() {
		if (moduleNames == null) {
			moduleNames = new ArrayList<String>();
			for (ResourceModule module : CloudManagerApp.getInstance().getAllResourceModules()) {
				moduleNames.add(module.getResourceType().getName());
			}
		}

		return moduleNames;
	}

	public List<SelectItem> getModuleItems() {
		List<SelectItem> result = new ArrayList<SelectItem>();
		for (ResourceModule module : CloudManagerApp.getInstance().getAllResourceModules()) {
			result.add(new SelectItem(module.getResourceType().getName(), module.getDisplayName()));
		}
		return result;
	}

	public String getVersionText() {
		InputStream in = ApplicationBean.class.getClassLoader().getResourceAsStream("acm-version.txt");
		if (in != null) {
			byte[] data = new byte[300];
			try {
				int read = in.read(data);
				if (read > 0) {
					return new String(data, 0, read, "UTF-8");
				}
			}
			catch (IOException e) {
				return null;
			}
		}
		return null;
	}

	private static Converter userConverter = new Converter() {
		@Override
		public String getAsString(FacesContext context, UIComponent component, Object value) throws ConverterException {
			if (!(value instanceof User)) {
				return null;
			}

			return ((User) value).getName();
		}

		@Override
		public Object getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
			// search for user in current user database
			try {
				return CloudManagerApp.getInstance().getSelectedUserDatabase().findUser(value);
			}
			catch (StoreException e) {
				throw new ConverterException("Cannot find user in user database");
			}
		}
	};

	public Converter getUserConverter() {
		return userConverter;
	}

}
