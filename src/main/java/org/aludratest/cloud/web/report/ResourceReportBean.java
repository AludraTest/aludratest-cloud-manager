package org.aludratest.cloud.web.report;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.aludratest.cloud.app.CloudManagerApp;
import org.aludratest.cloud.module.ResourceModule;
import org.aludratest.cloud.resource.ResourceType;
import org.aludratest.cloud.util.JSFUtil;
import org.joda.time.DateTime;
import org.slf4j.LoggerFactory;

@ViewScoped
@ManagedBean(name = "resourceReportBean")
public class ResourceReportBean {

	private Date reportStartDate;

	private Date reportEndDate;

	private String moduleName;

	private String cachedChartData;

	public ResourceReportBean() {
		reportEndDate = new Date();
		reportStartDate = new Date(reportEndDate.getTime() - 1000l * 60 * 60 * 24 * 7);
		moduleName = CloudManagerApp.getInstance().getAllResourceModules().get(0).getResourceType().getName();
	}

	public String getResourcesUsageData() {
		if (cachedChartData == null) {
			// find resource type
			ResourceType resourceType = getResourceType();
			if (resourceType == null) {
				return null;
			}

			try {
				cachedChartData = ResourceReportUtil.getResourceUsageData(resourceType, new DateTime(reportStartDate.getTime()),
						new DateTime(reportEndDate.getTime()), ResourceReportUtil.DEFAULT_TIME_EQUALITY_TOLERANCE_MS).toString();
			}
			catch (SQLException e) {
				FacesContext.getCurrentInstance().addMessage(null,
						JSFUtil.createErrorMessage("A database exception occurred when loading the report data"));
				LoggerFactory.getLogger(ResourceReportBean.class).error("A database exception has occurred", e);
			}
		}

		return cachedChartData;
	}

	public List<SelectItem> getModuleNames() {
		List<SelectItem> result = new ArrayList<SelectItem>();
		for (ResourceModule module : CloudManagerApp.getInstance().getAllResourceModules()) {
			result.add(new SelectItem(module.getResourceType().getName(), module.getDisplayName()));
		}
		return result;
	}

	public void updateChart() {
		cachedChartData = null;
	}

	public Date getReportStartDate() {
		return reportStartDate;
	}

	public void setReportStartDate(Date reportStartDate) {
		this.reportStartDate = reportStartDate;
	}

	public Date getReportEndDate() {
		return reportEndDate;
	}

	public void setReportEndDate(Date reportEndDate) {
		this.reportEndDate = reportEndDate;
	}

	public String getModuleName() {
		return moduleName;
	}

	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}

	private ResourceType getResourceType() {
		ResourceModule module = CloudManagerApp.getInstance().getResourceModule(moduleName);
		return module == null ? null : module.getResourceType();
	}
}
