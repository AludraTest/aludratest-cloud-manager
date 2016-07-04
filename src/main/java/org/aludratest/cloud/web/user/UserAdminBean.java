package org.aludratest.cloud.web.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.aludratest.cloud.app.CloudManagerApp;
import org.aludratest.cloud.impl.auth.SimpleResourceTypeAuthorization;
import org.aludratest.cloud.impl.user.SimpleResourceTypeAuthorizationConfig;
import org.aludratest.cloud.module.ResourceModule;
import org.aludratest.cloud.resource.ResourceType;
import org.aludratest.cloud.resource.user.ResourceTypeAuthorization;
import org.aludratest.cloud.resource.user.ResourceTypeAuthorizationConfig;
import org.aludratest.cloud.resource.user.ResourceTypeAuthorizationStore;
import org.aludratest.cloud.user.StoreException;
import org.aludratest.cloud.user.User;
import org.aludratest.cloud.user.UserDatabase;
import org.aludratest.cloud.util.JSFUtil;
import org.aludratest.cloud.web.util.MonitoringMap;
import org.primefaces.extensions.component.masterdetail.SelectLevelEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ManagedBean(name = "userAdminBean")
@ViewScoped
public class UserAdminBean implements Serializable {

	private static final long serialVersionUID = 935734987924256064L;

	private final static Logger LOG = LoggerFactory.getLogger(UserAdminBean.class);

	private final static char[] PASSWORD_CHARS = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'k', 'm', 'n', 'p', 'q', 'r', 's',
			't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R',
			'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '2', '3', '4', '5', '6', '7', '8', '9' };

	private final static int NEW_PASSWORD_LENGTH = 8;

	private transient List<EditUser> users;

	private transient List<SelectItem> availableLanguages;

	private EditUser newUser = createEmptyUser();

	public List<EditUser> getUsers() {
		UserDatabase db = CloudManagerApp.getInstance().getSelectedUserDatabase();
		if (db == null) {
			return null;
		}

		if (users == null) {
			users = new ArrayList<EditUser>();
			// load users
			try {
				Iterator<User> iter = db.getAllUsers(null);
				while (iter.hasNext()) {
					users.add(new EditUser(iter.next()));
				}
			}
			catch (StoreException se) {
				LOG.error("Could not load user list", se);
				FacesContext.getCurrentInstance().addMessage(null,
						JSFUtil.createErrorMessage("Could not load user list: " + se.getMessage()));
			}

			Collections.sort(users);
		}
		return users;
	}

	public boolean isReadOnly() {
		UserDatabase db = CloudManagerApp.getInstance().getSelectedUserDatabase();
		return db == null || db.isReadOnly();
	}

	public boolean isAttributeSupported(String attributeKey) {
		UserDatabase db = CloudManagerApp.getInstance().getSelectedUserDatabase();
		return db != null && db.supportsUserAttribute(attributeKey);
	}

	public EditUser getNewUser() {
		return newUser;
	}

	public List<SelectItem> getAvailableLanguages() {
		if (availableLanguages == null) {
			availableLanguages = new ArrayList<SelectItem>();
			for (String lang : Locale.getISOLanguages()) {
				Locale l = new Locale(lang);
				availableLanguages.add(new SelectItem(lang, l.getDisplayLanguage(Locale.ENGLISH) + " (" + lang + ")"));
			}
			Collections.sort(availableLanguages, new Comparator<SelectItem>() {
				@Override
				public int compare(SelectItem o1, SelectItem o2) {
					return o1.getLabel().compareToIgnoreCase(o2.getLabel());
				}
			});
		}

		return availableLanguages;
	}

	public List<SelectItem> getResourceTypeItems() {
		List<SelectItem> result = new ArrayList<SelectItem>();

		for (ResourceModule module : CloudManagerApp.getInstance().getAllResourceModules()) {
			result.add(new SelectItem(module.getResourceType().getName(), module.getDisplayName()));
		}

		return result;
	}

	public List<SelectItem> getNiceLevelItems() {
		List<SelectItem> result = new ArrayList<SelectItem>();

		for (int i = -19; i < 21; i++) {
			result.add(new SelectItem(Integer.valueOf(i), "" + i));
		}

		return result;
	}

	public int handleNavigation(SelectLevelEvent event) {
		// if any error message is present, do not navigate
		boolean error = false;
		List<FacesMessage> messages = FacesContext.getCurrentInstance().getMessageList(null);
		for (FacesMessage msg : messages) {
			if (msg.getSeverity() == FacesMessage.SEVERITY_ERROR) {
				error = true;
				break;
			}
		}

		return error ? event.getCurrentLevel() : event.getNewLevel();
	}

	public void reset() {
		users = null;
		newUser = createEmptyUser();
	}

	public void save(EditUser user) {
		UserDatabase db = CloudManagerApp.getInstance().getSelectedUserDatabase();
		if (user == null || db.isReadOnly()) {
			return;
		}

		// validate resource authorizations here
		Set<String> resourceTypes = new HashSet<String>();
		for (ResourceAuthorizationEntry entry : user.getResourceAuthorizations()) {
			if (resourceTypes.contains(entry.getResourceType())) {
				FacesContext.getCurrentInstance().addMessage(
						null,
						JSFUtil.createErrorMessage("The resource type " + entry.getResourceType()
								+ " must only be assigned once."));
				return;
			}
			resourceTypes.add(entry.getResourceType());
		}

		try {
			User dbUser;
			boolean includePwd = false;

			// create user first, if new user
			if (user == newUser) {
				try {
					dbUser = db.create(user.getName());
				}
				catch (IllegalArgumentException iae) {
					// name already existing
					FacesContext.getCurrentInstance().addMessage(null,
							JSFUtil.createErrorMessage("The user name does already exist."));
					return;
				}
				newUser = createEmptyUser();
				includePwd = true;
			}
			else {
				dbUser = db.findUser(user.getName());
			}

			if (user.newPassword != null) {
				db.changePassword(dbUser, user.newPassword);
				FacesContext.getCurrentInstance().addMessage(null,
						JSFUtil.createInfoMessage("The user password has been changed"
								+ (includePwd ? " to " + user.newPassword : ".")));
			}
			// attribute update
			for (String key : user.attributes.getRemovedKeys()) {
				db.modifyUserAttribute(dbUser, key, null);
			}
			for (String key : user.attributes.getAddedKeys()) {
				db.modifyUserAttribute(dbUser, key, user.attributes.get(key));
			}
			for (String key : user.attributes.getChangedKeys()) {
				db.modifyUserAttribute(dbUser, key, user.attributes.get(key));
			}

			// authorization update
			ResourceTypeAuthorizationStore store = CloudManagerApp.getInstance().getResourceTypeAuthorizationStore();

			for (ResourceModule module : CloudManagerApp.getInstance().getAllResourceModules()) {
				try {
					ResourceTypeAuthorizationConfig config = store.loadResourceTypeAuthorizations(module.getResourceType());
					if (config == null) {
						config = new SimpleResourceTypeAuthorizationConfig();
					}
					SimpleResourceTypeAuthorizationConfig mutableConfig = new SimpleResourceTypeAuthorizationConfig(config);

					ResourceAuthorizationEntry entry = user.getResourceAuthorization(module.getResourceType());
					boolean update = false;

					// if user does not contain authorization for type, but is configured, remove
					if (entry == null && mutableConfig.getConfiguredUsers().contains(dbUser)) {
						mutableConfig.removeUser(dbUser);
						update = true;
					}
					else if (entry != null) {
						if (!mutableConfig.getConfiguredUsers().contains(dbUser)) {
							mutableConfig.addUser(dbUser, entry.toResourceTypeAuthorization());
						}
						else {
							mutableConfig.editUserAuthorization(dbUser, entry.toResourceTypeAuthorization());
						}
						update = true;
					}
					if (update) {
						store.saveResourceTypeAuthorizations(module.getResourceType(), mutableConfig);
					}
				}
				catch (StoreException e) {
					LOG.error("Could not update resource type authorizations", e);
				}
			}

			FacesContext.getCurrentInstance().addMessage(null,
					JSFUtil.createInfoMessage("The user details have been updated successfully."));

			// reload users from database
			users = null;
		}
		catch (StoreException se) {
			LOG.error("Exception when saving user details", se);
			FacesContext.getCurrentInstance().addMessage(null,
					JSFUtil.createErrorMessage("Exception when saving user details: " + se.getMessage()));
		}

	}

	public void delete(EditUser user) {
		UserDatabase db = CloudManagerApp.getInstance().getSelectedUserDatabase();
		try {
			User dbUser = db.findUser(user.getName());
			if (dbUser != null) {
				db.delete(dbUser);
				FacesContext.getCurrentInstance().addMessage(null, JSFUtil.createInfoMessage("The user has been deleted."));
			}
		}
		catch (StoreException e) {
			LOG.error("Could not delete user", e);
			FacesContext.getCurrentInstance().addMessage(null,
					JSFUtil.createErrorMessage("The user could not be deleted: " + e.getMessage()));
		}
		users = null;
	}

	public void resetPassword(EditUser user) {
		user.setNewPassword(generatePassword());
		save(user);
	}

	private EditUser createEmptyUser() {
		return new EditUser("newuser", generatePassword());
	}

	private String generatePassword() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < NEW_PASSWORD_LENGTH; i++) {
			sb.append(PASSWORD_CHARS[(int) (Math.random() * PASSWORD_CHARS.length)]);
		}

		return sb.toString();
	}

	public static class EditUser implements Serializable, Comparable<EditUser> {

		private static final long serialVersionUID = 4116302615986762529L;

		private String name;

		private MonitoringMap<String, String> attributes = new MonitoringMap<String, String>();

		private String newPassword;

		private List<ResourceAuthorizationEntry> resourceAuthorizations = new ArrayList<ResourceAuthorizationEntry>();

		public EditUser(String name, String newPassword) {
			this.name = name;
			this.newPassword = newPassword;
		}

		public EditUser(User user) {
			name = user.getName();
			String email = user.getUserAttribute(User.USER_ATTRIBUTE_EMAIL);
			String language = user.getUserAttribute(User.USER_ATTRIBUTE_LANGUAGE);
			if (email != null) {
				attributes.put(User.USER_ATTRIBUTE_EMAIL, email);
			}
			if (language != null) {
				attributes.put(User.USER_ATTRIBUTE_LANGUAGE, language);
			}
			attributes.resetMonitor();

			// copy resource authorizations
			ResourceTypeAuthorizationStore store = CloudManagerApp.getInstance().getResourceTypeAuthorizationStore();
			for (ResourceModule module : CloudManagerApp.getInstance().getAllResourceModules()) {
				try {
					ResourceTypeAuthorizationConfig config = store.loadResourceTypeAuthorizations(module.getResourceType());
					if (config != null) {
						ResourceTypeAuthorization auth = config.getResourceTypeAuthorizationForUser(user);
						if (auth != null) {
							resourceAuthorizations.add(new ResourceAuthorizationEntry(module.getResourceType().getName(), auth));
						}
					}
				}
				catch (StoreException e) {
					LOG.error("Could not load resource type authorizations", e);
				}
			}
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getNewPassword() {
			return newPassword;
		}

		public void setNewPassword(String newPassword) {
			this.newPassword = newPassword;
		}

		public Map<String, String> getAttributes() {
			return attributes;
		}

		public List<ResourceAuthorizationEntry> getResourceAuthorizations() {
			return resourceAuthorizations;
		}

		public void setResourceAuthorizations(List<ResourceAuthorizationEntry> resourceAuthorizations) {
			this.resourceAuthorizations = resourceAuthorizations;
		}

		public ResourceAuthorizationEntry getResourceAuthorization(ResourceType resourceType) {
			for (ResourceAuthorizationEntry entry : resourceAuthorizations) {
				if (entry.resourceType.equals(resourceType.getName())) {
					return entry;
				}
			}
			return null;
		}

		public void removeAuthorization(ResourceAuthorizationEntry auth) {
			resourceAuthorizations.remove(auth);
		}

		public void addAuthorization() {
			String initialType;
			if (!resourceAuthorizations.isEmpty()) {
				initialType = resourceAuthorizations.get(resourceAuthorizations.size() - 1).resourceType;
			}
			else {
				initialType = CloudManagerApp.getInstance().getAllResourceModules().get(0)
						.getResourceType()
						.getName();
			}
			resourceAuthorizations.add(new ResourceAuthorizationEntry(initialType, new SimpleResourceTypeAuthorization(5, 0)));
		}

		@Override
		public int compareTo(EditUser o) {
			return name.compareToIgnoreCase(o.name);
		}
	}

	public static class ResourceAuthorizationEntry implements Serializable {

		private static final long serialVersionUID = 2408865279526728002L;

		private String resourceType;

		private int maxResources;

		private int niceLevel;

		public ResourceAuthorizationEntry(String resourceType, ResourceTypeAuthorization auth) {
			this.resourceType = resourceType;
			this.maxResources = auth.getMaxResources();
			this.niceLevel = auth.getNiceLevel();
		}

		public ResourceTypeAuthorization toResourceTypeAuthorization() {
			return new SimpleResourceTypeAuthorization(maxResources, niceLevel);
		}

		public String getResourceType() {
			return resourceType;
		}

		public void setResourceType(String resourceType) {
			this.resourceType = resourceType;
		}

		public int getMaxResources() {
			return maxResources;
		}

		public void setMaxResources(int maxResources) {
			this.maxResources = maxResources;
		}

		public int getNiceLevel() {
			return niceLevel;
		}

		public void setNiceLevel(int niceLevel) {
			this.niceLevel = niceLevel;
		}

		@Override
		public int hashCode() {
			return resourceType.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (obj == this) {
				return true;
			}
			if (obj.getClass() != getClass()) {
				return false;
			}
			ResourceAuthorizationEntry entry = (ResourceAuthorizationEntry) obj;
			return entry.maxResources == maxResources && entry.niceLevel == niceLevel && entry.resourceType.equals(resourceType);
		}

	}
}
