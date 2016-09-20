/*
 * Copyright (C) 2015 Hamburg Sud and the contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aludratest.cloud.web.resourcegroup;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.aludratest.cloud.app.CloudManagerApp;
import org.aludratest.cloud.config.ConfigException;
import org.aludratest.cloud.config.Configurable;
import org.aludratest.cloud.config.admin.ConfigurationAdmin;
import org.aludratest.cloud.resourcegroup.AuthorizingResourceGroup;
import org.aludratest.cloud.resourcegroup.AuthorizingResourceGroupAdmin;
import org.aludratest.cloud.resourcegroup.ResourceGroup;
import org.aludratest.cloud.resourcegroup.ResourceGroupManager;
import org.aludratest.cloud.resourcegroup.ResourceGroupManagerAdmin;
import org.aludratest.cloud.resourcegroup.ResourceGroupNature;
import org.aludratest.cloud.user.StoreException;
import org.aludratest.cloud.user.User;
import org.aludratest.cloud.util.JSFUtil;
import org.primefaces.model.DualListModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ManagedBean(name = "editGroupBean")
@ViewScoped
public class EditGroupBean implements Serializable {

	private static final long serialVersionUID = -4494782452419565202L;

	private static final Logger LOG = LoggerFactory.getLogger(EditGroupBean.class);

	private Integer groupId;

	private String groupName;

	private String resourceTypeDisplayName;

	private DualListModel<User> selectedUsersModel;

	private boolean limitingUsers;

	private transient List<SelectItem> availableNatures;

	private String natureToAdd;

	public void setGroupId(Integer groupId) {
		this.groupId = groupId;
		availableNatures = null;

		if (groupId != null) {
			ResourceGroup group = getGroup();
			groupName = getGroupManager().getResourceGroupName(groupId.intValue());
			resourceTypeDisplayName = group == null ? null : CloudManagerApp.getInstance()
					.getResourceModule(group.getResourceType()).getDisplayName();

			if (group instanceof AuthorizingResourceGroup) {
				limitingUsers = ((AuthorizingResourceGroup) group).isLimitingUsers();
			}
		}
		else {
			groupName = null;
			resourceTypeDisplayName = null;
			limitingUsers = false;
		}
	}

	public Integer getGroupId() {
		return groupId;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String getGroupName() {
		return groupName;
	}

	public String getResourceTypeDisplayName() {
		return resourceTypeDisplayName;
	}

	public void setNatureToAdd(String natureToAdd) {
		this.natureToAdd = natureToAdd;
	}

	public String getNatureToAdd() {
		return natureToAdd;
	}

	public boolean isLimitUsersSupported() {
		ResourceGroup group = getGroup();
		if (!(group instanceof AuthorizingResourceGroup)) {
			return false;
		}
		
		AuthorizingResourceGroupAdmin admin = getGroupAdmin(groupId.intValue(), AuthorizingResourceGroupAdmin.class);
		return admin != null;
	}

	public boolean isLimitingUsers() {
		return limitingUsers;
	}

	public void setLimitingUsers(boolean value) {
		this.limitingUsers = value;
	}

	public DualListModel<User> getSelectedUsersModel() {
		if (!isLimitUsersSupported()) {
			return null;
		}

		if (selectedUsersModel == null) {
			List<User> availableUsers = new ArrayList<User>();
			List<User> selectedUsers = new ArrayList<User>();

			AuthorizingResourceGroup group = (AuthorizingResourceGroup) getGroup();

			try {
				Iterator<User> iter = CloudManagerApp.getInstance().getSelectedUserDatabase()
						.getAllUsers(null);
				while (iter.hasNext()) {
					User user = iter.next();
					if (group.isUserAuthorized(user)) {
						selectedUsers.add(user);
					}
					else {
						availableUsers.add(user);
					}
				}

				selectedUsersModel = new DualListModel<User>(availableUsers, selectedUsers);
			}
			catch (StoreException e) {
				FacesContext.getCurrentInstance().addMessage(null, JSFUtil.createErrorMessage("Could not load user list"));
				selectedUsersModel = new DualListModel<User>();
			}
		}
		return selectedUsersModel;
	}

	public void setSelectedUsersModel(DualListModel<User> selectedUsersModel) {
		this.selectedUsersModel = selectedUsersModel;
	}

	private ResourceGroupManager getGroupManager() {
		return CloudManagerApp.getInstance().getResourceGroupManager();
	}

	private ResourceGroup getGroup() {
		getGroupId();
		if (groupId == null) {
			return null;
		}

		return getGroupManager().getResourceGroup(groupId.intValue());
	}

	private <T extends ConfigurationAdmin> T getGroupAdmin(int groupId, Class<T> adminIface) {
		ResourceGroupManager manager = CloudManagerApp.getInstance().getResourceGroupManager();
		ResourceGroup group = manager.getResourceGroup(groupId);
		if (group == null || !(group instanceof Configurable)) {
			return null;
		}

		return ((Configurable) group).getAdminInterface(adminIface);
	}

	private ResourceGroupManagerAdmin getGroupManagerAdmin() {
		ResourceGroupManager manager = CloudManagerApp.getInstance().getResourceGroupManager();
		return (manager instanceof Configurable) ? ((Configurable) manager).getAdminInterface(ResourceGroupManagerAdmin.class)
				: null;
	}

	public List<SelectItem> getAvailableNatures() {
		if (availableNatures != null) {
			return availableNatures;
		}
		if (groupId == null) {
			return null;
		}

		List<ResourceGroupNature> natures = CloudManagerApp.getInstance().getResourceGroupManager()
				.getAvailableNaturesFor(groupId.intValue());

		List<String> activeNatures = getActiveNatures();

		availableNatures = new ArrayList<SelectItem>();
		for (ResourceGroupNature nature : natures) {
			if (!activeNatures.contains(nature.getName())) {
				availableNatures.add(new SelectItem(nature.getName(), nature.getDisplayName()));
			}
		}

		return availableNatures;
	}

	public void addNature() {
		ResourceGroupManagerAdmin admin = getGroupManagerAdmin();
		if (admin != null && natureToAdd != null && groupId != null) {
			try {
				admin.addGroupNature(groupId.intValue(), natureToAdd);
				admin.commit();
				natureToAdd = null;
				availableNatures = null; // refresh cache
				FacesContext.getCurrentInstance().addMessage(null,
						JSFUtil.createInfoMessage("The nature has been added to the group."));
			}
			catch (ConfigException e) {
				FacesContext.getCurrentInstance().addMessage(null,
						JSFUtil.createErrorMessage("Could not add nature to group: " + e.getMessage()));
			}
		}
	}

	public void removeNature(String nature) {
		ResourceGroupManagerAdmin admin = getGroupManagerAdmin();
		if (admin != null && groupId != null) {
			try {
				admin.removeGroupNature(groupId.intValue(), nature);
				admin.commit();
				FacesContext.getCurrentInstance().addMessage(null,
						JSFUtil.createInfoMessage("The nature has been removed from the group."));
				availableNatures = null; // clear cache
			}
			catch (ConfigException e) {
				FacesContext.getCurrentInstance().addMessage(null,
						JSFUtil.createErrorMessage("Could not remove nature from group: " + e.getMessage()));
			}
		}
	}

	public List<String> getActiveNatures() {
		ResourceGroupManagerAdmin admin = getGroupManagerAdmin();
		if (admin == null || groupId == null) {
			return null;
		}

		return admin.getGroupNatures(groupId.intValue());
	}

	public String getNatureSettingsXhtml(String nature) {
		ResourceGroup group = getGroup();
		if (group == null) {
			return null;
		}

		URL url = JSFUtil.findModuleSpecificResource(FacesContext.getCurrentInstance(), nature, "natureSettings.xhtml");
		if (url != null) {
			return url.toString();
		}

		return null;
	}

	public String getNatureName(String nature) {
		if (groupId == null) {
			return null;
		}

		for (ResourceGroupNature rgn : CloudManagerApp.getInstance().getResourceGroupManager()
				.getAvailableNaturesFor(groupId.intValue())) {
			if (nature.equals(rgn.getName())) {
				return rgn.getDisplayName();
			}
		}

		return null;
	}

	public void rename() {
		if (groupId == null) {
			return;
		}

		try {
			ResourceGroupManagerAdmin admin = getGroupManagerAdmin();
			admin.renameResourceGroup(groupId.intValue(), groupName);
			admin.commit();
			FacesContext.getCurrentInstance().addMessage(null, JSFUtil.createInfoMessage("The group name has been updated."));
		}
		catch (ConfigException e) {
			FacesContext.getCurrentInstance().addMessage(null, JSFUtil.createErrorMessage(e.getMessage()));
		}
	}

	public String getGroupSettingsXhtml() {
		ResourceGroup group = getGroup();
		if (group == null) {
			return null;
		}

		URL url = JSFUtil.findModuleSpecificResource(FacesContext.getCurrentInstance(), group.getResourceType().getName(),
				"groupSettings.xhtml");
		if (url != null) {
			return url.toString();
		}

		return null;
	}

	public void save() {
		// transfer user configuration to admin
		if (isLimitUsersSupported() && selectedUsersModel != null) {

			AuthorizingResourceGroupAdmin admin = getGroupAdmin(groupId.intValue(), AuthorizingResourceGroupAdmin.class);

			for (User u : selectedUsersModel.getSource()) {
				admin.removeAuthorizedUser(u);
			}
			for (User u : selectedUsersModel.getTarget()) {
				admin.addAuthorizedUser(u);
			}

			admin.setLimitingUsers(limitingUsers);
			try {
				admin.commit();
				FacesContext.getCurrentInstance().addMessage(null,
						JSFUtil.createInfoMessage("The group configuration has been saved."));
			}
			catch (ConfigException e) {
				FacesContext.getCurrentInstance().addMessage(null,
						JSFUtil.createErrorMessage("Could not save configuration: " + e.getMessage()));
			}
		}
	}

	public String deleteGroup() {
		if (groupId == null) {
			return "index";
		}

		ResourceGroupManagerAdmin admin = getGroupManagerAdmin();
		try {
			admin.deleteResourceGroup(groupId.intValue());
			admin.commit();
		}
		catch (ConfigException e) {
			// should not occur.
			LOG.error("Exception when deleting resource group", e);
		}
		groupId = null;

		return "index";
	}

	private String ajaxText;

	public String getAjaxText() {
		return ajaxText;
	}

	public void ajaxTest() {
		try {
			Thread.sleep(12000);
		}
		catch (InterruptedException e) {
			return;
		}
		ajaxText = "Here is Ajax";
	}

}
