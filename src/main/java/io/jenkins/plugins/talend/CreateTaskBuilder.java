package io.jenkins.plugins.talend;

import hudson.Launcher;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.Item;
import hudson.model.Result;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.RelativePath;
import hudson.Util;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.kohsuke.stapler.verb.POST;

import com.talend.tmc.dom.TaskNew;
import com.talend.tmc.dom.Trigger;
import com.talend.tmc.dom.Runtime;
import com.talend.tmc.dom.Artifact;
import com.talend.tmc.dom.RunConfig;
import com.talend.tmc.dom.Task;
import com.talend.tmc.services.TalendCloudRegion;
import com.talend.tmc.services.TalendCredentials;
import com.talend.tmc.services.TalendError;
import com.talend.tmc.services.TalendRestException;
import com.talend.tmc.services.artifacts.ArtifactService;
import com.talend.tmc.services.executables.ExecutableRunConfig;
import com.talend.tmc.services.executables.ExecutableTask;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import org.kohsuke.stapler.AncestorInPath;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import jenkins.model.GlobalConfiguration;
import jenkins.tasks.SimpleBuildStep;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;

@Extension
public class CreateTaskBuilder extends Builder implements SimpleBuildStep {
	@edu.umd.cs.findbugs.annotations.SuppressFBWarnings
	private static final Logger LOGGER = Logger.getLogger(GlobalConfiguration.class.getName());

    private String tEnvironment;
    private String tWorkspace;
    private String tArtifact;
    private String tRuntimeType;
    private String tRuntime;
    private boolean tIsjob = true;
	private String tParameters = "";
	private String tAutoUpgradable = "true";
	private String tOverrideWithDefaultParameters = "false";

    @DataBoundConstructor
    public CreateTaskBuilder() {
    	this.tArtifact = "";
    }	

    public String getEnvironment() {
        return tEnvironment;
    }

    @DataBoundSetter
    public void setEnvironment(String value) {
        this.tEnvironment = value;
    }

    public String getWorkspace() {
        return tWorkspace;
    }

    @DataBoundSetter
    public void setWorkspace(String value) {
        this.tWorkspace = value;
    }

    public String getArtifact() {
        return tArtifact;
    }

    @DataBoundSetter
    public void setRuntimeType(String value) {
        this.tRuntimeType = value;
    }

    public String getRuntimeType() {
        return tRuntimeType;
    }

    @DataBoundSetter
    public void setRuntime(String value) {
        this.tRuntime = value;
    }

    public boolean getIsjob() {
        return tIsjob;
    }

    @DataBoundSetter
    public void setIsjob(boolean value) {
        this.tIsjob = value;
    }

    public String getRuntime() {
        return tRuntime;
    }

    @DataBoundSetter
    public void setArtifact(String value) {
        this.tArtifact = value;
    }

    public String getParameters () {
        return this.tParameters;
    }

    @DataBoundSetter
    public void setParameters(String value) {
    	tParameters = value;
    }

    public boolean getAutoUpgradable () {
        return this.tAutoUpgradable.equals("true");
    }

    @DataBoundSetter
    public void setAutoUpgradable(boolean value) {
    	tAutoUpgradable = (value == true) ? "true" : "false";
    }

    public boolean getOverrideWithDefaultParameters () {
        return this.tOverrideWithDefaultParameters.equals("true");
    }

    @DataBoundSetter
    public void setOverrideWithDefaultParameters(boolean value) {
    	tOverrideWithDefaultParameters = (value == true) ? "true" : "false";
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
		listener.getLogger().println("*** CREATETASK ***");
		listener.getLogger().println("environment=" + tEnvironment);
		listener.getLogger().println("workspace=" + tWorkspace);
		listener.getLogger().println("artifactname=" + tArtifact);
        String id = "";

        try {
			TalendCredentials credentials = TalendLookupHelper.getTalendCredentials();
			TalendCloudRegion region = TalendLookupHelper.getTalendRegion();

            ExecutableTask executableTask = ExecutableTask.instance(credentials, region);
            ArtifactService artifactService = ArtifactService.instance(credentials, region);

    		String environmentId = TalendLookupHelper.getEnvironmentIdByName(tEnvironment);
            String workspaceId = TalendLookupHelper.getWorkspaceIdByName(environmentId, tWorkspace);

            Artifact[] artifacts = artifactService.getByName(tArtifact, workspaceId);
        	if (artifacts.length > 0) {
        		Artifact artifact = artifacts[0];
        		TaskNew newTask = new TaskNew();
        		newTask.setName(tArtifact);
        		newTask.setDescription("Jenkins created Task");
        		newTask.setWorkspaceId(workspaceId);
        		newTask.setEnvironmentId(environmentId);
        		
        		Map<String, String> artifactMap = new HashMap<>();
        		artifactMap.put("id", artifact.getId());
        		String[] versions = artifact.getVersions();
        		artifactMap.put("version", versions[0]);
        		newTask.setArtifact(artifactMap);

        		Map<String, String> AutoUpgradeInfoMap = new HashMap<>();
        		AutoUpgradeInfoMap.put("autoUpgradable", tAutoUpgradable);
        		AutoUpgradeInfoMap.put("overrideWithDefaultParameters", tOverrideWithDefaultParameters);
        		newTask.setAutoUpgradeInfo(AutoUpgradeInfoMap);

				String[] values = tParameters.split("\n");
				Map<String, String> parameters = new HashMap<>();
				for (int i = 0; i < values.length; i++) {
					if (!(values[i].indexOf("=") < 0) ) {
						String key =values[i].split("=")[0].trim();
						String value =values[i].split("=")[1].trim();
						if (key.length() > 0 && value.length() > 0) {
							parameters.put(key, value);
						}
					}
				}
				if (parameters.size() > 0) {
					newTask.setParameters(parameters);
				}
	            LOGGER.info("Going to create task :" + newTask.toString() );

				Task createdTask = executableTask.create(newTask);

        		id = createdTask.getId();
            	listener.getLogger().println("New Task has Id =" + id);
            	String engineId = "";
            	
				switch (tRuntimeType) {
			    case "CLOUD":  			engineId = TalendLookupHelper.getPipelineEngineIdByName(tEnvironment, tRuntime);
			         				break;
			    case "REMOTE_ENGINE":	engineId = TalendLookupHelper.getRemoteEngineIdByName(tEnvironment, tRuntime);
			         				break;
			    case "REMOTE_ENGINE_CLUSTER":	engineId = TalendLookupHelper.getClusterIdByName(tEnvironment, tRuntime);
									break;
			    case "CLOUD_EXCLUSIVE":	engineId = "";
									break;
			    default: engineId = "";
			}

//            	String engineId = TalendLookupHelper.getRemoteEngineIdByName(tEnvironment, tRuntime);
            	if (!engineId.isEmpty()) {
                	RunConfig runConfig = new RunConfig();
                	Trigger trigger = new Trigger();
                	trigger.setType("MANUAL");
                	runConfig.setTrigger(trigger);
                	Runtime runtime = new Runtime();
                	runtime.setId(engineId);
                	runtime.setType(tRuntimeType);
                	runConfig.setRuntime(runtime);
                	// This can only be set on Clusters
//	                	runConfig.setParallelExecutionAllowed("false");
                    ExecutableRunConfig executableRunConfig = ExecutableRunConfig.instance(credentials, region);
    	            LOGGER.info("Going to add RunConfig :" + executableRunConfig.toString() );

                    executableRunConfig.update("task", id, runConfig);
            	} else {
                	listener.getLogger().println("No Engine in Workspace available, skipping updating RunConfig");            		
            	}
            	
            	listener.getLogger().println("The TaskID is stored in Environment variable TALEND_NEW_TASK_ID ");
            	env.put("TALEND_NEW_TASK_ID", id);
        	} else if (artifacts.length == 0) {
            	throw new IOException("Artifact " + tArtifact + " Not Found");
        	}
    		listener.getLogger().println("*** CREATETASK ***");
            Thread.sleep(10);  // to include the InterruptedException
        } catch (RuntimeException ex){
        	throw ex;
	    } catch (InterruptedException ex){
	    	throw ex;
	    } catch (Exception e) {
        	throw new IOException(e.getMessage());
	    }
    }

    @Symbol("createTask")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
    	private int jsLastTaskCreator = 0;
    	
    	@edu.umd.cs.findbugs.annotations.SuppressFBWarnings
    	private static final Logger LOGGER = Logger.getLogger(GlobalConfiguration.class.getName());

        @POST
		public ListBoxModel doFillEnvironmentItems(@CheckForNull @AncestorInPath Item item) {
            ListBoxModel model = new ListBoxModel();
            if (item == null) { // no context
            	return model;
            }
            item.checkPermission(Item.CONFIGURE);
			return TalendLookupHelper.getEnvironmentList();
		}

        @POST
		public ListBoxModel doFillWorkspaceItems(@AncestorInPath Item item, @QueryParameter String environment) {
            ListBoxModel model = new ListBoxModel();
            if (item == null) { // no context
            	return model;
            }
            item.checkPermission(Item.CONFIGURE);
        	if (!environment.isEmpty()) {
        		return TalendLookupHelper.getWorkspaceList(environment);
        	}
        	return model;
		}
            	    	
		public ListBoxModel doFillRuntimeTypeItems(@QueryParameter String environment) {
            ListBoxModel model = new ListBoxModel();
            model.add("Cloud", "CLOUD");
            model.add("Remote Engine", "REMOTE_ENGINE");
            model.add("Cluster", "REMOTE_ENGINE_CLUSTER");
            model.add("Cloud Exclusive","CLOUD_EXCLUSIVE");
            return model;
		}

        @POST
		public ListBoxModel doFillRuntimeItems(@AncestorInPath Item item, @QueryParameter String environment, @QueryParameter String runtimeType) {
            ListBoxModel model = new ListBoxModel();
            if (item == null) { // no context
            	return model;
            }
            item.checkPermission(Item.CONFIGURE);
        	if (!environment.isEmpty()) {
				switch (runtimeType) {
				    case "CLOUD":  			model = TalendLookupHelper.getPipelineEngineList(environment);
				         				break;
				    case "REMOTE_ENGINE":	model = TalendLookupHelper.getRemoteEngineList(environment);
				         				break;
				    case "REMOTE_ENGINE_CLUSTER":	model = TalendLookupHelper.getClusterList(environment);
										break;
				    case "CLOUD_EXCLUSIVE":	model.add("Runtime Type Not Implemented", "NOT");
										break;
				    default: model.add("Runtime Type Not Implemented", "NOT");
				}
        	}
			return model; 
		}

        @POST
		public FormValidation doCheckArtifact(@AncestorInPath Item item, @QueryParameter String artifact)
                throws IOException, ServletException {
            if (item == null) { // no context
                return FormValidation.error("No context");
            }
            item.checkPermission(Item.CONFIGURE);
            if (artifact.trim().isEmpty())
                return FormValidation.error("Artifact name is missing");
			if (!artifact.matches("[a-zA-Z0-9_]+")) {
				return FormValidation.error("Artifact name may only contain characters, numbers and underscores.");
			}
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckParameters(@AncestorInPath Item item, @QueryParameter String parameters)
			throws IOException, ServletException {
            if (item == null) { // no context
                return FormValidation.error("No context");
            }
            item.checkPermission(Item.CONFIGURE);
            if (Util.fixEmptyAndTrim(parameters) == null) {
                return FormValidation.ok();
              }
			if (!parameters.isEmpty() && (parameters.indexOf("=") < 0)) {
				return FormValidation.warning("Invalid Parameters");
			}
			if (!parameters.isEmpty() && (parameters.indexOf("=") == 0)) {
				return FormValidation.warning("Invalid Parameters");
			}
			String[] values = parameters.split("\n");
			for (int i = 0; i < values.length; i++) {
				if (!(values[i].indexOf("=") < 0)) {
					String key =values[i].split("=")[0].trim();
					if (!key.matches("[a-zA-Z0-9_]+")) {
						return FormValidation.warning("Keys may only contain characters, numbers and underscores: '" + key + "'");
					}
				}
			}
			return FormValidation.ok();
        }

        @JavaScriptMethod
        public synchronized String createCreatorId() {
            return String.valueOf(jsLastTaskCreator++);
        }
        
        @Override
        public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.CreateTaskBuilder_DescriptorImpl_DisplayName();
        }

    }

}
