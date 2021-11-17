package io.jenkins.plugins.talend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.cxf.jaxrs.ext.search.client.SearchConditionBuilder;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

import com.talend.tmc.dom.Engine;
import com.talend.tmc.dom.Executable;
import com.talend.tmc.dom.Workspace;
import com.talend.tmc.services.TalendBearerAuth;
import com.talend.tmc.services.TalendCloudRegion;
import com.talend.tmc.services.TalendCredentials;
import com.talend.tmc.services.TalendRestException;
import com.talend.tmc.services.executables.ExecutableTaskService;
import com.talend.tmc.services.runtime.EngineService;
import com.talend.tmc.services.workspaces.WorkspaceService;

import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;

public class TalendLookupHelper {
	@edu.umd.cs.findbugs.annotations.SuppressFBWarnings
	private static final Logger LOGGER = Logger.getLogger(GlobalConfiguration.class.getName());

	public static ListBoxModel getEnvironmentList() {
		ListBoxModel model = new ListBoxModel();

		TalendCredentials credentials = getTalendCredentials();
		TalendCloudRegion region = getTalendRegion();
		
		WorkspaceService workspaceService = WorkspaceService.instance(credentials, region);
		try {
			Workspace[] workSpaces = workspaceService.get();
			java.util.List<String> idList = new ArrayList<String>();
			for (Workspace space : workSpaces) {
				String id = space.getEnvironment().getId();
				java.util.List<String> idFilter = idList.stream().filter(x -> x.equals(id))
						.collect(Collectors.toList());
				if (idFilter.size() == 0) {
					model.add(space.getEnvironment().getName(), space.getEnvironment().getName());
					idList.add(id);
				}
			}
		} catch (TalendRestException | IOException ex) {
			LOGGER.warning(ex.getMessage());
			return null;
		}

		return model;
	}
	
	public static String getEnvironmentIdByName(String environment) {
		TalendCredentials credentials = getTalendCredentials();
		TalendCloudRegion region = getTalendRegion();

		String result = "";
		WorkspaceService workspaceService = WorkspaceService.instance(credentials, region);
		try {
			SearchConditionBuilder fiql = SearchConditionBuilder.instance("fiql");
			String query = fiql.is("environment.name").equalTo(environment).query();
			Workspace[] workSpaces = workspaceService.get(query);
			if (workSpaces.length > 0) {
				result = workSpaces[0].getEnvironment().getId();
			}
		} catch (TalendRestException | IOException ex) {
			LOGGER.warning(ex.getMessage());
		}

		return result;		
	}
	
	public static String getWorkspaceIdByName(String environment, String workspace) {
		TalendCredentials credentials = getTalendCredentials();
		TalendCloudRegion region = getTalendRegion();

		String result = "";
		WorkspaceService workspaceService = WorkspaceService.instance(credentials, region);
		try {
			SearchConditionBuilder fiql = SearchConditionBuilder.instance("fiql");
			String query = fiql.is("environment.id").equalTo(environment).and().is("name").equalTo(workspace).query();
			Workspace[] workSpaces = workspaceService.get(query);
			if (workSpaces.length > 0) {
				result = workSpaces[0].getId();
			}
		} catch (TalendRestException | IOException ex) {
			LOGGER.warning(ex.getMessage());
		}

		return result;
	}
	
	public static ListBoxModel getWorkspaceList(String environment) {
		if (environment == null) {
			LOGGER.warning("environment is null");
			return null;
		}
		TalendCredentials credentials = getTalendCredentials();
		TalendCloudRegion region = getTalendRegion();
		
        ListBoxModel model = new ListBoxModel();

		WorkspaceService workspaceService = WorkspaceService.instance(credentials, region);

		String environmentId = getEnvironmentIdByName(environment);
		
		try {
			SearchConditionBuilder fiql = SearchConditionBuilder.instance("fiql");
			String query = fiql.is("environment.id").equalTo(environmentId).query();
			Workspace[] workSpaces = workspaceService.get(query);
			java.util.List<String> idList = new ArrayList<String>();
			for (Workspace space : workSpaces) {
				String id = space.getId();
				String envId = space.getEnvironment().getId();
				java.util.List<String> idFilter = idList.stream().filter(x -> x.contains(envId))
						.collect(Collectors.toList());
				if (idFilter.size() == 0) {
					model.add(space.getName() + " - " + space.getOwner(), space.getName());
				}
				idList.add(id);
			}

		} catch (TalendRestException | IOException ex) {
			LOGGER.warning(ex.getMessage());
			return null;
		}

		return model;
	}
	
	public static ListBoxModel getTaskList(String environment, String workspace) {
		if (environment == null) {
			LOGGER.warning("environment is null");
			return null;
		}
		ListBoxModel model = new ListBoxModel();

		TalendCredentials credentials = getTalendCredentials();
		TalendCloudRegion region = getTalendRegion();

        ExecutableTaskService executableTaskService = ExecutableTaskService.instance(credentials, region);

		String environmentId = getEnvironmentIdByName(environment);
		String workspaceId = getWorkspaceIdByName(environmentId, workspace);

        try {
			Executable[] Executables = executableTaskService
					.getByQuery("environmentId=" + environmentId + "&workspaceId=" + workspaceId);
			for (Executable exec : Executables) {
				model.add(exec.getName(), exec.getName());
			}
		} catch (TalendRestException | IOException ex) {
			LOGGER.warning(ex.getMessage());
			return null;
		}

		return model;
	}

	public static String getRemoteEngineIdByName(String environment, String runtime) {
		TalendCredentials credentials = getTalendCredentials();
		TalendCloudRegion region = getTalendRegion();
		String result = "";
		EngineService engineService = EngineService.instance(credentials, region);

		String environmentId = getEnvironmentIdByName(environment);

		SearchConditionBuilder fiql = SearchConditionBuilder.instance("fiql");
    	
    	String query = fiql.is("workspace.environment.id").equalTo(environmentId).and().is("name").equalTo(runtime).query();
    	try {
	    	Engine[] engines = engineService.get(query);
			if (engines.length > 0) {
				result = engines[0].getId();
			}
		} catch (TalendRestException | IOException ex) {
			LOGGER.warning(ex.getMessage());
		}

		return result;
	}

	static ListBoxModel getRemoteEngineList(String environment) {
		if (environment == null) {
			LOGGER.warning("environment is null");
			return null;
		}
		ListBoxModel model = new ListBoxModel();
		String engineId = "";
		TalendCredentials credentials = getTalendCredentials();
		TalendCloudRegion region = getTalendRegion();
		
		String environmentId = getEnvironmentIdByName(environment);
		
		EngineService engineService = EngineService.instance(credentials, region);

		SearchConditionBuilder fiql = SearchConditionBuilder.instance("fiql");
    	
    	String query = fiql.is("workspace.environment.id").equalTo(environmentId).and().is("status").equalTo("PAIRED").query();
    	try {
	    	Engine[] engines = engineService.get(query);
			for (Engine engine : engines) {
				model.add(engine.getName(), engine.getName());
			}
		} catch (TalendRestException | IOException ex) {
			LOGGER.warning(ex.getMessage());
		}
    	return model;
	}

	static TalendCloudRegion getTalendRegion() {
		String region = TalendConfiguration.get().getRegion();
	    return TalendCloudRegion.valueOf(region);
	}
	
	static TalendCredentials getTalendCredentials() {
		String api = ""; 
		String token = TalendConfiguration.get().getCredentialsid();
		if (token != null) {
		    StringCredentials stringCredentials = CredentialsHelper.lookupSystemCredentials(token);
		    if (stringCredentials != null) {
			    Secret mySecret = stringCredentials.getSecret();
			    api = mySecret.getPlainText();
		    }
		}
        TalendCredentials credentials = new TalendBearerAuth(api);
        return credentials;
	}

}
