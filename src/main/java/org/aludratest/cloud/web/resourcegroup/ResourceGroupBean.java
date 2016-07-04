package org.aludratest.cloud.web.resourcegroup;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.aludratest.cloud.app.CloudManagerApp;
import org.aludratest.cloud.manager.ManagedResourceQuery;
import org.aludratest.cloud.resource.ResourceState;
import org.aludratest.cloud.resource.ResourceStateHolder;
import org.aludratest.cloud.resourcegroup.ResourceGroup;
import org.aludratest.cloud.util.JSFUtil;

@ManagedBean(name = "resourceGroupBean")
@ViewScoped
public class ResourceGroupBean implements Serializable {

	private static final long serialVersionUID = 7769303460902877901L;

	private Integer groupId;

	public ResourceGroup getGroup() {
		if (groupId == null) {
			return null;
		}
		return CloudManagerApp.getInstance().getResourceGroupManager().getResourceGroup(groupId.intValue());
	}

	public String getGroupName() {
		if (groupId == null) {
			return null;
		}

		return CloudManagerApp.getInstance().getResourceGroupManager().getResourceGroupName(groupId.intValue());
	}

	public List<ResourceStateHolder> getAllResources() {
		ResourceGroup group = getGroup();
		if (group == null) {
			return null;
		}

		List<ManagedResourceQuery> runningQueries = new ArrayList<ManagedResourceQuery>(CloudManagerApp.getInstance()
				.getResourceManager().getAllRunningQueries());

		List<ResourceStateHolder> ls = new ArrayList<ResourceStateHolder>();
		for (ResourceStateHolder rsh : group.getResourceCollection()) {
			// try to get a current user and job name for the resource
			String user = null;
			String jobName = null;
			if (rsh.getState() == ResourceState.IN_USE) {
				for (ManagedResourceQuery query : runningQueries) {
					if (rsh.equals(query.getReceivedResource())) {
						user = query.getRequest().getRequestingUser().getName();
						jobName = query.getRequest().getJobName();
					}
				}
			}

			ls.add(new ResourceInfo(rsh, user, jobName));
		}

		return ls;
	}

	public void setGroupId(Integer groupId) {
		this.groupId = groupId;
	}

	public Integer getGroupId() {
		return groupId;
	}

	public String getResourceBlockXhtml() {
		ResourceGroup group = getGroup();
		if (group == null) {
			return null;
		}

		URL url = JSFUtil.findModuleSpecificResource(FacesContext.getCurrentInstance(), group.getResourceType().getName(),
				"resourceBlock.xhtml");
		if (url != null) {
			return url.toString();
		}
		return "/WEB-INF/includes/resourceBlock-default.xhtml";
	}

	public String getResourceGroupSummaryXhtml() {
		ResourceGroup group = getGroup();
		if (group == null) {
			return null;
		}

		URL url = JSFUtil.findModuleSpecificResource(FacesContext.getCurrentInstance(), group.getResourceType().getName(),
				"resourceGroupSummary.xhtml");
		if (url != null) {
			return url.toString();
		}
		return "/WEB-INF/includes/resourceGroupSummary-default.xhtml";
	}

	/**
	 * Public only to allow web page to access methods.
	 * 
	 * @author falbrech
	 * 
	 */
	public static class ResourceInfo implements ResourceStateHolder {

		private ResourceStateHolder resource;
		private String userName;
		private String jobName;

		private ResourceInfo(ResourceStateHolder resource, String userName, String jobName) {
			this.resource = resource;
			this.userName = userName;
			this.jobName = jobName;
		}

		@Override
		public ResourceState getState() {
			return resource.getState();
		}

		public String getUserName() {
			return userName;
		}

		public String getJobName() {
			return jobName;
		}

		@Override
		public String toString() {
			return resource.toString();
		}
	}

}
