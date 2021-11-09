package io.jenkins.plugins.talend;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Item;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.GlobalConfiguration;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.talend.tmc.dom.Workspace;
import com.talend.tmc.services.TalendBearerAuth;
import com.talend.tmc.services.TalendCloudRegion;
import com.talend.tmc.services.TalendCredentials;
import com.talend.tmc.services.TalendError;
import com.talend.tmc.services.TalendRestException;
import com.talend.tmc.services.workspaces.WorkspaceService;

/**
 * Example of Jenkins global configuration.
 */
@Extension
public class TalendConfiguration extends GlobalConfiguration {
	private static final Logger LOGGER = Logger.getLogger(GlobalConfiguration.class.getName());
    /** @return the singleton instance */
    public static TalendConfiguration get() {
        return ExtensionList.lookupSingleton(TalendConfiguration.class);
    }

    private String lcredentialsid;
    private String region;

    public TalendConfiguration() {
        // When Jenkins is restarted, load any saved configuration from disk.
        load();
    }

    public String getCredentialsid() {
        return this.lcredentialsid;
    }

    @DataBoundSetter
    public void setCredentialsid(String value) {
        this.lcredentialsid = value;
        save();
    }

    public String getRegion() {
        return this.region;
    }

    @DataBoundSetter
    public void setRegion(String region) {
        this.region = region;
        save();
    }

    public ListBoxModel doFillCredentialsidItems(
        @AncestorInPath final Item item,
        @QueryParameter final String credentialsid) {
        return CredentialsHelper.doFillCredentialsIdItems(item, credentialsid);
    }

    public FormValidation doCheckCredentialsid(
        @AncestorInPath final Item item,
        @QueryParameter final String credentialsid) {
        return CredentialsHelper.doCheckFillCredentialsId(item, credentialsid);
    }
    
    public FormValidation doCheckRegion(@QueryParameter String value) {
        if (StringUtils.isEmpty(value)) {
            return FormValidation.warning("Please specify a Region.");
        }
        return FormValidation.ok();
    }
    
	public FormValidation doTestConnection(@AncestorInPath final Item item, 
			@QueryParameter("credentialsid") final String credentialsid, 
			@QueryParameter("region") final String region)
			throws IOException, ServletException {

		/*
		 * TODO: This only works once, because the static method .instance caches the connection parameters
		 * 
		 */
		Optional<StringCredentials> optStringCredentials = CredentialsHelper.findCredentials(item, credentialsid);

		TalendCredentials credentials = null;
		WorkspaceService workspaceService = null;
		try {
			StringCredentials stringCredentials = optStringCredentials.get();
			String api = stringCredentials.getSecret().getPlainText();
			credentials = new TalendBearerAuth(api);
			workspaceService = WorkspaceService.instance(credentials, TalendCloudRegion.valueOf(region));

			Workspace[] spaces = workspaceService.get("");
		} catch (TalendRestException | IOException err) {
			return FormValidation.error("Connection error : " + err.toString());
		} finally {
			credentials = null;
			workspaceService = null;
			optStringCredentials = null;
		}
		return FormValidation.ok("Connected to Talend Cloud succesfully");
	}
}