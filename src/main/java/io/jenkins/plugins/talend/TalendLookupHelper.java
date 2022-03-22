package io.jenkins.plugins.talend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.cxf.jaxrs.ext.search.client.SearchConditionBuilder;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

import com.talend.tmc.dom.Artifact;
import com.talend.tmc.dom.Cluster;
import com.talend.tmc.dom.Engine;
import com.talend.tmc.dom.Executable;
import com.talend.tmc.dom.PipelineEngine;
import com.talend.tmc.dom.Promotion;
import com.talend.tmc.dom.Workspace;
import com.talend.tmc.services.TalendBearerAuth;
import com.talend.tmc.services.TalendCloudRegion;
import com.talend.tmc.services.TalendCredentials;
import com.talend.tmc.services.TalendRestException;
import com.talend.tmc.services.artifacts.ArtifactService;
import com.talend.tmc.services.executables.ExecutablePlanService;
import com.talend.tmc.services.executables.ExecutablePromotionService;
import com.talend.tmc.services.executables.ExecutableTaskService;
import com.talend.tmc.services.runtime.ClusterService;
import com.talend.tmc.services.runtime.EngineService;
import com.talend.tmc.services.runtime.PipelineEngineService;
import com.talend.tmc.services.workspaces.WorkspaceService;

import hudson.util.ComboBoxModel;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;

// TODO: Auto-generated Javadoc
/**
 * The Class TalendLookupHelper.
 */
public class TalendLookupHelper {
	
	/** The Constant LOGGER. */
	@edu.umd.cs.findbugs.annotations.SuppressFBWarnings
	private static final Logger LOGGER = Logger.getLogger(GlobalConfiguration.class.getName());

	/**
	 * Gets the environment list.
	 *
	 * @return the environment list
	 */
	public static ListBoxModel getEnvironmentList() {
		ListBoxModel model = new ListBoxModel();

		TalendCredentials credentials = getTalendCredentials();
		TalendCloudRegion region = getTalendRegion();
		
		try {
			WorkspaceService workspaceService = WorkspaceService.instance(credentials, region);
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
		}
		return model;
	}
	
	/**
	 * Gets the environment id by name.
	 *
	 * @param environment the environment
	 * @return the environment id by name
	 */
	public static String getEnvironmentIdByName(String environment) {
		if (environment == null) {
			LOGGER.warning("environment is null");
			return "";
		}

		TalendCredentials credentials = getTalendCredentials();
		TalendCloudRegion region = getTalendRegion();

		String result = "";
		try {
			WorkspaceService workspaceService = WorkspaceService.instance(credentials, region);
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
	
	/**
	 * Gets the promotion list.
	 *
	 * @return the promotion list
	 */
	public static ListBoxModel getPromotionList() {
		ListBoxModel model = new ListBoxModel();

		TalendCredentials credentials = getTalendCredentials();
		TalendCloudRegion region = getTalendRegion();
		
		try {
			ExecutablePromotionService promotionService = ExecutablePromotionService.instance(credentials, region);
			Promotion[] promotions = promotionService.getByQuery(null);
			for (Promotion promotion: promotions) {
				model.add(promotion.getName(), promotion.getName());
			}
		} catch (TalendRestException | IOException ex) {
			LOGGER.warning(ex.getMessage());
		}

		return model;
	}
	
	/**
	 * Gets the promotion id by name.
	 *
	 * @param promotion the promotion
	 * @return the promotion id by name
	 */
	public static String getPromotionIdByName(String promotion) {
		TalendCredentials credentials = getTalendCredentials();
		TalendCloudRegion region = getTalendRegion();

		String result = "";
		try {
			ExecutablePromotionService promotionService = ExecutablePromotionService.instance(credentials, region);
			SearchConditionBuilder fiql = SearchConditionBuilder.instance("fiql");
			String query = fiql.is("name").equalTo(promotion).query();
			Promotion[] promotions = promotionService.getByQuery(query);
			if (promotions.length > 0) {
				result = promotions[0].getExecutable();
			}
		} catch (TalendRestException | IOException ex) {
			LOGGER.warning(ex.getMessage());
		}

		return result;		
	}

	/**
	 * Gets the workspace id by name.
	 *
	 * @param environment the environment
	 * @param workspace the workspace
	 * @return the workspace id by name
	 */
	public static String getWorkspaceIdByName(String environment, String workspace) {
		TalendCredentials credentials = getTalendCredentials();
		TalendCloudRegion region = getTalendRegion();

		String result = "";
		try {
			WorkspaceService workspaceService = WorkspaceService.instance(credentials, region);
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
	
	/**
	 * Gets the workspace list.
	 *
	 * @param environment the environment
	 * @return the workspace list
	 */
	public static ListBoxModel getWorkspaceList(String environment) {
        ListBoxModel model = new ListBoxModel();
		if (environment == null) {
			LOGGER.warning("environment is null");
			return model;
		}
		TalendCredentials credentials = getTalendCredentials();
		TalendCloudRegion region = getTalendRegion();
		

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
		}

		return model;
	}
	
	/**
	 * Gets the task list.
	 *
	 * @param environment the environment
	 * @param workspace the workspace
	 * @return the task list
	 */
	public static ComboBoxModel getTaskList(String environment, String workspace) {
		ComboBoxModel model = new ComboBoxModel();
		if (environment == null) {
			LOGGER.warning("environment is null");
			return model;
		}

		TalendCredentials credentials = getTalendCredentials();
		TalendCloudRegion region = getTalendRegion();

        try {
        	ExecutableTaskService executableTaskService = ExecutableTaskService.instance(credentials, region);

        	String environmentId = getEnvironmentIdByName(environment);
        	String workspaceId = getWorkspaceIdByName(environmentId, workspace);

			Executable[] Executables = executableTaskService
					.getByQuery("environmentId=" + environmentId + "&workspaceId=" + workspaceId);
			for (Executable exec : Executables) {
				model.add(exec.getName());
			}
		} catch (TalendRestException | IOException ex) {
			LOGGER.warning(ex.getMessage());
		}
        

		return model;
	}

	/**
	 * Gets the task id by name.
	 *
	 * @param environment the environment
	 * @param workspace the workspace
	 * @param task the task
	 * @return the task id by name
	 */
	public static String getTaskIdByName(String environment, String workspace, String task) {
		if (environment == null) {
			LOGGER.warning("environment is null");
			return "";
		}
		String result = "";
		
		TalendCredentials credentials = getTalendCredentials();
		TalendCloudRegion region = getTalendRegion();

        ExecutableTaskService executableTaskService = ExecutableTaskService.instance(credentials, region);

        try {
        	String environmentId = getEnvironmentIdByName(environment);
        	String workspaceId = getWorkspaceIdByName(environmentId, workspace);

			Executable[] executables = executableTaskService
					.getByQuery("environmentId=" + environmentId + "&workspaceId=" + workspaceId + "&name=" + task);
			if (executables.length > 0) {
				result = executables[0].getExecutable();
			}
		} catch (TalendRestException | IOException ex) {
			LOGGER.warning(ex.getMessage());
		}

		return result;
	}

	/**
	 * Gets the plan list.
	 *
	 * @param environment the environment
	 * @param workspace the workspace
	 * @return the plan list
	 */
	public static ListBoxModel getPlanList(String environment, String workspace) {
		ListBoxModel model = new ListBoxModel();
		if (environment == null) {
			LOGGER.warning("environment is null");
			return model;
		}

		TalendCredentials credentials = getTalendCredentials();
		TalendCloudRegion region = getTalendRegion();

        try {
        	ExecutablePlanService executablePlanService = ExecutablePlanService.instance(credentials, region);

        	String environmentId = getEnvironmentIdByName(environment);
        	String workspaceId = getWorkspaceIdByName(environmentId, workspace);

			Executable[] Executables = executablePlanService
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

	/**
	 * Gets the plan id by name.
	 *
	 * @param environment the environment
	 * @param workspace the workspace
	 * @param plan the plan
	 * @return the plan id by name
	 */
	public static String getPlanIdByName(String environment, String workspace, String plan) {
		if (environment == null) {
			LOGGER.warning("environment is null");
			return "";
		}
		String result = "";
		
		TalendCredentials credentials = getTalendCredentials();
		TalendCloudRegion region = getTalendRegion();

        try {
        	ExecutablePlanService executablePlanService = ExecutablePlanService.instance(credentials, region);

        	String environmentId = getEnvironmentIdByName(environment);
        	String workspaceId = getWorkspaceIdByName(environmentId, workspace);

			Executable[] executables = executablePlanService
					.getByQuery("environmentId=" + environmentId + "&workspaceId=" + workspaceId + "&name=" + plan);
			if (executables.length > 0) {
				result = executables[0].getExecutable();
			}
		} catch (TalendRestException | IOException ex) {
			LOGGER.warning(ex.getMessage());
		}

		return result;
	}

	/**
	 * Gets the artifact list.
	 *
	 * @param environment the environment
	 * @param workspace the workspace
	 * @return the artifact list
	 */
	public static ComboBoxModel getArtifactList(String environment, String workspace, String artifacttype) {
		ComboBoxModel model = new ComboBoxModel();
		if (environment == null) {
			LOGGER.warning("environment is null");
			return model;
		}

		TalendCredentials credentials = getTalendCredentials();
		TalendCloudRegion region = getTalendRegion();

        try {
        	ArtifactService artifactService = ArtifactService.instance(credentials, region);

        	String environmentId = getEnvironmentIdByName(environment);
        	String workspaceId = getWorkspaceIdByName(environmentId, workspace);

			Artifact[] artifacts = artifactService
					.getByQuery("environmentId=" + environmentId + "&workspaceId=" + workspaceId);
			for (Artifact artifact : artifacts) {
				if (artifacttype == null || artifacttype.isEmpty() || artifacttype.equals(artifact.getType().toUpperCase()))
				model.add(artifact.getName());
			}
		} catch (TalendRestException | IOException ex) {
			LOGGER.warning(ex.getMessage());
		}

		return model;
	}

	/**
	 * Gets the artifact id by name.
	 *
	 * @param environment the environment
	 * @param workspace the workspace
	 * @param artifact the artifact
	 * @return the artifact id by name
	 */
	public static String getArtifactIdByName(String environment, String workspace, String artifact) {
		if (environment == null) {
			LOGGER.warning("environment is null");
			return "";
		}
		String result = "";

		TalendCredentials credentials = getTalendCredentials();
		TalendCloudRegion region = getTalendRegion();

        try {
        	ArtifactService artifactService = ArtifactService.instance(credentials, region);

        	String environmentId = getEnvironmentIdByName(environment);
        	String workspaceId = getWorkspaceIdByName(environmentId, workspace);

			Artifact[] artifacts = artifactService
					.getByQuery("environmentId=" + environmentId + "&workspaceId=" + workspaceId + "&name=" + artifact);
			if (artifacts.length > 0) {
				result = artifacts[0].getId();
			}
		} catch (TalendRestException | IOException ex) {
			LOGGER.warning(ex.getMessage());
		}

		return result;
	}
	
	/**
	 * Gets the remote engine id by name.
	 *
	 * @param environment the environment
	 * @param runtime the runtime
	 * @return the remote engine id by name
	 */
	public static String getRemoteEngineIdByName(String environment, String runtime) {
		if (environment == null) {
			LOGGER.warning("environment is null");
			return "";
		}
		String result = "";

		TalendCredentials credentials = getTalendCredentials();
		TalendCloudRegion region = getTalendRegion();

    	try {
    		EngineService engineService = EngineService.instance(credentials, region);

    		String environmentId = getEnvironmentIdByName(environment);

    		SearchConditionBuilder fiql = SearchConditionBuilder.instance("fiql");
    	
    		String query = fiql.is("workspace.environment.id").equalTo(environmentId).and().is("name").equalTo(runtime).query();
	    	Engine[] engines = engineService.get(query);
			if (engines.length > 0) {
				result = engines[0].getId();
			}
		} catch (TalendRestException | IOException ex) {
			LOGGER.warning(ex.getMessage());
		}

		return result;
	}

	/**
	 * Gets the remote engine list.
	 *
	 * @param environment the environment
	 * @return the remote engine list
	 */
	static ListBoxModel getRemoteEngineList(String environment) {
		ListBoxModel model = new ListBoxModel();
		if (environment == null) {
			LOGGER.warning("environment is null");
			return model;
		}
		TalendCredentials credentials = getTalendCredentials();
		TalendCloudRegion region = getTalendRegion();
		
		String environmentId = getEnvironmentIdByName(environment);
		
    	try {
    		EngineService engineService = EngineService.instance(credentials, region);

    		SearchConditionBuilder fiql = SearchConditionBuilder.instance("fiql");
    	
    		String query = fiql.is("workspace.environment.id").equalTo(environmentId).and().is("status").equalTo("PAIRED").query();
	    	Engine[] engines = engineService.get(query);
			for (Engine engine : engines) {
				model.add(engine.getName(), engine.getName());
			}
		} catch (TalendRestException | IOException ex) {
			LOGGER.warning(ex.getMessage());
		}
    	return model;
	}

	/**
	 * Gets the cluster id by name.
	 *
	 * @param environment the environment
	 * @param runtime the runtime
	 * @return the cluster id by name
	 */
	public static String getClusterIdByName(String environment, String runtime) {
		if (environment == null) {
			LOGGER.warning("environment is null");
			return "";
		}
		String result = "";

		TalendCredentials credentials = getTalendCredentials();
		TalendCloudRegion region = getTalendRegion();

    	try {
    		ClusterService clusterService = ClusterService.instance(credentials, region);

    		String environmentId = getEnvironmentIdByName(environment);

    		SearchConditionBuilder fiql = SearchConditionBuilder.instance("fiql");
    	
    		String query = fiql.is("workspace.environment.id").equalTo(environmentId).and().is("name").equalTo(runtime).query();
	    	Cluster[] clusters = clusterService.get(query);
			if (clusters.length > 0) {
				result = clusters[0].getId();
			}
		} catch (TalendRestException | IOException ex) {
			LOGGER.warning(ex.getMessage());
		}

		return result;
	}

	/**
	 * Gets the cluster list.
	 *
	 * @param environment the environment
	 * @return the cluster list
	 */
	static ListBoxModel getClusterList(String environment) {
		ListBoxModel model = new ListBoxModel();
		if (environment == null) {
			LOGGER.warning("environment is null");
			return model;
		}
		TalendCredentials credentials = getTalendCredentials();
		TalendCloudRegion region = getTalendRegion();
		
		String environmentId = getEnvironmentIdByName(environment);
		
    	try {
    		ClusterService clusterService = ClusterService.instance(credentials, region);

    		SearchConditionBuilder fiql = SearchConditionBuilder.instance("fiql");
    	
    		String query = fiql.is("workspace.environment.id").equalTo(environmentId).and().is("status").equalTo("PAIRED").query();
	    	Cluster[] clusters = clusterService.get(query);
			for (Cluster cluster : clusters) {
				model.add(cluster.getName(), cluster.getName());
			}
		} catch (TalendRestException | IOException ex) {
			LOGGER.warning(ex.getMessage());
		}
    	return model;
	}

	
	/**
	 * Gets the pipeline engine id by name.
	 *
	 * @param environment the name of the environment
	 * @param runtime the name of the runtime
	 * @return the pipeline engine id by name
	 */
	public static String getPipelineEngineIdByName(String environment, String runtime) {
		if (environment == null) {
			LOGGER.warning("environment is null");
			return "";
		}
		String result = "";

		TalendCredentials credentials = getTalendCredentials();
		TalendCloudRegion region = getTalendRegion();

    	try {
    		PipelineEngineService pipelineEngineService = PipelineEngineService.instance(credentials, region);

    		String environmentId = getEnvironmentIdByName(environment);

    		SearchConditionBuilder fiql = SearchConditionBuilder.instance("fiql");
    	
    		String query = fiql.is("workspace.environment.id").equalTo(environmentId).and().is("name").equalTo(runtime).query();
    		PipelineEngine[] pipelineEngines = pipelineEngineService.get(query);
			if (pipelineEngines.length > 0) {
				result = pipelineEngines[0].getId();
			}
		} catch (TalendRestException | IOException ex) {
			LOGGER.warning(ex.getMessage());
		}

		return result;
	}

	/**
	 * Gets the pipeline engine list.
	 *
	 * @param environment the environment
	 * @return the pipeline engine list
	 */
	static ListBoxModel getPipelineEngineList(String environment) {
		ListBoxModel model = new ListBoxModel();
		if (environment == null) {
			LOGGER.warning("environment is null");
			return model;
		}
		TalendCredentials credentials = getTalendCredentials();
		TalendCloudRegion region = getTalendRegion();
		
		String environmentId = getEnvironmentIdByName(environment);
		
    	try {
    		PipelineEngineService pipelineEngineService = PipelineEngineService.instance(credentials, region);

    		SearchConditionBuilder fiql = SearchConditionBuilder.instance("fiql");
    	
    		String query = fiql.is("workspace.environment.id").equalTo(environmentId).and().is("status").equalTo("PAIRED").query();
    		PipelineEngine[] pipelineEngines = pipelineEngineService.get(query);
			for (PipelineEngine pipelineEngine : pipelineEngines) {
				model.add(pipelineEngine.getName(), pipelineEngine.getName());
			}
		} catch (TalendRestException | IOException ex) {
			LOGGER.warning(ex.getMessage());
		}
    	return model;
	}

	/**
	 * Gets the talend region.
	 *
	 * @return the talend region
	 */
	static TalendCloudRegion getTalendRegion() {
		String region = TalendConfiguration.get().getRegion();
	    return TalendCloudRegion.valueOf(region);
	}
	
	/**
	 * Gets the talend credentials.
	 *
	 * @return the talend credentials
	 */
	static TalendCredentials getTalendCredentials() {
		// TODO: die gracefully when there are no credentials yet 
		String api = ""; 
		String token = TalendConfiguration.get().getCredentialsid();
		if (token != null) {
		    StringCredentials stringCredentials = CredentialsHelper.lookupSystemCredentials(token);
		    if (stringCredentials != null) {
			    Secret mySecret = stringCredentials.getSecret();
			    api = mySecret.getPlainText();
		    }
		} else {
			LOGGER.warning("No Credential defined. Please create a Talend Connection.");
		}
        TalendCredentials credentials = new TalendBearerAuth(api);
        return credentials;
	}

}
