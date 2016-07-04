package org.aludratest.cloud.web.config;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.aludratest.cloud.app.CloudManagerApp;
import org.aludratest.cloud.config.ConfigException;
import org.aludratest.cloud.config.ConfigUtil;
import org.aludratest.cloud.config.Configurable;
import org.aludratest.cloud.config.MainPreferences;
import org.aludratest.cloud.config.MutablePreferences;
import org.aludratest.cloud.config.Preferences;
import org.aludratest.cloud.impl.app.CloudManagerApplicationHolder;
import org.aludratest.cloud.module.ResourceModule;
import org.aludratest.cloud.user.UserDatabase;
import org.aludratest.cloud.util.JSFUtil;

@ManagedBean
@ViewScoped
public class ConfigBean implements Serializable {

	private static final long serialVersionUID = 8328103311616234450L;

	private ConfigPreferences rootConfigPreferences;

	public ConfigBean() {
		// initialize root config preferences from stored preferences
		rootConfigPreferences = ConfigPreferences.createRootConfigPreferences(CloudManagerApplicationHolder.getInstance()
				.getRootPreferences());
	}

	public List<Map<String, Object>> getConfigIncludes() {
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		
		// add the "general" config section
		Map<String, Object> basic = new HashMap<String, Object>();
		basic.put("url", "/WEB-INF/includes/configSection-default.xhtml");
		basic.put("id", "basic");

		Preferences prefs = rootConfigPreferences.getChildNode("basic");
		basic.put("preferences", prefs);

		result.add(basic);
		
		// for each resource module, check if there is a config section available
		MutablePreferences modulesRoot = rootConfigPreferences.createChildNode("modules");

		for (ResourceModule module : CloudManagerApp.getInstance().getAllResourceModules()) {
			String moduleName = module.getResourceType().getName();
			URL url = JSFUtil.findModuleSpecificResource(FacesContext.getCurrentInstance(), moduleName, "configSection.xhtml");
			if (url != null) {
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("url", url.toString());
				map.put("preferences", modulesRoot.createChildNode(moduleName));
				map.put("id", moduleName);
				result.add(map);
			}
		}

		return result;
	}

	public List<SelectItem> getUserAuthenticationItems() {
		List<SelectItem> result = new ArrayList<SelectItem>();

		for (UserDatabase db : CloudManagerApp.getInstance().getUserDatabaseRegistry().getAllUserDatabases()) {
			result.add(new SelectItem(db.getSource()));
		}

		return result;
	}

	public void save() {
		Preferences originalRoot = CloudManagerApplicationHolder.getInstance().getRootPreferences();

		boolean valid = true;
		List<ResourceModule> modulesToUpdate = new ArrayList<ResourceModule>();

		ConfigPreferences modulesRoot = rootConfigPreferences.getChildNode("modules");
		
		for (ResourceModule module : CloudManagerApp.getInstance().getAllResourceModules()) {
			String moduleName = module.getResourceType().getName();
			URL url = JSFUtil.findModuleSpecificResource(FacesContext.getCurrentInstance(), moduleName, "configSection.xhtml");
			if (url != null && (module instanceof Configurable)) {
				Configurable configModule = (Configurable) module;

				// check if there is a CHANGE in the preferences node
				ConfigPreferences childNode = modulesRoot.getChildNode(moduleName);
				Preferences originalNode = originalRoot.getChildNode(moduleName);

				if (originalNode == null || ConfigUtil.differs(childNode, originalNode)) {
					// validate config; add Faces Messages if invalid
					try {
						configModule.validateConfiguration(childNode);
						modulesToUpdate.add(module);
					}
					catch (ConfigException ce) {
						valid = false;
						FacesContext.getCurrentInstance().addMessage(null,
								JSFUtil.createErrorMessage(module.getDisplayName() + ": " + ce.getMessage()));
					}
				}
			}
		}

		if (!valid) {
			return;
		}
		
		MainPreferences mainPrefs = CloudManagerApplicationHolder.getInstance().getRootPreferences();
		
		try {
			savePreferencesNode(rootConfigPreferences.getChildNode("basic"), mainPrefs.getOrCreateChildNode("basic"));

			for (ResourceModule module : modulesToUpdate) {
				savePreferencesNode(modulesRoot.getChildNode(module.getResourceType().getName()),
						mainPrefs.getOrCreateChildNode("modules").getOrCreateChildNode(module.getResourceType().getName()));
			}
			FacesContext.getCurrentInstance().addMessage(null,
					JSFUtil.createInfoMessage("The configuration has been saved successfully."));
		}
		catch (ConfigException e) {
			FacesContext.getCurrentInstance().addMessage(null, JSFUtil.createErrorMessage(e.getMessage()));
		}
	}

	private void savePreferencesNode(Preferences prefs, MainPreferences target) throws ConfigException {
		CloudManagerApp.getInstance().getConfigManager().applyConfig(prefs, target);
	}

	public ConfigPreferences getConfigPreferences() {
		return rootConfigPreferences;
	}

}
