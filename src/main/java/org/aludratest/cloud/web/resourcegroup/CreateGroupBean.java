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

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.aludratest.cloud.app.CloudManagerApp;
import org.aludratest.cloud.config.ConfigException;
import org.aludratest.cloud.config.Configurable;
import org.aludratest.cloud.module.ResourceModule;
import org.aludratest.cloud.resourcegroup.ResourceGroupManagerAdmin;
import org.aludratest.cloud.util.JSFUtil;
import org.aludratest.cloud.web.app.ApplicationBean;

@ManagedBean(name = "createGroupBean")
@ViewScoped
public class CreateGroupBean implements Serializable {

	private static final long serialVersionUID = 4005446857189463390L;

	private String groupName;

	private String resourceTypeName;

	public CreateGroupBean() {
		resourceTypeName = CloudManagerApp.getInstance().getAllResourceModules().get(0).getResourceType().getName();
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String getResourceTypeName() {
		return resourceTypeName;
	}

	public void setResourceTypeName(String resourceTypeName) {
		this.resourceTypeName = resourceTypeName;
	}

	public String create() {
		FacesContext context = FacesContext.getCurrentInstance();

		ResourceModule module = CloudManagerApp.getInstance().getResourceModule(resourceTypeName);
		if (module == null || groupName == null) {
			return null;
		}

		try {
			ResourceGroupManagerAdmin admin = ((Configurable) CloudManagerApp.getInstance().getResourceGroupManager())
					.getAdminInterface(ResourceGroupManagerAdmin.class);
			int groupId = admin.createResourceGroup(module.getResourceType(), groupName);
			admin.commit();
			ApplicationBean bean = JSFUtil.getExpressionValue(context, "#{applicationBean}", ApplicationBean.class);
			bean.setSelectedGroupId(Integer.valueOf(groupId));
		}
		catch (IllegalArgumentException e) {
			context.addMessage(null,
					JSFUtil.createErrorMessage("A group with this name already exists. Please enter another name."));
			return null;
		}
		catch (ConfigException e) {
			context.addMessage(null, JSFUtil.createErrorMessage(e.getMessage()));
			return null;
		}

		return "editgroup";
	}
}
