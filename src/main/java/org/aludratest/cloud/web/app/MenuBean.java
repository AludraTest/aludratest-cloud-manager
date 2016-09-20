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
package org.aludratest.cloud.web.app;

import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.aludratest.cloud.app.CloudManagerApp;
import org.aludratest.cloud.resourcegroup.ResourceGroup;
import org.aludratest.cloud.resourcegroup.ResourceGroupManager;
import org.primefaces.model.menu.DefaultMenuItem;
import org.primefaces.model.menu.DefaultMenuModel;
import org.primefaces.model.menu.MenuModel;

@ManagedBean(name = "modulesBean")
@RequestScoped
public class MenuBean implements Serializable {
	
	private static final long serialVersionUID = -2522180291147217027L;

	private DefaultMenuModel menuModel;

	public MenuModel getMenuModel() {
		if (menuModel == null) {
			menuModel = new DefaultMenuModel();
			
			// home
			DefaultMenuItem homeItem = new DefaultMenuItem("Home", "ui-icon-home");
			homeItem.setCommand("index");
			homeItem.setImmediate(true);
			homeItem.setProcess("@this");
			menuModel.addElement(homeItem);
			
			// all GROUPs
			ResourceGroupManager groupManager = CloudManagerApp.getInstance().getResourceGroupManager();
			for (int groupId : groupManager.getAllResourceGroupIds()) {
				ResourceGroup group = groupManager.getResourceGroup(groupId);
				DefaultMenuItem moduleItem = new DefaultMenuItem(groupManager.getResourceGroupName(groupId), "menu-icon-"
						+ group.getResourceType().getName());
				moduleItem.setUrl("/resourceGroup.jsf?groupId=" + groupId);
				moduleItem.setImmediate(true);
				moduleItem.setProcess("@this");
				menuModel.addElement(moduleItem);
			}
			
			// settings
			DefaultMenuItem settingsItem = new DefaultMenuItem("Settings", "ui-icon-gear");
			settingsItem.setCommand("config");
			settingsItem.setImmediate(true);
			settingsItem.setAjax(false);
			menuModel.addElement(settingsItem);

			// users
			DefaultMenuItem userItem = new DefaultMenuItem("User Administration", "ui-icon-person");
			userItem.setCommand("useradmin");
			userItem.setImmediate(true);
			userItem.setAjax(false);
			menuModel.addElement(userItem);

			// SQL reports
			DefaultMenuItem sqlItem = new DefaultMenuItem("SQL Reports", "ui-icon-script");
			sqlItem.setCommand("sql");
			sqlItem.setImmediate(true);
			sqlItem.setAjax(false);
			menuModel.addElement(sqlItem);

			// Resource usage report
			DefaultMenuItem usageItem = new DefaultMenuItem("Resources Usage", "ui-icon-clock");
			usageItem.setCommand("resourcesUsage");
			usageItem.setImmediate(true);
			usageItem.setAjax(false);
			menuModel.addElement(usageItem);

			// "Add new group"
			DefaultMenuItem addGroupItem = new DefaultMenuItem("Add Resources Group", "ui-icon-plus");
			addGroupItem.setCommand("addgroup");
			addGroupItem.setImmediate(true);
			addGroupItem.setAjax(false);
			menuModel.addElement(addGroupItem);

		}

		return menuModel;
	}

}
